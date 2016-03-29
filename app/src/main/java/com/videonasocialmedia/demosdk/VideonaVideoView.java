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
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.videonasocialmedia.sdk.opengl.OutputPlayerSurface;
import com.videonasocialmedia.sdk.overlay.Watermark;

public class VideonaVideoView extends GLSurfaceView{

    private final String TAG = "VideoPlayerView";
    private Context mContext;
    private VideonaVideoRender mRenderer;
    private static VideonaVideoView mSurfaceView;
    OutputPlayerSurface outputPlayerSurface;
    private Watermark watermark;

    private SurfaceTexture surface;

    public VideonaVideoView(Context context) {
        super(context);
        mContext = context;
        setEGLContextClientVersion(2);
        mRenderer = new VideonaVideoRender(mContext);
        setRenderer(mRenderer);
        mSurfaceView = this;

        Drawable watermark = context.getResources().getDrawable(R.drawable.watermark720);
        addWatermark(watermark, true);
    }

    public VideonaVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        setEGLContextClientVersion(2);
        mRenderer = new VideonaVideoRender(mContext);
        setRenderer(mRenderer);
        mSurfaceView = this;
    }

    @Override
    public void onResume() {

        queueEvent(new Runnable() {
            @Override
            public void run() {
                // mRenderer.setSurface(surface);
                //mRenderer.setMediaPlayer(mMediaPlayer);
                surface = mRenderer.getSurfaceTexture();
            }
        });

        super.onResume();
    }

    public SurfaceTexture getSurfaceTextureRenderer(){
        return  surface;
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
