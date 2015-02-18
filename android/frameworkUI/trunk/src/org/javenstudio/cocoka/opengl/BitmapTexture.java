package org.javenstudio.cocoka.opengl;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utils;

// BitmapTexture is a texture whose content is specified by a fixed Bitmap.
//
// The texture does not own the Bitmap. The user should make sure the Bitmap
// is valid during the texture's lifetime. When the texture is recycled, it
// does not free the Bitmap.
public class BitmapTexture extends UploadedTexture {
    protected BitmapRef mContentBitmap;

    public BitmapTexture(BitmapHolder holder, BitmapRef bitmap) {
        this(holder, bitmap, false);
    }

    public BitmapTexture(BitmapHolder holder, BitmapRef bitmap, boolean hasBorder) {
        super(holder, hasBorder);
        Utils.assertTrue(bitmap != null && !bitmap.isRecycled());
        mContentBitmap = bitmap;
    }

    @Override
    protected void onFreeBitmap(BitmapRef bitmap) {
        // Do nothing.
    }

    @Override
    protected BitmapRef onGetBitmap() {
        return mContentBitmap;
    }

    public BitmapRef getBitmap() {
        return mContentBitmap;
    }
    
}
