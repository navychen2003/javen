package org.javenstudio.cocoka.graphics;

import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.ColorFilter;

import org.javenstudio.cocoka.util.BitmapRef;

public class FastBitmapDrawable extends Drawable {
    private BitmapRef mBitmap;
    private int mWidth;
    private int mHeight;

    public FastBitmapDrawable(BitmapRef b) {
        mBitmap = b;
        reMeasure(); 
    }

    @Override
    public void draw(Canvas canvas) {
    	if (mBitmap != null && mWidth > 0 && mHeight > 0) 
    		canvas.drawBitmap(mBitmap.get(), 0.0f, 0.0f, null);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mHeight;
    }

    public void setBitmap(BitmapRef b) {
        mBitmap = b;
        reMeasure(); 
    }
    
    public void reMeasure() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
        } else {
            mWidth = mHeight = 0;
        }
    }

    public BitmapRef getBitmap() {
        return mBitmap;
    }
    
}