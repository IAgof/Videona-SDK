package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Watermark;
import com.videonasocialmedia.videonamediaframework.model.media.track.Track;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.AudioTrimmer;
import com.videonasocialmedia.videonamediaframework.muxer.IntermediateFileException;
import com.videonasocialmedia.videonamediaframework.muxer.Trimmer;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;


import com.googlecode.mp4parser.authoring.Movie;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Juan Javier Cabanas
 * @author Verónica Lago Fominaya
 */
public class VMCompositionExportSessionImpl implements VMCompositionExportSession {
    private static final String LOG_TAG = VMCompositionExportSessionImpl.class.getCanonicalName();
    private static final int MAX_SECONDS_WAITING_FOR_TEMP_FILES = 600;
    private static final int MAX_NUM_TRIES_TO_EXPORT_VIDEO = 3;
    private final String outputFilesDirectory;
    private final String outputAudioMixedFile;
    private String tempAudioPath;

    private ExportListener exportListener;
    private final VMComposition vmComposition;
    protected Trimmer audioTrimmer;
    protected Appender appender;
    private String tempExportFilePath;
    private String tempExportFileWatermark;
    private String intermediatesTempAudioFadeDirectory;
    private final MediaTranscoder mediaTranscoder;
    protected TranscoderHelper transcoderHelper;
    private String finalVideoExportedFilePath;

    public VMCompositionExportSessionImpl(
            VMComposition vmComposition, String outputFilesDirectory, String tempFilesDirectory, 
            String intermediatesTempAudioFadeDirectory, ExportListener exportListener) {
        // TODO(jliarte): 29/04/17 should move the parameters to export method to have them defined
        // by the interface?
        this.exportListener = exportListener;
        this.vmComposition = vmComposition;
        this.outputFilesDirectory = outputFilesDirectory;
        this.tempAudioPath = tempFilesDirectory;
        this.intermediatesTempAudioFadeDirectory = intermediatesTempAudioFadeDirectory;
        FileUtils.createDirectory(tempAudioPath);
        outputAudioMixedFile = tempFilesDirectory + File.separator
                + Constants.MIXED_AUDIO_FILE_NAME;
        finalVideoExportedFilePath = outputFilesDirectory + getNewExportedVideoFileName();
        audioTrimmer = new AudioTrimmer();
        appender = new Appender();
        this.mediaTranscoder = MediaTranscoder.getInstance();
        this.transcoderHelper = new TranscoderHelper(mediaTranscoder);
    }

