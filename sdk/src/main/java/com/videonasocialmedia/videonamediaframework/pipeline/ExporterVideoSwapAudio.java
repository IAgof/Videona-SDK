package com.videonasocialmedia.videonamediaframework.pipeline;

/**
 * Created by alvaro on 25/10/16.
 */
public interface ExporterVideoSwapAudio {
  void export(String videoFilePath, String newAudioFilePath, String outputFilePath);

  /**
   * Created by jca on 27/5/15.
   */
  public interface VideoAudioSwapperListener {
      void onExportError(String error);
      void onExportSuccess();
  }
}
