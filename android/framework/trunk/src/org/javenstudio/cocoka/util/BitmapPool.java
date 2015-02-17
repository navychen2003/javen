package org.javenstudio.cocoka.util;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public class BitmapPool {
	private static final Logger LOG = Logger.getLogger(BitmapPool.class);
	private static boolean DEBUG = false;
	
	private final long mIdentity = ResourceHelper.getIdentity();
    private final List<BitmapRef> mPool;
    private final int mPoolLimit;

    // mOneSize is true if the pool can only cache Bitmap with one size.
    private final boolean mOneSize;
    private final int mWidth, mHeight;  // only used if mOneSize is true

    // Construct a BitmapPool which caches bitmap with the specified size.
    public BitmapPool(int width, int height, int poolLimit) {
        mWidth = width;
        mHeight = height;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<BitmapRef>(poolLimit);
        mOneSize = true;
    }

    // Construct a BitmapPool which caches bitmap with any size;
    public BitmapPool(int poolLimit) {
        mWidth = -1;
        mHeight = -1;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<BitmapRef>(poolLimit);
        mOneSize = false;
    }

    public final long getIdentity() { return mIdentity; }
    
    // Get a Bitmap from the pool.
    public synchronized BitmapRef getBitmap() {
        Utils.assertTrue(mOneSize);
        int size = mPool.size();
        return size > 0 ? mPool.remove(size - 1) : null;
    }

    // Get a Bitmap from the pool with the specified size.
    public synchronized BitmapRef getBitmap(int width, int height) {
        Utils.assertTrue(!mOneSize);
        for (int i = mPool.size() - 1; i >= 0; i--) {
        	BitmapRef b = mPool.get(i);
            if (b.getWidth() == width && b.getHeight() == height) 
                return mPool.remove(i);
        }
        return null;
    }

    // Put a Bitmap into the pool, if the Bitmap has a proper size. Otherwise
    // the Bitmap will be recycled. If the pool is full, an old Bitmap will be
    // recycled.
    public void recycle(BitmapRef bitmap) {
        if (bitmap == null || bitmap.isRecycled()) 
        	return;
        
        if (mOneSize && ((bitmap.getWidth() != mWidth) ||
                (bitmap.getHeight() != mHeight))) {
            bitmap.recycle();
            return;
        }
        
        synchronized (this) {
            if (mPool.size() >= mPoolLimit) { 
            	BitmapRef bm = mPool.remove(0); 
            	if (bm != null && !bm.isRecycled()) { 
            		if (DEBUG && LOG.isDebugEnabled()) 
            			LOG.debug("BitmapPool(" + getIdentity() + ").removeFirst: bitmap=" + bm);
            		
            		bm.recycle();
            	}
            }
            
            mPool.add(bitmap);
            
            if (DEBUG && LOG.isDebugEnabled()) 
            	LOG.debug("BitmapPool(" + getIdentity() + ").add: bitmap=" + bitmap);
        }
    }

    public synchronized void clear() {
    	for (BitmapRef bm : mPool) { 
    		if (bm != null && !bm.isRecycled()) { 
        		if (DEBUG && LOG.isDebugEnabled()) 
        			LOG.debug("BitmapPool(" + getIdentity() + ").clear: bitmap=" + bm);
        		
        		bm.recycle();
        	}
    	}
        mPool.clear();
    }

    public boolean isOneSize() {
        return mOneSize;
    }
}
