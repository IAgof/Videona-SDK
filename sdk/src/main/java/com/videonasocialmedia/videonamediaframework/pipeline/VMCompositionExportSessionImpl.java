package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Watermark;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.AudioTrimmer;
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
import java.util.concurrent.ExecutionException;


/**
 * @author Juan Javier Cabanas
 * @author Verónica Lago Fominaya
 */
public class VMCompositionExportSessionImpl implements VMCompositionExportSession {
    private static final int MAX_SECONDS_WAITING_FOR_TEMP_FILES = 600;
    private static final String TAG = "VMCompositionExportSession implementation";
    private final String outputFilesDirectory;
    private final String outputAudioMixedFile;
    private String tempAudioPath;
    private String tempVideoExportedPath;
    private String exportedVideoFilePath;

    private ExportListener exportListener;
    private final VMComposition vmComposition;
    private boolean trimCorrect = true;
    protected Trimmer audioTrimmer;
    protected Appender appender;
    private AudioMixer audioMixer;

    public VMCompositionExportSessionImpl(VMComposition vmComposition, String outputFilesDirectory,
                                          String tempFilesDirectory,
                                          ExportListener exportListener) {
        // TODO(jliarte): 29/04/17 should move the parameters to export method to have them defined by the interface?
        this.exportListener = exportListener;
        this.vmComposition = vmComposition;
        this.outputFilesDirectory = outputFilesDirectory;
        tempAudioPath = tempFilesDirectory;
        FileUtils.createDirectory(tempAudioPath);
        outputAudioMixedFile = tempFilesDirectory + File.separator + Constants.MIXED_AUDIO_FILE_NAME;
        tempVideoExportedPath = outputFilesDirectory + File.separator + "export";
        audioTrimmer = new AudioTrimmer();
        appender = new Appender();
        audioMixer = new AudioMixer(outputAudioMixedFile);
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
        // TODO:(alvaro.martinez) 24/03/17 Add ListenableFuture AllAsList and Future isDone properties
        waitForVideoTempFilesFinished();

        LinkedList<Media> medias = getMediasFromComposition();
        ArrayList<String> videoTrimmedPaths = createVideoPathList(medias);

        try {
            Log.d(TAG, "export, appending temporal files");
            exportListener.onExportProgress("Joining the videos", EXPORT_STAGE_JOIN_VIDEOS);
            Movie result = createMovieFromComposition(videoTrimmedPaths);
            if (result != null) {
                exportedVideoFilePath = outputFilesDirectory + getNewExportedVideoFileName();
                saveFinalVideo(result, exportedVideoFilePath);
                applyWatermarkToVideoAndWaitForFinish();
                addMusicOrFinishExport();
                // TODO(jliarte): 29/12/16 is this generating errors for async processing of audio mixing?
//                FileUtils.cleanDirectory(new File(tempVideoExportedPath));
            }
        } catch (IOException e) {
            exportListener.onExportError(String.valueOf(e));
            e.printStackTrace();
        } catch (NullPointerException npe) {
            exportListener.onExportError(String.valueOf(npe));
            npe.printStackTrace(); 
        }
    }

