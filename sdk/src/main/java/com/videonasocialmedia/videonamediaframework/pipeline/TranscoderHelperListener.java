package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.videonamediaframework.model.media.Video;

/**
 * Created by alvaro on 23/02/17.
 */

public interface TranscoderHelperListener {
  void onSuccessTranscoding(Video video);
  void onErrorTranscoding(Video video, String message);
}
