package com.videonasocialmedia.transcoder.audio_mixer.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioMixerListener {

    void onAudioMixerSuccess(String outputFile);
    void onAudioMixerProgress(String progress);
    void onAudioMixerError(String error);
    void onAudioMixerCanceled();
}
