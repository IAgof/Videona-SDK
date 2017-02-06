package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.MediaTranscoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.video.format.VideoTranscoderFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;

import java.io.IOException;

public class TranscoderHelper {

  private TextToDrawable drawableGenerator;
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();

  public TranscoderHelper(TextToDrawable drawableGenerator, MediaTranscoder mediaTranscoder) {
    this.drawableGenerator = drawableGenerator;
    this.mediaTranscoder = mediaTranscoder;
  }

  // TODO:(alvaro.martinez) 22/11/16 unify in one constructor ¿?
  public TranscoderHelper(MediaTranscoder mediaTranscoder) {
        this.mediaTranscoder = mediaTranscoder;
  }

  public void generateOutputVideoWithOverlayImageAndTrimming(Drawable fadeTransition,
                                                             boolean isFadeActivated,
                                                             Video videoToEdit,
                                                             VideoTranscoderFormat format,
                                                             MediaTranscoderListener listener)
          throws IOException {
    Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

    mediaTranscoder.transcodeTrimAndOverlayImageToVideo(fadeTransition, isFadeActivated,
        videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, listener, imageText,
        videoToEdit.getStartTime(), videoToEdit.getStopTime());
  }

  public void generateOutputVideoWithOverlayImage(Drawable fadeTransition,
                                                  boolean isFadeActivated,
                                                  Video video, VideoTranscoderFormat format,
                                                  MediaTranscoderListener listener)
          throws IOException  {
    Image imageText = getImageFromTextAndPosition(video.getClipText(), video.getClipTextPosition());

    mediaTranscoder.transcodeAndOverlayImageToVideo(fadeTransition, isFadeActivated,
        video.getMediaPath(), video.getTempPath(), format, listener, imageText);
  }

  public void generateOutputVideoWithTrimming(Drawable fadeTransition,
                                              boolean isFadeActivated,
                                              Video video, VideoTranscoderFormat format,
                                              MediaTranscoderListener listener)
          throws IOException {
    mediaTranscoder.transcodeAndTrimVideo(fadeTransition, isFadeActivated, video.getMediaPath(),
        video.getTempPath(), format, listener, video.getStartTime(), video.getStopTime());
  }

  public void generateOutputVideoImport(Drawable fadeTransition,
                                              boolean isFadeActivated,
                                              Video video, VideoTranscoderFormat format,
                                              MediaTranscoderListener listener)
      throws IOException {
    mediaTranscoder.transcodeOnlyVideo(fadeTransition, isFadeActivated, video.getMediaPath(),
        video.getTempPath(), format, listener);
  }

  public void adaptVideoToTranscoder(String origVideoPath, VideoTranscoderFormat format,
                                     MediaTranscoderListener listener, String destVideoPath)
                                     throws IOException {

    mediaTranscoder.adaptVideo(origVideoPath, format, listener, destVideoPath);
  }

  @NonNull
  public Image getImageFromTextAndPosition(String text, String textPosition) {
    Drawable textDrawable = drawableGenerator.createDrawableWithTextAndPosition(text, textPosition,
            Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);

    return new Image(textDrawable, Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
  }

  public void generateFileWithAudioFadeInFadeOut(String inputFile, int timeFadeInMs, int timeFadeOutMs,
                                                 String tempDirectory, String outputFile,
                                                 OnAudioEffectListener listener) throws IOException {

    mediaTranscoder.audioFadeInFadeOutToFile(inputFile, timeFadeInMs, timeFadeOutMs, tempDirectory,
            outputFile, listener);
  }
}