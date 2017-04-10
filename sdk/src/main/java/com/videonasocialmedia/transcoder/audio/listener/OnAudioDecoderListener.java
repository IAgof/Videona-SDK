package com.videonasocialmedia.transcoder.audio.listener;

import com.videonasocialmedia.videonamediaframework.model.media.Media;

/**
 * Created by alvaro on 19/09/16.
 */
public interface OnAudioDecoderListener {
    void onFileDecodedError(String error);
    void onFileDecodedMediaSuccess(Media media, String outputFile);
}
