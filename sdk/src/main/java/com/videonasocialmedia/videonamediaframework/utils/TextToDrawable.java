package com.videonasocialmedia.videonamediaframework.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.videonasocialmedia.videonamediaframework.model.media.effects.TextEffect;

/**
 * Created by alvaro on 7/09/16.
 */
public class TextToDrawable {

    private final static float SIZE_FONT= 90f;
    private Context appContext;

    public TextToDrawable(Context appContext) {
        this.appContext = appContext;
    }

    public Drawable createDrawableWithTextAndPosition(String text, String positionText,
                                                      boolean textShadow, int width, int height) {
        Drawable drawable;
        TextPaint textPaint = null;
        Typeface typeFont;
        TextEffect.TextPosition position = getTypePositionFromString(positionText);
        switch (position){
            case TOP:
                typeFont= Typeface.createFromAsset(appContext.getAssets(),
                    "fonts/Roboto-Bold.ttf");
                textPaint= createPaint(Paint.Align.LEFT, typeFont, textShadow);
                break;
            case CENTER:
                typeFont= Typeface.createFromAsset(appContext.getAssets(),
                    "fonts/Roboto-Bold.ttf");
                textPaint =createPaint(Paint.Align.CENTER, typeFont, textShadow);
                break;
            case BOTTOM:
                typeFont= Typeface.createFromAsset(appContext.getAssets(),
                    "fonts/Roboto-Bold.ttf");
                textPaint= createPaint(Paint.Align.LEFT, typeFont, textShadow);
                break;
        }
        Bitmap bmp = createCanvas(text, width, height, textPaint, position);

        drawable = new BitmapDrawable(appContext.getResources(),bmp);

        return drawable;
    }


    public static TextEffect.TextPosition getTypePositionFromString(String position) {
        if(position.compareTo(TextEffect.TextPosition.BOTTOM.name()) == 0){
            return TextEffect.TextPosition.BOTTOM;
        }
        if(position.compareTo(TextEffect.TextPosition.CENTER.name()) == 0){
            return TextEffect.TextPosition.CENTER;
        }
        if(position.compareTo(TextEffect.TextPosition.TOP.name()) == 0){
            return TextEffect.TextPosition.TOP;
        }

        return TextEffect.TextPosition.CENTER;
    }

    private static Bitmap createCanvas(String text, int width, int height, TextPaint textPaint,
                                       TextEffect.TextPosition position) {

        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.TRANSPARENT);
        final Canvas canvas = new Canvas(bmp);
        int xPos = 0;
        int yPos = 0;
        switch (position){
            case TOP:
                xPos = 10;
                yPos = (int) SIZE_FONT;
                break;
            case CENTER:
                xPos = (canvas.getWidth() / 2);
                yPos = (int) ((canvas.getHeight() / 2) - ((textPaint.descent()
                    + textPaint.ascent()) / 2));
                break;
            case BOTTOM:
                xPos = 10;
                yPos = height - (height/6);
        }
        drawTextLines(text,textPaint, canvas, xPos, yPos);
        return bmp;
    }

    private static TextPaint createPaint(final Paint.Align align, final Typeface typeface,
                                         final boolean textShadow) {
        final TextPaint textPaint = new TextPaint() {
            {
                setColor(Color.WHITE);
                setTextAlign(align);
                setTypeface(typeface);
                setTextSize(SIZE_FONT);
                setAntiAlias(true);
                if (textShadow) {
                    setShadowLayer(1, 4, 4, Color.BLACK);
                }
            }
        };
        return textPaint;
    }

    private static void drawTextLines(String text, TextPaint textPaint, Canvas canvas, int xPos,
                                      int yPos) {
        String[] textLines = text.split("\n");
        for (int lineIndex = 0; lineIndex < textLines.length; ++lineIndex) {
            canvas.drawText(textLines[lineIndex], xPos, yPos, textPaint);
            yPos += textPaint.descent() - textPaint.ascent();
        }
    }
}
