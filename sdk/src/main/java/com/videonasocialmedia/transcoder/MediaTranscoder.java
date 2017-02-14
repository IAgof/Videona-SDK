/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.videonasocialmedia.transcoder;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.videonasocialmedia.transcoder.audio.AudioEffect;
import com.videonasocialmedia.transcoder.audio.AudioMixer;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.video.engine.MediaTranscoderEngine;
import com.videonasocialmedia.transcoder.video.format.MediaFormatStrategy;
import com.videonasocialmedia.transcoder.video.overlay.Overlay;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.pipeline.ApplyAudioFadeInFadeOutToVideo;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static final int MAXIMUM_THREAD = 1; // TODO
    private static volatile MediaTranscoder sMediaTranscoder;
    private ThreadPoolExecutor mExecutor;


    private MediaTranscoder() {
        mExecutor = new ThreadPoolExecutor(
                0, MAXIMUM_THREAD, 300, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "MediaTranscoder-Worker");
                    }
                });
    }

    public static MediaTranscoder getInstance() {
        if (sMediaTranscoder == null) {
            synchronized (MediaTranscoder.class) {
                if (sMediaTranscoder == null) {
                    sMediaTranscoder = new MediaTranscoder();
                }
            }
        }
        return sMediaTranscoder;
    }


    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     */
    public Future<Void> transcodeOnlyVideo(final Drawable drawableTransition,
                                           final boolean isFadeActivated,
                                           final String inPath,
                                           final String outPath,
                                           final MediaFormatStrategy outFormatStrategy,
                                           final MediaTranscoderListener listener) throws IOException {

        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }

        return transcodeOnlyVideo(drawableTransition, isFadeActivated, fileInputStream,
            inFileDescriptor, outPath, outFormatStrategy, listener);

    }

    public Future<Void> transcodeOnlyVideo(final Drawable drawableTransition,
                                           final boolean isFadeActivated,
                                           final FileInputStream fileInputStream,
                                           final FileDescriptor inFileDescriptor,
                                           final String outPath,
                                           final MediaFormatStrategy outFormatStrategy,
                                           final MediaTranscoderListener listener) throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeOnlyVideo(drawableTransition, isFadeActivated, outPath,
                        outFormatStrategy);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            closeStream(fileInputStream);
                            listener.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                closeStream(fileInputStream);
                                listener.onTranscodeCanceled();
                            } else {
                                closeStream(fileInputStream);
                                listener.onTranscodeFailed(exception);
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
    }


    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param startTimeUs
     * @param endTimeUs
     * @param listener          Listener instance for callback.
     */
    public Future<Void> transcodeAndTrimVideo(final Drawable drawableTransition,
                                              final boolean isFadeActivated,
                                              final String inPath,
                                              final String outPath,
                                              final MediaFormatStrategy outFormatStrategy,
                                              final MediaTranscoderListener listener,
                                              final int startTimeUs, final int endTimeUs)  throws IOException {


        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }

        return transcodeAndTrimVideo(drawableTransition, isFadeActivated, fileInputStream,
            inFileDescriptor,outPath,outFormatStrategy, listener,startTimeUs, endTimeUs);


    }

    public Future<Void> transcodeAndTrimVideo(final Drawable drawableTransition,
                                              final boolean isFadeActivated,
                                              final FileInputStream fileInputStream,
                                              final FileDescriptor inFileDescriptor,
                                              final String outPath,
                                              final MediaFormatStrategy outFormatStrategy,
                                              final MediaTranscoderListener listener,
                                              final int startTimeUs, final int endTimeUs)  throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeAndTrimVideo(drawableTransition, isFadeActivated, outPath,
                        outFormatStrategy, startTimeUs, endTimeUs);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            closeStream(fileInputStream);
                            listener.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                closeStream(fileInputStream);
                                listener.onTranscodeCanceled();
                            } else {
                                closeStream(fileInputStream);
                                listener.onTranscodeFailed(exception);
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;

    }

    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     * @param overlay
     */
    public Future<Void> transcodeAndOverlayImageToVideo(final Drawable drawableTransition,
                                                        final boolean isFadeActivated,
                                                        final String inPath,
                                                        final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
                                                        final MediaTranscoderListener listener,
                                                        final Overlay overlay) throws IOException {


            FileInputStream fileInputStream = null;
            FileDescriptor inFileDescriptor;
            try {
                fileInputStream = new FileInputStream(inPath);
                inFileDescriptor = fileInputStream.getFD();
            } catch (IOException e) {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException eClose) {
                        Log.e(TAG, "Can't close input stream: ", eClose);
                    }
                }
                throw e;
            }

        return transcodeAndOverlayImageToVideo(drawableTransition, isFadeActivated, fileInputStream,
            inFileDescriptor, outPath, outFormatStrategy, listener, overlay);

    }


    public Future<Void> transcodeAndOverlayImageToVideo(final Drawable drawableTransition,
                                                        final boolean isFadeActivated,
                                                        final FileInputStream fileInputStream,
                                                        final FileDescriptor inFileDescriptor,
                                                        final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
                                                        final MediaTranscoderListener listener,
                                                        final Overlay overlay) throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeAndOverlayImageVideo(drawableTransition, isFadeActivated,
                        outPath, outFormatStrategy, overlay);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            closeStream(fileInputStream);
                            listener.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                closeStream(fileInputStream);
                                listener.onTranscodeCanceled();
                            } else {
                                closeStream(fileInputStream);
                                listener.onTranscodeFailed(exception);
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
    }



    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     * @param overlay
     * @param startTimeUs
     * @param endTimeUs
     */
    public Future<Void> transcodeTrimAndOverlayImageToVideo(final Drawable drawableTransition,
                                                                      final boolean isFadeActivated,
                                                                      final String inPath,
                                                                      final String outPath,
                                                                      final MediaFormatStrategy outFormatStrategy,
                                                                      final MediaTranscoderListener listener,
                                                                      final Overlay overlay,
                                                                      final int startTimeUs, final int endTimeUs) throws IOException {
        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }


        return transcodeTrimAndOverlayImageToVideo(drawableTransition, isFadeActivated,
            fileInputStream,inFileDescriptor, outPath, outFormatStrategy, listener, overlay,
            startTimeUs, endTimeUs);


    }

    public Future<Void> transcodeTrimAndOverlayImageToVideo(final Drawable drawableTransition,
                                                            final boolean isFadeTransition,
                                                            final FileInputStream fileInputStream,
                                                            final FileDescriptor inFileDescriptor,
                                                            final String outPath,
                                                            final MediaFormatStrategy outFormatStrategy,
                                                            final MediaTranscoderListener listener,
                                                            final Overlay overlay,final int startTimeUs, final int endTimeUs) throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeTrimAndOverlayImageVideo(drawableTransition, isFadeTransition,
                        outPath, outFormatStrategy, overlay, startTimeUs, endTimeUs);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            closeStream(fileInputStream);
                            listener.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                closeStream(fileInputStream);
                                listener.onTranscodeCanceled();
                            } else {
                                closeStream(fileInputStream);
                                listener.onTranscodeFailed(exception);
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }

        });
        futureReference.set(createdFuture);
        return createdFuture;
    }

    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param overlay
     * @param startTimeUs
     * @param endTimeUs
     */
    public ListenableFuture<String> transcodeTrimAndOverlayImageToVideo(final Drawable drawableTransition,
                                                                        final boolean isFadeActivated,
                                                                        final String inPath,
                                                                        final String outPath,
                                                                        final MediaFormatStrategy outFormatStrategy,
                                                                        final Overlay overlay,
                                                                        final int startTimeUs, final int endTimeUs) throws IOException {
        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }


        return transcodeTrimAndOverlayImageToVideo(drawableTransition, isFadeActivated,
            fileInputStream,inFileDescriptor, outPath, outFormatStrategy, overlay,
            startTimeUs, endTimeUs);


    }

    public ListenableFuture<String> transcodeTrimAndOverlayImageToVideo(final Drawable drawableTransition,
                                                                        final boolean isFadeTransition,
                                                                        final FileInputStream fileInputStream,
                                                                        final FileDescriptor inFileDescriptor,
                                                                        final String outPath,
                                                                        final MediaFormatStrategy outFormatStrategy,
                                                                        final Overlay overlay,final int startTimeUs, final int endTimeUs) throws IOException {

        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final ListenableFuture<String> future = pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                //Exception caughtException = null;
                String caughtException = "ok";

                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
              /*  engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                    @Override
                    public void onProgress(final double progress) {
                        handler.post(new Runnable() { // TODO: reuse instance
                            @Override
                            public void run() {
                                listener.onTranscodeProgress(progress);
                            }
                        });
                    }
                });*/
                    engine.setDataSource(inFileDescriptor);
                    // TODO:(alvaro.martinez) 14/02/17 check how to stop the transcoding job from here

                    engine.transcodeTrimAndOverlayImageVideo(drawableTransition, isFadeTransition,
                        outPath, outFormatStrategy, overlay, startTimeUs, endTimeUs);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                        + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e.toString();
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e.toString();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e.toString();
                }

                return caughtException;
            }
        });

    /* future.addListener(new Runnable() {
        @Override
        public void run() {
            try {
                final String contents = future.get();
                Log.d(TAG, "Contents future.get() " + contents);
                closeStream(fileInputStream);
                if(contents.compareTo("ok") == 0){
                    // transcode ok, continue to next step
                    Log.d(TAG, "Transcode ok");
                } else {
                   // error transcoding
                    Log.d(TAG, "Transcode error " + contents);
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted", e);
            } catch (ExecutionException e) {
                Log.e(TAG, "Exception in task", e.getCause());
            }
        }
    }, MoreExecutors.sameThreadExecutor()); */

        Futures.addCallback(future, new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Transcode " + result);
                closeStream(fileInputStream);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                closeStream(fileInputStream);
            }
        });


        return future;
    }

    private void closeStream(FileInputStream fileInputStream) {
        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Future<Void> mixAudioTwoFiles(final String inputFile1, final String inputFile2, final float volume,
                                         final String tempDirectory, final String outputFile, final OnAudioMixerListener listener) throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AudioMixer mixer = new AudioMixer(inputFile1, inputFile2, volume, tempDirectory,
                        outputFile);
                mixer.setOnAudioMixerListener(listener);
                mixer.export();

                // TODO(jliarte): 2/01/17 why do we do this post with if? as exception is final and
                //                null, it will never change!
                final Exception exception = null;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listener.onAudioMixerSuccess(outputFile);
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                listener.onAudioMixerCanceled();
                            } else {
                                listener.onAudioMixerError(exception.getMessage());
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
    }

    public Future<Void> audioFadeInFadeOutToFile(final String inputFile, final int timeFadeIn,
                                                 final int timeFadeOut, final String tempDirectory,
                                                 final String outputFile, final OnAudioEffectListener listener) throws IOException {

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Exception caughtException = null;

                AudioEffect audioEffect = new AudioEffect(inputFile, timeFadeIn, timeFadeOut,
                        tempDirectory, outputFile);
               // audioEffect.setOnAudioEffectListener(listener);

                audioEffect.transitionFadeInOut();

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listener.onAudioEffectSuccess(outputFile);
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                listener.onAudioEffectCanceled();
                            } else {
                                listener.onAudioEffectError(exception.getMessage());
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
    }

}