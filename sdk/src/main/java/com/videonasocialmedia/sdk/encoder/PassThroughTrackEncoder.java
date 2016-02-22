package com.videonasocialmedia.sdk.encoder;
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

import com.videonasocialmedia.sdk.muxer.VideonaMuxer;
import com.videonasocialmedia.sdk.muxer.VideonaTrack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class PassThroughTrackEncoder implements VideonaTrack {


    private final MediaExtractor mExtractor;
    private final int mTrackIndex;
    private final VideonaMuxer mMuxer;
    private final VideonaMuxer.SampleType mSampleType;
    private final MediaCodec.BufferInfo mBufferInfo;
    private int mBufferSize;
    private ByteBuffer mBuffer;
    private boolean mIsEOS;
    private MediaFormat mActualOutputFormat;
    private long mWrittenPresentationTimeUs;


    public PassThroughTrackEncoder (MediaExtractor extractor, int trackIndex, VideonaMuxer muxer,
                                    VideonaMuxer.SampleType sampleType) {

        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mMuxer = muxer;
        mSampleType = sampleType;

        mBufferInfo =  new MediaCodec.BufferInfo();

        mActualOutputFormat = mExtractor.getTrackFormat(mTrackIndex);
        mMuxer.setOutputFormat(mSampleType, mActualOutputFormat);
        mBufferSize = mActualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        mBuffer = ByteBuffer.allocateDirect(mBufferSize).order(ByteOrder.nativeOrder());

    }

    @Override
    public void setup() {

    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return null;
    }

    @Override
    public void release() {

    }

    @Override
    public void advanceStart(long timeMs) {

    }

    @Override
    public void endOfDecoder(long timeMs) {

    }

    @Override
    public boolean isEncodedFinished() {
        return false;
    }

    @Override
    public boolean stepPipeline() {

        if (mIsEOS) return false;
        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex < 0) {
            mBuffer.clear();
            mBufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mMuxer.writeSampleData(mSampleType, mBuffer, mBufferInfo);
            mIsEOS = true;
            return true;
        }
        if (trackIndex != mTrackIndex) return false;

        mBuffer.clear();
        int sampleSize = mExtractor.readSampleData(mBuffer, 0);
        assert sampleSize <= mBufferSize;
        boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        int flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
        mBufferInfo.set(0, sampleSize, mExtractor.getSampleTime(), flags);
        mMuxer.writeSampleData(mSampleType, mBuffer, mBufferInfo);
        mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;

        mExtractor.advance();
        return true;

    }

    @Override
    public boolean isFinished() {
        return mIsEOS;
    }


    @Override
    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

}
