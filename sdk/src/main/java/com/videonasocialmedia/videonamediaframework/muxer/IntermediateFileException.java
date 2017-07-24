package com.videonasocialmedia.videonamediaframework.muxer;

/**
 * Created by jliarte on 21/07/17.
 */

public class IntermediateFileException extends Throwable {
  private final String videoPath;

  private final int videoIndex;

  public IntermediateFileException(String videoPath, int videoIndex) {
    this.videoPath = videoPath;
    this.videoIndex = videoIndex;
  }

  public String getMediaPath() {
    return videoPath;
  }

  public int getVideoIndex() {
    return videoIndex;
  }
}
