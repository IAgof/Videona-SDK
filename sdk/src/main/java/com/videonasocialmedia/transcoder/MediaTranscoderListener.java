package com.videonasocialmedia.transcoder;

import com.videonasocialmedia.videonamediaframework.model.media.Video;

/**
 * Created by alvaro on 15/02/17.
 */

public interface MediaTranscoderListener {
  void onSuccessTranscoding(Video video);
  void onErrorTranscoding(Video video, String message);
}