    private void applyWatermarkToVideoAndWaitForFinish() {
        if (vmComposition.hasWatermark()) {
          Log.d(TAG, "export, adding watermark to video appended");
          // TODO:(alvaro.martinez) 27/02/17 implement addWatermarkToGeneratedVideo feature
            ListenableFuture watermarkingJob = addWatermark(vmComposition.getWatermark(),
                exportedVideoFilePath);
            try {
                watermarkingJob.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void addMusicOrFinishExport() {
        if (vmComposition.hasMusic()
                && (vmComposition.getMusic().getVolume() < 1f)) {
            // (jliarte): 23/12/16 mixAudio is an async process so the execution is split here
            Log.d(TAG, "export, mixing audio");
            mixAudio();
        } else {
            Log.d(TAG, "export, video shared created, success");
            Video videoExported = new Video(exportedVideoFilePath);
            exportListener.onExportSuccess(videoExported);
            Log.d(TAG, "addMusicOrFinishExport: sent success to listener");
        }
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

    protected Movie createMovieFromComposition(ArrayList<String> videoTranscodedPaths)
            throws IOException {
        Movie merge;
        if (vmComposition.hasMusic() && checkMusicPath()
                && (vmComposition.getMusic().getVolume() >= 1f)) {
            merge = appender.appendVideos(videoTranscodedPaths, false);
            double movieDuration = getMovieDuration(merge);
            merge = addAudio(merge, vmComposition.getMusic().getMediaPath(), movieDuration);
        } else {
            merge = appender.appendVideos(videoTranscodedPaths, true);
        }
        return merge;
    }

    @NonNull
    private boolean checkMusicPath() {
        File musicFile = new File(vmComposition.getMusic().getMediaPath());
        // TODO(jliarte): 28/12/16 this method for checking path does not work, as the File object is still created. Should check exists() method
        if (musicFile == null) {
            exportListener.onExportError("Music not found");
            return false;
        }
        return true;
    }

    protected void saveFinalVideo(Movie result, String outputFilePath) throws IOException {
        exportListener.onExportProgress("Writing video to disk", EXPORT_STAGE_WRITE_VIDEO_TO_DISK);

        long start = System.currentTimeMillis();
        Utils.createFile(result, outputFilePath);
        long spent = System.currentTimeMillis() - start;
        Log.d("WRITING VIDEO FILE", "time spent in millis: " + spent);
    }

    protected double getMovieDuration(Movie mergedVideoWithoutAudio) {
        double movieDuration = mergedVideoWithoutAudio.getTracks().get(0).getDuration();
        double timeScale = mergedVideoWithoutAudio.getTimescale();
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

    private void waitForVideoTempFilesFinished() {
        LinkedList<Media> medias = getMediasFromComposition();
        for (Media media : medias) {
            Video video = (Video) media;
            if (video.isEdited() && video.getTranscodingTask()!= null) {
                try {
                    video.getTranscodingTask().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NonNull
    private String getNewExportedVideoFileName() {
        return File.separator + "V_EDIT_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
    }

    protected void mixAudio() {
        exportListener.onExportProgress("Mixing audio", EXPORT_STAGE_MIX_AUDIO);

        final String videoExportedWithVoiceOverPath = outputFilesDirectory
                + getNewExportedVideoFileName();
        Music voiceOver = vmComposition.getMusic();
        long durationVideoFile = FileUtils.getDurationFile(exportedVideoFilePath);
        audioMixer.mixAudio(exportedVideoFilePath, voiceOver.getMediaPath(), voiceOver.getVolume(),
                tempAudioPath, durationVideoFile,
                new AudioMixer.OnMixAudioListener() {
                    @Override
                    public void onMixAudioSuccess(String path) {
                        exportListener.onExportProgress("Applying mixed audio", EXPORT_STAGE_APPLY_AUDIO_MIXED);
                        Log.d(TAG, "export, swapping audio mixed in video appended");
                        VideoAudioSwapper videoAudioSwapper = new VideoAudioSwapper();
                        videoAudioSwapper.export(exportedVideoFilePath, path,
                                videoExportedWithVoiceOverPath,
                                new ExporterVideoSwapAudio.VideoAudioSwapperListener() {
                                    @Override
                                    public void onExportError(String error) {
                                        exportListener.onExportProgress("error Mixing audio", EXPORT_STAGE_MIX_AUDIO);
                                        exportListener.onExportError("error mixing audio");
                                    }

                                    @Override
                                    public void onExportSuccess() {
                                        // TODO(jliarte): 23/12/16 too many callbacks??
                                        // TODO(jliarte): 23/12/16 onSuccess will be called twice in this case!
                                        Log.d(TAG, "export, video with music/voiceOver exported, success");
                                        exportListener.onExportSuccess(
                                                new Video(videoExportedWithVoiceOverPath));
                                    }
                                });
                    }

                    @Override
                    public void onMixAudioError() {
                        exportListener.onExportError("Error mixing audio");
                    }

                    @Override
                    public void onMixAudioProgress(String progress) {
                        exportListener.onExportProgress(progress, EXPORT_STAGE_MIX_AUDIO);
                    }
                });
    }

    protected ListenableFuture<Void> addWatermark(Watermark watermark, final String inFilePath) {
        MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
        TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);
        Image imageWatermark = new Image(watermark.getResourceWatermarkFilePath(),
            Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
        ListenableFuture watermarkFuture = null;
        final String outputFilePath = outputFilesDirectory + getNewExportedVideoFileName();
        exportedVideoFilePath = outputFilePath;

        try {
            exportListener.onExportProgress("Applying watermark", EXPORT_STAGE_APPLY_WATERMARK);

            watermarkFuture = transcoderHelper
                .generateOutputVideoWithWatermarkImage(inFilePath, outputFilePath,
                    vmComposition.getVideoFormat(), imageWatermark);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return watermarkFuture;
    }
}