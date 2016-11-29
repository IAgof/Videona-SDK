package com.videonasocialmedia.transcoder.video.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

/**
 * Created by alvaro on 28/11/16.
 */

public class TransitionFade extends Overlay {

  public TransitionFade(Drawable overlayImage, int width, int height) {
    super(overlayImage, width, height, 0, 0);
  }

  @Override
  protected void setBlendMode() {

    // alpha transition white working
    //GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_COLOR);

    //alpha transition black
    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

  }
}
