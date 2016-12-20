package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;
import android.util.Log;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.AudioTrimmer;
import com.videonasocialmedia.videonamediaframework.muxer.Trimmer;
import com.videonasocialmedia.videonamediaframework.muxer.VideoTrimmer;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
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
    private final String tempFilesDirectory;
    private String tempVideoExportedPath;
    private String tempTranscodePath;

    private OnExportEndedListener onExportEndedListener;
    private final VMComposition vMComposition;
    private boolean trimCorrect = true;
    private Profile profile;
    protected Trimmer audioTrimmer;
    protected Appender appender;

    public VMCompositionExportSessionImpl(String tempFilesDirectory, VMComposition vmComposition, Profile profile,
                                          OnExportEndedListener onExportEndedListener) {
        this.onExportEndedListener = onExportEndedListener;
        this.vMComposition = vmComposition;
        this.profile = profile;
        this.tempFilesDirectory = tempFilesDirectory;
        tempTranscodePath = tempFilesDirectory  + File.separator + "transcode";
        tempVideoExportedPath = tempFilesDirectory + File.separator + "export";
        audioTrimmer = new AudioTrimmer();
        appender = new Appender();
    }

    @Override
    public void export() {
        waitForOutputFilesFinished();

        LinkedList<Media> medias = getMediasFromComposition();
        ArrayList<String> videoTrimmedPaths = createVideoPathList(medias);

        Movie result = createMovieFromComposition(videoTrimmedPaths);
        if (result != null) {
            saveFinalVideo(result, tempFilesDirectory + File.separator + "V_EDIT_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4");
            FileUtils.cleanDirectory(new File(tempVideoExportedPath));
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
        LinkedList<Media> medias = vMComposition.getMediaTrack().getItems();
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

    protected Movie createMovieFromComposition(ArrayList<String> videoTranscoded) {
        Movie merge;
        if (vMComposition.hasMusic() && checkMusicPath()) {
            merge = appendVideos(videoTranscoded, false);
            double movieDuration = getMovieDuration(merge);
            try {
                merge = addAudio(merge, vMComposition.getMusic().getMediaPath(), movieDuration);
            } catch (IOException e) {
                e.printStackTrace();
                onExportEndedListener.onExportError(String.valueOf(e));
            }
        } else {
            merge = appendVideos(videoTranscoded, true);
        }
        return merge;
    }

    @NonNull
    private boolean checkMusicPath() {
        File musicFile = new File(vMComposition.getMusic().getMediaPath());
        if (musicFile == null) {
            onExportEndedListener.onExportError("Music not found");
            return false;
        }
        return true;
    }

    private void saveFinalVideo(Movie result, String outputFilePath) {
        try {
            long start = System.currentTimeMillis();
            Utils.createFile(result, outputFilePath);
            long spent = System.currentTimeMillis() - start;
            Log.d("WRITING VIDEO FILE", "time spent in millis: " + spent);
            onExportEndedListener.onExportSuccess(new Video(outputFilePath));
        } catch (IOException | NullPointerException e) {
            onExportEndedListener.onExportError(String.valueOf(e));
        }
    }

    protected Movie appendVideos(ArrayList<String> videoTranscodedPaths, boolean addOriginalAudio) {
        try {
            return appender.appendVideos(videoTranscodedPaths, addOriginalAudio);
        } catch (Exception e) {
            onExportEndedListener.onExportError(String.valueOf(e));
            return null;
        }
    }

    private double getMovieDuration(Movie mergedVideoWithoutAudio) {
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
}