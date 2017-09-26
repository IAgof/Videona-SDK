package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;



import java.io.IOException;


public class RendererBuilder implements ExtractorSampleSource.EventListener,
        MediaCodecVideoTrackRenderer.EventListener, MediaCodecAudioTrackRenderer.EventListener {
  public static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  public static final int BUFFER_SEGMENT_COUNT = 256;
  public static final int RENDERER_COUNT = 3;

  private final static String TAG = "RendererBuilder";

  private final Context context;
  private final String userAgent;
  private MediaCodecAudioTrackRenderer audioRenderer;
  private MediaCodecVideoTrackRenderer videoRenderer;

  public MediaCodecAudioTrackRenderer getAudioRenderer() {
    return audioRenderer;
  }


  public RendererBuilder(Context context, String userAgent) {
    this.context = context;
    this.userAgent = userAgent;
  }

  protected void buildRenderers(RendererBuilderListener playerListener, Uri videoUri,
                                Handler playerHandler) {
    final long startTime = System.currentTimeMillis();

    Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

    // Build the video and audio renderers.
    DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(playerHandler, null);
      DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
      ExtractorSampleSource sampleSource = new ExtractorSampleSource(videoUri, dataSource,
          allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE, playerHandler, this, 0);
      videoRenderer = new MediaCodecVideoTrackRenderer(context,
          sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
          5000, playerHandler, this, 50);
      audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
          MediaCodecSelector.DEFAULT, null, true, playerHandler, this,
          AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

    // (jliarte): 1/09/16 maybe it's easier with the traditional method using audio manager,
    //      as here we would to advance the audio track to the current position in timeline
    //      on each clip played
    //            if (musicTrack != null) {
    //                MediaCodecAudioTrackRenderer musicTrackRenderer =
    //                    new MediaCodecAudioTrackRenderer(musicSource,
    //                        MediaCodecSelector.DEFAULT, null)
    //            }

    // Invoke the callback.
    TrackRenderer[] renderers = new TrackRenderer[RENDERER_COUNT];
    renderers[VideonaPlayerExo.TYPE_VIDEO] = videoRenderer;
    renderers[VideonaPlayerExo.TYPE_AUDIO] = audioRenderer;
    playerListener.onRenderers(renderers, bandwidthMeter);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    Log.d(TAG, "----------- time spent building renderers: " + elapsedTime);
  }

  public void cancel() {
    // Do nothing.
  }

  /**
   * ExtractorSampleSource.EventListener methods
   */

  @Override
  public void onLoadError(int sourceId, IOException e) {

  }

  /**
   * MediaCodecVideoTrackRenderer.EventListener methods
   */

  @Override
  public void onDroppedFrames(int count, long elapsed) {

  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

  }

  @Override
  public void onDrawnToSurface(Surface surface) {

  }

  @Override
  public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {

  }

  @Override
  public void onCryptoError(MediaCodec.CryptoException e) {

  }

  @Override
  public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {

  }

  /**
   * MediaCodecAudioTrackRenderer.EventListener methods
   */

  @Override
  public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {

  }

  @Override
  public void onAudioTrackWriteError(AudioTrack.WriteException e) {

  }

  @Override
  public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

  }

  public interface RendererBuilderListener {
    void onRenderers(TrackRenderer[] renderers, DefaultBandwidthMeter bandwidthMeter);
  }
}