    @Override
    public void exportAsyncronously() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                export();
            }
        }).start();
    }

    @Override
    public void export() {
        Log.d(LOG_TAG, "export, waiting for finish temporal files generation ");
        try {
            // TODO:(alvaro.martinez) 24/03/17 Add ListenableFuture AllAsList and Future isDone properties
            waitForVideoTempFilesFinished();

            exportListener.onExportProgress("Joining the videos", EXPORT_STAGE_JOIN_VIDEOS);
            ArrayList<String> videoTrimmedPaths = createVideoPathList(getMediasFromComposition());
            Log.d(LOG_TAG, "export, appending temporal files");
            Movie result = createMovieFromComposition(videoTrimmedPaths);
            // TODO(jliarte): 5/10/17 raising exception if null result flattens this method
            if (result != null) {
                tempExportFilePath = outputFilesDirectory + File.separator + "V_Appended.mp4";
                saveFinalVideo(result, tempExportFilePath);
                applyWatermark();
                mixAudio(getMediasAndVolumesToMixFromProjectTracks(tempExportFilePath),
                    tempExportFilePath);
            } else {
                exportListener.onExportError("Unable to generate appended video!");
            }
        } catch (IOException exportIOError) {
            Log.e(LOG_TAG, "Catched " +  exportIOError.getClass().getName() + " while exporting, " +
                    "message: " + exportIOError.getMessage());
            exportListener.onExportError(String.valueOf(exportIOError.getMessage()));
        } catch (IntermediateFileException | ExecutionException | InterruptedException
                | NullPointerException exportError) {
            Log.e(LOG_TAG, "Catched " +  exportError.getClass().getName() + " while exporting",
                    exportError);
            exportListener.onExportError(String.valueOf(exportError));
        }
    }

    @Override
    public void cancel() {
        exportListener.onCancelExport();
    }

    private void applyWatermark() throws IOException {
        if (vmComposition.hasWatermark()) {
            Log.d(LOG_TAG, "About to apply watermark!");
            String tempFileAppended = tempExportFilePath;
            applyWatermarkToVideoAndWaitForFinish(tempExportFilePath);
            FileUtils.removeFile(tempFileAppended);
        }
    }

    private void applyWatermarkToVideoAndWaitForFinish(String tempExportFilePath) throws IOException {
        if (vmComposition.hasWatermark()) {
          Log.d(LOG_TAG, "export, adding watermark to video appended");
          // TODO:(alvaro.martinez) 27/02/17 implement addWatermarkToGeneratedVideo feature
            ListenableFuture watermarkingJob = addWatermark(vmComposition.getWatermark(),
                tempExportFilePath);
            try {
                watermarkingJob.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.d(LOG_TAG, "Error applying watermark!");
                e.printStackTrace();
                exportListener.onExportError("Error applying watermark");
            }
        }
    }

    private List<Media> getMediasAndVolumesToMixFromProjectTracks(
            String exportedVideoAppendedPath) {
        List<Media> mediaList = new ArrayList<>();
        if (vmComposition.hasVideos()) {
            Media video = getMediaItemToMix(
                    new Video(exportedVideoAppendedPath, Video.DEFAULT_VOLUME),
                    vmComposition.getMediaTrack());
            if (video.getVolume() > 0.00f) {
                mediaList.add(video);
            }
        }
        // (jliarte): 4/10/17 made a copy of music and voice over objects to not alter its original volume!
        if (vmComposition.hasMusic()) {
            Media music = getMediaItemToMix(new Music(vmComposition.getMusic()),
                    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACK_MUSIC));
            if(music.getVolume()>0f) {
                mediaList.add(music);
            }
        }
        if (vmComposition.hasVoiceOver()) {
            Media voiceOver = getMediaItemToMix(new Music(vmComposition.getVoiceOver()),
                    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACK_VOICE_OVER));
            if(voiceOver.getVolume()>0f) {
                mediaList.add(voiceOver);
            }
        }
        return mediaList;
    }

    @NonNull
    private Media getMediaItemToMix(Media media, Track track) {
        if (track.isMuted()) {
            media.setVolume(0f);
        } else {
            media.setVolume(track.getVolume());
        }
        return media;
    }

    private ArrayList<String> createVideoPathList(LinkedList<Media> medias) {
        ArrayList <String> result = new ArrayList<>();
        for (Media media : medias) {
            Video video = (Video) media;
            if (video.isEdited()) {
                result.add(video.getTempPath());
            } else {
                result.add(video.getMediaPath());
            }
        }
        return result;
    }

    private LinkedList<Media> getMediasFromComposition() {
        LinkedList<Media> medias = vmComposition.getMediaTrack().getItems();
        return medias;
    }

