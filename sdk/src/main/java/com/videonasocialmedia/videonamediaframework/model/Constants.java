package com.videonasocialmedia.videonamediaframework.model;

/**
 * Created by jliarte on 18/11/16.
 */

public class Constants {
  public static final String INTERMEDIATE_FILE_PREFIX = "temp_";
  // TODO(jliarte): 21/11/16 make them class fields and let user initialize them? or maybe get
  //                them from the composition resolution?
  public static final String MIXED_AUDIO_FILE_NAME ="AudioMixed.mp4";

  public final static int INDEX_AUDIO_TRACK_MUSIC = 0;
  public final static int INDEX_AUDIO_TRACK_VOICE_OVER = 1;
  public static final int INDEX_MEDIA_TRACK = 2;
}
