package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.MediaTranscoder.MediaTranscoderListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alvaro on 23/10/16.
 */

public class VideoAudioFadeGenerator {

  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
  protected TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);
  private VideoAudioFadeListener listener;

  String tempFileAudio;
  String tempDirectoryFilesAudio;

  public VideoAudioFadeGenerator(VideoAudioFadeListener listener,
                                 String tempDirectory) {
    this.tempDirectoryFilesAudio = tempDirectory;
    this.tempFileAudio = tempDirectory + File.separator + "AudioFadeInOut_"
        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".m4a";
    this.listener = listener;
  }

  public void getAudioFadeInFadeOutFromVideo(String videoToEditPath, int timeFadeInMs,
                                             int timeFadeOutMs) throws IOException {
    transcoderHelper.generateFileWithAudioFadeInFadeOutAsync(videoToEditPath, timeFadeInMs,
            timeFadeOutMs, tempDirectoryFilesAudio, tempFileAudio, new MediaTranscoderListener() {
              @Override
              public void onTranscodeSuccess(String outputFile) {
                listener.onGetAudioFadeInFadeOutFromVideoSuccess(outputFile);
              }

              @Override
              public void onTranscodeProgress(String progress) {
              }

              @Override
              public void onTranscodeError(String error) {
                listener.onGetAudioFadeInFadeOutFromVideoError(error);
              }

              @Override
              public void onTranscodeCanceled() {
                listener.onGetAudioFadeInFadeOutFromVideoError("canceled");
              }
            });
  }

  /**
   * Created by alvaro on 23/10/16.
   */
  public interface VideoAudioFadeListener {
      void onGetAudioFadeInFadeOutFromVideoSuccess(String audioFile);
      void onGetAudioFadeInFadeOutFromVideoError(String message);
  }
}
