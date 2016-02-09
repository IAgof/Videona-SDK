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
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/TextureRender.java
// blob: 4125dcfcfed6ed7fddba5b71d657dec0d433da6a
// modified: removed unused method bodies
// modified: use GL_LINEAR for GL_TEXTURE_MIN_FILTER to improve quality.
package com.videonasocialmedia.decoder.engine;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;

import com.videonasocialmedia.decoder.Filters;
import com.videonasocialmedia.decoder.FullFrameRect;
import com.videonasocialmedia.decoder.Texture2dProgram;
import com.videonasocialmedia.decoder.overlay.Overlay;
import com.videonasocialmedia.decoder.overlay.Watermark;

import java.util.List;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
class TextureRender {

    private static final String TAG = "TextureRender";
    private static final boolean VERBOSE = true;

    private static final int FLOAT_SIZE_BYTES = 4;

    private float[] mSTMatrix = new float[16];


    private int mTextureID = -12345;

    private FullFrameRect mFullScreenOutput;
    private int mInputTextureId;
    private OutputSurface mOutputSurface;
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

    public TextureRender(OutputSurface outputSurface) {


        mOutputSurface = outputSurface;

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

        mEncodingEnabled = false;


    }
    public int getTextureId() {
        return mTextureID;
    }


    public void drawFrame(SurfaceTexture st) {

        if (VERBOSE) {
            if (mFrameCount % 30 == 0) {
                Log.d(TAG, "onDrawFrame tex=" + mInputTextureId);
               // mOutputSurface.logSavedEglState();
            }
        }

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

        // Draw the video frame.
        if (mOutputSurface.isSurfaceTextureReady()) {
            mOutputSurface.getSurfaceTexture().updateTexImage();
            mOutputSurface.getSurfaceTexture().getTransformMatrix(mSTMatrix);
            GLES20.glViewport(0, 0, screenWidth, screenHeight);
            mFullScreenOutput.drawFrame(mInputTextureId, mSTMatrix);

            drawOverlayList();
            if (watermark != null) {
                if (!watermark.isInitialized())
                    watermark.initProgram();
                watermark.draw();
            }

            GLES20.glDisable(GLES20.GL_BLEND);
        }
        mFrameCount++;


    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {

        Log.d(TAG, "onSurfaceCreated");
        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreenOutput = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mInputTextureId = mFullScreenOutput.createTextureObject();
       // mOutputSurface.onSurfaceCreated(mInputTextureId);
        mFrameCount = 0;



    }

    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    /**
     * Saves the current frame to disk as a PNG image.  Frame starts from (0,0).
     * <p>
     * Useful for debugging.
     */
    public static void saveFrame(String filename, int width, int height) {
        throw new UnsupportedOperationException("Not implemented.");
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

    public void setOverlayList(List<Overlay> overlayList) {
        this.overlayList = overlayList;
    }

    public void setWatermark(Watermark watermark) {
        this.watermark = watermark;
    }

    public void removeWatermark() {
        watermark = null;
    }
}
