/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.transcoder.audio;

import java.io.File;

/**
 * Created by alvaro on 13/7/18.
 *
 * Aux class to manage temporal files path generated, audio files extensions and volume.
 *
 */

public class AudioHelper {

  private String PCM_EXTENSION = ".pcm";
  private String WAV_EXTENSION = ".wav";
  private String mediaPath;
  private float volume;
  private String folderTemp;
  private String audioDecodePcm;
  private String audioWav;
  private String audioWavWithVolume;

  public AudioHelper(String mediaPath, float volume, String folderTemp) {
    this.mediaPath = mediaPath;
    this.volume = volume;
    this.folderTemp = folderTemp;
    initAudioTempPath();
  }

  private void initAudioTempPath() {
    setAudioDecodePcm();
    setAudioWav();
    setAudioWavWithVolume();
  }

  private void setAudioDecodePcm() {
    audioDecodePcm = folderTemp + File.separator + "DECODE_" + System.currentTimeMillis()
        + PCM_EXTENSION;
  }

  private void setAudioWav() {
    audioWav = folderTemp + File.separator + "WAV_" + System.currentTimeMillis()
        + WAV_EXTENSION;
  }

  private void setAudioWavWithVolume() {
    audioWavWithVolume = folderTemp + File.separator + "WAV_VOL" + System.currentTimeMillis()
        + WAV_EXTENSION;
  }

  public String getMediaPath() {
    return mediaPath;
  }

  public float getVolume() {
    return volume;
  }

  public String getAudioDecodePcm() {
    return audioDecodePcm;
  }

  public String getAudioWav() {
    return audioWav;
  }

  public String getAudioWavWithVolume() {
    return audioWavWithVolume;
  }
}
