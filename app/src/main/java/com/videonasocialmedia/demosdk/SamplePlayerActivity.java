package com.videonasocialmedia.demosdk;
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

import android.app.Activity;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;

public class SamplePlayerActivity extends Activity {

    private static final String TAG = "MediaPlayerSurfaceStubActivity";

    protected Resources mResources;

    private VideoMediaPlayerView mVideoMediaPlayerView = null;
    private MediaPlayer mMediaPlayer = null;
    private boolean isPause = false;
    private int length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResources = getResources();
        mMediaPlayer = new MediaPlayer();

        try {
            // Load video file from SD Card
             File dir = Environment
             .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
             File file = new File(dir,
             "InputVideo.mp4");
             mMediaPlayer.setDataSource(file.getAbsolutePath());
            // -----------------------------------------------------------------------
            // Load video file from Assets directory
           // AssetFileDescriptor afd = getAssets().openFd("sample.mp4");
           // mMediaPlayer.setDataSource(afd.getFileDescriptor(),
             //       afd.getStartOffset(), afd.getLength());
        } catch (Exception e) {
            //Log.e(TAG, e.getMessage(), e);

        }
        // Initialize VideoSurfaceView using code
        // mVideoView = new VideoSurfaceView(this);
        // setContentView(mVideoView);

        mVideoMediaPlayerView = new VideoMediaPlayerView(this);
        setContentView(mVideoMediaPlayerView);

        // or
        //setContentView(R.layout.sample_decoder);
        //mVideoView = (VideoSurfaceView) findViewById(R.id.mVideoSurfaceView);
       // mVideoView.init(mMediaPlayer,
         //       new DuotoneEffect(Color.YELLOW, Color.RED));
        mVideoMediaPlayerView.init(mMediaPlayer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoMediaPlayerView.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
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

    private void startPlayback(){

        //mVideoPlayerView.init(mMediaPlayer);
       // mVideoPlayerView.onResume();
        mMediaPlayer.start();
    }

    private void resumePlayback(){
        mMediaPlayer.pause();
        length=mMediaPlayer.getCurrentPosition();
    }

    private void pausePlayback(){
        mMediaPlayer.seekTo(length);
        mMediaPlayer.start();
    }

    private void advancePlayback(){
        mMediaPlayer.seekTo(15000);
    }
}
