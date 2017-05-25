package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.MediaTranscoder.MediaTranscoderListener;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TranscoderHelper {

  private TextToDrawable drawableGenerator;
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
  private String TAG = "TranscoderHelper";

  // TODO:(alvaro.martinez) 21/02/17 Where we define this default values?
  public static final int TIME_FADE_IN_MS = 125;
  public static final int TIME_FADE_OUT_MS = 125;

  public TranscoderHelper(TextToDrawable drawableGenerator, MediaTranscoder mediaTranscoder) {
    this.drawableGenerator = drawableGenerator;
    this.mediaTranscoder = mediaTranscoder;
  }

  // TODO:(alvaro.martinez) 22/11/16 unify in one constructor ¿?
  public TranscoderHelper(MediaTranscoder mediaTranscoder) {
        this.mediaTranscoder = mediaTranscoder;
  }

  public void generateOutputVideoWithAVTransitions(final Drawable fadeTransition,
                                                   final boolean isVideoFadeActivated,
                                                   final boolean isAudioFadeActivated,
                                                   final Video videoToEdit,
                                                   final VideonaFormat format,
                                                   final String intermediatesTempAudioFadeDirectory,
                                                   final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        cancelPendingTranscodingTasks(videoToEdit);

        ListenableFuture<Void> transcodingJob = null;
        try {
          transcodingJob = mediaTranscoder.transcodeOnlyVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format);
        } catch (IOException e) {
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
          e.printStackTrace();
          return;
        }

        ListenableFuture<Void> chainedTranscodingJob;
        if (isAudioFadeActivated) {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory, listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setTranscodingTask(chainedTranscodingJob);
        waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToEdit);
      }
    }).start();
  }

  public void generateOutputVideoWithAudioTransition(final Video videoToEdit,
                                                     final String intermediatesTempAudioFadeDirectory,
                                                     final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory, listener);
      }
    }).start();
  }

  public void generateOutputVideoWithOverlayImageAndTrimming(
          final Drawable fadeTransition,
          final boolean isVideoFadeActivated,
          final boolean isAudioFadeActivated,
          final Video videoToEdit,
          final VideonaFormat format,
          final String intermediatesTempAudioFadeDirectory,
          final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        cancelPendingTranscodingTasks(videoToEdit);

        Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

        ListenableFuture<Void> transcodingJob = null;
        try {
          transcodingJob = mediaTranscoder.transcodeTrimAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, imageText,
              videoToEdit.getStartTime(), videoToEdit.getStopTime());
        } catch (IOException e) {
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
          e.printStackTrace();
          return;
        }

        ListenableFuture<Void> chainedTranscodingJob;
        if (isAudioFadeActivated) {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory, listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setTranscodingTask(chainedTranscodingJob);
        waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToEdit);
      }
    }).start();
  }

  public void generateOutputVideoWithOverlayImage(final Drawable fadeTransition,
                                                final boolean isVideoFadeActivated,
                                                final boolean isAudioFadeActivated,
                                                final Video videoToEdit, final VideonaFormat format,
                                                final String intermediatesTempAudioFadeDirectory,
                                                final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        cancelPendingTranscodingTasks(videoToEdit);

        Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
            videoToEdit.getClipTextPosition());

        ListenableFuture<Void> transcodingJob =
            null;
        try {
          transcodingJob = mediaTranscoder.transcodeAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, imageText);
        } catch (IOException e) {
          e.printStackTrace();
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
          return;
        }

        ListenableFuture<Void> chainedTranscodingJob;
        if(isAudioFadeActivated){
          chainedTranscodingJob = Futures.transform(transcodingJob,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory,listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setTranscodingTask(chainedTranscodingJob);
        waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToEdit);
      }
    }).start();
  }

  public ListenableFuture<Void> generateOutputVideoWithWatermarkImage(final String inFilePath,
                                                    final String outFilePath,
                                                    final VideonaFormat format,
                                                    final Image watermark)
      throws IOException  {
    ListenableFuture<Void> transcodingJobWatermark = null;
    Drawable fakeDrawable = Drawable.createFromPath("");
    try {
      transcodingJobWatermark = mediaTranscoder.transcodeAndOverlayImageToVideo(fakeDrawable, false,
          inFilePath, outFilePath, format, watermark);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return transcodingJobWatermark;
  }

  public void generateOutputVideoWithTrimming(final Drawable fadeTransition,
                                              final boolean isVideoFadeActivated,
                                              final boolean isAudioFadeActivated,
                                              final Video videoToEdit, final VideonaFormat format,
                                              final String intermediatesTempAudioFadeDirectory,
                                              final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        cancelPendingTranscodingTasks(videoToEdit);

        ListenableFuture<Void> transcodingJob =
            null;
        try {
          transcodingJob = mediaTranscoder.transcodeAndTrimVideo(fadeTransition, isVideoFadeActivated,
              videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format,
              videoToEdit.getStartTime(), videoToEdit.getStopTime());
        } catch (IOException e) {
          e.printStackTrace();
          listener.onErrorTranscoding(videoToEdit, e.getMessage());
          return;
        }

        ListenableFuture<Void> chainedTranscodingJob;
        if(isAudioFadeActivated){
          chainedTranscodingJob = Futures.transform(transcodingJob,
              applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory,listener),
              MoreExecutors.newDirectExecutorService());
        } else {
          chainedTranscodingJob = Futures.transform(transcodingJob,
              updateVideo(videoToEdit, listener), MoreExecutors.newDirectExecutorService());
        }
        videoToEdit.setTranscodingTask(chainedTranscodingJob);
        waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToEdit);
      }
    }).start();
  }

  public void adaptVideoWithRotationToDefaultFormat(final Video videoToAdapt, final VideonaFormat format,
                                                    final String destVideoPath, final int rotation,
                                                    final Drawable fadeTransition, final boolean isFadeActivated,
                                                    final TranscoderHelperListener listener) throws IOException {
    new Thread(new Runnable() {
      @Override
      public void run() {
        ListenableFuture<Void> transcodingJob = null;
        try {
          transcodingJob = mediaTranscoder.transcodeVideoWithRotationToDefaultFormat(
                  videoToAdapt.getMediaPath(), format, destVideoPath, rotation, fadeTransition,
              isFadeActivated);
        } catch (IOException e) {
          e.printStackTrace();
          listener.onErrorTranscoding(videoToAdapt, e.getMessage());
          return;
        }
        ListenableFuture<Void> chainedTranscodingJob;
        chainedTranscodingJob = Futures.transform(transcodingJob,
        updateVideo(videoToAdapt, listener), MoreExecutors.newDirectExecutorService());

        videoToAdapt.setTranscodingTask(chainedTranscodingJob);
        waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToAdapt);
      }
    }).start();
  }

  private void waitTranscodingJobAndCheckState(ListenableFuture<Void> chainedTranscodingJob,
                                             TranscoderHelperListener listener, Video videoToEdit) {
    try {
      chainedTranscodingJob.get();
    } catch (InterruptedException e) {
      listener.onErrorTranscoding(videoToEdit, e.getMessage());
      e.printStackTrace();
    } catch (ExecutionException e) {
      listener.onErrorTranscoding(videoToEdit, e.getMessage());
      e.printStackTrace();
    } finally {
      if(!chainedTranscodingJob.isDone())
        listener.onErrorTranscoding(videoToEdit, "generateOutputVideoWithAVTransitions");
    }
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
                                                 final MediaTranscoderListener listener) {
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
                                                          final TranscoderHelperListener listener) {
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
                                                   final TranscoderHelperListener listener) {
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

  private void successVideoTranscoded(Video videoToEdit, TranscoderHelperListener listener) {
    Log.d(TAG, "successVideoTranscoded");
    listener.onSuccessTranscoding(videoToEdit);
  }

  private void cancelPendingTranscodingTasks(Video videoToEdit) {
    ListenableFuture<Void> transcodingTask = videoToEdit.getTranscodingTask();
    if (transcodingTask != null && !transcodingTask.isDone()) {
      Log.d(TAG, "Cancel transcoding task " + transcodingTask.toString());
      transcodingTask.cancel(true);
    }
  }

  /*private void successVideoTranscodedWithWatermark(String outputFile,
                                                   VMCompositionExportSession listener){
    Log.d(TAG, "succesVideoExportedWithWatermark");
    listener.onVMCompositionExportWatermarkAdded();
  }*/

}