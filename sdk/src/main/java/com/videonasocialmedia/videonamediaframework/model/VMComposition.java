package com.videonasocialmedia.videonamediaframework.model;

import android.graphics.drawable.Drawable;

import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
import com.videonasocialmedia.videonamediaframework.model.media.Watermark;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.track.AudioTrack;
import com.videonasocialmedia.videonamediaframework.model.media.track.MediaTrack;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoQuality;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoResolution;

import java.util.ArrayList;

import static com.videonasocialmedia.videonamediaframework.model.Constants.INDEX_AUDIO_TRACK_MUSIC;
import static com.videonasocialmedia.videonamediaframework.model.Constants.INDEX_AUDIO_TRACK_VOICE_OVER;

/**
 * Videona Media Composition class for representing a media composition with audio and video
 * tracks, effects and transformations.
 */
public class VMComposition {

  /**
   * Lenght of the VMComposition.
   */
  private int duration = 0;

  private boolean videoFadeTransitionActivated = false;
  private boolean audioFadeTransitionActivated = false;
  // TODO(jliarte): 20/04/18 remove android dependency
  private Drawable drawableFadeTransitionVideo = null;

  public MediaTrack getMediaTrack() {
    return mediaTrack;
  }

  public void setMediaTrack(MediaTrack mediaTrack) {
    this.mediaTrack = mediaTrack;
  }

  /**
   * Track of Video and Image objects.
   */
  private MediaTrack mediaTrack;

  public ArrayList<AudioTrack> getAudioTracks() {
    return audioTracks;
  }

  public void setAudioTracks(ArrayList<AudioTrack> audioTracks) {
    this.audioTracks = audioTracks;
  }

  /**
   * Audio tracks to form the final audio track. One by default, could be maximum defined on
   * project profile.
   */
  private ArrayList<AudioTrack> audioTracks = new ArrayList<>();

  private boolean isWatermarkActivated;

  private Watermark watermark;

  private Profile profile;

  public VMComposition(String resourceWatermarkFilePath, Profile profile) {
    this.mediaTrack = new MediaTrack();
    this.audioTracks = new ArrayList<>();
    audioTracks.add(0, new AudioTrack(INDEX_AUDIO_TRACK_MUSIC));
    this.watermark = new Watermark(resourceWatermarkFilePath);
    this.profile = profile;
  }

  public VMComposition() {
    this.mediaTrack = new MediaTrack();
    this.audioTracks = new ArrayList<>();
    audioTracks.add(0, new AudioTrack(INDEX_AUDIO_TRACK_MUSIC));
  }

  public VMComposition(VMComposition vmComposition) throws IllegalItemOnTrack {
    this.mediaTrack = new MediaTrack(vmComposition.getMediaTrack());
    this.audioTracks = new ArrayList<>();
    for (AudioTrack audioTrack : vmComposition.getAudioTracks()) {
      audioTracks.add(new AudioTrack(audioTrack));
    }
    this.watermark = new Watermark(vmComposition.getWatermark().getResourceWatermarkFilePath());
    this.isWatermarkActivated = vmComposition.hasWatermark();
    this.profile = new Profile(vmComposition.getProfile());
    this.setAudioFadeTransitionActivated(vmComposition.isAudioFadeTransitionActivated());
    this.setVideoFadeTransitionActivated(vmComposition.isVideoFadeTransitionActivated());
  }

  public int getDuration() {
    updateCompositionDuration();
    return this.duration;
  }

  private void updateCompositionDuration() {
    this.duration = 0;
    for (Media video : mediaTrack.getItems()) {
      duration = duration + video.getDuration();
    }
  }

  /**
   * Returns the music of the composition.
   *
   * @return the first element in the music track of the composition.
   */
  public Music getMusic() {
    try {
      return  (Music) getAudioTracks().get(INDEX_AUDIO_TRACK_MUSIC).getItems().get(0);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * Returns the voiceover of the VM composition.
   *
   * @return the first element of the voiceover track
   */
  public Music getVoiceOver() {
    try {
      return (Music) getAudioTracks().get(INDEX_AUDIO_TRACK_VOICE_OVER).getItems().get(0);
    } catch (Exception exception) {
      return null;
    }
  }

  public boolean hasVoiceOver() {
    return (getVoiceOver() != null);
  }

  public boolean hasMusic() {
    return (getMusic() != null);
  }

  public boolean hasVideos() {
    return getMediaTrack().getItems().size() > 0;
  }

  public boolean hasWatermark() {
    return isWatermarkActivated;
  }

  public void setWatermarkActivated(boolean isWatermarkActivated) {
    this.isWatermarkActivated = isWatermarkActivated;
  }

  public Watermark getWatermark() {
    return watermark;
  }

  public VideonaFormat getVideoFormat() {
    VideoResolution resolution =  profile.getVideoResolution();
    VideoQuality quality = profile.getVideoQuality();
    VideonaFormat videonaFormat;
    if (resolution != null && quality != null) {
      videonaFormat = new VideonaFormat(quality.getVideoBitRate(), resolution.getWidth(),
          resolution.getHeight());
    } else {
      videonaFormat = new VideonaFormat();
    }
    return videonaFormat;
  }

  public Profile getProfile() {
    return profile;
  }

  public void setVideoFadeTransitionActivated(boolean videoFadeTransitionActivated) {
    this.videoFadeTransitionActivated = videoFadeTransitionActivated;
  }

  public void setAudioFadeTransitionActivated(boolean audioFadeTransitionActivated) {
    this.audioFadeTransitionActivated = audioFadeTransitionActivated;
  }

  public boolean isVideoFadeTransitionActivated() {
    return videoFadeTransitionActivated;
  }

  public boolean isAudioFadeTransitionActivated() {
    return audioFadeTransitionActivated;
  }

  public Drawable getDrawableFadeTransitionVideo() {
    return drawableFadeTransitionVideo;
  }

  public void setDrawableFadeTransitionVideo(Drawable drawableFadeTransitionVideo) {
    this.drawableFadeTransitionVideo = drawableFadeTransitionVideo;
  }

}
