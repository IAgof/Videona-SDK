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
package com.videonasocialmedia.transcoder.video.engine;

import android.graphics.drawable.Drawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import com.videonasocialmedia.transcoder.video.exceptions.InvalidOutputFormatException;
import com.videonasocialmedia.transcoder.video.format.MediaFormatStrategy;
import com.videonasocialmedia.transcoder.video.overlay.Overlay;
import com.videonasocialmedia.transcoder.video.utils.MediaExtractorUtils;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Internal engine, do not use this directly.
 */
// TODO: treat encrypted data
public class MediaTranscoderEngine {

    private static final String TAG = "MediaTranscoderEngine";

    private static final double PROGRESS_UNKNOWN = -1.0;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;
    private FileDescriptor mInputFileDescriptor;
    private TrackTranscoder mVideoTrackTranscoder;
    private TrackTranscoder mAudioTrackTranscoder;
    private MediaExtractor mExtractor;
    private MediaMuxer mMuxer;
    private volatile double mProgress;
    private ProgressCallback mProgressCallback;
    private long mDurationUs;

    private MediaExtractorUtils.TrackResult trackResult;
    private MediaFormat videoOutputFormat;
    private MediaFormat audioOutputFormat;
    private Muxer muxer;

    /**
     * Do not use this constructor unless you know what you are doing.
     */
    public MediaTranscoderEngine() {
    }

    public void setDataSource(FileDescriptor fileDescriptor) {
        mInputFileDescriptor = fileDescriptor;
    }

    public ProgressCallback getProgressCallback() {
        return mProgressCallback;
    }

    public void setProgressCallback(ProgressCallback progressCallback) {
        mProgressCallback = progressCallback;
    }

    /**
     * NOTE: This method is thread safe.
     */
    public double getProgress() {
        return mProgress;
    }


    /**
     * Run video transcoding. Blocks current thread.
     *
     * @param outputPath     File path to output transcoded video file.
     * @param formatStrategy Output format strategy.
     * @throws IOException                  when input or output file could not be opened.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException         when cancel to transcode.
     */
    public void transcodeOnlyVideo(Drawable fadeTransition, boolean isFadeActivated,
                                   String outputPath, MediaFormatStrategy formatStrategy)
            throws IOException, InterruptedException {


        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            // NOTE: use single extractor to keep from running out audio track fast.
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupMetadata();
            setupOutputFormat(formatStrategy);
            setupAudioTranscoder();
            setupVideoTranscoder(fadeTransition, isFadeActivated);
            //setupTrackTranscoders(formatStrategy);
            runPipelines();
            mMuxer.stop();
        } finally {
            try {
                if (mVideoTrackTranscoder != null) {
                    mVideoTrackTranscoder.release();
                    mVideoTrackTranscoder = null;
                }
                if (mAudioTrackTranscoder != null) {
                    mAudioTrackTranscoder.release();
                    mAudioTrackTranscoder = null;
                }
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown extractor, codecs and muxer pipeline.", e);
            }
            try {
                if (mMuxer != null) {
                    mMuxer.release();
                    mMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release muxer.", e);
            }
        }
    }


    /**
     * Run video transcoding. Blocks current thread.
     *
     * @param outputPath     File path to output transcoded video file.
     * @param formatStrategy Output format strategy.
     * @param overlay Drawable image to overlay in video
     * @throws IOException                  when input or output file could not be opened.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException         when cancel to transcode.
     */
    public void transcodeAndOverlayImageVideo(Drawable fadeTransition,  boolean isFadeActivated,
                                              String outputPath,
                                              MediaFormatStrategy formatStrategy,
                                              Overlay overlay)
            throws IOException, InterruptedException {


        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            // NOTE: use single extractor to keep from running out audio track fast.
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupMetadata();
            setupOutputFormat(formatStrategy);
            setupAudioTranscoder();
            setupVideoTranscoder(fadeTransition, isFadeActivated, overlay);
            //setupTrackTranscoders(formatStrategy, overlay);
            runPipelines();
            mMuxer.stop();
        } finally {
            try {
                if (mVideoTrackTranscoder != null) {
                    mVideoTrackTranscoder.release();
                    mVideoTrackTranscoder = null;
                }
                if (mAudioTrackTranscoder != null) {
                    mAudioTrackTranscoder.release();
                    mAudioTrackTranscoder = null;
                }
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown extractor, codecs and muxer pipeline.", e);
            }
            try {
                if (mMuxer != null) {
                    mMuxer.release();
                    mMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release muxer.", e);
            }
        }
    }

