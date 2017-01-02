package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.videonamediaframework.model.media.Video;

import java.io.IOException;

/**
 * Created by alvaro on 23/10/16.
 */

public class ApplyAudioFadeInFadeOutToVideo
        implements VideoAudioSwapper.VideoAudioSwapperListener,
        VideoAudioFadeGenerator.VideoAudioFadeListener {
  private VideoAudioFadeGenerator videoAudioFadeGenerator;
  private VideoAudioSwapper videoAudioSwapper;
  private Video videoToEdit;
  private OnApplyAudioFadeInFadeOutToVideoListener listener;
  private String tempPreviousPath;
  // TODO:(alvaro.martinez) 22/11/16 use project tmp directory
  private String intermediatesTempDirectory;

  public ApplyAudioFadeInFadeOutToVideo(OnApplyAudioFadeInFadeOutToVideoListener listener,
                                        String intermediatesTempDirectory) {
    videoAudioFadeGenerator = new VideoAudioFadeGenerator(this, intermediatesTempDirectory);
    videoAudioSwapper = new VideoAudioSwapper();
    this.listener = listener;
    this.intermediatesTempDirectory = intermediatesTempDirectory;
  }

  public void applyAudioFadeToVideo(Video videoToEdit, int timeFadeInMs, int timeFadeOutMs)
      throws IOException {
    this.videoToEdit = videoToEdit;
    tempPreviousPath = videoToEdit.getTempPath();
    videoAudioFadeGenerator.getAudioFadeInFadeOutFromVideo(videoToEdit.getTempPath(),
        timeFadeInMs, timeFadeOutMs);
  }

  @Override
  public void onExportError(String error) {
    listener.OnGetAudioFadeInFadeOutError(error,videoToEdit);
  }

  @Override
  public void onExportSuccess() {
    listener.OnGetAudioFadeInFadeOutSuccess(videoToEdit);
  }

  @Override
  public void onGetAudioFadeInFadeOutFromVideoSuccess(String audioFile) {
    // TODO:(alvaro.martinez) 22/11/16 use project tmp directory
    videoToEdit.setTempPath(intermediatesTempDirectory);
    videoAudioSwapper.export(tempPreviousPath, audioFile,
        videoToEdit.getTempPath(), this);
  }

  @Override
  public void onGetAudioFadeInFadeOutFromVideoError(String message) {
    listener.OnGetAudioFadeInFadeOutError(message, videoToEdit);
  }

  /**
   * Created by alvaro on 25/10/16.
   */
  public interface OnApplyAudioFadeInFadeOutToVideoListener {
    void OnGetAudioFadeInFadeOutError(String message, Video video);
    void OnGetAudioFadeInFadeOutSuccess(Video video);
  }
}