//    // TODO(jliarte): 17/11/16 check if this code is still relevant
//    private ArrayList<String> trimVideosWithMuxer(LinkedList<Media> medias) {
//        final File tempDir = new File(tempVideoExportedPath);
//        if (!tempDir.exists())
//            tempDir.mkdirs();
//        ArrayList<String> videoTrimmedPaths = new ArrayList<>();
//        Trimmer trimmer;
//        Movie movie;
//        int index = 0;
//        do {
//            try {
//                String videoTrimmedTempPath = tempVideoExportedPath
//                        + File.separator + "video_trimmed_" + index + ".mp4";
//                int startTime = medias.get(index).getStartTime();
//                int endTime = medias.get(index).getStopTime();
//                int editedFileDuration = medias.get(index).getStopTime()
//                        - medias.get(index).getStartTime();
//                int originalFileDuration = ( (Video) medias.get(index) ).getFileDuration();
//                if (editedFileDuration < originalFileDuration) {
//                    trimmer = new VideoTrimmer();
//                    movie = trimmer.trim(medias.get(index).getMediaPath(), startTime, endTime);
//                    Utils.createFile(movie, videoTrimmedTempPath);
//                    videoTrimmedPaths.add(videoTrimmedTempPath);
//                } else {
//                    videoTrimmedPaths.add(medias.get(index).getMediaPath());
//                }
//            } catch (IOException | NullPointerException e) {
//                trimCorrect = false;
//                videoTrimmedPaths = null;
//                exportListener.onExportError(String.valueOf(e));
//            }
//            index++;
//        } while (trimCorrect && medias.size() > index);
//
//        return videoTrimmedPaths;
//    }

    protected Movie createMovieFromComposition(final ArrayList<String> videoTranscodedPaths)
            throws IOException, IntermediateFileException, ExecutionException,
            InterruptedException {
        Movie movie = null;
        try {
            movie = appender.appendVideos(videoTranscodedPaths, true);
        } catch (final IntermediateFileException intermediateFileError) {
            Log.d(LOG_TAG, "Catched intermediate files error");
            intermediateFileError.printStackTrace();
            final int failedVideoIndex = intermediateFileError.getVideoIndex();
            Video videoToUpdate = (Video) vmComposition.getMediaTrack().getItems()
                    .get(failedVideoIndex);
            videoToUpdate.resetNumTriesToExportVideo();
            if (videoToUpdate.getNumTriesToExportVideo() <= MAX_NUM_TRIES_TO_EXPORT_VIDEO) {
                regenerateIntermediateFor(failedVideoIndex,
                        intermediateFileError.getMediaPath(),
                        new TranscoderHelperListener() {
                            @Override
                            public void onSuccessTranscoding(Video video) {
                                video.notifyChanges();
                                videoTranscodedPaths.set(failedVideoIndex, video.getTempPath());
                            }

                            @Override
                            public void onErrorTranscoding(Video video, String message) {
                                video.increaseNumTriesToExportVideo();
                                Log.d(LOG_TAG, "error updating intermediate for video "
                                        + video.getMediaPath());
                            }
                        });
                waitForVideoTempFilesFinished();
                return createMovieFromComposition(videoTranscodedPaths);
            } else {
                throw intermediateFileError;
            }
        }
        return movie;
    }

    private void regenerateIntermediateFor(int videoIndex, String mediaPath,
                                           TranscoderHelperListener transcoderHelperListener)
            throws IOException {
        Video videoToEdit = (Video) vmComposition.getMediaTrack().getItems().get(videoIndex);
        Drawable drawableFadeTransition = vmComposition.getDrawableFadeTransitionVideo();
        boolean isVideoFadeTransitionActivated = vmComposition.isVideoFadeTransitionActivated();
        boolean isAudioFadeTransitionActivated = vmComposition.isAudioFadeTransitionActivated();
        VideonaFormat videonaFormat = vmComposition.getVideoFormat();
        transcoderHelper.updateIntermediateFile(drawableFadeTransition,
                isVideoFadeTransitionActivated, isAudioFadeTransitionActivated, videoToEdit,
                videonaFormat, intermediatesTempAudioFadeDirectory);
    }

    protected void saveFinalVideo(Movie result, String outputFilePath) throws IOException {
        exportListener.onExportProgress("Writing video to disk", EXPORT_STAGE_WRITE_VIDEO_TO_DISK);

        long start = System.currentTimeMillis();
        Utils.createFile(result, outputFilePath);
        long spent = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "saveFinalVideo WRITING VIDEO FILE - time spent in millis: " + spent);
    }

    protected double getMovieDuration(Movie movie) {
        double movieDuration = movie.getTracks().get(0).getDuration();
        double timeScale = movie.getTimescale();
        movieDuration = movieDuration / timeScale * 1000;
        return movieDuration;
    }


//    // TODO(jliarte): 19/12/16 what happens when there is more than a path? is that really needed now?
//    protected Movie addAudioList(Movie movie, ArrayList<String> audioPaths, double movieDuration) {
//        ArrayList<Movie> audioList = new ArrayList<>();
//        List<Track> audioTracks = new LinkedList<>();
//
//        // TODO change this for do while
//        for (String audio : audioPaths) {
//            try {
//                audioList.add(audioTrimmer.trim(audio, 0, movieDuration));
//            } catch (IOException | NullPointerException e) {
//                exportListener.onExportError(String.valueOf(e));
//            }
//        }
//
//        for (Movie m : audioList) {
//            for (Track t : m.getTracks()) {
//                if (t.getHandler().equals("soun")) {
//                    audioTracks.add(t);
//                }
//            }
//        }
//
//        if (audioTracks.size() > 0) {
//            try {
//                movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
//            } catch (IOException | NullPointerException e) {
//                exportListener.onExportError(String.valueOf(e));
//                // TODO se debe continuar sin música o lo paro??
//            }
//        }
//
//        return movie;
//    }

//    protected Movie addAudio(Movie movie, String audioPath, double movieDuration)
//            throws IOException {
//        Movie audioMovie = audioTrimmer.trim(audioPath, 0, movieDuration);
//        List<Track> audioTracks = extractAudioTracks(audioMovie);
//        if (audioTracks.size() > 0) {
//            exportListener.onExportProgress("Adding audio tracks", EXPORT_STAGE_ADD_AUDIO_TRACKS);
//            movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
//        }
//        return movie;
//    }

