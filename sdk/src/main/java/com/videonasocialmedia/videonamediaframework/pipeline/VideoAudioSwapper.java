package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;
import android.util.Log;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by alvaro on 23/10/16.
 */

public class VideoAudioSwapper implements ExporterVideoSwapAudio {
  private VideoAudioSwapperListener videoAudioSwapperListener;

  public VideoAudioSwapper() {
  }

  @Override
  public void export(String videoFilePath, String newAudioFilePath, String outputFilePath,
                     VideoAudioSwapperListener videoAudioSwapperListener) {
    this.videoAudioSwapperListener = videoAudioSwapperListener;

    Movie result = null;
    try {
      result = getFinalMovie(videoFilePath, newAudioFilePath);
    } catch (IOException e) {
      e.printStackTrace();
      videoAudioSwapperListener.onExportError(String.valueOf(e));
    }
    if (result != null) {
      saveFinalVideo(result, outputFilePath);
    }
  }

  private Movie getFinalMovie(String videoFilePath, String newAudioFilePath) throws IOException {
    Movie result;
    Movie movie = MovieCreator.build(videoFilePath);

    File musicFile = new File(newAudioFilePath);
    if (musicFile == null) {
      videoAudioSwapperListener.onExportError("Music not found");
    }
    ArrayList<String> audio = new ArrayList<>();
    audio.add(musicFile.getPath());
    result = swapAudio(movie, newAudioFilePath);

    return result;
  }

  private Movie swapAudio(Movie originalMovie, String audioPath)
          throws IOException {
    Movie finalMovie = new Movie();
    List<Track> videoTrack = extractVideoTracks(originalMovie);
    finalMovie.addTrack(new AppendTrack(videoTrack.toArray(new Track[videoTrack.size()])));

    Movie audioMovie = MovieCreator.build(audioPath);
    List<Track> audioTracks = extractAudioTracks(audioMovie);
    finalMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));

    return finalMovie;
  }

  @NonNull
  private List<Track> extractAudioTracks(Movie audioMovie) {
    return extractTracks(audioMovie, "soun");
  }

  @NonNull
  private List<Track> extractVideoTracks(Movie originalMovie) {
    return extractTracks(originalMovie, "vide");
  }

  @NonNull
  private List<Track> extractTracks(Movie movie, String trackType) {
    List<Track> videoTrack = new LinkedList<>();
    for (Track t : movie.getTracks()) {
      if (t.getHandler().equals(trackType)) {
        videoTrack.add(t);
      }
    }
    return videoTrack;
  }

  private void saveFinalVideo(Movie result, String outputFilePath) {
    try {
      long start = System.currentTimeMillis();
      Utils.createFile(result, outputFilePath);
      long spent = System.currentTimeMillis() - start;
      Log.d("WRITING VIDEO FILE", "time spent in millis: " + spent);
      videoAudioSwapperListener.onExportSuccess();
    } catch (IOException | NullPointerException e) {
      videoAudioSwapperListener.onExportError(String.valueOf(e));
    }
  }

}
