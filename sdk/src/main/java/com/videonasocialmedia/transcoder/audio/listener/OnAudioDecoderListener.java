package com.videonasocialmedia.transcoder.audio.listener;

import com.videonasocialmedia.transcoder.audio.AudioToExport;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioDecoderListener {

    void OnFileDecodedError(String error);
    void onFileDecodedMediaSuccess(AudioToExport media, String outputFile);
    void OnFileDecodedSuccess(String outputFile);
}
