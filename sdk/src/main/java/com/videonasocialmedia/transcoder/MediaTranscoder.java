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

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.videonasocialmedia.transcoder.audio.AudioEffect;
import com.videonasocialmedia.transcoder.audio.AudioEncoder;
import com.videonasocialmedia.transcoder.audio.AudioMixer;
import com.videonasocialmedia.transcoder.video.engine.MediaTranscoderEngine;
import com.videonasocialmedia.transcoder.video.format.MediaFormatStrategy;
import com.videonasocialmedia.transcoder.video.overlay.Overlay;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;
import com.videonasocialmedia.videonamediaframework.model.media.Media;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static final int N_THREADS = 10;
    private static volatile MediaTranscoder sMediaTranscoderInstance;
    private final ListeningExecutorService executorPool;
    private MediaTranscoder() {
        executorPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(N_THREADS));
    }

    public static MediaTranscoder getInstance() {
        if (sMediaTranscoderInstance == null) {
            synchronized (MediaTranscoder.class) {
                if (sMediaTranscoderInstance == null) {
                    sMediaTranscoderInstance = new MediaTranscoder();
                }
            }
        }
        return sMediaTranscoderInstance;
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
        final InputFileProcessor inputFileProcessor = new InputFileProcessor(inPath)
                .processInputFile();
        final MediaTranscoderEngine engine =  new MediaTranscoderEngine();

        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inputFileProcessor.getInFileDescriptor());
                engine.transcodeOnlyVideo(drawableTransition, isFadeActivated,
                        outPath, outFormatStrategy);
                return null;
            }
        });

        Futures.addCallback(transcodingJob, new LoggerAndCleanerCallback(
                engine, inputFileProcessor.getFileInputStream(), outPath));
        return transcodingJob;
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
        final InputFileProcessor inputFileProcessor = new InputFileProcessor(inPath)
                .processInputFile();
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();

        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inputFileProcessor.getInFileDescriptor());
                engine.transcodeAndTrimVideo(drawableTransition, isFadeActivated, outPath,
                        outFormatStrategy, startTimeUs, endTimeUs);
                return null;
            }
        });

        Futures.addCallback(transcodingJob, new LoggerAndCleanerCallback(
                engine, inputFileProcessor.getFileInputStream(), outPath));
        return transcodingJob;
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
        final InputFileProcessor inputFileProcessor = new InputFileProcessor(inPath)
                .processInputFile();
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();

        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inputFileProcessor.getInFileDescriptor());
                engine.transcodeAndOverlayImageVideo(drawableTransition, isFadeActivated,
                        outPath, outFormatStrategy, overlay);
                return null;
            }
        });

        Futures.addCallback(transcodingJob, new LoggerAndCleanerCallback(
                engine, inputFileProcessor.getFileInputStream(), outPath));
        return transcodingJob;
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
                                                        final int endTimeUs) throws IOException {
        final InputFileProcessor inputFileProcessor = new InputFileProcessor(inPath)
                .processInputFile();
        final MediaTranscoderEngine engine = new MediaTranscoderEngine();

        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inputFileProcessor.getInFileDescriptor());
                engine.transcodeTrimAndOverlayImageVideo(drawableTransition, isFadeActivated,
                        outPath, outFormatStrategy, overlay, startTimeUs, endTimeUs);
                return null;
            }
        });

        Futures.addCallback(transcodingJob, new LoggerAndCleanerCallback(
                engine, inputFileProcessor.getFileInputStream(), outPath));
        return transcodingJob;
    }

    public ListenableFuture<Void>
    transcodeVideoWithRotationToDefaultFormat(final String origVideoPath,
                                              final MediaFormatStrategy outFormatStrategy,
                                              final String destVideoPath, final int rotation)
            throws IOException {
        final InputFileProcessor inputFileProcessor = new InputFileProcessor(origVideoPath)
                .processInputFile();
        final MediaTranscoderEngine engine = new MediaTranscoderEngine("0");

        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                engine.setDataSource(inputFileProcessor.getInFileDescriptor());
                engine.adaptMediaToFormatStrategyAndRotation(destVideoPath, outFormatStrategy,
                        rotation);
                return null;
            }
        });

        Futures.addCallback(transcodingJob, new LoggerAndCleanerCallback(
                engine, inputFileProcessor.getFileInputStream(), destVideoPath));
        return transcodingJob;
    }

    public ListenableFuture<Boolean> mixAudioFiles(
            final List<Media> mediaList, final String tempDirectory, final String outputFile,
            final long outputFileDuration) {
        return executorPool.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AudioMixer mixer = new AudioMixer();
                return mixer.export(mediaList, tempDirectory, outputFile, outputFileDuration);
            }
        });
    }

    public ListenableFuture<Boolean> mixAudioFilesWithFFmpeg (
        final List<Media> mediaList, final String tempDirectory, final String outputFile,
        final long outputFileDuration, final FFmpeg ffmpeg) throws IOException,
        TranscodingException {
        return executorPool.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AudioMixer mixer = new AudioMixer();
                return mixer.exportWithFFmpeg(mediaList, tempDirectory, outputFile,
                    outputFileDuration, ffmpeg);
            }
        });
    }

    public ListenableFuture<Void> audioFadeInFadeOutToFile(final String inputFile,
                                                           final int timeFadeIn,
                                                           final int timeFadeOut,
                                                           final String tempDirectory,
                                                           final String outputFile,
                                                           final MediaTranscoderListener listener) {
        final ListenableFuture<Void> transcodingJob = executorPool.submit(new Callable<Void>() {
           @Override
           public Void call() throws Exception {
               AudioEffect audioEffect = new AudioEffect(inputFile, timeFadeIn, timeFadeOut,
                   tempDirectory, outputFile);
               // TODO(jliarte): 18/04/17 why do we have commented this out?
               // audioEffect.setMediaTranscoderListener(listener);
               audioEffect.transitionFadeInOut();
               return null;
           }
        });

        Futures.addCallback(transcodingJob,
                new LoggerAndListenerNotifierCallback(listener, outputFile, transcodingJob));
        return transcodingJob;
    }

    public ListenableFuture<String> transcodeAudioVoiceOver(final String originFilePath,
                                                             final String destFilePath) {
        return executorPool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
            AudioEncoder audioEncoder = new AudioEncoder();
            audioEncoder.encodeToMp4(originFilePath, destFilePath);
            return destFilePath;
            }
        });
    }

    /**
     * New helper classes, mainly listenableFutures callbacks and delegates
     */

    public static class LoggerDelegate {
        public void onSuccess(Void result) {
            Log.d(TAG, "Transcode success " + result);
        }

        public void onFailure(Throwable t) {
            Log.d(TAG, "onFailure " + t.getMessage());
            Log.e(TAG, "Exception in task ", t.getCause());
        }
    }

    // TODO(jliarte): 18/04/17 Inspect the three different types of callbacks to see wether to unifiy all callbacks in one or not
    private static class LoggerCallback implements FutureCallback<Void> {
        private final LoggerDelegate loggerDelegate = new LoggerDelegate();

        @Override
        public void onSuccess(Void result) {
            loggerDelegate.onSuccess(result);
        }

        @Override
        public void onFailure(Throwable t) {
            loggerDelegate.onFailure(t);
        }
    }

    private static class LoggerAndListenerNotifierCallback extends LoggerCallback {
        private final MediaTranscoderListener listener;
        private final String outputFile;
        private final ListenableFuture<Void> transcodingJob;

        public LoggerAndListenerNotifierCallback(MediaTranscoderListener listener,
                                                 String outputFile,
                                                 ListenableFuture<Void> transcodingJob) {
            this.listener = listener;
            this.outputFile = outputFile;
            this.transcodingJob = transcodingJob;
        }

        @Override
        public void onSuccess(Void result) {
            // TODO(jliarte): 18/04/17 use delegate also here?
            super.onSuccess(result);
            listener.onTranscodeSuccess(outputFile);
        }

        @Override
        public void onFailure(Throwable t) {
            super.onFailure(t);
            if (transcodingJob != null && transcodingJob.isCancelled()) {
                listener.onTranscodeCanceled();
            } else {
                listener.onTranscodeError(t.getMessage());
            }
        }
    }

    private static class LoggerAndCleanerCallback implements FutureCallback<Void> {
        private final MediaTranscoderEngine engine;
        private final String outPath;
        private final FileInputStream fileInputStream;
        private final LoggerDelegate loggerDelegate = new LoggerDelegate();

        public LoggerAndCleanerCallback(MediaTranscoderEngine engine,
                                        FileInputStream fileInputStream, String outPath) {
            this.engine = engine;
            this.fileInputStream = fileInputStream;
            this.outPath = outPath;
        }

        @Override
        public void onSuccess(Void result) {
            loggerDelegate.onSuccess(result);
            closeStream(fileInputStream);
        }

        @Override
        public void onFailure(Throwable t) {
            loggerDelegate.onFailure(t);
            closeStream(fileInputStream);
            if ((t.getMessage() != null) && (t.getMessage().compareTo("cancelled") == 0)) {
                engine.interruptTranscoding();
                FileUtils.removeFile(outPath);
                Log.d(TAG, "clean, remove temp file ");
            }
        }

        private void closeStream(FileInputStream fileInputStream) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public interface MediaTranscoderListener {
        void onTranscodeSuccess(String outputFile);

        void onTranscodeProgress(String progress);

        void onTranscodeError(String error);

        void onTranscodeCanceled();
    }

    private class InputFileProcessor {
        private String inPath;
        private FileInputStream fileInputStream;
        private FileDescriptor inFileDescriptor;

        public InputFileProcessor(String inPath) {
            this.inPath = inPath;
        }

        public FileInputStream getFileInputStream() {
            return fileInputStream;
        }

        public FileDescriptor getInFileDescriptor() {
            return inFileDescriptor;
        }

        public InputFileProcessor processInputFile() throws IOException {
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
            return this;
        }
    }
}