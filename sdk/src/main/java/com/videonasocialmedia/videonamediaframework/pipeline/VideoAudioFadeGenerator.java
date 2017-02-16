package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alvaro on 23/10/16.
 */

public class VideoAudioFadeGenerator implements OnAudioEffectListener {
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
  protected TranscoderHelper transcoderHelper = new TranscoderHelper(mediaTranscoder);
  private VideoAudioFadeListener listener;

  String tempFileAudio;
  String tempDirectoryFilesAudio;

  public VideoAudioFadeGenerator(VideoAudioFadeListener listener,
                                 String tempDirectory) {
    this.tempDirectoryFilesAudio = tempDirectory;
    // TODO(jliarte): 14/02/17  Extract AudioFadeInOut_ to a constant to unify name in all three classes that use it
    this.tempFileAudio = tempDirectory + File.separator + "AudioFadeInOut_"
        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".m4a";
    this.listener = listener;
  }

  public void getAudioFadeInFadeOutFromVideo(String videoToEditPath, int timeFadeInMs,
                                             int timeFadeOutMs) throws IOException {
    transcoderHelper.generateFileWithAudioFadeInFadeOut(videoToEditPath, timeFadeInMs,
        timeFadeOutMs, tempDirectoryFilesAudio, tempFileAudio, this);
  }

  @Override
  public void onAudioEffectSuccess(String outputFile) {
    listener.onGetAudioFadeInFadeOutFromVideoSuccess(outputFile);
  }

  @Override
  public void onAudioEffectProgress(String progress) {
  }

  @Override
  public void onAudioEffectError(String error) {
    listener.onGetAudioFadeInFadeOutFromVideoError(error);
  }

  @Override
  public void onAudioEffectCanceled() {
    listener.onGetAudioFadeInFadeOutFromVideoError("canceled");
  }

  /**
   * Created by alvaro on 23/10/16.
   */
  public static interface VideoAudioFadeListener {
      void onGetAudioFadeInFadeOutFromVideoSuccess(String audioFile);
      void onGetAudioFadeInFadeOutFromVideoError(String message);
  }
}
