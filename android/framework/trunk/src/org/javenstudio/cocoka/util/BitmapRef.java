package org.javenstudio.cocoka.util;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public final class BitmapRef {
	static final Logger LOG = Logger.getLogger(BitmapRef.class);
	
	private final long mIdentity = ResourceHelper.getIdentity();
	private final BitmapHolder mHolder;
	private final Bitmap mBitmap;
	
	private BitmapRef(BitmapHolder holder, Bitmap bitmap) { 
		mHolder = holder;
		mBitmap = bitmap;
		mHolder.addBitmap(this);
		onBitmapCreated(this);
	}
	
	public final Bitmap get() { return mBitmap; }
	public final BitmapHolder getHolder() { return mHolder; }
	public final long getIdentity() { return mIdentity; }
	
	public final int getWidth() { return mBitmap.getWidth(); }
	public final int getHeight() { return mBitmap.getHeight(); }
	
	public final int getDensity() { return mBitmap.getDensity(); }
	public final void setDensity(int density) { mBitmap.setDensity(density); }
	
	public final int getByteCount() { return getByteCount(mBitmap); }
	
	public final boolean isRecycled() { 
		return mBitmap.isRecycled(); 
	}
	
	public final void recycle() { 
		if (!mBitmap.isRecycled()) {
			mBitmap.recycle(); 
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("recycle: " + this + " totalSize=" + getTotalBitmapSize());
		}
	}
	
	public final Bitmap.Config getConfig() {
		return mBitmap.getConfig();
	}
	
	public BitmapRef copy(BitmapHolder holder, 
			Bitmap.Config config, boolean isMutable) {
		return newBitmap(holder, mBitmap.copy(config, isMutable));
	}
	
	public boolean compress(Bitmap.CompressFormat format, 
			int quality, OutputStream stream) {
		return mBitmap.compress(format, quality, stream);
	}
	
	public void setPixels(int[] pixels, int offset, int stride,
            int x, int y, int width, int height) {
		mBitmap.setPixels(pixels, offset, stride, x, y, width, height);
	}
	
	public int getPixel(int x, int y) {
		return mBitmap.getPixel(x, y);
	}
	
	public void getPixels(int[] pixels, int offset, int stride,
            int x, int y, int width, int height) {
		mBitmap.getPixels(pixels, offset, stride, x, y, width, height);
	}
	
	public void eraseColor(int c) { 
		mBitmap.eraseColor(c);
	}
	
	public byte[] getNinePatchChunk() { 
		return mBitmap.getNinePatchChunk();
	}
	
	@Override
	public String toString() { 
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("Bitmap{id=").append(getIdentity());
		sbuf.append(",recycled=").append(isRecycled());
		sbuf.append(",size=").append(getWidth()).append('x').append(getHeight());
		sbuf.append(",bytes=").append(getByteCount());
		sbuf.append("}");
		return sbuf.toString();
	}
	
	public static BitmapRef newBitmap(BitmapHolder holder, Object obj) { 
		if (obj != null && obj instanceof Bitmap) {
			Bitmap bitmap = (Bitmap)obj;
			
			synchronized (sBitmaps) { 
				for (BitmapRef ref : sBitmaps) { 
					if (ref.get() == bitmap) 
						return ref;
				}
				
				return new BitmapRef(holder, (Bitmap)bitmap);
			}
		}
		
		return null;
	}
	
	private static final Bitmap.Config sBitmapConfig = Bitmap.Config.ARGB_8888;
	private static long sBitmapLength = 128 * 1024;
	
	static void onHandleOOM() { 
		sBitmapLength = 32 * 1024;
	}
	
	public static Bitmap.Config getBitmapConfig() { 
		return sBitmapConfig;
	}
	
	public static BitmapFactory.Options createBitmapOptions(long fileLength) { 
		long length = fileLength > 0 ? fileLength : 1024; 
		float scale = (float)length / (float)(sBitmapLength); 
		
		BitmapFactory.Options opt = new BitmapFactory.Options(); 
		opt.inPreferredConfig = getBitmapConfig(); 
		opt.inPurgeable = true; 
		opt.inInputShareable = true; 
		
		if (scale > 1.0f) 
			opt.inSampleSize = (int)(scale + 1.0f); 
		
		return opt;
	}
	
	private static final List<BitmapRef> sBitmaps = new Vector<BitmapRef>();
	private static long sBitmapSize = 0;
	
	public static long getTotalBitmapSize() { return sBitmapSize; }
	
	private static void onBitmapCreated(BitmapRef bitmap) { 
		synchronized (sBitmaps) { 
			boolean found = false; 
			long size = 0;
			
			for (int i=0; i < sBitmaps.size();) { 
				final BitmapRef d = sBitmaps.get(i); 
				
				if (d == null || d.isRecycled()) { 
					sBitmaps.remove(i); 
					continue; 
					
				} else if (d == bitmap) 
					found = true; 
				
				size += d.getByteCount();
				i ++; 
			}
			
			if (!found && bitmap != null && !bitmap.isRecycled()) {
				sBitmaps.add(bitmap);
				
				//if (LOG.isDebugEnabled()) 
				//	LOG.debug("add: " + bitmap);
				
				size += bitmap.getByteCount();
			}
			
			sBitmapSize = size;
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("bitmapCount=" + count + " byteCount=" + size);
		}
	}
	
	private static int getByteCount(Bitmap bitmap) { 
		if (bitmap == null) return 0;
		
		try { 
			Method method = bitmap.getClass().getMethod("getByteCount");
			if (method != null) 
				return ((Number)method.invoke(bitmap, (Object[])null)).intValue();
			
		} catch (Throwable e) { 
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("getByteCount error: " + e, e);
		}
		
		return bitmap.getWidth() * bitmap.getHeight();
	}
	
	public static BitmapRef createBitmap(BitmapHolder holder, 
			int width, int height) {
		return createBitmap(holder, width, height, Bitmap.Config.ARGB_8888);
	}
	
	public static BitmapRef createBitmap(BitmapHolder holder, 
			int width, int height, Bitmap.Config config) {
		try {
			Bitmap bitmap = Bitmap.createBitmap(width, height, config);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef createBitmap(BitmapHolder holder, 
			BitmapRef source, int x, int y, int width, int height, 
			Matrix m, boolean filter) {
		try {
			Bitmap bitmap = Bitmap.createBitmap(source.get(), x, y, width, height, m, filter);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return source;
	}
	
	public static BitmapRef createScaledBitmap(BitmapHolder holder, 
			BitmapRef src, int dstWidth, int dstHeight, boolean filter) {
		try {
			Bitmap bitmap = Bitmap.createScaledBitmap(src.get(), dstWidth, dstHeight, filter);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return src;
	}
	
	public static BitmapRef decodeResource(BitmapHolder holder, 
			Resources res, int id) {
		try {
			Bitmap bitmap = BitmapFactory.decodeResource(res, id);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeResource(BitmapHolder holder, 
			Resources res, int id, BitmapFactory.Options options) {
		try {
			Bitmap bitmap = BitmapFactory.decodeResource(res, id, options);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeByteArray(BitmapHolder holder, 
			byte[] data, int offset, int length) {
		try {
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, offset, length);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeByteArray(BitmapHolder holder, 
			byte[] data, int offset, int length, BitmapFactory.Options opts) {
		try {
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, offset, length, opts);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeStream(BitmapHolder holder, 
			InputStream is, Rect outPadding, BitmapFactory.Options opts) {
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(is, outPadding, opts);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeFileDescriptor(BitmapHolder holder, 
			FileDescriptor fd, Rect outPadding, BitmapFactory.Options opts) {
		try {
			Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd, outPadding, opts);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
	public static BitmapRef decodeRegion(BitmapHolder holder, 
			BitmapRegionDecoder decoder, Rect rect, BitmapFactory.Options options) {
		try {
			Bitmap bitmap = decoder.decodeRegion(rect, options);
			if (bitmap != null) 
				return new BitmapRef(holder, bitmap);
		} catch (OutOfMemoryError e) { 
			Utilities.handleOOM(e);
		}
		
		return null;
	}
	
}
