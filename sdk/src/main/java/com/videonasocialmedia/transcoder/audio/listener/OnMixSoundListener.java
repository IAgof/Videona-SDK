package com.videonasocialmedia.transcoder.audio.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnMixSoundListener {

    void OnMixSoundSuccess(String outputFile);
    void OnMixSoundError(String error);
}