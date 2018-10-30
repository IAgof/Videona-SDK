package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.Util;
import com.videonasocialmedia.videonamediaframework.model.media.Music;

import java.io.File;

import static com.videonasocialmedia.videonamediaframework.playback.RendererBuilder.BUFFER_SEGMENT_COUNT;
import static com.videonasocialmedia.videonamediaframework.playback.RendererBuilder.BUFFER_SEGMENT_SIZE;

// TODO(jliarte): 21/11/16 move layout to SDK lib, thus R should be used from there

/**
 * Created by jliarte on 25/08/16.
 */
public class VideonaAudioPlayerExo implements VideonaAudioPlayer{

  private static final String TAG = "VideonaAudioPlayerExo";
  private static final int BUFFER_LENGTH_MIN = 50;
  private static final int REBUFFER_LENGTH_MIN = 100;
  private static final int RENDERER_BUILDING_STATE_IDLE = 1;
  private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
  private static final int RENDERER_BUILDING_STATE_BUILT = 3;
  private int rendererBuildingState;
  private ExoPlayer player;
  private String userAgent;
  private MediaCodecAudioTrackRenderer audioRenderer;
  private int NUM_RENDERERS = 1;

  public VideonaAudioPlayerExo(Context context, Music music) {
    initAudioPlayerComponents(context);
    initClipPreview(context, music);
  }

  private void initAudioPlayerComponents(Context context) {
    player = ExoPlayer.Factory.newInstance(NUM_RENDERERS, BUFFER_LENGTH_MIN, REBUFFER_LENGTH_MIN);
    rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
    userAgent = Util.getUserAgent(context, "VideonaExoAudioPlayer");
  }

  public void initClipPreview(Context context, Music clipToPlay) {
    if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
      player.stop();
    }

    rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
    Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
    DataSource dataSource = new DefaultUriDataSource(context, null, userAgent);
    ExtractorSampleSource sampleSource = new ExtractorSampleSource(
        Uri.fromFile(new File(clipToPlay.getMediaPath())), dataSource, allocator,
        BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
    audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
        MediaCodecSelector.DEFAULT);
    player.prepare(audioRenderer);
    setAudioVolume(clipToPlay.getVolume());
    rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
  }

  public void setAudioVolume(float volume) {
    player.sendMessage(audioRenderer,
        MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
  }

  @Override
  public void playAudio(){
    if (player != null) {
      player.setPlayWhenReady(true);
    }
  }

  @Override
  public void pauseAudio(){
    if (player != null) {
      player.setPlayWhenReady(false);
    }
  }

  @Override
  public void releaseAudio(){
    if (player != null) {
      player.stop();
      player.release();
    }
  }

  @Override
  public void seekAudioTo(long positionMs){
    if (player != null) {
      player.seekTo(positionMs);
    }
  }

  public void stop() {
    if (player != null) {
      player.stop();
    }
  }
}
