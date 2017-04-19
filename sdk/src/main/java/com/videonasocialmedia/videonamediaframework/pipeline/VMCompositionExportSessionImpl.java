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
import com.videonasocialmedia.videonamediaframework.model.media.track.AudioTrack;
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

    private OnExportEndedListener onExportEndedListener;
    private final VMComposition vmComposition;
    private boolean trimCorrect = true;
    protected Trimmer audioTrimmer;
    protected Appender appender;

    public VMCompositionExportSessionImpl(VMComposition vmComposition, String outputFilesDirectory,
                                          String tempFilesDirectory, OnExportEndedListener onExportEndedListener) {
        this.onExportEndedListener = onExportEndedListener;
        this.vmComposition = vmComposition;
        this.outputFilesDirectory = outputFilesDirectory;
        // (jliarte): 2/01/17 originally was PATH_APP/.temporal/intermediate_files/.temAudio
        tempAudioPath = tempFilesDirectory;
        FileUtils.createDirectory(tempAudioPath);
        outputAudioMixedFile = tempFilesDirectory + File.separator + Constants.MIXED_AUDIO_FILE_NAME;
        tempVideoExportedPath = outputFilesDirectory + File.separator + "export";
        audioTrimmer = new AudioTrimmer();
        appender = new Appender();
    }

    @Override
    public void export() {
        // TODO:(alvaro.martinez) 24/03/17 Add ListenableFuture AllAsList and Future isDone properties
        waitForOutputFilesFinished();

        LinkedList<Media> medias = getMediasFromComposition();
        ArrayList<String> videoTrimmedPaths = createVideoPathList(medias);
        try {
            Movie result = createMovieFromComposition(videoTrimmedPaths);
            if (result != null) {

                exportedVideoFilePath = outputFilesDirectory + getNewExportedVideoFileName();
                saveFinalVideo(result, exportedVideoFilePath);
                long durationMovie = FileUtils.getDurationFile(exportedVideoFilePath); // result.getTimescale();
                if(vmComposition.hasWatermark()){
                    // TODO:(alvaro.martinez) 27/02/17 implement addWatermark feature vmComposition.getResourceWatermarkFilePath()
                    ListenableFuture watermarkFuture =
                        addWatermark(vmComposition.getWatermark(), exportedVideoFilePath);
                    waitFutureToFinish(watermarkFuture);
                    addMusicOrFinishExport(exportedVideoFilePath, durationMovie);
                } else {
                    addMusicOrFinishExport(exportedVideoFilePath, durationMovie);
                }
                // TODO(jliarte): 29/12/16 is this generating errors for async processing of audio mixing?
//                FileUtils.cleanDirectory(new File(tempVideoExportedPath));
            }
        } catch (IOException e) {
            onExportEndedListener.onExportError(String.valueOf(e));
            e.printStackTrace();
        } catch (NullPointerException npe) {
            onExportEndedListener.onExportError(String.valueOf(npe));
            npe.printStackTrace(); 
        }
    }

    private void waitFutureToFinish(ListenableFuture future) {
        while(!future.isDone()){
            try {
                int countWaiting = 0;
                if (countWaiting > MAX_SECONDS_WAITING_FOR_TEMP_FILES) {
                    break;
                }
                countWaiting++;
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void addMusicOrFinishExport(String exportedVideoFilePath, long durationMovie) {
        if (vmComposition.hasMusic()
                || vmComposition.hasVoiceOver()) {
            // (jliarte): 23/12/16 mixAudio is an async process so the execution is split here
            mixAudio(getMediasAndVolumesToMixFromProjectTracks(exportedVideoFilePath), durationMovie);
        } else {
            Video videoExported = new Video(exportedVideoFilePath, 1f);
            onExportEndedListener.onExportSuccess(videoExported);
        }
    }

    private List<Media> getMediasAndVolumesToMixFromProjectTracks(String exportedVideoFilePath) {

        float videoVolume;
        if(vmComposition.getMediaTrack().isMute()){
            videoVolume = 0f;
        } else {
            videoVolume = vmComposition.getMediaTrack().getVolume();
        }
        Video video = new Video(exportedVideoFilePath, videoVolume);
        List<Media> mediaList = new ArrayList<>();
        mediaList.add(video);
        if(vmComposition.hasMusic()) {
            Music music = vmComposition.getMusic();
            AudioTrack musicTrack = vmComposition.getAudioTracks()
                .get(Constants.INDEX_AUDIO_TRACKS_MUSIC);
            if(musicTrack.isMute()){
                music.setVolume(0f);
            } else {
                music.setVolume(musicTrack.getVolume());
            }
            mediaList.add(music);
        }
        if(vmComposition.hasVoiceOver()) {
            Music voiceOver = vmComposition.getVoiceOver();
            AudioTrack voiceOverTrack = vmComposition.getAudioTracks()
                .get(Constants.INDEX_AUDIO_TRACKS_VOICE_OVER);
            if(voiceOverTrack.isMute()){
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
    private ArrayList<String> trimVideos(LinkedList<Media> medias) {
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
                onExportEndedListener.onExportError(String.valueOf(e));
            }
            index++;
        } while (trimCorrect && medias.size() > index);

        return videoTrimmedPaths;
    }

    protected Movie createMovieFromComposition(ArrayList<String> videoTranscodedPaths)
            throws IOException {
        return appender.appendVideos(videoTranscodedPaths, true);
    }

    protected void saveFinalVideo(Movie result, String outputFilePath) throws IOException {
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
                onExportEndedListener.onExportError(String.valueOf(e));
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
                onExportEndedListener.onExportError(String.valueOf(e));
                // TODO se debe continuar sin música o lo paro??
            }
        }

        return movie;
    }

    protected Movie addAudio(Movie movie, String audioPath, double movieDuration) throws IOException {
        Movie audioMovie = audioTrimmer.trim(audioPath, 0, movieDuration);
        List<Track> audioTracks = extractAudioTracks(audioMovie);
        if (audioTracks.size() > 0) {
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

    private void waitForOutputFilesFinished() {
        LinkedList<Media> medias = getMediasFromComposition();
        int countWaiting = 0;
        for (Media media : medias) {
            Video video = (Video) media;
            if (video.isEdited()) {
                while (!video.outputVideoIsFinished()) {
                    try {
                        if (countWaiting > MAX_SECONDS_WAITING_FOR_TEMP_FILES) {
                            break;
                        }
                        countWaiting++;
                        Thread.sleep(1000);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    @NonNull
    private String getNewExportedVideoFileName() {
        return File.separator + "V_EDIT_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
    }

    protected void mixAudio(List<Media> mediaList, long durationMovie) {
        final String videoExportedWithVoiceOverPath = outputFilesDirectory
                + getNewExportedVideoFileName();
        MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
        TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);

        ListenableFuture<Void> mixAudioListenableFuture =
            transcoderHelper.generateTempFileMixAudio(mediaList, tempAudioPath, outputAudioMixedFile,
                durationMovie);
        waitFutureToFinish(mixAudioListenableFuture);

        // TODO:(alvaro.martinez) 19/04/17 Implement ListenableFuture in VideoAudioSwapper and remove listener
        VideoAudioSwapper videoAudioSwapper = new VideoAudioSwapper();
        videoAudioSwapper.export(exportedVideoFilePath, outputAudioMixedFile,
            videoExportedWithVoiceOverPath,
            new ExporterVideoSwapAudio.VideoAudioSwapperListener() {
                @Override
                public void onExportError(String error) {
                    onExportEndedListener.onExportError("error mixing audio");
                }

                @Override
                public void onExportSuccess() {
                    // TODO(jliarte): 23/12/16 too many callbacks??
                    // TODO(jliarte): 23/12/16 onSuccess will be called twice in this case!
                    onExportEndedListener.onExportSuccess(
                        new Video(videoExportedWithVoiceOverPath, 1f));
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
            watermarkFuture = transcoderHelper
                .generateOutputVideoWithWatermarkImage(inFilePath, outputFilePath,
                    vmComposition.getVideonaFormat(), imageWatermark);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return watermarkFuture;
    }
}