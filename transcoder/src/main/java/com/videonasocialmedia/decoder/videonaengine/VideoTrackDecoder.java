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

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.videonasocialmedia.decoder.format.MediaFormatExtraConstants;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoTrackDecoder implements VideonaDecoder {

    private static final String TAG = "VideoTrackDecoder";

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private MediaExtractor mExtractor;
    private int mTrackIndex;

    private MediaCodec mDecoder;

    private MediaFormat mediaFormat;

    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private ByteBuffer[] mDecoderInputBuffers;

    private OutputSurface mDecoderOutputSurfaceWrapper;

    private boolean mDecoderStarted;

    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS;

    private boolean IsRender;

    public VideoTrackDecoder(MediaExtractor mediaExtractor, int trackIndex){
        mExtractor = mediaExtractor;
        mTrackIndex = trackIndex;
    }

    /**
     * Update outputSurface.
     *
     * @param outputSurface
     */
    @Override
    public void setOutSurface(OutputSurface outputSurface) {
        mDecoderOutputSurfaceWrapper = outputSurface;
    }


    /**
     * Go to exact time in Decoder
     *
     * Advance mediaExtractor to previous I frame sync, and advance to specific time.
     *
     * @param timeMs
     */
    @Override
    public void seekTo(long timeMs) {

        long timeUs = timeMs*1000;
        int numDropFrames = 0;

        mExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        Log.d(TAG, "seekTo Previous time " + mExtractor.getSampleTime());
        while(mExtractor.getSampleTime()<timeUs) {
            mExtractor.advance();
            numDropFrames++;
        }

        mExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        while(numDropFrames>0) {
            drainDecoder(0);
            numDropFrames--;
        }

    }


    /**
     * Initialize and start Decoder
     */
    @Override
    public void setup() {

        mediaFormat = mExtractor.getTrackFormat(mTrackIndex);
        if (mediaFormat.containsKey(MediaFormatExtraConstants.KEY_ROTATION_DEGREES)) {
            // Decoded video is rotated automatically in Android 5.0 lollipop.
            // Turn off here because we don't want to encode rotated one.
            // refer: https://android.googlesource.com/platform/frameworks/av/+blame/lollipop-release/media/libstagefright/Utils.cpp
            mediaFormat.setInteger(MediaFormatExtraConstants.KEY_ROTATION_DEGREES, 0);
        }

        try {
            mDecoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        mDecoder.configure(mediaFormat, mDecoderOutputSurfaceWrapper.getSurface(), null, 0);

        mDecoder.start();
        mDecoderStarted = true;
        mDecoderInputBuffers = mDecoder.getInputBuffers();

    }

    /**
     *
     * Advance decoder
     *
     * @return false if there is not more info to decode.
     */
    @Override
    public boolean stepPipeline() {
        boolean busy = false;

        int status;
      //  while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
        do {
            status = drainDecoder(0);
            if (status != DRAIN_STATE_NONE) busy = true;
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
        while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

        return busy;
    }

    /**
     * Stop and release decoder and outputSurface
     */
    @Override
    public void release() {

        if (mDecoderOutputSurfaceWrapper != null) {
            mDecoderOutputSurfaceWrapper.release();
            mDecoderOutputSurfaceWrapper = null;
        }

        if (mDecoder != null) {
            if (mDecoderStarted) mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }

    }

    @Override
    public int drainExtractor(long timeoutUs) {

        if (mIsExtractorEOS) return DRAIN_STATE_NONE;

        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != mTrackIndex) {
            return DRAIN_STATE_NONE;
        }
        int result = mDecoder.dequeueInputBuffer(timeoutUs);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            mIsExtractorEOS = true;
            mDecoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }
        int sampleSize = mExtractor.readSampleData(mDecoderInputBuffers[result], 0);
        boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mDecoder.queueInputBuffer(result, 0, sampleSize, mExtractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    @Override
    public boolean isRender(){
        return IsRender;
    }

    @Override
    public int drainDecoder(long timeoutUs) {

        if (mIsDecoderEOS) return DRAIN_STATE_NONE;
        int result = mDecoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          //  mEncoder.signalEndOfInputStream();
            mIsDecoderEOS = true;
            mBufferInfo.size = 0;
        }


        boolean doRender = (mBufferInfo.size > 0);
        // NOTE: doRender will block if buffer (of encoder) is full.
        // Refer: http://bigflake.com/mediacodec/CameraToMpegTest.java.txt
        mDecoder.releaseOutputBuffer(result, doRender);
        if (doRender) {

            IsRender = true;

            mDecoderOutputSurfaceWrapper.onFrameAvailable(mDecoderOutputSurfaceWrapper.getSurfaceTexture());

          //  mDecoderOutputSurfaceWrapper.awaitNewImage();
          //  mDecoderOutputSurfaceWrapper.drawImage();

            // Only decoder
           // mEncoderInputSurfaceWrapper.setPresentationTime(mBufferInfo.presentationTimeUs * 1000);
           // mEncoderInputSurfaceWrapper.swapBuffers();

        }
        return DRAIN_STATE_CONSUMED;
    }

}
