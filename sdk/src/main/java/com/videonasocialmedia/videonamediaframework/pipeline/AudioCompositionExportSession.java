package com.videonasocialmedia.videonamediaframework.pipeline;

import com.googlecode.mp4parser.authoring.Movie;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import static com.videonasocialmedia.videonamediaframework.muxer.utils.Utils.createFile;

/**
 * Created by jliarte on 29/11/16.
 */

public class AudioCompositionExportSession {
  private final VMComposition avComposition;
  private Appender appender;

  public AudioCompositionExportSession(VMComposition audioComposition) {
    this.avComposition = audioComposition;
    // TODO(jliarte): 29/11/16 inject or pass in the constructor?
    this.appender = new Appender();
  }

  public void exportAsyncronously(String outputPath, ExportSessionListener listener) {
    ArrayList<String> audioPathList = getAudioPathListFromComposition(avComposition);
    try {
      Movie merge = appender.appendVideos(audioPathList, true);
      createFile(merge, outputPath);
      listener.onSuccess();
    } catch (IOException | NullPointerException | NoSuchElementException errorExportingSession) {
      errorExportingSession.printStackTrace();
      listener.onError(String.valueOf(errorExportingSession));
    }
  }

  private ArrayList<String> getAudioPathListFromComposition(VMComposition avComposition) {
    ArrayList<String> audioPathList = new ArrayList<>();
    for (Media mediaItem : avComposition.getAudioTracks().get(0).getItems()) {
      audioPathList.add(mediaItem.getMediaPath());
    }
    return audioPathList;
  }

  public interface ExportSessionListener {
    void onSuccess();
    void onError(String errorMessage);
  }
}
