package org.javenstudio.cocoka.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utils;

// ResourceTexture is a texture whose Bitmap is decoded from a resource.
// By default ResourceTexture is not opaque.
public class ResourceTexture extends UploadedTexture {

    protected final Context mContext;
    protected final int mResId;

    public ResourceTexture(Context context, BitmapHolder holder, int resId) {
    	super(holder);
        mContext = Utils.checkNotNull(context);
        mResId = resId;
        setOpaque(false);
    }

    @Override
    protected BitmapRef onGetBitmap() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapRef.decodeResource(mHolder, 
                mContext.getResources(), mResId, options);
    }

    @Override
    protected void onFreeBitmap(BitmapRef bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
