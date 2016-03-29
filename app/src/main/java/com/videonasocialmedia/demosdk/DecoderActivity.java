/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.videonasocialmedia.demosdk;


import android.animation.TimeAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;

import com.videonasocialmedia.sdk.decoder.MediaCodecWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This activity uses a {@link TextureView} to render the frames of a video decoded using
 * {@link MediaCodec} API.
 */
public class DecoderActivity extends Activity implements TimeAnimator.TimeListener, MediaCodecWrapper.OutputSampleListener {

    private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator;

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodecWrapper mCodecWrapper;
    private MediaExtractor mExtractor = new MediaExtractor();

    MediaCodec.BufferInfo out_bufferInfo;

    private boolean isPause = false;
    private String TAG = "DecoderActivity";


    Surface surface;
    //OutputSurface surface;

    private int numSamples;
    private long durationFileMs = 30000;
    private double fps;
    private int countFps;

    /*@Bind(R.id.menu_play)
    Button buttonPlay;*/

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_decoder);

        mPlaybackView = (TextureView) findViewById(R.id.PlaybackView);

        mTimeAnimator = new TimeAnimator();
        mTimeAnimator.setTimeListener(this);
      //  mTimeAnimator.setDuration(durationFileMs); // length of video


    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    private void setupMediaCodec() {

        // Construct a URI that points to the video resource that we want to play
        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                + R.raw.inputvideo);

        try {

            // BEGIN_INCLUDE(initialize_extractor)
            mExtractor.setDataSource(this, videoUri, null);
            int nTracks = mExtractor.getTrackCount();

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < nTracks; ++i) {
                mExtractor.unselectTrack(i);
            }


            surface = new Surface(mPlaybackView.getSurfaceTexture());


            // Find the first video track in the stream. In a real-world application
            // it's possible that the stream would contain multiple tracks, but this
            // sample assumes that we just want to play the first one.
            for (int i = 0; i < nTracks; ++i) {
                // Try to create a video codec for this track. This call will return null if the
                // track is not a video track, or not a recognized video format. Once it returns
                // a valid MediaCodecWrapper, we can break out of the loop.
                mCodecWrapper = MediaCodecWrapper.fromVideoFormat(mExtractor.getTrackFormat(i),surface);
                        //new Surface(mPlaybackView.getSurfaceTexture()));
                if (mCodecWrapper != null) {
                    mExtractor.selectTrack(i);
                    break;
                }

            }

            mCodecWrapper.setmOutputSampleListener(this);

            out_bufferInfo = new MediaCodec.BufferInfo();

           // END_INCLUDE(initialize_extractor)

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null ) {
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_play) {
            if(isPause){
                resumePlayback();
            } else {
                isPause = true;
                startPlayback();
            }

        }

        if (item.getItemId() == R.id.menu_pause) {

            isPause = true;
            pausePlayback();

        }

        if (item.getItemId() == R.id.menu_advance) {

            advancePlayback();

        }

        return true;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void advancePlayback() {

        Log.d(TAG, " getCurrentPlayTime " + mTimeAnimator.getCurrentPlayTime());

        long timeUs = 15000000;

        mExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        int numAdvance = 0;
        Log.d(TAG, "seekTo Previous time " + mExtractor.getSampleTime());
        while(mExtractor.getSampleTime()<timeUs) {
            mExtractor.advance();
            Log.d(TAG, "advance " + numAdvance++);
        }

       // pausePlayback();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void pausePlayback() {
        Log.d(TAG, "Player pausePlayBack");
        mTimeAnimator.pause();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void resumePlayback() {
        Log.d(TAG, "Player resumePlayBack");
        mTimeAnimator.resume();
    }


    public void startPlayback() {

        setupMediaCodec();

        mTimeAnimator.start();

    }

    @Override
    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {

        Log.d(TAG, "onTimeUpdate totalTime " + totalTime/1000 + " deltaTime " + deltaTime/1000);

        countFps++;

        if(countFps % 2 == 0){
            return;
        }


        // CurrentFps

        double currentFps;
        if (deltaTime != 0)
            currentFps = 1000.0 / (double) deltaTime;
        else
            currentFps = 0.9 * fps;
        if (fps < 0.0)
            fps = currentFps;
        else
            fps = 0.9 * fps + 0.1 * currentFps;

        Log.d(TAG, " fps " + String.format("fps: %.2f", fps));



        boolean isEos = ((mExtractor.getSampleFlags() & MediaCodec
                .BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        // BEGIN_INCLUDE(write_sample)
        if (!isEos) {

            // Try to submit the sample to the codec and if successful advance the
            // extractor to the next available sample to read.
            boolean result = mCodecWrapper.writeSample(mExtractor, false,
                    mExtractor.getSampleTime(), mExtractor.getSampleFlags());

            Log.d(TAG, "onTimeUpdate result " + result);

            if (result) {
                // Advancing the extractor is a blocking operation and it MUST be
                // executed outside the main thread in real applications.
                mExtractor.advance();

                Log.d(TAG, "onTimeUpdate advance " + mExtractor.getSampleTime());

            }
        }
        // END_INCLUDE(write_sample)

        // Examine the sample at the head of the queue to see if its ready to be
        // rendered and is not zero sized End-of-Stream record.
         MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();


        // BEGIN_INCLUDE(render_sample)
        if (out_bufferInfo.size <= 0 && isEos) {
            mTimeAnimator.end();
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
            // } else if ((out_bufferInfo.presentationTimeUs / 1000 - timeAdvanced) < totalTime) {

        }  else if(out_bufferInfo.presentationTimeUs/1000 < durationFileMs) {
            // Pop the sample off the queue and send it to {@link Surface}
            mCodecWrapper.popSample(true);

            Log.d(TAG, "onTimeUpdate render");

            if(totalTime > durationFileMs){
                pausePlayback();
            }
        }
        // END_INCLUDE(render_sample)
    }

    @Override
    public void outputSample(MediaCodecWrapper sender, MediaCodec.BufferInfo info, ByteBuffer buffer) {
            Log.d(TAG, "outputSample " + numSamples++ + " info " + info.presentationTimeUs/1000);

            out_bufferInfo = info;


    }
}
