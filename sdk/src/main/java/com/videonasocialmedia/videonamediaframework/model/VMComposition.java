package com.videonasocialmedia.videonamediaframework.model;

import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.track.AudioTrack;
import com.videonasocialmedia.videonamediaframework.model.media.track.MediaTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Videona Media Composition class for representing a media composition with audio and video
 * tracks, effects and transformations.
 */
public class VMComposition {
  /**
   * Lenght of the VMComposition
   */
  private int duration = 0;

  public MediaTrack getMediaTrack() {
    return mediaTrack;
  }

  public void setMediaTrack(MediaTrack mediaTrack) {
    this.mediaTrack = mediaTrack;
  }

  /**
   * Track of Video and Image objects
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
  private ArrayList<AudioTrack> audioTracks;

  public VMComposition() {
    this.mediaTrack = new MediaTrack();
    this.audioTracks = new ArrayList<>();
    audioTracks.add(new AudioTrack());
  }

  public VMComposition(VMComposition vmComposition) throws IllegalItemOnTrack {
    this.mediaTrack = new MediaTrack(vmComposition.getMediaTrack());
    this.audioTracks = new ArrayList<>();
    for (AudioTrack audioTrack : vmComposition.getAudioTracks()) {
      audioTracks.add(new AudioTrack(audioTrack));
    }
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

  public Music getMusic() {
    /**
     * TODO(jliarte): review this method and matching use case
     * @see com.videonasocialmedia.vimojo.domain.editor.GetMusicFromProjectUseCase
     */
    Music result = null;
    try {
      result = (Music) getAudioTracks().get(0).getItems().get(0);
    } catch (Exception exception) {
      // exception retrieving music, we'll return null
    }
    return result;
  }

  public boolean hasMusic() {
    return (getMusic() != null);
  }

  public boolean hasVideos() {
    return getMediaTrack().getItems().size() >0;
  }

}
