package org.javenstudio.cocoka.opengl;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;

// CanvasTexture is a texture whose content is the drawing on a Canvas.
// The subclasses should override onDraw() to draw on the bitmap.
// By default CanvasTexture is not opaque.
public abstract class CanvasTexture extends UploadedTexture {
	
	private final Bitmap.Config mConfig;
    protected Canvas mCanvas;
    
    public CanvasTexture(BitmapHolder holder, int width, int height) {
    	super(holder);
        mConfig = Bitmap.Config.ARGB_8888;
        setSize(width, height);
        setOpaque(false);
    }

    @Override
    protected BitmapRef onGetBitmap() {
        BitmapRef bitmap = BitmapRef.createBitmap(mHolder, mWidth, mHeight, mConfig);
        mCanvas = new Canvas(bitmap.get());
        onDraw(mCanvas, bitmap);
        return bitmap;
    }

    @Override
    protected void onFreeBitmap(BitmapRef bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }

    protected abstract void onDraw(Canvas canvas, BitmapRef backing);
    
}
