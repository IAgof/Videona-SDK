package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;

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

import java.io.IOException;

public class TranscoderHelper {

  private TextToDrawable drawableGenerator;
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
  private String TAG = "TranscoderHelper";

  // TODO:(alvaro.martinez) 21/02/17 Where we define this default values?
  public static final int TIME_FADE_IN_MS = 500;
  public static final int TIME_FADE_OUT_MS = 500;

  public TranscoderHelper(TextToDrawable drawableGenerator, MediaTranscoder mediaTranscoder) {
    this.drawableGenerator = drawableGenerator;
    this.mediaTranscoder = mediaTranscoder;
  }

  // TODO:(alvaro.martinez) 22/11/16 unify in one constructor ¿?
  public TranscoderHelper(MediaTranscoder mediaTranscoder) {
        this.mediaTranscoder = mediaTranscoder;
  }

  public void generateOutputVideoWithOverlayImageAndTrimming(
                                                  final Drawable fadeTransition,
                                                  final boolean isVideoFadeActivated,
                                                  final boolean isAudioFadeActivated,
                                                  final Video videoToEdit,
                                                  final VideonaFormat format,
                                                  final String intermediatesTempAudioFadeDirectory,
                                                  final MediaTranscoderListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {

        checkVideoIsBeenTranscoded(videoToEdit);

        Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

        ListenableFuture<Void> listenableFuture
            = null;
        try {
          listenableFuture = mediaTranscoder.transcodeTrimAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, imageText,
              videoToEdit.getStartTime(), videoToEdit.getStopTime());
        } catch (IOException e) {
          e.printStackTrace();
        }


        ListenableFuture<Void> future;
        if (isAudioFadeActivated) {
          future = Futures.transform(listenableFuture,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory, listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          future = Futures.transform(listenableFuture,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setListentableFuture(future);

      }
    }).start();
  }

  public void generateOutputVideoWithOverlayImage(final Drawable fadeTransition,
                                                final boolean isVideoFadeActivated,
                                                final boolean isAudioFadeActivated,
                                                final Video videoToEdit, final VideonaFormat format,
                                                final String intermediatesTempAudioFadeDirectory,
                                                final MediaTranscoderListener listener) {

    new Thread(new Runnable() {
      @Override
      public void run() {

        checkVideoIsBeenTranscoded(videoToEdit);

        Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

        ListenableFuture<Void> listenableFuture =
            null;
        try {
          listenableFuture = mediaTranscoder.transcodeAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, imageText);
        } catch (IOException e) {
          e.printStackTrace();
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
        }

        ListenableFuture<Void> future;
        if(isAudioFadeActivated){
          future = Futures.transform(listenableFuture,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory,listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          future = Futures.transform(listenableFuture,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setListentableFuture(future);
      }
    }).start();
  }

  public void generateOutputVideoWithTrimming(final Drawable fadeTransition,
                                              final boolean isVideoFadeActivated,
                                              final boolean isAudioFadeActivated,
                                              final Video videoToEdit, final VideonaFormat format,
                                              final String intermediatesTempAudioFadeDirectory,
                                              final MediaTranscoderListener listener) {

    new Thread(new Runnable() {
      @Override
      public void run() {

        checkVideoIsBeenTranscoded(videoToEdit);

        ListenableFuture<Void> listenableFuture =
            null;
        try {
          listenableFuture = mediaTranscoder.transcodeAndTrimVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format,
              videoToEdit.getStartTime(), videoToEdit.getStopTime());
        } catch (IOException e) {
          e.printStackTrace();
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
        }

        ListenableFuture<Void> future;
        if(isAudioFadeActivated){
          future = Futures.transform(listenableFuture,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory,listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          future = Futures.transform(listenableFuture,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setListentableFuture(future);
      }
    }).start();
  }

  public Image getImageFromTextAndPosition(String text, String textPosition) {
    Drawable textDrawable = drawableGenerator.createDrawableWithTextAndPosition(text, textPosition,
            Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);

    return new Image(textDrawable, Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
  }

  public void generateFileWithAudioFadeInFadeOut(final String inputFile, final int timeFadeInMs,
                                                 final int timeFadeOutMs,
                                                 final String tempDirectory,
                                                 final String outputFile,
                                                 final OnAudioEffectListener listener) {

    new Thread(new Runnable() {
      @Override
      public void run() {

        mediaTranscoder.audioFadeInFadeOutToFile(inputFile, timeFadeInMs, timeFadeOutMs,
            tempDirectory, outputFile, listener);

      }
    }).start();
  }

  private Function<? super Void, ? extends Void> updateVideo(
                                                          final Video videoToEdit,
                                                          final MediaTranscoderListener listener) {
    return new Function<Void, Void>() {
      @Override
      public Void apply(Void input) {
        successVideoTranscoded(videoToEdit, listener);
        return input;
      }
    };
  }

  private Function<? super Void, ? extends Void> applyAudioFadeInOut(
                                                   final Video videoToEdit,
                                                   final String intermediatesTempAudioFadeDirectory,
                                                   final MediaTranscoderListener listener) {
    return new Function<Void, Void>() {

      @Override
      public Void apply(Void input) {
        ApplyAudioFadeInFadeOutToVideo.OnApplyAudioFadeInFadeOutToVideoListener audioListener =
            new ApplyAudioFadeInFadeOutToVideo.OnApplyAudioFadeInFadeOutToVideoListener() {
              @Override
              public void OnGetAudioFadeInFadeOutError(String message, Video video) {
                Log.d(TAG, "OnGetAudioFadeInFadeOutError ");
                listener.onErrorTranscoding(video, message);
              }

              @Override
              public void OnGetAudioFadeInFadeOutSuccess(Video video) {
                Log.d(TAG, "OnGetAudioFadeInFadeOutSuccess ");
                successVideoTranscoded(videoToEdit, listener);
              }
            };
        ApplyAudioFadeInFadeOutToVideo applyAudioFadeInFadeOutToVideo =
            new ApplyAudioFadeInFadeOutToVideo(audioListener, intermediatesTempAudioFadeDirectory);
        try {
          applyAudioFadeInFadeOutToVideo.applyAudioFadeToVideo(videoToEdit,
              TIME_FADE_IN_MS, TIME_FADE_OUT_MS);
        } catch (IOException e) {
          e.printStackTrace();
        }
        return input;
      }
    };
  }

  private void successVideoTranscoded(Video videoToEdit, MediaTranscoderListener listener) {
    videoToEdit.setTempPathFinished(true);
    listener.onSuccessTranscoding(videoToEdit);
  }

  private void checkVideoIsBeenTranscoded(Video videoToEdit) {
    if(!videoToEdit.outputVideoIsFinished()) {
      ListenableFuture<Void> listenableFuture = videoToEdit.getListenableFuture();
      if(listenableFuture!=null && !listenableFuture.isDone()){
        Log.d(TAG, "Cancel future " + listenableFuture.toString());
        listenableFuture.cancel(true);
      }
    }
  }
}