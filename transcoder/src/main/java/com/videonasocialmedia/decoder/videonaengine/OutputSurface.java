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
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/OutputSurface.java
// blob: fc8ad9cd390c5c311f015d3b7c1359e4d295bc52
// modified: change TIMEOUT_MS from 500 to 10000
package com.videonasocialmedia.decoder.videonaengine;

import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Trace;
import android.util.Log;
import android.view.Surface;

import com.videonasocialmedia.decoder.Filters;
import com.videonasocialmedia.decoder.FullFrameRect;
import com.videonasocialmedia.decoder.Texture2dProgram;
import com.videonasocialmedia.decoder.format.SessionConfig;
import com.videonasocialmedia.decoder.overlay.Filter;
import com.videonasocialmedia.decoder.overlay.Overlay;
import com.videonasocialmedia.decoder.overlay.Watermark;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 * <p>
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 * <p>
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 * <p>
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "OutputSurface";
    private static final boolean VERBOSE = true;
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;
    private TextureRenderGL mTextureRender;

    private float[] mTransform = new float[16];


    private static final boolean TRACE = true;         // Systrace
    private int mCurrentFilter;
    private int mNewFilter;
    private boolean mIncomingSizeUpdated;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private List<Overlay> overlayList;
    private Watermark watermark;
    private boolean mEncodedFirstFrame;
    private boolean mThumbnailRequested;
    private int mThumbnailScaleFactor;
    private int mThumbnailRequestedOnFrame;
    private int mFrameNum;

    private final Object mSurfaceTextureFence = new Object();   // guards mSurfaceTexture shared with GLSurfaceView.Renderer

    private SessionConfig config;


    /**
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
    public OutputSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        eglSetup(width, height);
        makeCurrent();
        setup();
    }
    /**
     * Creates an OutputSurface using the current EGL context (rather than establishing a
     * new one).  Creates a Surface that can be passed to MediaCodec.configure().
     */
    public OutputSurface(Drawable drawable, List<Drawable> drawableList, SessionConfig config) {

        this.config = config;

        setup();

      //  addWatermark(drawable, false);
       // addOverlayFilter(drawableList.get(0),config.getVideoWidth() , config.getVideoHeight());

    }

    public OutputSurface() {

        this.config = new SessionConfig();

        setup();
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private void setup() {

        synchronized (mSurfaceTextureFence) {

            mTextureRender = new TextureRenderGL(this);

            mTextureRender.surfaceCreated();
            // Even if we don't access the SurfaceTexture after the constructor returns, we
            // still need to keep a reference to it.  The Surface doesn't retain a reference
            // at the Java level, so if we don't either then the object can get GCed, which
            // causes the native finalizer to run.
            if (VERBOSE) Log.d(TAG, "textureID=" + mTextureRender.getTextureId());
            mTextureId = mTextureRender.getTextureId();
            mSurfaceTexture = new SurfaceTexture(mTextureId);

            // This doesn't work if OutputSurface is created on the thread that CTS started for
            // these test cases.
            //
            // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
            // create a Handler that uses it.  The "frame available" message is delivered
            // there, but since we're not a Looper-based thread we'll never see it.  For
            // this to do anything useful, OutputSurface must be created on a thread without
            // a Looper, so that SurfaceTexture uses the main application Looper instead.
            //
            // Java language note: passing "this" out of a constructor is generally unwise,
            // but we should be able to get away with it here.
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mSurface = new Surface(mSurfaceTexture);

            mFrameNum = 0;

            mEncodedFirstFrame = false;

            mCurrentFilter = -1;
            mNewFilter = Filters.FILTER_NONE;

            mThumbnailRequested = false;
            mThumbnailRequestedOnFrame = -1;

            if (mFullScreen != null) mFullScreen.release();
            mFullScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mFullScreen.getProgram().setTexSize(config.getVideoWidth(), config.getVideoHeight());
            mIncomingSizeUpdated = true;

        }
    }
    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private void eglSetup(int width, int height) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }
        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        checkEglError("eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }
        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }
    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mSurface.release();
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }
    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }
    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }
    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        mTextureRender.changeFragmentShader(fragmentShader);
    }
    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 10000;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
    }
    /**
     * Wait up to given timeout until new image become available.
     * @param timeoutMs
     * @return true if new image is available. false for no new image until timeout.
     */
    public boolean checkForNewImage(int timeoutMs) {
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(timeoutMs);
                    if (!mFrameAvailable) {
                        return false;
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
        return true;
    }
    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage() {
        mTextureRender.drawFrame(mSurfaceTexture);

        mFrameNum++;

        if (TRACE) Trace.endSection();
        if (mCurrentFilter != mNewFilter) {
            Filters.updateFilter(mFullScreen, mNewFilter);
            mCurrentFilter = mNewFilter;
            mIncomingSizeUpdated = true;
        }

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(config.getVideoWidth(), config.getVideoHeight());
            mIncomingSizeUpdated = false;
        }

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mSurfaceTexture.getTransformMatrix(mTransform);
        if (TRACE) Trace.beginSection("drawVEncoderFrame");
        GLES20.glViewport(0, 0, config.getVideoWidth(), config.getVideoHeight());
        mFullScreen.drawFrame(mTextureId, mTransform);
        drawOverlayList();
        if (watermark != null) {
            if (!watermark.isInitialized())
                watermark.initProgram();
            watermark.draw();
        }
        if (TRACE) Trace.endSection();
        if (!mEncodedFirstFrame) {
            mEncodedFirstFrame = true;
        }

        if (mThumbnailRequestedOnFrame == mFrameNum) {
            mThumbnailRequested = true;
        }
        if (mThumbnailRequested) {
            saveFrameAsImage();
            mThumbnailRequested = false;
        }

    }

    /**
     * Called from Renderer thread
     *
     * @return The SurfaceTexture containing the camera frame to display. The
     * display EGLContext is current on the calling thread
     * when this call completes
     */
    public SurfaceTexture getSurfaceTexture() {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture == null)
                Log.w(TAG, "getSurfaceTexture called before ST created");
            return mSurfaceTexture;
        }
    }

    public boolean isSurfaceTextureReady() {
        synchronized (mSurfaceTextureFence) {
            return !(mSurfaceTexture == null);
        }
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

    private void saveFrameAsImage() {

        // Implement, TextureRenderer has methos saveFrame

        /* try {
           File recordingDir = new File(mSessionConfig.getMuxer().getOutputPath()).getParentFile();
            File imageFile = new File(recordingDir, String.format("%d.jpg", System.currentTimeMillis()));
            mInputWindowSurface.saveFrame(imageFile, mThumbnailScaleFactor);
        } catch (IOException e) {
            e.printStackTrace();
        } */
    }

    public void addOverlayFilter(Drawable overlayImage, int width, int height) {
        Overlay overlayToAdd = new Filter(overlayImage, height, width);
        if (overlayList == null) {
            overlayList = new ArrayList<>();
            //if (mTextureRender != null)
              //  mTextureRender.setOverlayList(overlayList);
        }
        overlayList.add(overlayToAdd);
    }


   /* public void addAnimatedOverlayFilter(List<Drawable> images, int videoWidth, int videoHeight) {
        AnimatedOverlay overlayToAdd = new AnimatedOverlay(images, videoHeight, videoWidth);
        if(this.overlayList == null) {
            this.overlayList = new ArrayList();
            if(this.mTextureRender != null) {
                this.mTextureRender.setOverlayList(this.overlayList);
            }
        }

        this.overlayList.add(overlayToAdd);
    }*/

    public void removeOverlayFilter(Overlay overlay) {

        overlayList = null;
       // mTextureRender.setOverlayList(null);
        //overlayList.remove(overlay);
    }

    public void addWatermark(Drawable overlayImage,
                             int positionX, int positionY, int width, int height, boolean preview) {
        this.watermark = new Watermark(overlayImage, height, width, positionX, positionY);
        //if (preview && mTextureRender != null)
           // mTextureRender.setWatermark(watermark);
    }

    public void addWatermark(Drawable overlayImage, boolean preview) {
        int[] size = calculateDefaultWatermarkSize();
        int margin = calculateWatermarkDefaultPosition();
        //addWatermark(overlayImage, 15, 15, 265, 36, preview);
        addWatermark(overlayImage, margin, margin, size[0], size[1], preview);
    }

    private int[] calculateDefaultWatermarkSize() {
        int width = (config.getVideoWidth() * 265) / 1280;
        int height = (config.getVideoHeight() * 36) / 720;
        return new int[]{width, height};
    }

    private int calculateWatermarkDefaultPosition() {
        return (config.getVideoWidth() * 15) / 1280;
    }

    public void removeWaterMark() {
        watermark = null;
       // mTextureRender.removeWatermark();
    }


    /**
     * Request a thumbnail be generated from
     * the next available frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnail(int scaleFactor) {
        mThumbnailRequested = true;
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = -1;
    }

    /**
     * Request a thumbnail be generated from
     * the given frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnFrameWithScaling(int frame, int scaleFactor) {
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = frame;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        if (VERBOSE) Log.d(TAG, "onFrameAvailable new frame available " + mFrameNum);
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("onFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }
    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
