/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.ffmpeg;

@SuppressWarnings("unused")
public enum LogLevel {
  NO_LOG(0), ERRORS_ONLY(1), FULL(2);

  private final int integerValue;

  LogLevel(int value) {
    integerValue = value;
  }

  public int getValue() {
    return integerValue;
  }
}
