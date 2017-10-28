package com.videonasocialmedia.videonamediaframework.muxer;

import android.util.Log;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Veronica Lago Fominaya
 */
public class Appender {
    private static final String LOG_TAG = Appender.class.getCanonicalName();
    public static final int MAGIC_NUMBER_AUDIO_SAMPLES_DURATION = 42666;// 2 x 1024(buffer samples) / 48000 (48khz)

    public Movie appendVideos(List<String> videoPaths) throws IOException,
        IntermediateFileException {
        List<Movie> movieList = getMovieList(videoPaths);
        List<Track> videoTracks = new LinkedList<>();
        List<Track> audioTracks = new LinkedList<>();
        for (Movie movie : movieList) {
            Track audioTrack = getAudioTrackFromMovie(movie);
            Track videoTrack = getVideoTrackFromMovie(movie);
            videoTracks.add(videoTrack);
            audioTracks.add(audioTrack);
           /* if(audioTrack == null || videoTrack == null){
                int index = movieList.indexOf(movie);
                throw new IntermediateFileException(videoPaths.get(index), index);
            }
            if(videoTrack.getDuration() > audioTrack.getDuration()) {
                long diffTrackDuration = videoTrack.getDuration() - audioTrack.getDuration();
                int diffNumSamples = (int) (diffTrackDuration / MAGIC_NUMBER_AUDIO_SAMPLES_DURATION);
                if (diffNumSamples > 0) {
                    CroppedTrack audioTrackShort = new CroppedTrack(audioTrack, diffNumSamples,
                        audioTrack.getSamples().size());
                    audioTracks.add(audioTrackShort);
                } else {
                    audioTracks.add(audioTrack);
                }
                videoTracks.add(videoTrack);
            } else {
                long diffTrackDuration = audioTrack.getDuration() - videoTrack.getDuration();
                int diffNumSamples = (int) (diffTrackDuration / MAGIC_NUMBER_AUDIO_SAMPLES_DURATION);
                if (diffNumSamples > 0) {
                    CroppedTrack videoTrackShort = new CroppedTrack(videoTrack, diffNumSamples,
                        videoTrack.getSamples().size());
                    videoTracks.add(videoTrackShort);
                } else {
                    videoTracks.add(videoTrack);
                }
                audioTracks.add(audioTrack);
            } */
        }
        return createMovie(audioTracks, videoTracks);
    }

    private Track getVideoTrackFromMovie(Movie movie) {
        for (Track track : movie.getTracks()) {
            if (track.getHandler().equals("vide")) {
                return track;
            }
        }
        Log.d(LOG_TAG, "Video track not found");
        return null;
    }

    private Track getAudioTrackFromMovie(Movie movie) {
        for (Track track : movie.getTracks()) {
            if (track.getHandler().equals("soun")) {
                return track;
            }
        }
        Log.d(LOG_TAG, "Audio track not found");
        return null;
    }

    private List<Movie> getMovieList(List<String> videoPaths) throws IOException,
            IntermediateFileException {
        List<Movie> movieList = new ArrayList<>();

        for (String videoPath : videoPaths) {
           long start = System.currentTimeMillis();
            try {
                Movie movie = MovieCreator.build(videoPath);
                long spent = System.currentTimeMillis()-start;
                Log.d("BUILDING MOVIE", "time spent in millis: " + spent);
                movieList.add(movie);
            } catch (FileNotFoundException fileError) {
                Log.e(LOG_TAG, "Missing file, index " + videoPaths.indexOf(videoPath) +
                        " path " + videoPath);
                throw new IntermediateFileException(videoPath, videoPaths.indexOf(videoPath));
            } catch (NullPointerException npe) {
                Log.e(LOG_TAG, "Null pointer while getting movie list for index "
                        + videoPaths.indexOf(videoPath));
                throw new IntermediateFileException(videoPath, videoPaths.indexOf(videoPath));
            }
        }
        return movieList;
    }

    private Movie createMovie(List<Track> audioTracks, List<Track> videoTracks) throws IOException {
        Movie result = new Movie();

        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
        return result;
    }

    public Movie addAudio(Movie movie, ArrayList<String> audioPaths, double movieDuration) throws IOException {
        ArrayList<Movie> audioList = new ArrayList<>();
        Trimmer trimer = new AudioTrimmer();
        List<Track> audioTracks = new LinkedList<>();

        for (String audio : audioPaths) {
            audioList.add(trimer.trim(audio, 0, movieDuration));
        }
        for (Movie m : audioList) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
            }
        }
        if (audioTracks.size() > 0) {
            movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        return movie;
    }

}
