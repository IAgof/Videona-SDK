package com.videonasocialmedia.videonamediaframework.muxer;

import android.util.Log;

//import com.googlecode.mp4parser.authoring.Movie;
//import com.googlecode.mp4parser.authoring.Track;
//import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
//import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
//import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AppendTrack;
import org.mp4parser.muxer.tracks.ClippedTrack;

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

    public Movie appendVideos(List<String> videoPaths, boolean matchTracksLength)
            throws IOException, IntermediateFileException {
        List<Movie> movieList = getMovieList(videoPaths);
        LinkedList<Track> videoTracks = new LinkedList<>();
        LinkedList<Track> audioTracks = new LinkedList<>();

        for (Movie m : movieList) {
            Track audioTrack = null;
            Track videoTrack = null;
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                    audioTrack = t;
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                    videoTrack = t;
                }
            }
            if (matchTracksLength && audioTrack != null && videoTrack != null) {
                adjustTracksLength(audioTracks, videoTracks, audioTrack, videoTrack);
            }
        }
        return createMovie(audioTracks, videoTracks);
    }

    private void adjustTracksLength(LinkedList<Track> audioTracks, LinkedList<Track> videoTracks,
                                    Track audioTrack, Track videoTrack) {
        float audioDuration = (float) audioTrack.getDuration()
                / (float) audioTrack.getTrackMetaData().getTimescale();
        float videoDuration = (float) videoTrack.getDuration()
                / (float) videoTrack.getTrackMetaData().getTimescale();
        if (audioDuration > videoDuration) {
            cropTrack(audioTrack, audioTracks, audioDuration - videoDuration);
        } else {
            cropTrack(videoTrack, videoTracks, videoDuration - audioDuration);
        }
    }

    private void cropTrack(Track track, LinkedList<Track> tracks, float offset) {
        float audioSampleDuration = 0;
        tracks.removeLast();
        audioSampleDuration = getTrackSampleDuration(audioSampleDuration, track);
        int toSample = (int) (track.getSamples().size() - (offset / audioSampleDuration));
        Log.e(LOG_TAG, "Cropping audio to sample " + toSample);
        tracks.addLast(new ClippedTrack(track, 0, toSample));
    }

    private float getTrackSampleDuration(float sampleDuration, Track t) {
        for (long a : t.getSampleDurations()) sampleDuration += a;
        sampleDuration /= (float) t.getSamples().size();
        sampleDuration /= (float) t.getTrackMetaData().getTimescale();
        return sampleDuration;
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
