package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.MediaTranscoder.MediaTranscoderListener;
import com.videonasocialmedia.transcoder.TranscodingException;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import com.videonasocialmedia.videonamediaframework.utils.FileUtils;
import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.List;

public class TranscoderHelper {
  private final String TAG = TranscoderHelper.class.getSimpleName();
  private TextToDrawable drawableGenerator;
  private MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();

  // TODO:(alvaro.martinez) 21/02/17 Where we define this default values?
  private static final int TIME_FADE_IN_MS = 125;
  private static final int TIME_FADE_OUT_MS = 125;

  public TranscoderHelper(TextToDrawable drawableGenerator, MediaTranscoder mediaTranscoder) {
    this.drawableGenerator = drawableGenerator;
    this.mediaTranscoder = mediaTranscoder;
  }

  // TODO:(alvaro.martinez) 22/11/16 unify in one constructor ¿?
  public TranscoderHelper(MediaTranscoder mediaTranscoder) {
        this.mediaTranscoder = mediaTranscoder;
  }

  public void generateOutputVideoWithAVTransitionsAsync(
          final Drawable fadeTransition, final boolean isVideoFadeActivated,
          final boolean isAudioFadeActivated, final Video videoToEdit, final VideonaFormat format,
          final String intermediatesTempAudioFadeDirectory,
          final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        generateOutputVideoWithAVTransitions(videoToEdit, fadeTransition, isVideoFadeActivated,
                format, listener, isAudioFadeActivated, intermediatesTempAudioFadeDirectory);
      }
    }).start();
  }

  private void generateOutputVideoWithAVTransitions(
          Video videoToEdit, Drawable fadeTransition, boolean isVideoFadeActivated,
          VideonaFormat format, TranscoderHelperListener listener, boolean isAudioFadeActivated,
          String intermediatesTempAudioFadeDirectory) {
    cancelPendingTranscodingTasks(videoToEdit);

    ListenableFuture<Void> transcodingJob;
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
          applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory),
          MoreExecutors.newDirectExecutorService());
    } else {
      chainedTranscodingJob = Futures.transform(transcodingJob,
          getSuccessNotifierFunction(videoToEdit, listener),
              MoreExecutors.newDirectExecutorService());
    }
    ListenableFuture<Video> videoListenableFuture =
            Futures.transform(chainedTranscodingJob, transformIntoVideo(videoToEdit));
    videoToEdit.setTranscodingTask(videoListenableFuture);
    waitTranscodingJobAndCheckState(chainedTranscodingJob, listener, videoToEdit);
  }

  public void generateOutputVideoWithAudioTransitionAsync(
          final Video videoToEdit, final String intermediatesTempAudioFadeDirectory,
          final TranscoderHelperListener listener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory);
      }
    }).start();
  }

  public ListenableFuture<Video> updateIntermediateFile(
          Drawable drawableFadeTransition, boolean isVideoFadeActivated,
          boolean isAudioFadeActivated, Video videoToEdit, VideonaFormat format,
          String intermediatesTempAudioFadeDirectory) {
    if (videoToEdit.hasText()) {
      if (videoToEdit.isTrimmedVideo()) {
        return generateOutputVideoWithOverlayImageAndTrimmingAsync(drawableFadeTransition,
                isVideoFadeActivated, isAudioFadeActivated, videoToEdit, format,
                intermediatesTempAudioFadeDirectory);
      } else {
        return generateOutputVideoWithOverlayImageAsync(drawableFadeTransition,
                isVideoFadeActivated, isAudioFadeActivated, videoToEdit, format,
                intermediatesTempAudioFadeDirectory);
      }
    } else {
      return generateOutputVideoWithTrimmingAsync(drawableFadeTransition,
              isVideoFadeActivated, isAudioFadeActivated, videoToEdit, format,
              intermediatesTempAudioFadeDirectory);
    }
  }

  // TODO(jliarte): 18/09/17 check if this method is still needed
  ListenableFuture<Video> generateOutputVideoWithOverlayImageAndTrimmingAsync(
          final Drawable fadeTransition, final boolean isVideoFadeActivated,
          final boolean isAudioFadeActivated, final Video videoToEdit, final VideonaFormat format,
          final String intermediatesTempAudioFadeDirectory) {
    return generateOutputVideoWithOverlayImageAndTrimming(videoToEdit, fadeTransition,
            isVideoFadeActivated, format, isAudioFadeActivated,
            intermediatesTempAudioFadeDirectory);
  }

  private ListenableFuture<Video> generateOutputVideoWithOverlayImageAndTrimming(
          Video videoToEdit, Drawable fadeTransition, boolean isVideoFadeActivated,
          VideonaFormat format, boolean isAudioFadeActivated,
          String intermediatesTempAudioFadeDirectory) {
    cancelPendingTranscodingTasks(videoToEdit);

    Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
        videoToEdit.getClipTextPosition());

    ListenableFuture<Void> transcodingJob;
    try {
      transcodingJob = mediaTranscoder
              .transcodeTrimAndOverlayImageToVideo(fadeTransition, isVideoFadeActivated,
                      videoToEdit.getMediaPath(), videoToEdit.getTempPath(), format, imageText,
                      videoToEdit.getStartTime(), videoToEdit.getStopTime());
    } catch (IOException ex) {
      ex.printStackTrace();
//      listener.onErrorTranscoding(videoToEdit, ex.getMessage());
      // TODO(jliarte): 15/09/17 generate a custom exception here?
      throw new RuntimeException(ex);
    }

    if (isAudioFadeActivated) {
      transcodingJob = Futures.transform(transcodingJob,
          applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory),
              // TODO(jliarte): 15/09/17 check if this executor is the appropriate
          MoreExecutors.newDirectExecutorService());
    }
    ListenableFuture<Video> videoListenableFuture = Futures.transform(transcodingJob,
            transformIntoVideo(videoToEdit));
    videoToEdit.setTranscodingTask(videoListenableFuture);
    return videoListenableFuture;
  }

  private Function<? super Void, ? extends Video> transformIntoVideo(final Video videoToEdit) {
    return new Function<Void, Video>() {
      @Override
      public Video apply(Void input) {
        return videoToEdit;
      }
    };
  }

  public ListenableFuture<Video> generateOutputVideoWithOverlayImageAsync(
          final Drawable fadeTransition, final boolean isVideoFadeActivated,
          final boolean isAudioFadeActivated, final Video videoToEdit, final VideonaFormat format,
          final String intermediatesTempAudioFadeDirectory) {
    return generateOutputVideoWithOverlayImage(videoToEdit, fadeTransition, isVideoFadeActivated,
            format, isAudioFadeActivated, intermediatesTempAudioFadeDirectory);
  }

  private ListenableFuture<Video> generateOutputVideoWithOverlayImage(
          Video videoToEdit, Drawable fadeTransition, boolean isVideoFadeActivated,
          VideonaFormat format, boolean isAudioFadeActivated,
          String intermediatesTempAudioFadeDirectory) {
    cancelPendingTranscodingTasks(videoToEdit);

    Image imageText = getImageFromTextAndPosition(videoToEdit.getClipText(),
        videoToEdit.getClipTextPosition());

    ListenableFuture<Void> transcodingTask;
    try {
      transcodingTask = mediaTranscoder.transcodeAndOverlayImageToVideo(fadeTransition,
              isVideoFadeActivated, videoToEdit.getMediaPath(), videoToEdit.getTempPath(),
              format, imageText);
    } catch (IOException ex) {
      ex.printStackTrace();
      // TODO(jliarte): 15/09/17 generate a custom exception here?
      throw new RuntimeException(ex);
    }

    if (isAudioFadeActivated) {
      transcodingTask = Futures.transform(transcodingTask,
          applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory),
          MoreExecutors.newDirectExecutorService());
    }
    ListenableFuture<Video> videoListenableFuture = Futures.transform(transcodingTask,
            transformIntoVideo(videoToEdit));
    videoToEdit.setTranscodingTask(videoListenableFuture);
    return videoListenableFuture;
  }

  public ListenableFuture<Void> generateOutputVideoWithWatermarkImage(
          final String inFilePath, final String outFilePath, final VideonaFormat format,
          final Image watermark) throws IOException  {
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

  public ListenableFuture<Boolean> generateTempFileMixAudio(
          List<Media> mediaList, String tempAudioPath, String outputFilePath,
          long audioFileDuration) {
    return mediaTranscoder.mixAudioFiles(mediaList, tempAudioPath, outputFilePath,
            audioFileDuration);
  }

  public ListenableFuture<Video> generateOutputVideoWithTrimmingAsync(
          final Drawable fadeTransition, final boolean isVideoFadeActivated,
          final boolean isAudioFadeActivated, final Video videoToEdit, final VideonaFormat format,
          final String intermediatesTempAudioFadeDirectory) {
    return generateOutputVideoWithTrimming(videoToEdit, fadeTransition, isVideoFadeActivated,
            format, isAudioFadeActivated, intermediatesTempAudioFadeDirectory);
  }

  private ListenableFuture<Video> generateOutputVideoWithTrimming(
          Video videoToEdit, Drawable fadeTransition, boolean isVideoFadeActivated,
          VideonaFormat format, boolean isAudioFadeActivated,
          String intermediatesTempAudioFadeDirectory) {
    cancelPendingTranscodingTasks(videoToEdit);

    ListenableFuture<Void> transcodingJob;
    try {
      transcodingJob = mediaTranscoder.transcodeAndTrimVideo(fadeTransition,
              isVideoFadeActivated, videoToEdit.getMediaPath(), videoToEdit.getTempPath(),
              format, videoToEdit.getStartTime(), videoToEdit.getStopTime());
    } catch (IOException ex) {
      ex.printStackTrace();
      // TODO(jliarte): 15/09/17 generate a custom exception here?
      throw new RuntimeException(ex);
    }

    if (isAudioFadeActivated) {
      transcodingJob = Futures.transform(transcodingJob,
          applyAudioFadeInOut(videoToEdit, intermediatesTempAudioFadeDirectory),
          MoreExecutors.newDirectExecutorService());
    }
    ListenableFuture<Video> videoListenableFuture = Futures.transform(transcodingJob,
            transformIntoVideo(videoToEdit));
    videoToEdit.setTranscodingTask(videoListenableFuture);
    return videoListenableFuture;
  }

  public void adaptVideoWithRotationToDefaultFormatAsync(
          final Video videoToAdapt, final VideonaFormat format, final String destVideoPath,
          final int rotation, final TranscoderHelperListener listener, final String tempDirectory)
          throws IOException {
    ListenableFuture<Void> transcodingJob;
    String tempPath;
    try {
      tempPath = videoToAdapt.getVolume() != Video.DEFAULT_VOLUME
              ? videoToAdapt.getTempPath() : destVideoPath;
      transcodingJob = mediaTranscoder.transcodeVideoWithRotationToDefaultFormat(
              videoToAdapt.getMediaPath(), format, tempPath, rotation);
    } catch (IOException e) {
      e.printStackTrace();
      listener.onErrorTranscoding(videoToAdapt, e.getMessage());
      return;
    }

    if (videoToAdapt.getVolume() != Video.DEFAULT_VOLUME) {
      transcodingJob = Futures.transform(transcodingJob,
              getAudioGainApplierFunction(tempDirectory, videoToAdapt, destVideoPath, listener));
    } else {
//          FileUtils.moveFile(videoToAdapt.getTempPath(), destVideoPath);
//          Log.d(TAG, "Moving file " + videoToAdapt.getTempPath() + " to " + destVideoPath);
      // (jliarte): 14/07/17 seems that application expects the mediapath to be the temp file
//          videoToAdapt.setMediaPath(destVideoPath);
      transcodingJob = Futures.transform(transcodingJob,
              getSuccessNotifierFunction(videoToAdapt, listener),
              MoreExecutors.newDirectExecutorService());
    }
    ListenableFuture<Video> videoListenableFuture = Futures.transform(transcodingJob,
            transformIntoVideo(videoToAdapt));
    videoToAdapt.setTranscodingTask(videoListenableFuture);
    // TODO(jliarte): 18/09/17 do we still need to wait for job to start?
//    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
//    executor.schedule(runnable, 2, TimeUnit.SECONDS);
  }

  @NonNull
  private Function<Void, Void> getAudioGainApplierFunction(
          final String tempDirectory, final Video videoToAdapt,
          final String destVideoPath, final TranscoderHelperListener listener) {
    return new Function<Void, Void>() {
      @Override
      public Void apply(Void input) {
        try {
          // TODO(jliarte): 13/07/17 should I use this name?
          final String audioWithGainOutputFile = tempDirectory
                  + File.separator + Constants.MIXED_AUDIO_FILE_NAME;
          List<Media> mediaList = Collections.singletonList((Media) videoToAdapt);
          long videoDuration = FileUtils.getDurationFile(videoToAdapt.getTempPath());
          ListenableFuture<Boolean> booleanListenableFuture = mediaTranscoder.mixAudioFiles(
                  mediaList, tempDirectory, audioWithGainOutputFile, videoDuration);
          booleanListenableFuture.get();
          VideoAudioSwapper videoAudioSwapper = new VideoAudioSwapper();
          videoAudioSwapper.export(videoToAdapt.getTempPath(), audioWithGainOutputFile,
                  destVideoPath);
          FileUtils.removeFile(audioWithGainOutputFile);
          listener.onSuccessTranscoding(videoToAdapt);
        } catch (InterruptedException | ExecutionException e) {
          // TODO(jliarte): 5/10/17 should we propagate error?
          e.printStackTrace();
//              listener.onErrorTranscoding(videoToAdapt, e.getMessage());
        } catch (TranscodingException | IOException transcodingError) {
          transcodingError.printStackTrace();
//          FileUtils.removeFile(audioWithGainOutputFile);
          listener.onErrorTranscoding(videoToAdapt, transcodingError.getMessage());
        }
        return null;
      }
    };
  }

  public ListenableFuture<Void> generateOutputAudioVoiceOver(String originFilePath, String
                                                                   destFilePath){
    return mediaTranscoder.transcodeAudioVoiceOver(originFilePath, destFilePath);
  }


  private void waitTranscodingJobAndCheckState(ListenableFuture<Void> chainedTranscodingJob,
                                             TranscoderHelperListener listener, Video videoToEdit) {
    try {
      chainedTranscodingJob.get();
    } catch (ExecutionException e) {
      listener.onErrorTranscoding(videoToEdit, e.getMessage());
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO(jliarte): 18/07/17 seems to be also catched when the job has ended
      e.printStackTrace();
    } finally {
      if(!chainedTranscodingJob.isDone())
        listener.onErrorTranscoding(videoToEdit, "generateOutputVideoWithAVTransitionsAsync");
    }
  }

  private Image getImageFromTextAndPosition(String text, String textPosition) {
    Drawable textDrawable = drawableGenerator.createDrawableWithTextAndPosition(text, textPosition,
            Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
    return new Image(textDrawable, Constants.DEFAULT_CANVAS_WIDTH, Constants.DEFAULT_CANVAS_HEIGHT);
  }

  void generateFileWithAudioFadeInFadeOutAsync(final String inputFile, final int timeFadeInMs,
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

  private Function<? super Void, ? extends Void> getSuccessNotifierFunction(
          final Video videoToEdit, final TranscoderHelperListener listener) {
    return new Function<Void, Void>() {
      @Override
      public Void apply(Void input) {
        notifySuccessVideoTranscodedToListener(videoToEdit, listener);
        return input;
      }
    };
  }

  private Function<? super Void, ? extends Void> applyAudioFadeInOut(
          final Video videoToEdit,
          final String intermediatesTempAudioFadeDirectory) {
    return new Function<Void, Void>() {
      @Override
      public Void apply(Void input) {
        ApplyAudioFadeInFadeOutToVideo.OnApplyAudioFadeInFadeOutToVideoListener audioListener =
            new ApplyAudioFadeInFadeOutToVideo.OnApplyAudioFadeInFadeOutToVideoListener() {
              @Override
              public void OnGetAudioFadeInFadeOutError(String message, Video video) {
                Log.d(TAG, "OnGetAudioFadeInFadeOutError ");
                // TODO(jliarte): 15/09/17 how to adapt this function now?
//                listener.onErrorTranscoding(video, message);
                throw new RuntimeException(message);
              }

              @Override
              public void OnGetAudioFadeInFadeOutSuccess(Video video) {
                Log.d(TAG, "OnGetAudioFadeInFadeOutSuccess ");
                // TODO(jliarte): 15/09/17 how to adapt this function now?
//                notifySuccessVideoTranscodedToListener(videoToEdit, listener);
              }
            };
        ApplyAudioFadeInFadeOutToVideo applyAudioFadeInFadeOutToVideo =
            new ApplyAudioFadeInFadeOutToVideo(audioListener, intermediatesTempAudioFadeDirectory);
        try {
          applyAudioFadeInFadeOutToVideo.applyAudioFadeToVideo(videoToEdit,
              TIME_FADE_IN_MS, TIME_FADE_OUT_MS);
        } catch (IOException ex) {
          ex.printStackTrace();
          throw new RuntimeException(ex);
        }
        return input;
      }
    };
  }

  private void notifySuccessVideoTranscodedToListener(Video videoToEdit,
                                                      TranscoderHelperListener listener) {
    Log.d(TAG, "notifySuccessVideoTranscodedToListener " + videoToEdit.getMediaPath());
    listener.onSuccessTranscoding(videoToEdit);
  }

  private void cancelPendingTranscodingTasks(Video videoToEdit) {
    ListenableFuture<Video> transcodingTask = videoToEdit.getTranscodingTask();
    if (transcodingTask != null && !transcodingTask.isDone()) {
      Log.d(TAG, "Cancel transcoding task " + transcodingTask.toString());
      transcodingTask.cancel(true);
    }
  }
}