package com.videonasocialmedia.transcoder.audio;


import java.util.UUID;

/**
 * Created by alvaro on 8/03/17.
 */

public class AudioToExport {

  private String mediaPath;
  private float mediaVolume;

  private String mediaDecodeAudioPath;
  private String mediaUUID = UUID.randomUUID().toString();;

  public AudioToExport(String mediaPath, float mediaVolume){
    this.mediaPath = mediaPath;
    this.mediaVolume = mediaVolume;
  }

  public void setMediaDecodeAudioPath(String mediaDecodeAudioPath) {
    this.mediaDecodeAudioPath = mediaDecodeAudioPath;
  }

  public String getMediaDecodeAudioPath() {
    return mediaDecodeAudioPath;
  }

  public String getMediaUUID() {
    return mediaUUID;
  }

  public String getMediaPath() {
    return mediaPath;
  }

  public float getMediaVolume() {
    return mediaVolume;
  }

  public boolean isMediaAudioDecoded(){
    return mediaDecodeAudioPath != null;
  }
}
