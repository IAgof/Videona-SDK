package com.videonasocialmedia.transcoder.audio.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioEffectListener {

  void onAudioEffectSuccess(String outputFile);

  void onAudioEffectProgress(String progress);

  void onAudioEffectError(String error);

  void onAudioEffectCanceled();
}
