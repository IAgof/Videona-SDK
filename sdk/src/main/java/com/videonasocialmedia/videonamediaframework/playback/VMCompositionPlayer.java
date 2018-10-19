/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;

import com.videonasocialmedia.videonamediaframework.model.VMComposition;

/**
 * Created by alvaro on 18/10/18.
 */

public interface VMCompositionPlayer {
  void initComponents(Context context);
  //void destroyPlayer();
  //void onPause();
  void setListener(VMCompositionPlayerListener vmCompositionPlayerListener);
  void init(VMComposition vmComposition);
  void initSingleClip(VMComposition vmComposition, int clipPosition);
  void playPreview();
  void pausePreview();
  //void seekClipToTime(int seekTimeInMsec);
  //void updatePreviewTimeLists();
  void seekTo(int timeInMsec);
  void seekToClip(int position);
  int getCurrentPosition();
  void setSeekBarLayoutEnabled(boolean seekBarEnabled);

  /**
   * Created by jliarte on 13/05/16.
   */
  interface VMCompositionPlayerListener {
    void newClipPlayed(int currentClipIndex);
    void playerReady();
  }
}