//    // TODO(jliarte): 20/12/16 similar methods on VideoAudioSwapper
//    @NonNull
//    private List<Track> extractAudioTracks(Movie audioMovie) {
//        List<Track> audioTracks = new LinkedList<>();
//        for (Track t : audioMovie.getTracks()) {
//            if (t.getHandler().equals("soun")) {
//                audioTracks.add(t);
//            }
//        }
//        return audioTracks;
//    }

    private void waitForVideoTempFilesFinished() throws ExecutionException, InterruptedException {
        exportListener.onExportProgress("Waiting for video transcoding to finish",
                EXPORT_STAGE_WAIT_FOR_TRANSCODING);
        LinkedList<Media> medias = getMediasFromComposition();
        for (Media media : medias) {
            Video video = (Video) media;
            if (video.isEdited() && video.getTranscodingTask()!= null) {
                try {
                    video.getTranscodingTask().get();
                } catch (InterruptedException | ExecutionException
                        | CancellationException waitingForIntermediatesError) {
                    Log.d(LOG_TAG, "Got " + waitingForIntermediatesError.getClass().getName()
                            + " while waiting for intermediate generation tasks to finish: " +
                    waitingForIntermediatesError.getMessage());
                    throw waitingForIntermediatesError;
                }
            }
        }
    }

    @NonNull
    private String getNewExportedVideoFileName() {
        return File.separator + "V_EDIT_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss.SSS").format(new Date()) + ".mp4";
    }

    protected void mixAudio(List<Media> mediaList, final String videoPath) {
        exportListener.onExportProgress("Mixing audio", EXPORT_STAGE_MIX_AUDIO);
        long movieDuration = vmComposition.getDuration() * 1000;
        try {
            movieDuration = FileUtils.getDurationFileAsync(tempExportFilePath)
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Do nothing as duration is already taken from composition first
        }

        ListenableFuture<Boolean> mixAudioTask =
            transcoderHelper.generateTempFileMixAudio(mediaList, tempAudioPath,
                    outputAudioMixedFile, movieDuration);
        AsyncFunction<? super Boolean, ?> swapAudioTask = new AsyncFunction<Boolean, Object>() {
            @Override
            public ListenableFuture<Object> apply(Boolean input) throws Exception {
                exportListener.onExportProgress("Applying mixed audio", EXPORT_STAGE_APPLY_AUDIO_MIXED);
                Log.d(LOG_TAG, "export, swapping audio mixed in video appended");
                // TODO(jliarte): 5/10/17 move to constructor to allow dependency injection?
                VideoAudioSwapper videoAudioSwapper = new VideoAudioSwapper();
                return videoAudioSwapper.exportAsync(videoPath, outputAudioMixedFile, finalVideoExportedFilePath);
            }
        };
        ListenableFuture<Object> chainedTask = Futures.transform(mixAudioTask, swapAudioTask);

        try {
            chainedTask.get();
            Log.d(LOG_TAG, "export, video with music/voiceOver exported, success "
                    + finalVideoExportedFilePath);
            FileUtils.removeFile(videoPath);
            Log.d(LOG_TAG, "export, video appended, removed "
                    + videoPath);
            notifyFinalSuccess(finalVideoExportedFilePath);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            exportListener.onExportProgress("Error Mixing audio", EXPORT_STAGE_MIX_AUDIO);
            exportListener.onExportError(e.getMessage());
        }
    }

    private void notifyFinalSuccess(String finalVideoExportedFilePath) {
        FileUtils.cleanDirectoryFiles(new File(tempAudioPath));
        exportListener.onExportSuccess(
            new Video(finalVideoExportedFilePath, Video.DEFAULT_VOLUME));
    }

    protected ListenableFuture<Void> addWatermark(Watermark watermark, final String inFilePath) throws IOException {
        Image imageWatermark = new Image(watermark.getResourceWatermarkFilePath(),
            Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
        ListenableFuture watermarkFuture = null;
        tempExportFileWatermark = outputFilesDirectory + File.separator + "V_with_wm.mp4";
        tempExportFilePath = tempExportFileWatermark;
        try {
            exportListener.onExportProgress("Applying watermark", EXPORT_STAGE_APPLY_WATERMARK);

            watermarkFuture = transcoderHelper
                .generateOutputVideoWithWatermarkImage(inFilePath, tempExportFileWatermark,
                    vmComposition.getVideoFormat(), imageWatermark);
        } catch (IOException e) {
            e.printStackTrace();
            exportListener.onExportError(e.getMessage());
            throw e;
        }
        return watermarkFuture;
    }

}