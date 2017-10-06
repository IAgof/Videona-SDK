package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.transcoder.TranscodingException;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by alvaro on 23/10/16.
 */

public class VideoAudioSwapper implements ExporterVideoSwapAudio {
  private static final String LOG_TAG = VideoAudioSwapper.class.getSimpleName();

  @Override
  public void export(String videoFilePath, String newAudioFilePath, String outputFilePath)
          throws IOException, TranscodingException {
    Movie result = getFinalMovie(videoFilePath, newAudioFilePath);
    if (result != null) {
      saveFinalVideo(result, outputFilePath);
    } else {
      throw new TranscodingException("Error swapping video audio track.");
    }
  }

  @Override
  public ListenableFuture<Object> exportAsync(final String videoPath,
                                              final String outputAudioMixedFile,
                                              final String finalVideoExportedFilePath) {
    return MoreExecutors.newDirectExecutorService()
            .submit(new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                export(videoPath, outputAudioMixedFile, finalVideoExportedFilePath);
                return null;
              }
            });
  }

  private Movie getFinalMovie(String videoFilePath, String newAudioFilePath) throws IOException,
          TranscodingException {
    Movie movie = MovieCreator.build(videoFilePath);
    File musicFile = new File(newAudioFilePath);
    if (!musicFile.exists()) {
      throw new TranscodingException("Error swapping video audio track - Music not found.");
    }
    return swapAudio(movie, newAudioFilePath);
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

  private void saveFinalVideo(Movie result, String outputFilePath) throws IOException {
    long start = System.currentTimeMillis();
    Utils.createFile(result, outputFilePath);
    long spent = System.currentTimeMillis() - start;
    Log.d(LOG_TAG, "WRITING VIDEO FILE - time spent in millis: " + spent);
  }

}
