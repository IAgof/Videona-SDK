package com.videonasocialmedia.transcoder.video.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.videonasocialmedia.transcoder.video.FullFrameRect;
import com.videonasocialmedia.transcoder.video.GlUtil;
import com.videonasocialmedia.transcoder.video.Texture2dProgram;


/**
 * Created by jca on 25/11/15.
 */
public abstract class Overlay {

    Drawable drawableImage;
    private float[] IDENTITY_MATRIX = new float[16];
    private FullFrameRect overlayLayer;
    private int height;
    private int width;
    private int positionX;
    private int positionY;
    private int textureId;

    public Overlay(Drawable drawableImage, int width, int height, int positionX, int positionY) {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        Matrix.scaleM(IDENTITY_MATRIX,0,1,-1,-1);
        this.drawableImage = drawableImage;
        this.width = width;
        this.height = height;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public Overlay(String drawableImagePath, int width, int height, int positionX, int positionY) {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        Matrix.scaleM(IDENTITY_MATRIX,0,1,-1,-1);
        this.drawableImage = Drawable.createFromPath(drawableImagePath);;
        this.width = width;
        this.height = height;
        this.positionX = positionX;
        this.positionY = positionY;
    }


    /**
     * Creates a texture and a shader program. It MUST be called on the GL thread
     */
    public final void initProgram() {
        textureId = GlUtil.createTextureFromDrawable(drawableImage, width, height);
        Texture2dProgram program =
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
        program.setTexSize(width, height);
        overlayLayer = new FullFrameRect(program);
    }

    /**
     * Creates a texture and a shader program. It MUST be called on the GL thread
     */
    public final void initProgram(int alpha) {
        textureId = GlUtil.createTextureFromDrawable(drawableImage, alpha, width, height);
        Texture2dProgram program =
            new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
        program.setTexSize(width, height);
        overlayLayer = new FullFrameRect(program);
    }


    public final void draw() {
        setGlViewportSize();
        setBlendMode();
        overlayLayer.drawFrame(textureId, IDENTITY_MATRIX);
    }

    public final void draw(int alpha) {
        textureId = GlUtil.createTextureFromDrawable(drawableImage, alpha, width, height);
        setGlViewportSize();
        setBlendMode();
        overlayLayer.drawFrame(textureId, IDENTITY_MATRIX);
    }

    protected abstract void setBlendMode();

    protected void setGlViewportSize() {
        GLES20.glViewport(positionX, positionY, width, height);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Drawable getDrawableImage(){
        return drawableImage;
    }

    public int getPositionX(){
        return positionX;
    }

    public int getPositionY(){
        return positionY;
    }

    public boolean isInitialized() {
        return overlayLayer != null;
    }

}
