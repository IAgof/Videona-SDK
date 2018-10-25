/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;

import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

/**
 * Created by alvaro on 18/10/18.
 */

public interface VMCompositionPlayer {
  void attachView(Context context);
  void detachView();
  void setVMCompositionPlayerListener(VMCompositionPlayerListener vmCompositionPlayerListener);
  void init(VMComposition vmComposition);
  void initSingleClip(VMComposition vmComposition, int clipPosition);
  void initSingleVideo(Video video);
  void playPreview();
  void pausePreview();
  //void seekClipToTime(int seekTimeInMsec);
  //void updatePreviewTimeLists();
  void seekTo(int timeInMsec);
  void seekToClip(int position);
  void setSeekBarLayoutEnabled(boolean seekBarEnabled);
  void setAspectRatioVerticalVideos(int height);
  void setImageText(String text, String textPosition, boolean textWithShadow, int width, int height);
  void setVideoVolume(float volume);
  void setVoiceOverVolume(float volume);
  void setMusicVolume(float volume);

  /**
   * Created by jliarte on 13/05/16.
   */
  interface VMCompositionPlayerListener {
    void newClipPlayed(int currentClipIndex);
    void playerReady();
  }
}
