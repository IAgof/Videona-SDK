/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.ffmpeg;

public interface ProcessingListener {
  void onSuccess(String path);

  void onFailure(int returnCode);
}
