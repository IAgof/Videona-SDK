package com.videonasocialmedia.transcoder.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

/**
 * Created by jca on 1/12/15.
 */
public class Filter extends Overlay{

    public Filter(Drawable filterImage, int width, int height) {
        super(filterImage, width, height, 0, 0);
    }

    @Override
    protected void setBlendMode() {
        // Old blend. GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_COLOR);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void setGlViewportSize() {
        //TODO set the viewport to full screen
    }

}
