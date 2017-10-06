package com.videonasocialmedia.transcoder;

/**
 * Created by jliarte on 5/10/17.
 */

public class TranscodingException extends Exception {
  private final String message;

  public TranscodingException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
