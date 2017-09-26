package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;

import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import java.util.List;

/**
 * Created by jliarte on 13/05/16.
 */
public interface VideonaAudioPlayer {
  void playAudio();
  void pauseAudio();
  void releaseAudio();
  void seekAudioTo(long timeInMs);
}
