package org.javenstudio.cocoka.opengl;

import android.annotation.TargetApi;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;

import org.javenstudio.cocoka.util.ApiHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapPool;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public class TileImageViewAdapter implements TileImageView.Model {
    private static final Logger LOG = Logger.getLogger(TileImageViewAdapter.class);
    
	private final BitmapHolder mHolder;
	
    protected ScreenNail mScreenNail;
    protected boolean mOwnScreenNail;
    protected BitmapRegionDecoder mRegionDecoder;
    protected int mImageWidth;
    protected int mImageHeight;
    protected int mLevelCount;

    public TileImageViewAdapter(BitmapHolder holder) {
    	mHolder = holder;
    }

    public synchronized void clear() {
        mScreenNail = null;
        mImageWidth = 0;
        mImageHeight = 0;
        mLevelCount = 0;
        mRegionDecoder = null;
    }

    // Caller is responsible to recycle the ScreenNail
    public synchronized void setScreenNail(
            ScreenNail screenNail, int width, int height) {
        Utils.checkNotNull(screenNail);
        mScreenNail = screenNail;
        mImageWidth = width;
        mImageHeight = height;
        mRegionDecoder = null;
        mLevelCount = 0;
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder) {
        mRegionDecoder = Utils.checkNotNull(decoder);
        mImageWidth = decoder.getWidth();
        mImageHeight = decoder.getHeight();
        mLevelCount = calculateLevelCount();
    }

    private int calculateLevelCount() {
        return Math.max(0, Utils.ceilLog2(
                (float) mImageWidth / mScreenNail.getWidth()));
    }

    // Gets a sub image on a rectangle of the current photo. For example,
    // getTile(1, 50, 50, 100, 3, pool) means to get the region located
    // at (50, 50) with sample level 1 (ie, down sampled by 2^1) and the
    // target tile size (after sampling) 100 with border 3.
    //
    // From this spec, we can infer the actual tile size to be
    // 100 + 3x2 = 106, and the size of the region to be extracted from the
    // photo to be 200 with border 6.
    //
    // As a result, we should decode region (50-6, 50-6, 250+6, 250+6) or
    // (44, 44, 256, 256) from the original photo and down sample it to 106.
    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    @Override
    public BitmapRef getTile(int level, int x, int y, int tileSize,
            int borderSize, BitmapPool pool) {
        if (!ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER) 
            return getTileWithoutReusingBitmap(level, x, y, tileSize, borderSize);

        int b = borderSize << level;
        int t = tileSize << level;

        Rect wantRegion = new Rect(x - b, y - b, x + t + b, y + t + b);

        boolean needClear;
        BitmapRegionDecoder regionDecoder = null;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;

            // We need to clear a reused bitmap, if wantRegion is not fully
            // within the image.
            needClear = !new Rect(0, 0, mImageWidth, mImageHeight)
                    .contains(wantRegion);
        }

        BitmapRef bitmap = pool == null ? null : pool.getBitmap();
        if (bitmap != null) {
            if (needClear) bitmap.eraseColor(0);
        } else {
            int s = tileSize + 2 * borderSize;
            bitmap = BitmapRef.createBitmap(mHolder, s, s);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        options.inBitmap = bitmap.get();

        BitmapRef bitmapIn = bitmap;
        try {
            // In CropImage, we may call the decodeRegion() concurrently.
            synchronized (regionDecoder) {
                bitmap = BitmapRef.decodeRegion(mHolder, regionDecoder, wantRegion, options);
            }
        } finally {
            if (bitmap == null ||  bitmap.get() != bitmapIn.get()) {
                if (pool != null) pool.recycle(bitmapIn);
                options.inBitmap = null;
            }
        }

        if (bitmap == null) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("fail in decoding region");
        }
        
        return bitmap;
    }

    private BitmapRef getTileWithoutReusingBitmap(
            int level, int x, int y, int tileSize, int borderSize) {
        int b = borderSize << level;
        int t = tileSize << level;
        Rect wantRegion = new Rect(x - b, y - b, x + t + b, y + t + b);

        BitmapRegionDecoder regionDecoder;
        Rect overlapRegion;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;
            overlapRegion = new Rect(0, 0, mImageWidth, mImageHeight);
            Utils.assertTrue(overlapRegion.intersect(wantRegion));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        
        BitmapRef bitmap = null;

        // In CropImage, we may call the decodeRegion() concurrently.
        synchronized (regionDecoder) {
            bitmap = BitmapRef.decodeRegion(mHolder, regionDecoder, overlapRegion, options);
        }

        if (bitmap == null) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("fail in decoding region");
        }

        if (wantRegion.equals(overlapRegion)) return bitmap;

        int s = tileSize + 2 * borderSize;
        BitmapRef result = BitmapRef.createBitmap(mHolder, s, s);
        Canvas canvas = new Canvas(result.get());
        canvas.drawBitmap(bitmap.get(),
                (overlapRegion.left - wantRegion.left) >> level,
                (overlapRegion.top - wantRegion.top) >> level, null);
        return result;
    }


    @Override
    public ScreenNail getScreenNail() {
        return mScreenNail;
    }

    @Override
    public int getImageHeight() {
        return mImageHeight;
    }

    @Override
    public int getImageWidth() {
        return mImageWidth;
    }

    @Override
    public int getLevelCount() {
        return mLevelCount;
    }
    
}
