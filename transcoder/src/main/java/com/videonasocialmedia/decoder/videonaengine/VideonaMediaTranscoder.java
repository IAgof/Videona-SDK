package com.videonasocialmedia.decoder.videonaengine;
/*
 * Copyright (C) 2015 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 *
 * Authors:
 * Álvaro Martínez Marco
 *
 */

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import com.videonasocialmedia.decoder.exceptions.InvalidOutputFormatException;
import com.videonasocialmedia.decoder.utils.MediaExtractorUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VideonaMediaTranscoder {

    private static final String TAG = "VideonaMediaTranscoder";

    private FileDescriptor mInputFileDescriptor;
    private MediaExtractor mExtractor;
    private MediaMuxer mMuxer;

    private VideonaTrack mVideoTrackTranscoder;
    private VideonaTrack mAudioTrackTranscoder;

    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;

    private long mDurationUs;

    private static volatile VideonaMediaTranscoder sMediaTranscoder;
    private ThreadPoolExecutor mExecutor;
    private static final int MAXIMUM_THREAD = 1; // TODO

    private String outputPathVideo;


    private VideonaMediaTranscoder() {
        mExecutor = new ThreadPoolExecutor(
                0, MAXIMUM_THREAD, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "MediaTranscoder-Worker");
                    }
                });
    }

    public static VideonaMediaTranscoder getInstance() {
        if (sMediaTranscoder == null) {
            synchronized (VideonaMediaTranscoder.class) {
                if (sMediaTranscoder == null) {
                    sMediaTranscoder = new VideonaMediaTranscoder();
                }
            }
        }
        return sMediaTranscoder;
    }

    public void setDataSource(FileDescriptor fileDescriptor) {
        mInputFileDescriptor = fileDescriptor;
    }

    /**
     * Run video transcoding. Blocks current thread.
     * Audio data will not be transcoded; original stream will be wrote to output file.
     *
     * @param outputPath     File path to output transcoded video file.
     * @param formatStrategy Output format strategy.
     * @throws IOException                  when input or output file could not be opened.
     * @throws InvalidOutputFormatException when output format is not supported.
     * @throws InterruptedException         when cancel to transcode.
     */
    public Future<Void> transcodeVideoFile(String outputPath, VideonaFormatStrategy formatStrategy)
            throws IOException, InterruptedException {


        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }

        outputPathVideo = outputPath;

        try {
            // NOTE: use single extractor to keep from running out audio track fast.
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupMetadata();
            setupTrackTranscoders(formatStrategy);
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
        return null;
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

    private void setupTrackTranscoders(VideonaFormatStrategy formatStrategy) {
        MediaExtractorUtils.TrackResult trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(mExtractor);
        MediaFormat videoOutputFormat = formatStrategy.createVideoOutputFormat(trackResult.mVideoTrackFormat);
        MediaFormat audioOutputFormat = formatStrategy.createAudioOutputFormat(trackResult.mAudioTrackFormat);
        if (videoOutputFormat == null && audioOutputFormat == null) {
            throw new InvalidOutputFormatException("MediaFormatStrategy returned pass-through for both video and audio. No transcoding is necessary.");
        }
        VideonaMuxer muxer = new VideonaMuxer(mMuxer, new VideonaMuxer.Listener() {
            @Override
            public void onDetermineOutputFormat() {
           //     VideonaMediaFormatValidator.validateVideoOutputFormat(mVideoTrackTranscoder.getDeterminedFormat());
            //    VideonaMediaFormatValidator.validateAudioOutputFormat(mAudioTrackTranscoder.getDeterminedFormat());
            }
        });

        if (videoOutputFormat == null) {
            mVideoTrackTranscoder = new PassThroughTrackEncoder(mExtractor, trackResult.mVideoTrackIndex,
                    muxer, VideonaMuxer.SampleType.VIDEO);
        } else {
            mVideoTrackTranscoder = new VideonaTrackTranscoder(mExtractor, trackResult.mVideoTrackIndex,
                    videoOutputFormat, muxer);
        }
        mVideoTrackTranscoder.setup();
        if (audioOutputFormat == null) {
            mAudioTrackTranscoder = new PassThroughTrackEncoder(mExtractor, trackResult.mAudioTrackIndex, muxer, VideonaMuxer.SampleType.AUDIO);
        } else {
            throw new UnsupportedOperationException("Transcoding audio tracks currently not supported.");
        }
        mAudioTrackTranscoder.setup();
        mExtractor.selectTrack(trackResult.mVideoTrackIndex);
        mExtractor.selectTrack(trackResult.mAudioTrackIndex);

       /* if(startTimeMs*1000 > mDurationUs) {
            throw new InvalidOutputFormatException("start time bigger than durations file");
        } else {
            mVideoTrackTranscoder.advanceStart(startTimeMs);
        }

        if(endTimeMs*1000 > mDurationUs){
            throw new InvalidOutputFormatException("end time bigger than durations file");
        } else {
            mVideoTrackTranscoder.endOfDecoder(endTimeMs);
        } */
    }

    private void runPipelines() {
        long loopCount = 0;

        while (!(mVideoTrackTranscoder.isFinished() && mAudioTrackTranscoder.isFinished())) {

            if(mVideoTrackTranscoder.isEncodedFinished()){
                return;
            }

            boolean stepped = mVideoTrackTranscoder.stepPipeline()
                    || mAudioTrackTranscoder.stepPipeline();
            loopCount++;

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
