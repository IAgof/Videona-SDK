package com.videonasocialmedia.videonamediaframework.playback;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Range;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.Util;



import com.videonasocialmedia.sdk.R;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.playback.customviews.AspectRatioVideoView;
import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;
import com.videonasocialmedia.videonamediaframework.utils.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// TODO(jliarte): 21/11/16 move layout to SDK lib, thus R should be used from there

/**
 * Created by jliarte on 25/08/16.
 */
public class VideonaPlayerExo extends RelativeLayout implements VMCompositionPlayer,
    SeekBar.OnSeekBarChangeListener, ExoPlayer.Listener, RendererBuilder.RendererBuilderListener {
  private static final String TAG = "VideonaPlayerExo";
  private static final int BUFFER_LENGTH_MIN = 50;
  private static final int REBUFFER_LENGTH_MIN = 100;
  protected static final int TYPE_VIDEO = 0;
  protected static final int TYPE_AUDIO = 1;
  protected static final int TYPE_TEXT = 2;
  protected static final int TYPE_METADATA = 3;
  private static final int DISABLED_TRACK = -1;

  private static final int RENDERER_BUILDING_STATE_IDLE = 1;
  private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
  private static final int RENDERER_BUILDING_STATE_BUILT = 3;
  private static final int TIME_TRANSITION_FADE = 500; // 500ms
  private final TextToDrawable drawableGenerator;

  AspectRatioVideoView videoPreview;
  SeekBar seekBar;
  ImageButton playButton;
  ImageView imageTransitionFade;
  ImageView imageTextPreview;
  TextView textTimeCurrentSeekbar;
  TextView textTimeProjectSeekbar;
  LinearLayout seekBarLayout;

  private final View videonaPlayerView;
  private VMCompositionPlayer.VMCompositionPlayerListener vmCompositionPlayerListener;
  private ExoPlayer player;
  private RendererBuilder rendererBuilder;
  private TrackRenderer videoRenderer;
  private CodecCounters codecCounters;
  private BandwidthMeter bandwidthMeter;
  private Surface surface;

  private VideonaAudioPlayerExo musicPlayer;
  private VideonaAudioPlayerExo voiceOverPlayer;
  private List<Video> videoList;
  private int currentClipIndex = 0;
  private int currentTimePositionInList = 0;
  private int totalVideoDuration = 0;
  private int rendererBuildingState;
  // TODO:(alvaro.martinez) 23/11/16 Get this functionality compatible with API 18 to downgrade API module from 21 to 18
  private List<Range> clipTimesRanges;
  private Handler mainHandler;
  private Handler seekBarUpdaterHandler = new Handler();

  private final Runnable updateTimeTask = new Runnable() {
    @Override
    public void run() {
      try {
        updateSeekBarProgress();
      } catch (Exception exception) {
        Log.d(TAG, "Exception in update seekbar progress thread");
        Log.d(TAG, String.valueOf(exception));
      }
    }
  };

  private ValueAnimator outAnimator;
  private ValueAnimator inAnimator;
  private String userAgent;
  private TrackRenderer[] nextClipRenderers;
  private boolean isSetVideoTransitionFadeActivated = false;
  private boolean isSetAudioTransitionFadeActivated = false;
  private boolean isInAnimatorLaunched;
  private boolean isOutAnimatorLaunched;
  private float videoVolume;

  /**
   * Default constructor.
   *
   * @param context view context
   */
  public VideonaPlayerExo(Context context) {
    super(context);
    drawableGenerator = new TextToDrawable(context);
    this.videonaPlayerView = ((Activity) getContext()).getLayoutInflater()
        .inflate(R.layout.video_preview, this, true);
    initLayoutsComponents();
    initVideonaPlayerComponents(context);
  }

  /**
   * Constructor with attributes.
   *
   * @param context view context
   * @param attrs   view attributes
   */
  public VideonaPlayerExo(Context context, AttributeSet attrs) {
    super(context, attrs);
    drawableGenerator = new TextToDrawable(context);
    this.videonaPlayerView = ((Activity) getContext()).getLayoutInflater()
        .inflate(R.layout.video_preview, this, true);
    initLayoutsComponents();
    initVideonaPlayerComponents(context);
  }

  /**
   * Contructor with attributes and style.
   *
   * @param context  view context
   * @param attrs    view attributes
   * @param defStyle view style
   */
  public VideonaPlayerExo(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    drawableGenerator = new TextToDrawable(context);
    this.videonaPlayerView = ((Activity) getContext()).getLayoutInflater()
        .inflate(R.layout.video_preview, this, true);
    initLayoutsComponents();
    initVideonaPlayerComponents(context);
  }

  private void initLayoutsComponents() {
    videoPreview = (AspectRatioVideoView) findViewById(R.id.video_editor_preview);
    videoPreview.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        onTouchPreview(motionEvent);
        return false;
      }
    });
    seekBar = (SeekBar) findViewById(R.id.seekbar_editor_preview);
    playButton = (ImageButton) findViewById(R.id.button_editor_play_pause);
    playButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        onClickPlayPauseButton();
      }
    });
    imageTextPreview = (ImageView) findViewById(R.id.image_text_preview);
    imageTransitionFade = (ImageView) findViewById(R.id.image_transition_fade);
    textTimeCurrentSeekbar = (TextView) findViewById(R.id.video_view_time_current);
    textTimeProjectSeekbar = (TextView) findViewById(R.id.video_view_time_project);
    seekBarLayout = (LinearLayout) findViewById(R.id.video_view_seekbar_layout);
  }

  /**
   * Common VideonaPlayerExo initialization code called in constructors.
   * Creates mainHandler, initializes exoplayer without videos and audio manager
   */
  private void initVideonaPlayerComponents(Context context) {
    mainHandler = new Handler();
    player = ExoPlayer.Factory.newInstance(RendererBuilder.RENDERER_COUNT, BUFFER_LENGTH_MIN, REBUFFER_LENGTH_MIN);
    player.addListener(this);
    player.setSelectedTrack(TYPE_TEXT, DISABLED_TRACK);
    surface = videoPreview.getHolder().getSurface();
    rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
    userAgent = Util.getUserAgent(context, "VideonaExoPlayer");
    rendererBuilder = new RendererBuilder(context, userAgent);

    // TODO(jliarte): 1/09/16 instantiate field when need to adjust volume
    //        AudioManager audioManager = (AudioManager) context.getApplicationContext()
    //                .getSystemService(Context.AUDIO_SERVICE);
    initClipList();
    initSeekBar();
    clipTimesRanges = new ArrayList<>();
    setupInAnimator();
    setupOutAnimator();
  }

  private void updateSeekBarProgress() {
    if (player != null && isPlaying()) {
      try {
        if (currentClipIndex() < videoList.size()) {
          int progress = ((int) player.getCurrentPosition())
              + (int) clipTimesRanges.get(currentClipIndex()).getLower()
              - videoList.get(currentClipIndex()).getStartTime();
          setSeekBarProgress(progress);
          currentTimePositionInList = progress;
          previewAVTransitions(progress);

          // detect end of trimming and play next clip or stop
          if (isCurrentClipEnded()) {
            stopPlayer();
            // (jliarte): 18/07/17 if a clip duration is reported greater than actual, next clip
            // was never been played
            playNextClip();
          }
        }
      } catch (Exception exception) {
        Log.d(TAG, "updateSeekBarProgress: exception updating videonaPlayer seekbar");
        Log.d(TAG, String.valueOf(exception));
      }

      if (seekBar.getProgress() >= (int) clipTimesRanges.get(clipTimesRanges.size()-1).getUpper()) {
        pausePreview();
      }
      if (isPlaying()) {
        seekBarUpdaterHandler.postDelayed(updateTimeTask, 20);
      }
    }
  }

  /**
   * VMCompositionPlayer methods starts.
   **/

  @Override
  public void attachView(Context context) {
    if (player == null) {
      initVideonaPlayerComponents(context);
    }
  }

  // TODO: 18/10/18 Study if it is needed @Override
  private void destroyPlayer() {
    seekBarUpdaterHandler.removeCallbacksAndMessages(null);
    mainHandler.removeCallbacksAndMessages(null);
  }


  @Override
  public void detachView() {
    pausePreview();
    releasePlayerView();
  }

  @Override
  public void setListener(VMCompositionPlayerListener vmCompositionPlayerListener) {
    this.vmCompositionPlayerListener = vmCompositionPlayerListener;
  }

  @Override
  public void init(VMComposition vmComposition) {
    if (vmComposition.hasVideos()) {
      List videoList = vmComposition.getMediaTrack().getItems();
      bindVideoList(videoList);
    }
    if (vmComposition.hasMusic()) {
      musicPlayer = new VideonaAudioPlayerExo(getContext(), vmComposition.getMusic());
    }
    if (vmComposition.hasVoiceOver()) {
      voiceOverPlayer = new VideonaAudioPlayerExo(getContext(), vmComposition.getVoiceOver());
    }
  }

  @Override
  public void initSingleClip(VMComposition vmComposition, int clipPosition) {
    if (vmComposition.hasVideos()) {
      List<Video> videoList = new ArrayList();
      videoList.add((Video) vmComposition.getMediaTrack().getItems().get(clipPosition));
      bindVideoList(videoList);
    }
    if (vmComposition.hasMusic()) {
      musicPlayer = new VideonaAudioPlayerExo(getContext(), vmComposition.getMusic());
    }
    if (vmComposition.hasVoiceOver()) {
      voiceOverPlayer = new VideonaAudioPlayerExo(getContext(), vmComposition.getVoiceOver());
    }
  }

  @Override
  public void playPreview() {
    playPlayer();
    showPauseButton();
    seekBarUpdaterHandler.postDelayed(updateTimeTask, 20);
  }

  @Override
  public void pausePreview() {
    pausePlayer();
    showPlayButton();
    seekBarUpdaterHandler.removeCallbacksAndMessages(null);
  }

  // TODO: 18/10/18 Study if it is needed @Override
  private void seekClipToTime(int seekTimeInMsec) {
    if (player != null) {
      currentTimePositionInList = seekTimeInMsec
          - videoList.get(currentClipIndex()).getStartTime()
          + (int) clipTimesRanges.get(currentClipIndex()).getLower();
      setSeekBarProgress(currentTimePositionInList);
      player.seekTo(seekTimeInMsec);
      seekTo(currentTimePositionInList);
    }
  }

  // TODO: 18/10/18 Study if it is needed @Override
  private void updatePreviewTimeLists() {
    totalVideoDuration = 0;
    clipTimesRanges.clear();
    for (Video clip : videoList) {
      if (videoList.indexOf(clip) != 0) {
        totalVideoDuration++;
      }
      int clipStartTime = this.totalVideoDuration;
      int clipStopTime = this.totalVideoDuration + clip.getDuration();
      clipTimesRanges.add(new Range(clipStartTime, clipStopTime));
      this.totalVideoDuration = clipStopTime;
    }
    updateUiVideoDuration(totalVideoDuration);
  }

  @Override
  public void seekTo(int seekTimeInMsec) {
    if (player != null && seekTimeInMsec < totalVideoDuration) {
      currentTimePositionInList = seekTimeInMsec;
      setSeekBarProgress(currentTimePositionInList);
      int clipIndex = getClipIndexByProgress(currentTimePositionInList);
      if (currentClipIndex() != clipIndex) {
        // (jliarte): 26/04/17 moved into seekToClip
//        clearNextBufferedClip();
        seekToClip(clipIndex);
      } else {
        player.seekTo(getClipPositionFromTimeLineTime());
        if (videoHasMusic()) {
          seekAudioTo(currentTimePositionInList);
        }
      }
    }
  }

  @Override
  public void seekToClip(int position) {
    Log.d(TAG, "onClipClicked: " + position);
    pausePreview();
    clearNextBufferedClip();
    currentClipIndex = position;
    // TODO(jliarte): 6/09/16 (hot)fix for Pablo's Index out of bonds
    if (position >= videoList.size() && position >= clipTimesRanges.size()) {
      position = 0;
    }
    if (videoList.size() == 0) {
      return;
    }
    currentTimePositionInList = (int) clipTimesRanges.get(currentClipIndex()).getLower();
    initClipPreview(videoList.get(position));
    setSeekBarProgress(currentTimePositionInList);
    if (videoHasMusic()) {
      seekAudioTo(currentTimePositionInList);
    }
  }

  @Override
  public int getCurrentPosition() {
    return currentTimePositionInList;
  }

  @Override
  public void setSeekBarLayoutEnabled(boolean seekBarEnabled) {
    if (seekBarEnabled) {
      seekBarLayout.setVisibility(VISIBLE);
    } else {
      seekBarLayout.setVisibility(GONE);
    }
  }

  @Override
  public void setAspectRatioVerticalVideos(int height) {
    LayoutParams params = (LayoutParams) videoPreview.getLayoutParams();
    int heightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        height, getResources().getDisplayMetrics());
    double aspectRatio = 0.5625D;
    int widthDp = (int) (heightDp * aspectRatio);
    params.height = heightDp;
    params.width = widthDp;
    videoPreview.setLayoutParams(params);
    videoPreview.setAspectRatio(aspectRatio);
  }

  /**
   * VMCompositionPlayer methods ends.
   **/

  private void releasePlayerView() {
    if (player != null) {
      currentTimePositionInList = getCurrentPosition();
      player.release();
      player = null;
      clearNextBufferedClip();
    }
    if (videoHasMusic()) {
      releaseAudio();
    }
  }

  private void bindVideoList(List<Video> videoList) {
    initPreviewLists(videoList);
    initPreview(currentTimePositionInList);
  }

  private void initPreviewLists(List<Video> videoList) {
    this.videoList = videoList;
    updatePreviewTimeLists();
    setSeekBarTotalVideoDuration();
  }

  private void setSeekBarTotalVideoDuration() {
    seekBar.setMax(totalVideoDuration);
    updateTextProjectDuration(totalVideoDuration);
  }

  private void updateTextProjectDuration(int totalProjectDuration) {
    textTimeProjectSeekbar.setText(
        TimeUtils.toFormattedTimeHoursMinutesSecond(totalProjectDuration));
  }

  private void initPreview(int instantTime) {
    if (playerHasVideos()) {
      this.currentTimePositionInList = instantTime;
      setSeekBarProgress(currentTimePositionInList);
      Video clipToPlay = videoList.get(getClipIndexByProgress(this.currentTimePositionInList));
      videoVolume = clipToPlay.getVolume();
      initClipPreview(clipToPlay);
    }
  }

  private void setSeekBarProgress(int progress) {
    seekBar.setProgress(progress);
    textTimeCurrentSeekbar.setText(TimeUtils.toFormattedTimeHoursMinutesSecond(progress));
  }

  private void playPlayer() {
    if (player != null) {
      player.setPlayWhenReady(true);
    }
    if (videoHasMusic()) {
      if (musicPlayer != null) {
        musicPlayer.playAudio();
      }
      if (voiceOverPlayer != null) {
        voiceOverPlayer.playAudio();
      }
    }
  }

  private void pausePlayer() {
    if (player != null) {
      player.setPlayWhenReady(false);
    }
    if (videoHasMusic()) {
      if (musicPlayer != null) {
        musicPlayer.pauseAudio();
      }
      if (voiceOverPlayer != null) {
        voiceOverPlayer.pauseAudio();
      }
    }
  }

  private void showPlayButton() {
    playButton.setImageResource(R.drawable.common_icon_play_normal);
  }

  private void showPauseButton() {
    playButton.setImageResource(R.drawable.common_icon_pause_normal);
  }

  private void setBlackBackgroundColor() {
    videoPreview.setBackgroundColor(Color.BLACK);
  }

  private void clearImageText() {
    imageTextPreview.setImageDrawable(null);
  }

  private void previewAVTransitions(int progress) {
    if (isSetVideoTransitionFadeActivated || isSetAudioTransitionFadeActivated) {
      if (isPlaying() && shouldLaunchStartTransition(progress)) {
        if (!isInAnimatorLaunched) {
          Log.d(TAG, "updateSeekBarProgress inAnimator start ");
          isInAnimatorLaunched = true;
          inAnimator.start();
        }
      }
      if (isPlaying() && shouldLaunchEndTransition(progress)) {
        if (!isOutAnimatorLaunched) {
          Log.d(TAG, "updateSeekBarProgress outAnimator start ");
          isOutAnimatorLaunched = true;
          outAnimator.start();
        }
      }
    }
  }

  private boolean isCurrentClipEnded() {
    return seekBar.getProgress() >= (int) clipTimesRanges.get(currentClipIndex()).getUpper();
  }

  private boolean shouldLaunchEndTransition(int seekBarProgress) {
    int timeEndClip = (int) clipTimesRanges.get(currentClipIndex()).getUpper();
    return (seekBarProgress < timeEndClip && seekBarProgress > (timeEndClip - TIME_TRANSITION_FADE) );
  }

  private boolean shouldLaunchStartTransition(int seekBarProgress) {
    int timeStartLastClip;
    if (currentClipIndex() > 0) {
      timeStartLastClip = (int) clipTimesRanges.get(currentClipIndex()-1).getUpper();
    } else {
      timeStartLastClip = 0;
    }
    return ((seekBarProgress > timeStartLastClip) && (seekBarProgress < (timeStartLastClip + TIME_TRANSITION_FADE)));
  }

  private void initClipList() {
    this.videoList = new ArrayList<>();
  }

  private void initSeekBar() {
    seekBar.setProgress(0);
    seekBar.setOnSeekBarChangeListener(this);
  }

  private void initClipPreview(Video clipToPlay) {
    // Init video player
    if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
      stopPlayer();
    }
    rendererBuilder.cancel();
    videoRenderer = null;
    if (nextClipRenderers == null) {
      rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
      maybeReportPlayerState();
      rendererBuilder.buildRenderers(this, Uri.fromFile(new File(clipToPlay.getMediaPath())),
            this.getMainHandler());
    } else {
      onRenderers(nextClipRenderers, (DefaultBandwidthMeter) bandwidthMeter);
    }

    // init Music Players
    if (videoHasMusic()) {
      seekAudioTo(currentTimePositionInList);
    }

  }

  private void stopPlayer() {
    if (player != null) {
      player.stop();
    }
  }

  private boolean playerHasVideos() {
    return this.videoList.size() > 0;
  }

  private int getClipIndexByProgress(int progress) {
    for (int i = 0; i < clipTimesRanges.size(); i++) {
      if (clipTimesRanges.get(i).contains(progress)) {
        return i;
      }
    }
    return 0;
  }

  private void maybeReportPlayerState() {
    // TODO(jliarte): 31/08/16 is this necessary?
    //        boolean playWhenReady = player.getPlayWhenReady();
    //        int playbackState = getPlaybackState();
    //        if (lastReportedPlayWhenReady != playWhenReady
    //                || lastReportedPlaybackState != playbackState) {
    //            for (Listener listener : listeners) {
    //                listener.onStateChanged(playWhenReady, playbackState);
    //            }
    //            lastReportedPlayWhenReady = playWhenReady;
    //            lastReportedPlaybackState = playbackState;
    //        }
  }

  private void updateUiVideoDuration(int totalVideoDuration){
    textTimeProjectSeekbar.setText(
        TimeUtils.toFormattedTimeHoursMinutesSecond(totalVideoDuration));
    seekBar.setMax(totalVideoDuration);
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void setVideoVolume(float volume) {
    if (player != null) {
      player.sendMessage(rendererBuilder.getAudioRenderer(),
              MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
    }
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void setMusicVolume(float volume) {
    if (musicPlayer != null) {
      musicPlayer.setAudioVolume(volume);
    }
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void setVoiceOverVolume(float volume) {
    if (voiceOverPlayer != null) {
      voiceOverPlayer.setAudioVolume(volume);
    }
  }

  private void clearNextBufferedClip() {
    nextClipRenderers = null;
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void releaseAudio() {
    if (musicPlayer != null) {
      musicPlayer.releaseAudio();
      musicPlayer = null;
    }
    if (voiceOverPlayer != null) {
      voiceOverPlayer.releaseAudio();
      voiceOverPlayer = null;
    }
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void seekAudioTo(long timeInMs) {
    if (musicPlayer != null) {
      musicPlayer.seekAudioTo(timeInMs);
    }
    if (voiceOverPlayer != null) {
      voiceOverPlayer.seekAudioTo(timeInMs);
    }
  }

  private boolean videoHasMusic() {
    return musicPlayer != null || voiceOverPlayer != null;
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  public void setVideoTransitionFade() {
    isSetVideoTransitionFadeActivated = true;
  }


  // TODO: 18/10/18 Move to VMCompositionPlayer   @Override
  public void setAudioTransitionFade() {
    isSetAudioTransitionFadeActivated = true;
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  private void setMusic(Music music) {
    musicPlayer = new VideonaAudioPlayerExo(getContext(), music);
  }

  // TODO: 18/10/18 Move to VMCompositionPlayer @Override
  private void setVoiceOver(Music voiceOver) {
    voiceOverPlayer = new VideonaAudioPlayerExo(getContext(), voiceOver);
  }

  /**
   * Sets text and text position for video preview.
   *
   * @param text         the text to render
   * @param textPosition the text position
   */
  private void setImageText(String text, String textPosition, boolean textShadow, int width,
                           int height) {
    Drawable textDrawable = drawableGenerator.createDrawableWithTextAndPosition(
        text, textPosition, textShadow, width, height);
    imageTextPreview.setImageDrawable(textDrawable);
  }

  private void setupInAnimator() {
    inAnimator = ValueAnimator.ofFloat(1f, 0f);
    inAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();
        Log.d(TAG, "setupInAnimator value " + value);
        if (isSetVideoTransitionFadeActivated) {
          imageTransitionFade.setAlpha(value);
        }
        if (isSetAudioTransitionFadeActivated) {
          setVideoVolume(value * videoVolume);
        }
      }
    });

    inAnimator.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {
        if (imageTransitionFade.getVisibility() == INVISIBLE) {
            imageTransitionFade.setVisibility(VISIBLE);
        }
      }

      @Override
      public void onAnimationEnd(Animator animation) {
          isInAnimatorLaunched = false;
        Log.d(TAG, "AnimatorListener onAnimationEnd ");
      }

      @Override
      public void onAnimationCancel(Animator animation) {

      }

      @Override
      public void onAnimationRepeat(Animator animation) {

      }
    });

    inAnimator.setDuration(TIME_TRANSITION_FADE);
    inAnimator.setInterpolator(new LinearInterpolator());
    inAnimator.setRepeatCount(0);
  }

  private void setupOutAnimator() {
    outAnimator = ValueAnimator.ofFloat(0f, 1f);
    outAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();
        Log.d(TAG, "setupOutAnimator value " + value);
        if(isSetVideoTransitionFadeActivated)
          imageTransitionFade.setAlpha(value);
        if(isSetAudioTransitionFadeActivated)
          setVideoVolume(value*videoVolume);
      }
    });
    outAnimator.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        isOutAnimatorLaunched = false;
        Log.d(TAG, "AnimatorListener onAnimationEnd ");
      }

      @Override
      public void onAnimationCancel(Animator animation) {

      }

      @Override
      public void onAnimationRepeat(Animator animation) {

      }
    });

    outAnimator.setDuration(TIME_TRANSITION_FADE);
    outAnimator.setInterpolator(new LinearInterpolator());
    outAnimator.setRepeatCount(0);
  }

  /***
   * End of VideonaPlayer methods.
   ***/

  private void onClickPlayPauseButton() {
    if (playerHasVideos()) {
      if (isPlaying()) {
        pausePreview();
      } else {
        playPreview();
      }
    }
  }

  /**
   * Touch event listener for video preview.
   *
   * @param event received @{@link MotionEvent}
   * @return true if event has beeen processed
   */
  private boolean onTouchPreview(MotionEvent event) {
    boolean eventProcessed = false;
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      onClickPlayPauseButton();
      eventProcessed = true;
    }
    return eventProcessed;
  }

  /***
   * Seekbar listener methods.
   ***/

  @Override
  public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
    if (fromUser) {
      if (isPlaying()) {
        pausePreview();
      }
      if (playerHasVideos()) {
        showPauseButton();
        seekTo(progress);
        notifyNewClipPlayed();
      } else {
        setSeekBarProgress(0);
      }
    }
  }

  private boolean isPlaying() {
    if (player == null) {
      return false;
    } else {
      return player.getPlayWhenReady();
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    showPlayButton();
  }

  /***
   * end of Seekbar listener methods.
   ***/

  private void playNextClip() {
    currentClipIndex++;
    if (hasNextClipToPlay()) {
      Video video = videoList.get(currentClipIndex());
      videoVolume = video.getVolume();
      currentTimePositionInList = (int) clipTimesRanges.get(currentClipIndex()).getLower();
      initClipPreview(video);
    } else {
      pausePreview();
      clearNextBufferedClip();
      seekToClip(0);
      // avoid black frame, imageTransitionFade over player
      imageTransitionFade.setVisibility(INVISIBLE);
    }
    notifyNewClipPlayed();
  }

  private boolean hasNextClipToPlay() {
    return currentClipIndex < videoList.size();
  }

  private void notifyNewClipPlayed() {
    if (vmCompositionPlayerListener != null) {
      vmCompositionPlayerListener.newClipPlayed(currentClipIndex());
    }
  }

  /***
   * exo player listener.
   ***/

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    Log.d(TAG, "Playwhenready: " + playWhenReady + " state: " + playbackState);
    clearImageText();
    switch (playbackState) {
      case ExoPlayer.STATE_BUFFERING: // state 3
        break;
      case ExoPlayer.STATE_ENDED: // state 5
        clearNextBufferedClip();
        if (playWhenReady) {
          // (jliarte): 14/10/16 as playNextClip() was called from both here and
          // updateSeekbarProgress thread, sometimes it's called twice in a clip end, causing
          // a double currentClipIndex++ and thus skipping a clip
          // (jliarte): 18/07/17 recovered again playNextClip here as some clips duration is
          // reported less than actual causing VideonaPlayer not reaching end of clips, now we stop
          // the player if end of clip is detected and in both cases we end up here!
//                    playNextClip();
        }
        break;
      case ExoPlayer.STATE_IDLE:
        break;
      case ExoPlayer.STATE_PREPARING:
        break;
      case ExoPlayer.STATE_READY: // state 4
        updateClipTextPreview();
        if (playWhenReady) {
          // player.seekAudioTo(getClipPositionFromTimeLineTime());
        }
        notifyPlayerReady();
        break;
      default:
        break;
    }
  }

  private void notifyPlayerReady() {
   if (vmCompositionPlayerListener != null) {
     vmCompositionPlayerListener.playerReady();
   }
  }

  /**
   * Renders text if has been set for current clip.
   */
  private void updateClipTextPreview() {
    if (videoList.size() > 0 && getCurrentClip().hasText()) {
      setImageText(getCurrentClip().getClipText(), getCurrentClip().getClipTextPosition(),
          getCurrentClip().hasClipTextShadow(), videoPreview.getMeasuredWidth(),
          videoPreview.getHeight());
    } else {
      clearImageText();
    }
  }

  private int currentClipIndex() {
    if (currentClipIndex >= videoList.size()) {
      return 0;
    }
    return currentClipIndex;
  }

  private Video getCurrentClip() {
    if (videoList.size() > 0) {
      return videoList.get(currentClipIndex());
    }
    return null;
  }

  /**
   * Sets video preview mute status.
   *
   * @param shouldMute true if preview should mute
   */
  private void muteVideo(boolean shouldMute) {
       /* // TODO(jliarte): 1/09/16 test mute
        if (player != null)
            if (shouldMute) {
                musicVolume = 0f;
                player.sendMessage(rendererBuilder.getAudioRenderer(),
                                   MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,musicVolume);
            } else {
                musicVolume = DEFAULT_VOLUME;
                player.sendMessage(rendererBuilder.getAudioRenderer(),
                                   MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,DEFAULT_VOLUME);
            }
            */
  }


  @Override
  public void onPlayWhenReadyCommitted() {
  }

  @Override
  public void onPlayerError(ExoPlaybackException error) {
  }

  private Handler getMainHandler() {
    return mainHandler;
  }

  @Override
  public void onRenderers(TrackRenderer[] renderers, DefaultBandwidthMeter bandwidthMeter) {
    prebufferNextClip();
    for (int i = 0; i < RendererBuilder.RENDERER_COUNT; i++) {
      if (renderers[i] == null) {
        // Convert a null renderer to a dummy renderer.
        renderers[i] = new DummyTrackRenderer();
      }
    }
    // Complete preparation.
    this.videoRenderer = renderers[TYPE_VIDEO];
    this.codecCounters = videoRenderer instanceof MediaCodecTrackRenderer
        ? ((MediaCodecTrackRenderer) videoRenderer).codecCounters
        : renderers[TYPE_AUDIO] instanceof MediaCodecTrackRenderer
        ? ((MediaCodecTrackRenderer) renderers[TYPE_AUDIO]).codecCounters : null;
    this.bandwidthMeter = bandwidthMeter;

    if (player == null) {
      // TODO(jliarte): 25/07/17 should reinit components?
      return;
    }
    pushSurface(false);
    player.prepare(renderers[TYPE_VIDEO], renderers[TYPE_AUDIO]);
    player.seekTo(getClipPositionFromTimeLineTime());
    if (videoHasMusic()) {
      seekAudioTo(currentTimePositionInList);
    }
    setVideoVolume(videoVolume);
    rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
  }


  private void prebufferNextClip() {
    if (currentClipIndex < videoList.size() - 1) {
      clearNextBufferedClip();
      RendererBuilder nextClipRendererBuilder = new RendererBuilder(getContext(), userAgent);
      Video nextClipToPlay = videoList.get(currentClipIndex + 1);
      nextClipRendererBuilder
              .buildRenderers(new RendererBuilder.RendererBuilderListener() {
                @Override
                public void onRenderers(TrackRenderer[] renderers,
                                        DefaultBandwidthMeter bandwidthMeter) {
                  nextClipRenderers = renderers;
                }
              },
              Uri.fromFile(new File(nextClipToPlay.getMediaPath())),
              this.getMainHandler());
    }
  }

  protected int getClipPositionFromTimeLineTime() {
    int clipIndex = getClipIndexByProgress(currentTimePositionInList);
    if (clipIndex >= videoList.size()) {
      return 0;
    }
    return currentTimePositionInList
        - (int) clipTimesRanges.get(clipIndex).getLower()
        + videoList.get(clipIndex).getStartTime();
  }

  private void pushSurface(boolean blockForSurfacePush) {
    if (videoRenderer == null || player == null) {
      return;
    }

    if (blockForSurfacePush) {
      player.blockingSendMessage(
          videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
    } else {
      player.sendMessage(
          videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
    }
  }

}
