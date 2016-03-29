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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.videonasocialmedia.sdk.opengl.OutputPlayerSurface;
import com.videonasocialmedia.sdk.overlay.Watermark;

public class VideoMediaPlayerView extends GLSurfaceView{

    private final String TAG = "VideoPlayerView";
    private Context mContext;
    private VideoMediaPlayerRender mRenderer;
    private static VideoMediaPlayerView mSurfaceView;
    OutputPlayerSurface outputPlayerSurface;
    private Watermark watermark;
    private MediaPlayer mMediaPlayer = null;

    public VideoMediaPlayerView(Context context) {
        super(context);
        mContext = context;
        setEGLContextClientVersion(2);
        mRenderer = new VideoMediaPlayerRender(mContext);
        setRenderer(mRenderer);
        mSurfaceView = this;

        Drawable watermark = context.getResources().getDrawable(R.drawable.watermark720);
        addWatermark(watermark, true);
    }

    public VideoMediaPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        setEGLContextClientVersion(2);
        mRenderer = new VideoMediaPlayerRender(mContext);
        setRenderer(mRenderer);
        mSurfaceView = this;
    }

    @Override
    public void onResume() {
        if (mMediaPlayer == null) {
            Log.e(TAG, "Call init() before Continuing");
            return;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setMediaPlayer(mMediaPlayer);
            }
        });

        super.onResume();
    }

    public void init(MediaPlayer mediaPlayer) {
        if (mediaPlayer == null)
            Toast.makeText(mContext, "Set MediaPlayer before continuing",
                    Toast.LENGTH_LONG).show();
        else
            mMediaPlayer = mediaPlayer;
    }


    public void addWatermark(Drawable overlayImage,
                             int positionX, int positionY, int width, int height, boolean preview) {
        this.watermark = new Watermark(overlayImage, height, width, positionX, positionY);
        if (preview && mRenderer != null)
            mRenderer.setWatermark(watermark);
    }

    public void addWatermark(Drawable overlayImage, boolean preview) {
        int[] size = calculateDefaultWatermarkSize();
        int margin = calculateWatermarkDefaultPosition();
        //addWatermark(overlayImage, 15, 15, 265, 36, preview);
        addWatermark(overlayImage, margin, margin, size[0], size[1], preview);
    }


    private int[] calculateDefaultWatermarkSize() {
        int width = (1280 * 265) / 1280;
        int height = (720 * 36) / 720;
        return new int[]{width, height};
    }

    private int calculateWatermarkDefaultPosition() {
        return (1280 * 15) / 1280;
    }

}
