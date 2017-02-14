package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.MediaTranscoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;

import java.io.File;
import java.io.IOException;

public class TranscoderHelper {

  private TextToDrawable drawableGenerator;
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
  private String tempVideoPathPreviewFadeInFadeOut;
  private String intermediatesTempAudioFadeDirectory;
  private String TAG = "TranscoderHelper";

  public TranscoderHelper(TextToDrawable drawableGenerator, MediaTranscoder mediaTranscoder) {
    this.drawableGenerator = drawableGenerator;
    this.mediaTranscoder = mediaTranscoder;
    intermediatesTempAudioFadeDirectory = "";
  }

  // TODO:(alvaro.martinez) 22/11/16 unify in one constructor ¿?
  public TranscoderHelper(MediaTranscoder mediaTranscoder) {
        this.mediaTranscoder = mediaTranscoder;
  }

  public void generateOutputVideoWithOverlayImageAndTrimming(Drawable fadeTransition,
                                                             boolean isFadeActivated,
                                                             Video videoToEdit,
                                                             VideonaFormat format,
                                                             MediaTranscoderListener listener)
          throws IOException {
    Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

    mediaTranscoder.transcodeTrimAndOverlayImageToVideo(fadeTransition, isFadeActivated,
        videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, listener, imageText,
        videoToEdit.getStartTime(), videoToEdit.getStopTime());

  }

  public void generateOutputVideoWithOverlayImageAndTrimming(Drawable fadeTransition,
                                                             boolean isVideoFadeActivated,
                                                             boolean isAudioFadeActivated,
                                                             Video videoToEdit,
                                                             VideonaFormat format)
      throws IOException {


    if(!videoToEdit.outputVideoIsFinished()) {
      ListenableFuture<String> listenableFuture = videoToEdit.getListenableFuture();
      if(listenableFuture!=null){
        Log.d(TAG, "Cancel future " + listenableFuture.toString());
        // TODO:(alvaro.martinez) 14/02/17 delete temporal file
        listenableFuture.cancel(true);
      }
    }

    Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
        videoToEdit.getClipTextPosition());

    ListenableFuture<String> listenableFuture =
        mediaTranscoder.transcodeTrimAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
            videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format,imageText,
            videoToEdit.getStartTime(), videoToEdit.getStopTime());
    ListenableFuture<String> future;
    if(isAudioFadeActivated){
      future = Futures.transform(listenableFuture,
          applyAudioFadeInOut(videoToEdit), MoreExecutors.sameThreadExecutor());
      Log.d(TAG, "audioFadeActivated future " + future.toString());
    } else {
      future = Futures.transform(listenableFuture,
          updateVideoRepository(videoToEdit), MoreExecutors.sameThreadExecutor());
      Log.d(TAG, "not audioFadeActivated future " + future.toString());
    }
    videoToEdit.setListentableFuture(future);
    Log.d(TAG, "Set future " + future.toString() + " to video");

  }

  private Function<? super String, ? extends String> updateVideoRepository(final Video videoToEdit) {
    return new Function<String, String>() {
      @Nullable
      @Override
      public String apply(String input) {
        Log.d(TAG, "Function apply updateVideoRepository " + input);
        videoToEdit.setTempPathFinished(true);
        return input;
      }
    };
  }

  private Function<? super String, ? extends String> applyAudioFadeInOut(final Video videoToEdit) {
    return new Function<String, String>() {
      @Nullable
      @Override
      public String apply(String input) {
        Log.d(TAG, "Function apply applyAudioFadeInOut " + input);
        if(input.compareTo("ok") == 0){
          // applyFadeInOut
          try {
            // TODO:(alvaro.martinez) 14/02/17 Get tempDirectory
            mediaTranscoder.audioFadeInFadeOutToFile(videoToEdit.getMediaPath(), 500, 500,
                new File(videoToEdit.getTempPath()).getParent(), videoToEdit.getTempPath(),
                new OnAudioEffectListener() {
                  @Override
                  public void onAudioEffectSuccess(String outputFile) {
                    Log.d(TAG, "Function apply onAudioEffectSuccess " + outputFile);
                    updateVideoRepository(videoToEdit);
                  }

                  @Override
                  public void onAudioEffectProgress(String progress) {

                  }

                  @Override
                  public void onAudioEffectError(String error) {
                    Log.d(TAG, "Function apply onAudioEffectSuccess " + error);
                  }

                  @Override
                  public void onAudioEffectCanceled() {
                    Log.d("TranscodeHelper", "Function apply onAudioEffectCanceled ");
                  }
                });
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        return input;
      }
    };
  }

  public void generateOutputVideoWithOverlayImage(Drawable fadeTransition,
                                                  boolean isFadeActivated,
                                                  Video video, VideonaFormat format,
                                                  MediaTranscoderListener listener)
          throws IOException  {
    Image imageText = getImageFromTextAndPosition(video.getClipText(), video.getClipTextPosition());

    mediaTranscoder.transcodeAndOverlayImageToVideo(fadeTransition, isFadeActivated,
        video.getMediaPath(), video.getTempPath(), format, listener, imageText);
  }

  public void generateOutputVideoWithTrimming(Drawable fadeTransition,
                                              boolean isFadeActivated,
                                              Video video, VideonaFormat format,
                                              MediaTranscoderListener listener)
          throws IOException {
    mediaTranscoder.transcodeAndTrimVideo(fadeTransition, isFadeActivated, video.getMediaPath(),
        video.getTempPath(), format, listener, video.getStartTime(), video.getStopTime());
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