package com.videonasocialmedia.transcoder.audio.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioDecoderListener {

    void OnFileDecodedSuccess(String outputFile);
    void OnFileDecodedError(String error);
}
