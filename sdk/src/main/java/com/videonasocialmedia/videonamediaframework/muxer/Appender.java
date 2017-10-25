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
    public static final int AUDIO_SAMPLES_TO_JUMP = 5;

    public Movie appendVideos(List<String> videoPaths)
            throws IOException, IntermediateFileException {
        List<Movie> movieList = getMovieList(videoPaths);
        List<Track> videoTracks = new LinkedList<>();
        List<Track> audioTracks = new LinkedList<>();

        for (Movie m : movieList) {
            for (Track track : m.getTracks()) {
                if (track.getHandler().equals("soun")) {
                    // removes the first samples and shortens the AAC track by ~22ms per sample
                    CroppedTrack audioTrackShort = new CroppedTrack(track, AUDIO_SAMPLES_TO_JUMP,
                            track.getSamples().size());
                    audioTracks.add(audioTrackShort);
                }
                if (track.getHandler().equals("vide")) {
                    videoTracks.add(track);
                }
            }
        }
        return createMovie(audioTracks, videoTracks);
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