    /**
     * Run video transcoding. Blocks current thread.
     *
     * @param outputPath     File path to output transcoded video file.
     * @param formatStrategy Output format strategy.
     * @param startTimeMs Seek video to startTime and begin transcode
     * @param endTimeMs Stop time transcoding, instead of EOS,
     * @throws IOException                  when input or output file could not be opened.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException         when cancel to transcode.
     */
    public void transcodeAndTrimVideo(Drawable fadeTransition, boolean isFadeActivated,
                                      String outputPath, MediaFormatStrategy formatStrategy,
                                      int startTimeMs, int endTimeMs)
        throws IOException, InterruptedException {


        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            // NOTE: use single extractor to keep from running out audio track fast.
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupMetadata();
            setupOutputFormat(formatStrategy);
            setupAudioTranscoder();
            setupVideoTranscoder(fadeTransition, isFadeActivated);
            setupRangeTimeVideoTranscoder(startTimeMs, endTimeMs);
            //setupTrackTranscoders(formatStrategy, startTimeMs, endTimeMs);
            runPipelines();
            mMuxer.stop();
        } finally {
            try {
                if (mVideoTrackTranscoder != null) {
                    mVideoTrackTranscoder.release();
                    mVideoTrackTranscoder = null;
                }
                if (mAudioTrackTranscoder != null) {
                    mAudioTrackTranscoder.release();
                    mAudioTrackTranscoder = null;
                }
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown extractor, codecs and muxer pipeline.", e);
            }
            try {
                if (mMuxer != null) {
                    mMuxer.release();
                    mMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release muxer.", e);
            }
        }
    }

    public void transcodeTrimAndOverlayImageVideo(Drawable fadeTransition, boolean isFadeTransition,
                                                  String outputPath,
                                                  MediaFormatStrategy formatStrategy,
                                                  Overlay overlay,
                                                  int startTimeMs, int endTimeMs)
        throws IOException, InterruptedException {


        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            // NOTE: use single extractor to keep from running out audio track fast.
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupMetadata();
            setupOutputFormat(formatStrategy);
            setupAudioTranscoder();
            setupVideoTranscoder(fadeTransition, isFadeTransition, overlay);
            setupRangeTimeVideoTranscoder(startTimeMs, endTimeMs);
            //setupTrackTranscoders(formatStrategy,overlay,startTimeMs, endTimeMs);
            runPipelines();
            mMuxer.stop();
        } finally {
            try {
                if (mVideoTrackTranscoder != null) {
                    mVideoTrackTranscoder.release();
                    mVideoTrackTranscoder = null;
                }
                if (mAudioTrackTranscoder != null) {
                    mAudioTrackTranscoder.release();
                    mAudioTrackTranscoder = null;
                }
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown extractor, codecs and muxer pipeline.", e);
            }
            try {
                if (mMuxer != null) {
                    mMuxer.release();
                    mMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release muxer.", e);
            }
        }
    }

    private void setupOutputFormat(MediaFormatStrategy formatStrategy) {
        trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(mExtractor);
        videoOutputFormat = formatStrategy.createVideoOutputFormat(trackResult.mVideoTrackFormat);
        audioOutputFormat = formatStrategy.createAudioOutputFormat(trackResult.mAudioTrackFormat);

        if (videoOutputFormat == null && audioOutputFormat == null) {
            throw new InvalidOutputFormatException("MediaFormatStrategy returned pass-through for" +
                " both video and audio. No transcoding is necessary.");
        }

        muxer = new Muxer(mMuxer, new Muxer.Listener() {
            @Override
            public void onDetermineOutputFormat() {
            MediaFormatValidator.validateVideoOutputFormat(mVideoTrackTranscoder.getDeterminedFormat());
            MediaFormatValidator.validateAudioOutputFormat(mAudioTrackTranscoder.getDeterminedFormat());
            }
        });

    }

