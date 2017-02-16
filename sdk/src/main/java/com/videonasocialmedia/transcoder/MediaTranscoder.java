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
import android.util.Log;

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
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static volatile MediaTranscoder sMediaTranscoder;

    private MediaTranscoder() {
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
     */
    public ListenableFuture<Void> transcodeOnlyVideo(final Drawable drawableTransition,
                                                     final boolean isFadeActivated,
                                                     final String inPath,
                                                     final String outPath,
                                                     final MediaFormatStrategy outFormatStrategy)
                                                        throws IOException {

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
            inFileDescriptor, outPath, outFormatStrategy);

    }

    public ListenableFuture<Void> transcodeOnlyVideo(final Drawable drawableTransition,
                                           final boolean isFadeActivated,
                                           final FileInputStream fileInputStream,
                                           final FileDescriptor inFileDescriptor,
                                           final String outPath,
                                           final MediaFormatStrategy outFormatStrategy) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();
        final ListenableFuture<Void> future = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inFileDescriptor);
                engine.transcodeOnlyVideo(drawableTransition, isFadeActivated,
                    outPath, outFormatStrategy);

                return null;
            }
        });

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                closeStream(fileInputStream);
            }
            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                closeStream(fileInputStream);
                engine.setInterruptTranscoding();
            }
        });

        return future;
    }


    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param startTimeUs
     * @param endTimeUs
     */
    public ListenableFuture<Void> transcodeAndTrimVideo(final Drawable drawableTransition,
                                              final boolean isFadeActivated,
                                              final String inPath,
                                              final String outPath,
                                              final MediaFormatStrategy outFormatStrategy,
                                              final int startTimeUs, final int endTimeUs)
                                                throws IOException {

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
            inFileDescriptor,outPath,outFormatStrategy, startTimeUs, endTimeUs);


    }

    public ListenableFuture<Void> transcodeAndTrimVideo(final Drawable drawableTransition,
                                              final boolean isFadeActivated,
                                              final FileInputStream fileInputStream,
                                              final FileDescriptor inFileDescriptor,
                                              final String outPath,
                                              final MediaFormatStrategy outFormatStrategy,
                                              final int startTimeUs, final int endTimeUs) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();
        final ListenableFuture<Void> createdFuture = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inFileDescriptor);
                engine.transcodeAndTrimVideo(drawableTransition, isFadeActivated, outPath,
                    outFormatStrategy, startTimeUs, endTimeUs);
                return null;
            }
        });

        Futures.addCallback(createdFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                closeStream(fileInputStream);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                closeStream(fileInputStream);
                engine.setInterruptTranscoding();
            }
        });

        return createdFuture;
    }
    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     * @param inPath  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param overlay
     */
    public ListenableFuture<Void> transcodeAndOverlayImageToVideo(final Drawable drawableTransition,
                                                        final boolean isFadeActivated,
                                                        final String inPath,
                                                        final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
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
            inFileDescriptor, outPath, outFormatStrategy, overlay);

    }


    public ListenableFuture<Void> transcodeAndOverlayImageToVideo(final Drawable drawableTransition,
                                                        final boolean isFadeActivated,
                                                        final FileInputStream fileInputStream,
                                                        final FileDescriptor inFileDescriptor,
                                                        final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
                                                        final Overlay overlay) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();
        final ListenableFuture<Void> createdFuture = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inFileDescriptor);
                engine.transcodeAndOverlayImageVideo(drawableTransition, isFadeActivated,
                    outPath, outFormatStrategy, overlay);
                return null;
            }
        });

        Futures.addCallback(createdFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                closeStream(fileInputStream);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                closeStream(fileInputStream);
                engine.setInterruptTranscoding();
            }
        });

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
    public ListenableFuture<Void> transcodeTrimAndOverlayImageToVideo(
                                                        final Drawable drawableTransition,
                                                        final boolean isFadeActivated,
                                                        final String inPath,
                                                        final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
                                                        final Overlay overlay,
                                                        final int startTimeUs,
                                                        final int endTimeUs)
                                                            throws IOException {
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

    public ListenableFuture<Void> transcodeTrimAndOverlayImageToVideo(
                                                    final Drawable drawableTransition,
                                                    final boolean isFadeTransition,
                                                    final FileInputStream fileInputStream,
                                                    final FileDescriptor inFileDescriptor,
                                                    final String outPath,
                                                    final MediaFormatStrategy outFormatStrategy,
                                                    final Overlay overlay, final int startTimeUs,
                                                    final int endTimeUs) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();
        final ListenableFuture<Void> createdFuture = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inFileDescriptor);
                engine.transcodeTrimAndOverlayImageVideo(drawableTransition, isFadeTransition,
                    outPath, outFormatStrategy, overlay, startTimeUs, endTimeUs);
                return null;
            }
        });

        Futures.addCallback(createdFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                closeStream(fileInputStream);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                closeStream(fileInputStream);
                engine.setInterruptTranscoding();
            }
        });

        return createdFuture;
    }

    private void closeStream(FileInputStream fileInputStream) {
        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ListenableFuture<Void> mixAudioTwoFiles(final String inputFile1,
                                                     final String inputFile2,
                                                     final float volume,
                                                     final String tempDirectory,
                                                     final String outputFile,
                                                     final OnAudioMixerListener listener) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final ListenableFuture<Void> createdFuture = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AudioMixer mixer = new AudioMixer(inputFile1, inputFile2, volume, tempDirectory,
                    outputFile);
                mixer.setOnAudioMixerListener(listener);
                mixer.export();
                return null;
            }
        });

        Futures.addCallback(createdFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                listener.onAudioMixerSuccess(outputFile);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                if (createdFuture != null && createdFuture.isCancelled()) {
                    listener.onAudioMixerCanceled();
                } else {
                    listener.onAudioMixerError(t.getMessage());
                }
            }
        });
        return createdFuture;
    }

    public ListenableFuture<Void> audioFadeInFadeOutToFile(final String inputFile,
                                                             final int timeFadeIn,
                                                             final int timeFadeOut,
                                                             final String tempDirectory,
                                                             final String outputFile,
                                                             final OnAudioEffectListener listener) {

        ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        final ListenableFuture<Void> createdFuture = pool.submit(new Callable<Void>() {
           @Override
           public Void call() throws Exception {

               AudioEffect audioEffect = new AudioEffect(inputFile, timeFadeIn, timeFadeOut,
                   tempDirectory, outputFile);
               // audioEffect.setOnAudioEffectListener(listener);
               audioEffect.transitionFadeInOut();
               return null;
           }
        });

        Futures.addCallback(createdFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Transcode " + result);
                listener.onAudioEffectSuccess(outputFile);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Exception in task", t.getCause());
                if (createdFuture != null && createdFuture.isCancelled()) {
                    listener.onAudioEffectCanceled();
                } else {
                    listener.onAudioEffectError(t.getMessage());
                }
            }
        });
        return createdFuture;
    }

}