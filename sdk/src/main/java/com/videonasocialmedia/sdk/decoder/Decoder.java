package com.videonasocialmedia.sdk.decoder;

import android.opengl.GLSurfaceView;

/**
 * Created by Veronica Lago Fominaya on 04/04/2016.
 */
public interface Decoder {

    void setOutputSurface(GLSurfaceView textureView);

    void inputSource(String path);

    void start();

    void pause();

    void resume();

    void stop();

    void seekTo(long time);

    void release();

}