    private void setupAudioTranscoder(){
        if (audioOutputFormat == null) {
            mAudioTrackTranscoder = new PassThroughTrackTranscoder(mExtractor,
                trackResult.mAudioTrackIndex, muxer, Muxer.SampleType.AUDIO);
        } else {
            mAudioTrackTranscoder = new AudioTrackTranscoder(mExtractor,
                trackResult.mAudioTrackIndex, audioOutputFormat, muxer);
        }
        mAudioTrackTranscoder.setup();
        mExtractor.selectTrack(trackResult.mAudioTrackIndex);
    }

    private void setupVideoTranscoder(Drawable fadeTransition, boolean isFadeActivated,
                                      Overlay overlay){
        if (videoOutputFormat == null) {
            mVideoTrackTranscoder = new PassThroughTrackTranscoder(mExtractor,
                trackResult.mVideoTrackIndex, muxer, Muxer.SampleType.VIDEO);
        } else {
            mVideoTrackTranscoder = new VideoTrackTranscoder(fadeTransition, isFadeActivated,
                mDurationUs, mExtractor, trackResult.mVideoTrackIndex,videoOutputFormat, muxer,
                overlay);
        }
        mVideoTrackTranscoder.setup();
        mExtractor.selectTrack(trackResult.mVideoTrackIndex);
    }

    private void setupVideoTranscoder(Drawable fadeTransition, boolean isFadeActivated){
        if (videoOutputFormat == null) {
            mVideoTrackTranscoder = new PassThroughTrackTranscoder(mExtractor,
                trackResult.mVideoTrackIndex, muxer, Muxer.SampleType.VIDEO);
        } else {
            mVideoTrackTranscoder = new VideoTrackTranscoder(fadeTransition, isFadeActivated,
                mDurationUs, mExtractor, trackResult.mVideoTrackIndex,videoOutputFormat, muxer);
        }
        mVideoTrackTranscoder.setup();
        mExtractor.selectTrack(trackResult.mVideoTrackIndex);
    }

    private void setupRangeTimeVideoTranscoder(int startTimeMs, int endTimeMs){

        if(startTimeMs*1000 > mDurationUs) {
            throw new InvalidOutputFormatException("start time bigger than durations file");
        } else {
            mVideoTrackTranscoder.advanceStart(startTimeMs);
        }

        if(endTimeMs*1000 > mDurationUs){
            throw new InvalidOutputFormatException("end time bigger than durations file");
        } else {
            mVideoTrackTranscoder.endOfDecoder(endTimeMs);
        }
    }


    private void setupMetadata() throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mInputFileDescriptor);

        String rotationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        try {
            mMuxer.setOrientationHint(Integer.parseInt(rotationString));
        } catch (NumberFormatException e) {
            // skip
        }

        // TODO: parse ISO 6709
        // String locationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
        // mMuxer.setLocation(Integer.getInteger(rotationString, 0));

        try {
            mDurationUs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
        } catch (NumberFormatException e) {
            mDurationUs = -1;
        }
        Log.d(TAG, "Duration (us): " + mDurationUs);
    }

    private void runPipelines() {
        long loopCount = 0;
        if (mDurationUs <= 0) {
            double progress = PROGRESS_UNKNOWN;
            mProgress = progress;
            if (mProgressCallback != null) mProgressCallback.onProgress(progress); // unknown
        }
        while (!(mVideoTrackTranscoder.isFinished() && mAudioTrackTranscoder.isFinished())) {

            if(mVideoTrackTranscoder.isEncodedFinished()){
                return;
            }

            boolean stepped = mVideoTrackTranscoder.stepPipeline()
                    || mAudioTrackTranscoder.stepPipeline();
            loopCount++;
            if (mDurationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = mVideoTrackTranscoder.isFinished() ? 1.0 : Math.min(1.0, (double) mVideoTrackTranscoder.getWrittenPresentationTimeUs() / mDurationUs);
                double audioProgress = mAudioTrackTranscoder.isFinished() ? 1.0 : Math.min(1.0, (double) mAudioTrackTranscoder.getWrittenPresentationTimeUs() / mDurationUs);
                double progress = (videoProgress + audioProgress) / 2.0;
                mProgress = progress;
                if (mProgressCallback != null) mProgressCallback.onProgress(progress);
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }
    }


    public interface ProgressCallback {
        /**
         * Called to notify progress. Same thread which initiated transcode is used.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);
    }
}
