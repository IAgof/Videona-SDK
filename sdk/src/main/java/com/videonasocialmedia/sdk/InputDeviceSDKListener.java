package com.videonasocialmedia.sdk;

import android.graphics.SurfaceTexture;

/**
 * Created by Veronica Lago Fominaya on 04/04/2016.
 */
public interface InputDeviceSDKListener {

    void onFinished();

    void onFrameAvailable(SurfaceTexture outputSurface);

}
