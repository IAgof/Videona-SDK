package com.videonasocialmedia.videonamediaframework.pipeline;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.TranscodingException;

import java.io.IOException;

/**
 * Created by alvaro on 25/10/16.
 */
public interface ExporterVideoSwapAudio {
  void export(String videoFilePath, String newAudioFilePath, String outputFilePath)
          throws IOException, TranscodingException;

  ListenableFuture<Object> exportAsync(String videoPath, String outputAudioMixedFile,
                                       String finalVideoExportedFilePath);
}
