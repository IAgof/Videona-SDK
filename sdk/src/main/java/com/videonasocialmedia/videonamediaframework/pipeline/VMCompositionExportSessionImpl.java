package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Watermark;
import com.videonasocialmedia.videonamediaframework.model.media.track.AudioTrack;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.AudioTrimmer;
import com.videonasocialmedia.videonamediaframework.muxer.IntermediateFileException;
import com.videonasocialmedia.videonamediaframework.muxer.Trimmer;
import com.videonasocialmedia.videonamediaframework.muxer.VideoTrimmer;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * @author Juan Javier Cabanas
 * @author Verónica Lago Fominaya
 */
public class VMCompositionExportSessionImpl implements VMCompositionExportSession {
    private static final int MAX_SECONDS_WAITING_FOR_TEMP_FILES = 600;
    private static final String TAG = VMCompositionExportSessionImpl.class.getCanonicalName();
    public static final int MAX_NUM_TRIES_TO_EXPORT_VIDEO = 3;
    private final String outputFilesDirectory;
    private final String outputAudioMixedFile;
    private String tempAudioPath;
    private String tempVideoExportedPath;

    private ExportListener exportListener;
    private final VMComposition vmComposition;
    private boolean trimCorrect = true;
    protected Trimmer audioTrimmer;
    protected Appender appender;
    private String tempExportFilePath;
    private String tempExportFileWatermark;
    private String intermediatesTempAudioFadeDirectory;

    public VMCompositionExportSessionImpl(
            VMComposition vmComposition, String outputFilesDirectory, String tempFilesDirectory, 
            String intermediatesTempAudioFadeDirectory, ExportListener exportListener) {
        // TODO(jliarte): 29/04/17 should move the parameters to export method to have them defined by the interface?
        this.exportListener = exportListener;
        this.vmComposition = vmComposition;
        this.outputFilesDirectory = outputFilesDirectory;
        this.tempAudioPath = tempFilesDirectory;
        this.intermediatesTempAudioFadeDirectory = intermediatesTempAudioFadeDirectory;
        FileUtils.createDirectory(tempAudioPath);
        outputAudioMixedFile = tempFilesDirectory + File.separator + Constants.MIXED_AUDIO_FILE_NAME;
        tempVideoExportedPath = outputFilesDirectory + File.separator + "export";
        audioTrimmer = new AudioTrimmer();
        appender = new Appender();
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
        Log.d(TAG, "export, waiting for finish temporal files generation ");
        exportListener.onExportProgress("Waiting for video transcoding to finish",
                EXPORT_STAGE_WAIT_FOR_TRANSCODING);
        try {
            // TODO:(alvaro.martinez) 24/03/17 Add ListenableFuture AllAsList and Future isDone properties
            waitForVideoTempFilesFinished();

            LinkedList<Media> medias = getMediasFromComposition();
            ArrayList<String> videoTrimmedPaths = createVideoPathList(medias);
            Log.d(TAG, "export, appending temporal files");

            exportListener.onExportProgress("Joining the videos", EXPORT_STAGE_JOIN_VIDEOS);
            Movie result = createMovieFromComposition(videoTrimmedPaths);
            if (result != null) {
                tempExportFilePath = outputFilesDirectory + File.separator + "V_Appended.mp4";
                saveFinalVideo(result, tempExportFilePath);
                long movieDuration = FileUtils.getDurationFile(tempExportFilePath);
                applyWatermark();
                mixAudio(getMediasAndVolumesToMixFromProjectTracks(tempExportFilePath),
                    tempExportFilePath, movieDuration);
            }
        } catch (IOException | IntermediateFileException | NullPointerException
                // from waitForVideoTempFilesFinished
                | InterruptedException | ExecutionException exportError) {
            Log.d(TAG, "Catched " +  exportError.getClass().getName() + " while exporting");
            exportListener.onExportError(String.valueOf(exportError));
        }
    }

