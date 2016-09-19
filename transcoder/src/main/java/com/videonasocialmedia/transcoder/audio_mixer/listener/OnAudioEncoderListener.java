package com.videonasocialmedia.transcoder.audio_mixer.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioEncoderListener {

    void OnFileEncodedSuccess(String outputFile);
    void OnFileEncodedError(String error);
}
