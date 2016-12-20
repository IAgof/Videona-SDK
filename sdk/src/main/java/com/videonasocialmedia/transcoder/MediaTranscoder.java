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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.videonasocialmedia.transcoder.audio.AudioEffect;
import com.videonasocialmedia.transcoder.audio.AudioMixer;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.video.engine.MediaTranscoderEngine;
import com.videonasocialmedia.transcoder.video.format.MediaFormatStrategy;
import com.videonasocialmedia.transcoder.video.overlay.Overlay;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
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
    public Future<Void> transcodeOnlyVideo(final String inPath, final String outPath,
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

        return transcodeOnlyVideo(fileInputStream, inFileDescriptor, outPath, outFormatStrategy,
                listener);

    }

    public Future<Void> transcodeOnlyVideo(final FileInputStream fileInputStream,final FileDescriptor inFileDescriptor, final String outPath,
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
                    engine.transcodeOnlyVideo(outPath, outFormatStrategy);
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
    public Future<Void> transcodeAndTrimVideo(final String inPath, final String outPath,
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

        return transcodeAndTrimVideo(fileInputStream, inFileDescriptor,outPath,outFormatStrategy,
                listener,startTimeUs, endTimeUs);


    }

    public Future<Void> transcodeAndTrimVideo(final FileInputStream fileInputStream,final FileDescriptor inFileDescriptor, final String outPath,
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
                    engine.transcodeAndTrimVideo(outPath, outFormatStrategy, startTimeUs, endTimeUs);
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
    public Future<Void> transcodeAndOverlayImageToVideo(final String inPath, final String outPath,
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

        return transcodeAndOverlayImageToVideo(fileInputStream, inFileDescriptor, outPath, outFormatStrategy,
                listener, overlay);

    }


    public Future<Void> transcodeAndOverlayImageToVideo(final FileInputStream fileInputStream,final FileDescriptor inFileDescriptor,final String outPath,
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
                    engine.transcodeAndOverlayImageVideo(outPath, outFormatStrategy, overlay);
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
    public Future<Void> transcodeTrimAndOverlayImageToVideo(final String inPath, final String outPath,
                                                        final MediaFormatStrategy outFormatStrategy,
                                                        final MediaTranscoderListener listener,
                                                        final Overlay overlay,final int startTimeUs, final int endTimeUs) throws IOException {
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


        return transcodeTrimAndOverlayImageToVideo(fileInputStream,inFileDescriptor, outPath, outFormatStrategy,
                listener, overlay, startTimeUs, endTimeUs);


    }

    public Future<Void> transcodeTrimAndOverlayImageToVideo(final FileInputStream fileInputStream,final FileDescriptor inFileDescriptor, final String outPath,
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
                    engine.transcodeTrimAndOverlayImageVideo(outPath, outFormatStrategy, overlay,
                            startTimeUs, endTimeUs);
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
                Exception caughtException = null;
                    // TODO(jliarte): 20/12/16 we don't set audioMixer listener, do we need to do it?
                    AudioMixer mixer = new AudioMixer(inputFile1, inputFile2, volume, tempDirectory,
                            outputFile);

                    mixer.export();

                final Exception exception = caughtException;
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