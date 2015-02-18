package org.javenstudio.cocoka.opengl;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.FloatMath;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;

// StringTexture is a texture shows the content of a specified String.
//
// To create a StringTexture, use the newInstance() method and specify
// the String, the font size, and the color.
public class StringTexture extends CanvasTexture {
    private final String mText;
    private final TextPaint mPaint;
    private final FontMetricsInt mMetrics;

    private StringTexture(BitmapHolder holder, String text, TextPaint paint,
            FontMetricsInt metrics, int width, int height) {
        super(holder, width, height);
        mText = text;
        mPaint = paint;
        mMetrics = metrics;
    }

    public static TextPaint getDefaultPaint(float textSize, int color) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setShadowLayer(2f, 0f, 0f, Color.BLACK);
        return paint;
    }

    public static StringTexture newInstance(BitmapHolder holder, 
            String text, float textSize, int color) {
        return newInstance(holder, text, getDefaultPaint(textSize, color));
    }

    public static StringTexture newInstance(BitmapHolder holder, 
            String text, float textSize, int color,
            float lengthLimit, boolean isBold) {
        TextPaint paint = getDefaultPaint(textSize, color);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        if (lengthLimit > 0) {
            text = TextUtils.ellipsize(
                    text, paint, lengthLimit, TextUtils.TruncateAt.END).toString();
        }
        return newInstance(holder, text, paint);
    }

    private static StringTexture newInstance(BitmapHolder holder, 
    		String text, TextPaint paint) {
        FontMetricsInt metrics = paint.getFontMetricsInt();
        int width = (int) FloatMath.ceil(paint.measureText(text));
        int height = metrics.bottom - metrics.top;
        // The texture size needs to be at least 1x1.
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        return new StringTexture(holder, text, paint, metrics, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas, BitmapRef backing) {
        canvas.translate(0, -mMetrics.ascent);
        canvas.drawText(mText, 0, 0, mPaint);
    }
}
