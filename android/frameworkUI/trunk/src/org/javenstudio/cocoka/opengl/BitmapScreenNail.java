package org.javenstudio.cocoka.opengl;

import android.graphics.RectF;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;

public class BitmapScreenNail implements ScreenNail {
    private final BitmapTexture mBitmapTexture;

    public BitmapScreenNail(BitmapHolder holder, BitmapRef bitmap) {
        mBitmapTexture = new BitmapTexture(holder, bitmap);
    }

    @Override
    public int getWidth() {
        return mBitmapTexture.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmapTexture.getHeight();
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        mBitmapTexture.draw(canvas, x, y, width, height);
    }

    @Override
    public void noDraw() {
        // do nothing
    }

    @Override
    public void recycle() {
        mBitmapTexture.recycle();
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        canvas.drawTexture(mBitmapTexture, source, dest);
    }
    
}
