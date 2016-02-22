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
package com.videonasocialmedia.decoder.engine;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.videonasocialmedia.decoder.videonaengine.VideonaMuxer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PassThroughTrackTranscoder implements TrackTranscoder {

    private static final String TAG = "PassThroughTrackTranscoder";

    
    private final MediaExtractor mExtractor;
    private final int mTrackIndex;
    private final VideonaMuxer mMuxer;
    private final VideonaMuxer.SampleType mSampleType;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private int mBufferSize;
    private ByteBuffer mBuffer;
    private boolean mIsEOS;
    private MediaFormat mActualOutputFormat;
    private long mWrittenPresentationTimeUs;
    private int numDropFrames = 0;

    private int offset = 0;
    private int framesToEncoded;
    private long startVideoTime;
    private long framesToEncode;

    public PassThroughTrackTranscoder(MediaExtractor extractor, int trackIndex,
                                      VideonaMuxer muxer, VideonaMuxer.SampleType sampleType) {
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mMuxer = muxer;
        mSampleType = sampleType;

        mActualOutputFormat = mExtractor.getTrackFormat(mTrackIndex);
        mMuxer.setOutputFormat(mSampleType, mActualOutputFormat);
        mBufferSize = mActualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        mBuffer = ByteBuffer.allocateDirect(mBufferSize).order(ByteOrder.nativeOrder());
    }

    @Override
    public void setup() {
    }

    @Override
    public void advanceStart(long startTimeMs) {

        long timeUs = startTimeMs*1000;

        mExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
       /* Log.d(TAG, "seekTo Previous time " + mExtractor.getSampleTime() );

        while(mExtractor.getSampleTime()<timeUs) {
            mExtractor.advance();
            numDropFrames++;
            Log.d("MediaTranscoderEngine", "advanceVideo frame " + numDropFrames);
        }

        mExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        startVideoTime = startTimeMs; */


    }

    @Override
    public void endOfDecoder(long endTimeMs) {

        float videoTrimmedDurationS = (startVideoTime - endTimeMs) / 1000;
        framesToEncode = (long) videoTrimmedDurationS * 30;

    }

    @Override
    public boolean isEncodedFinished() {
        return false;
    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return mActualOutputFormat;
    }

    @SuppressLint("Assert")
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
    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

    @Override
    public boolean isFinished() {
        return mIsEOS;
    }

    @Override
    public void release() {
    }
    
}
