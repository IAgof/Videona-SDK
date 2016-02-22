package com.videonasocialmedia.sdk.transcoder;
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

import com.videonasocialmedia.sdk.decoder.VideoTrackDecoder;
import com.videonasocialmedia.sdk.decoder.VideonaDecoder;
import com.videonasocialmedia.sdk.encoder.VideoTrackEncoder;
import com.videonasocialmedia.sdk.encoder.VideonaEncoder;
import com.videonasocialmedia.sdk.muxer.VideonaMuxer;
import com.videonasocialmedia.sdk.muxer.VideonaTrack;
import com.videonasocialmedia.sdk.opengl.InputSurface;
import com.videonasocialmedia.sdk.opengl.OutputSurface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideonaTrackTranscoder implements VideonaTrack {

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor mExtractor;
    private final int mTrackIndex;
    private final MediaFormat mOutputFormat;
    private final VideonaMuxer mMuxer;

    private VideonaDecoder decoder;
    private VideonaEncoder encoder;
    private OutputSurface outputSurface;


    private boolean endOfVideoToEncode;
    private InputSurface mEncoderInputSurfaceWrapper;
    private String TAG = "VideonaTrackTranscoder";

    public VideonaTrackTranscoder (MediaExtractor extractor, int trackIndex,
                                   MediaFormat outputFormat, VideonaMuxer muxer) {
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mOutputFormat = outputFormat;
        mMuxer = muxer;

    }

    public void setup() {

        setupEncoder();

        setupDecoder();


    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return null;
    }

    private void setupEncoder() {
        encoder = new VideoTrackEncoder(mExtractor, mTrackIndex, mOutputFormat, mMuxer);
        encoder.setup();

        mEncoderInputSurfaceWrapper = encoder.getEncoderInputSurface();
    }

    private void setupDecoder() {

        decoder = new VideoTrackDecoder(mExtractor, mTrackIndex);
        outputSurface = new OutputSurface();
        decoder.setOutSurface(outputSurface);
        decoder.setup();
    }

    private File createOutputFile(String path) {
        // Not time stamp, reuse name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String fileName = timeStamp + ".mp4"; //"VID_temp.mp4";
        File rootDir = new File(path);
        rootDir.mkdir();

        return new File(rootDir, fileName);
    }

    public boolean isEncodedFinished() {
        return endOfVideoToEncode;
    }

    public void advanceStart(long startTimeMs) {


    }

    public boolean stepPipeline() {

        boolean busy = false;

        int status;
        while (encoder.drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
        do {
            status = decoder.drainDecoder(0);
            if (status != DRAIN_STATE_NONE){

                if(decoder.isRender()) {

                    outputSurface.awaitNewImage();
                  //  outputSurface.checkForNewImage(1000);
                    outputSurface.drawImage();

                    mEncoderInputSurfaceWrapper.setPresentationTime(encoder.getWrittenPresentationTimeUs() * 1000);
                    mEncoderInputSurfaceWrapper.swapBuffers();
                }

                busy = true;
            }
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
        while (decoder.drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

        return busy;

    }

    @Override
    public long getWrittenPresentationTimeUs() {
        return encoder.getWrittenPresentationTimeUs();
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void endOfDecoder(long endTimeMs) {

      /*  float videoTrimmedDurationS = (endTimeMs - startVideoTime) / 1000;
        framesToEncode = (long) videoTrimmedDurationS * 30;

        Log.d(TAG, " endOfDecoder frames to encode " + framesToEncode); */

    }

    public void release() {

        decoder.release();
        encoder.release();

    }
}
