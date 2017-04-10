package com.videonasocialmedia.transcoder.audio.listener;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioEncoderListener {
    void OnFileEncodedSuccess(String outputFile);
    void OnFileEncodedError(String error);
    void OnFileDecodedSuccess(String outputFile);
}