    private void applyWatermark() {
        if (vmComposition.hasWatermark()) {
            String tempFileAppended = tempExportFilePath;
            applyWatermarkToVideoAndWaitForFinish(tempExportFilePath);
            FileUtils.removeFile(tempFileAppended);
        }
    }

    private void applyWatermarkToVideoAndWaitForFinish(String tempExportFilePath) {
        if (vmComposition.hasWatermark()) {
          Log.d(TAG, "export, adding watermark to video appended");
          // TODO:(alvaro.martinez) 27/02/17 implement addWatermarkToGeneratedVideo feature
            ListenableFuture watermarkingJob = addWatermark(vmComposition.getWatermark(),
                tempExportFilePath);
            try {
                watermarkingJob.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Media> getMediasAndVolumesToMixFromProjectTracks(
            String exportedVideoAppendedPath) {
        List<Media> mediaList = new ArrayList<>();
        if (vmComposition.hasVideos()) {
            float videoVolume;
            if (vmComposition.getMediaTrack().isMuted()) {
                videoVolume = 0f;
            } else {
                videoVolume = vmComposition.getMediaTrack().getVolume();
            }
            Video video = new Video(exportedVideoAppendedPath, videoVolume);
            mediaList.add(video);
        }
        if (vmComposition.hasMusic()) {
            Music music = vmComposition.getMusic();
            AudioTrack musicTrack = vmComposition.getAudioTracks()
                .get(Constants.INDEX_AUDIO_TRACK_MUSIC);
            if (musicTrack.isMuted()) {
                music.setVolume(0f);
            } else {
                music.setVolume(musicTrack.getVolume());
            }
            mediaList.add(music);
        }
        if (vmComposition.hasVoiceOver()) {
            Music voiceOver = vmComposition.getVoiceOver();
            AudioTrack voiceOverTrack = vmComposition.getAudioTracks()
                .get(Constants.INDEX_AUDIO_TRACK_VOICE_OVER);
            if(voiceOverTrack.isMuted()){
                voiceOver.setVolume(0f);
            } else {
                voiceOver.setVolume(voiceOverTrack.getVolume());
            }
            mediaList.add(voiceOver);
        }
        return mediaList;
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

    // TODO(jliarte): 17/11/16 check if this code is still relevant
    private ArrayList<String> trimVideosWithMuxer(LinkedList<Media> medias) {
        final File tempDir = new File(tempVideoExportedPath);
        if (!tempDir.exists())
            tempDir.mkdirs();
        ArrayList<String> videoTrimmedPaths = new ArrayList<>();
        Trimmer trimmer;
        Movie movie;
        int index = 0;
        do {
            try {
                String videoTrimmedTempPath = tempVideoExportedPath
                        + File.separator + "video_trimmed_" + index + ".mp4";
                int startTime = medias.get(index).getStartTime();
                int endTime = medias.get(index).getStopTime();
                int editedFileDuration = medias.get(index).getStopTime()
                        - medias.get(index).getStartTime();
                int originalFileDuration = ( (Video) medias.get(index) ).getFileDuration();
                if (editedFileDuration < originalFileDuration) {
                    trimmer = new VideoTrimmer();
                    movie = trimmer.trim(medias.get(index).getMediaPath(), startTime, endTime);
                    Utils.createFile(movie, videoTrimmedTempPath);
                    videoTrimmedPaths.add(videoTrimmedTempPath);
                } else {
                    videoTrimmedPaths.add(medias.get(index).getMediaPath());
                }
            } catch (IOException | NullPointerException e) {
                trimCorrect = false;
                videoTrimmedPaths = null;
                exportListener.onExportError(String.valueOf(e));
            }
            index++;
        } while (trimCorrect && medias.size() > index);

        return videoTrimmedPaths;
    }

    protected Movie createMovieFromComposition(final ArrayList<String> videoTranscodedPaths)
            throws IOException, IntermediateFileException, ExecutionException, InterruptedException {
        Movie movie = null;
        try {
            movie = appender.appendVideos(videoTranscodedPaths, true);
        } catch (final IntermediateFileException intermediateFileError) {
            Log.d(TAG, "Catched intermediate files error");
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
                                Log.d(TAG, "error updating intermediate for video "
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
                                           TranscoderHelperListener transcoderHelperListener) {
        MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
        TranscoderHelper transcoderHelper =
                new TranscoderHelper(mediaTranscoder);

        Video videoToEdit = (Video) vmComposition.getMediaTrack().getItems().get(videoIndex);
        Drawable drawableFadeTransition = vmComposition.getDrawableFadeTransitionVideo();
        boolean isVideoFadeTransitionActivated = vmComposition.isVideoFadeTransitionActivated();
        boolean isAudioFadeTransitionActivated = vmComposition.isAudioFadeTransitionActivated();
        // TODO(jliarte): 17/03/17 move this logic to TranscoderHelper?
        // copied from /data/repos/videona/ViMoJo/app/src/main/java/com/videonasocialmedia/vimojo/export/domain/RelaunchTranscoderTempBackgroundUseCase.java
        VideonaFormat videonaFormat = vmComposition.getVideoFormat();
        if (videoToEdit.hasText()) {
            transcoderHelper.generateOutputVideoWithOverlayImageAndTrimmingAsync(drawableFadeTransition,
                    isVideoFadeTransitionActivated,isAudioFadeTransitionActivated, videoToEdit,
                    videonaFormat, intermediatesTempAudioFadeDirectory, transcoderHelperListener);
        } else {
            transcoderHelper.generateOutputVideoWithTrimmingAsync(drawableFadeTransition,
                    isVideoFadeTransitionActivated, isAudioFadeTransitionActivated, videoToEdit,
                    videonaFormat, intermediatesTempAudioFadeDirectory, transcoderHelperListener);
        }
    }

    protected void saveFinalVideo(Movie result, String outputFilePath) throws IOException {
        exportListener.onExportProgress("Writing video to disk", EXPORT_STAGE_WRITE_VIDEO_TO_DISK);

        long start = System.currentTimeMillis();
        Utils.createFile(result, outputFilePath);
        long spent = System.currentTimeMillis() - start;
        Log.d("WRITING VIDEO FILE", "time spent in millis: " + spent);
    }

    protected double getMovieDuration(Movie movie) {
        double movieDuration = movie.getTracks().get(0).getDuration();
        double timeScale = movie.getTimescale();
        movieDuration = movieDuration / timeScale * 1000;
        return movieDuration;
    }


    // TODO(jliarte): 19/12/16 what happens when there is more than a path? is that really needed now?
    protected Movie addAudioList(Movie movie, ArrayList<String> audioPaths, double movieDuration) {
        ArrayList<Movie> audioList = new ArrayList<>();
        List<Track> audioTracks = new LinkedList<>();

        // TODO change this for do while
        for (String audio : audioPaths) {
            try {
                audioList.add(audioTrimmer.trim(audio, 0, movieDuration));
            } catch (IOException | NullPointerException e) {
                exportListener.onExportError(String.valueOf(e));
            }
        }

        for (Movie m : audioList) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
            }
        }

        if (audioTracks.size() > 0) {
            try {
                movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            } catch (IOException | NullPointerException e) {
                exportListener.onExportError(String.valueOf(e));
                // TODO se debe continuar sin música o lo paro??
            }
        }

        return movie;
    }

    protected Movie addAudio(Movie movie, String audioPath, double movieDuration)
            throws IOException {
        Movie audioMovie = audioTrimmer.trim(audioPath, 0, movieDuration);
        List<Track> audioTracks = extractAudioTracks(audioMovie);
        if (audioTracks.size() > 0) {
            exportListener.onExportProgress("Adding audio tracks", EXPORT_STAGE_ADD_AUDIO_TRACKS);
            movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        return movie;
    }

    // TODO(jliarte): 20/12/16 similar methods on VideoAudioSwapper
    @NonNull
    private List<Track> extractAudioTracks(Movie audioMovie) {
        List<Track> audioTracks = new LinkedList<>();
        for (Track t : audioMovie.getTracks()) {
            if (t.getHandler().equals("soun")) {
                audioTracks.add(t);
            }
        }
        return audioTracks;
    }

    private void waitForVideoTempFilesFinished() throws ExecutionException, InterruptedException {
        LinkedList<Media> medias = getMediasFromComposition();
        for (Media media : medias) {
            Video video = (Video) media;
            if (video.isEdited() && video.getTranscodingTask()!= null) {
                try {
                    video.getTranscodingTask().get();
                } catch (InterruptedException | ExecutionException
                        | CancellationException waitingForIntermediatesError) {
                    Log.d(TAG, "Got " + waitingForIntermediatesError.getClass().getName()
                            + " while waiting for intermediate generation tasks to finish: " +
                    waitingForIntermediatesError.getMessage());
//                    exportListener.onExportError(waitingForIntermediatesError.getMessage());
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

    protected void mixAudio(List<Media> mediaList, final String videoPath,
                            long movieDuration) {
        exportListener.onExportProgress("Mixing audio", EXPORT_STAGE_MIX_AUDIO);
        final String finalVideoExportedFilePath = outputFilesDirectory
                + getNewExportedVideoFileName();

        boolean resultMixAudioFiles = applyMixAudioAndWaitForFinish(mediaList, movieDuration);
        if (!resultMixAudioFiles) {
          Log.d(TAG, "error mixing audio, applyMixAudioAndWaitForFinish");
          exportListener.onExportProgress("error Mixing audio", EXPORT_STAGE_MIX_AUDIO);
          exportListener.onExportError("error mixing audio");
          return;
        }

        exportListener.onExportProgress("Applying mixed audio", EXPORT_STAGE_APPLY_AUDIO_MIXED);
        Log.d(TAG, "export, swapping audio mixed in video appended");
        // TODO:(alvaro.martinez) 19/04/17 Implement ListenableFuture in VideoAudioSwapper and remove listener
        VideoAudioSwapper videoAudioSwapper = new VideoAudioSwapper();
        videoAudioSwapper.export(videoPath, outputAudioMixedFile,
            finalVideoExportedFilePath,
            new ExporterVideoSwapAudio.VideoAudioSwapperListener() {
                @Override
                public void onExportError(String error) {
                  exportListener.onExportProgress("error Mixing audio", EXPORT_STAGE_MIX_AUDIO);
                  exportListener.onExportError("error mixing audio, swapping");
                }

                @Override
                public void onExportSuccess() {
                  // TODO(jliarte): 23/12/16 too many callbacks??
                  // TODO(jliarte): 23/12/16 onSuccess will be called twice in this case!
                  Log.d(TAG, "export, video with music/voiceOver exported, success "
                      + finalVideoExportedFilePath);
                  FileUtils.removeFile(videoPath);
                    Log.d(TAG, "export, video appended, removed "
                        + videoPath);
                  FileUtils.cleanDirectoryFiles(new File(tempAudioPath));
                  exportListener.onExportSuccess(
                      new Video(finalVideoExportedFilePath, Video.DEFAULT_VOLUME));
                }
            });
    }

    public boolean applyMixAudioAndWaitForFinish(List<Media> mediaList, long movieDuration) {
        MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
        TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);
        ListenableFuture<Boolean> mixAudioJob =
            transcoderHelper.generateTempFileMixAudio(mediaList, tempAudioPath,
                    outputAudioMixedFile, movieDuration);
        boolean result = false;
        try {
            result = mixAudioJob.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            exportListener.onExportError(e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            exportListener.onExportError(e.getMessage());
        }

        return result;
    }

    protected ListenableFuture<Void> addWatermark(Watermark watermark, final String inFilePath) {
        MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
        TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);
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
        }
        return watermarkFuture;
    }

}