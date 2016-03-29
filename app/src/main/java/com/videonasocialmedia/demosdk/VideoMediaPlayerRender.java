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
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.videonasocialmedia.sdk.opengl.EglStateSaver;
import com.videonasocialmedia.sdk.opengl.Filters;
import com.videonasocialmedia.sdk.opengl.FullFrameRect;
import com.videonasocialmedia.sdk.opengl.Texture2dProgram;
import com.videonasocialmedia.sdk.overlay.Overlay;
import com.videonasocialmedia.sdk.overlay.Watermark;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoMediaPlayerRender implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {
    private static String TAG = "VideoRender";
    private final EglStateSaver mEglSaver;

    private final float[] mSTMatrix = new float[16];


    private SurfaceTexture mSurface;
    private boolean updateSurface = false;

    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private MediaPlayer mMediaPlayer;

    private FullFrameRect mFullScreenOutput;
    private int mInputTextureId;
    private int mFrameCount;

    // Keep track of selected filters + relevant state
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;
    private int mCurrentFilter;
    private int mNewFilter;

    private boolean mEncodingEnabled;
    private int screenWidth;
    private int screenHeight;

    private List<Overlay> overlayList;
    private Watermark watermark;

    public VideoMediaPlayerRender(Context context) {

        mEglSaver = new EglStateSaver();

        mInputTextureId = -1;
        mFrameCount = -1;

        //SessionConfig config = recorder.getConfig();
        //mIncomingWidth = config.getVideoWidth();
        //mIncomingHeight = config.getVideoHeight();
        mIncomingWidth = 1280;
        mIncomingHeight = 720;
        mIncomingSizeUpdated = true;        // Force texture size update on next onDrawFrame

        // There is not onSurfaceChange
        screenWidth = mIncomingWidth;
        screenHeight = mIncomingHeight;

        mCurrentFilter = -1;
        mNewFilter = Filters.FILTER_NONE;

    }

    public void setMediaPlayer(MediaPlayer player) {
        mMediaPlayer = player;
    }


    public void setOverlayList(List<Overlay> overlayList) {
        this.overlayList = overlayList;
    }

    public void setWatermark(Watermark watermark) {
        this.watermark = watermark;
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {

        if (mCurrentFilter != mNewFilter) {
            Filters.updateFilter(mFullScreenOutput, mNewFilter);
            mCurrentFilter = mNewFilter;
            mIncomingSizeUpdated = true;
        }

        if (mIncomingSizeUpdated) {
            mFullScreenOutput.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
            Log.i(TAG, "setTexSize on display Texture");
        }

        synchronized (this) {
            if (updateSurface) {
                mSurface.updateTexImage();
                mSurface.getTransformMatrix(mSTMatrix);
                GLES20.glViewport(0, 0, screenWidth, screenHeight);
                mFullScreenOutput.drawFrame(mInputTextureId, mSTMatrix);
                drawOverlayList();
                if (watermark != null) {
                    if (!watermark.isInitialized())
                        watermark.initProgram();
                    watermark.draw();
                }

                GLES20.glDisable(GLES20.GL_BLEND);
                updateSurface = false;
            }
        }

        mFrameCount++;


    }

    private void drawOverlayList() {
        if (overlayList != null && overlayList.size() > 0) {
            GLES20.glEnable(GLES20.GL_BLEND);
            for (Overlay overlay : overlayList) {
                if (!overlay.isInitialized())
                    overlay.initProgram();
                overlay.draw();
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
        screenWidth = width;
        screenHeight = height;

    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        Log.d(TAG, "onSurfaceCreated");
        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreenOutput = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mInputTextureId = mFullScreenOutput.createTextureObject();
        // mOutputSurface.onSurfaceCreated(mInputTextureId);
        mFrameCount = 0;


			/*
			 * Create the SurfaceTexture that will feed this textureID, and pass
			 * it to the MediaPlayer
			 */
        mSurface = new SurfaceTexture(mInputTextureId);
        mSurface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(mSurface);
        mMediaPlayer.setSurface(surface);
        mMediaPlayer.setScreenOnWhilePlaying(true);
        surface.release();

        try {
            mMediaPlayer.prepare();
        } catch (IOException t) {
            Log.e(TAG, "media player prepare failed");
        }

        synchronized (this) {
            updateSurface = false;
        }

        //mMediaPlayer.start();
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }


    public void signalVerticalVideo(FullFrameRect.SCREEN_ROTATION isVertical) {
        if (mFullScreenOutput != null) mFullScreenOutput.adjustForVerticalVideo(isVertical, false);
    }

} // End of class VideoPlayerRender.

