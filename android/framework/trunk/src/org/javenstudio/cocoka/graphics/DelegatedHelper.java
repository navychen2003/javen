package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public class DelegatedHelper {
	private static final Logger LOG = Logger.getLogger(DelegatedHelper.class);
	private static final boolean DEBUG = BaseDrawable.DEBUG;

	public static interface BitmapGetter extends Recycleable {
		public BitmapRef getExpectedBitmap(); 
		public Rect getPaddingRect(); 
		
		public int getExpectedBitmapWidth(); 
		public int getExpectedBitmapHeight(); 
	}
	
	public static DelegatedBitmap createBitmap(final BitmapGetter getter) { 
		return new DelegatedBitmapImpl(getter); 
	}
	
	public static DelegatedBitmapDrawable createDrawable(final BitmapGetter getter) {
		return new DelegatedBitmapDrawable(createBitmap(getter)) {
					public boolean isRecycled() { return getter.isRecycled(); }
					public void recycle() { getter.recycle(); }
				};
	}
	
	private static class DelegatedBitmapImpl implements DelegatedBitmap { 
		
		private final BitmapGetter mGetter;
		private final Paint mPaint;
		
		private Rect mSource = null; 
		private RectF mBounds = null; 
		private RectF mInput = null;
		
		public DelegatedBitmapImpl(BitmapGetter getter) { 
			this(getter, Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
		}
		
		public DelegatedBitmapImpl(BitmapGetter getter, int flags) { 
			mGetter = getter; 
			mPaint = new Paint(flags);
			
			if (mGetter == null) 
				throw new NullPointerException("BitmapGetter is null");
		}
		
		private int getPaddingTop() { 
			Rect rect = mGetter.getPaddingRect(); 
			return rect != null ? rect.top : 0; 
		}
		
		private int getPaddingLeft() { 
			Rect rect = mGetter.getPaddingRect(); 
			return rect != null ? rect.left : 0; 
		}
		
		private int getPaddingRight() { 
			Rect rect = mGetter.getPaddingRect(); 
			return rect != null ? rect.right : 0; 
		}
		
		private int getPaddingBottom() { 
			Rect rect = mGetter.getPaddingRect(); 
			return rect != null ? rect.bottom : 0; 
		}
		
		@Override
		public int getWidth() { 
			return mGetter.getExpectedBitmapWidth() + getPaddingLeft() + getPaddingRight(); 
		}
		
		@Override
		public int getHeight() {
			return mGetter.getExpectedBitmapHeight() + getPaddingTop() + getPaddingBottom(); 
		}
		
		public int getBitmapWidth() { 
			return mGetter.getExpectedBitmapWidth(); 
		}
		
		public int getBitmapHeight() { 
			return mGetter.getExpectedBitmapHeight(); 
		}
		
		@Override
		public void setAlpha(int alpha) { 
			mPaint.setAlpha(alpha);
		}
		
		@Override
		public void draw(Canvas canvas, RectF bounds) {
			final BitmapRef bitmap = mGetter.getExpectedBitmap(); 
			
			if (bitmap != null && !bitmap.isRecycled()) {
				Rect src = mSource; 
				RectF bds = mBounds; 
				
				if (src == null || bds == null || bounds != mInput) { 
					int pt = getPaddingTop(); 
					int pb = getPaddingBottom(); 
					int pl = getPaddingLeft(); 
					int pr = getPaddingRight(); 
					
					if (bounds != null) {
						bds = new RectF(bounds.left + pl, bounds.top + pt, 
								bounds.right - pr, bounds.bottom - pb); 
					} else {
						bds = new RectF(pl, pt, 
								pl + getBitmapWidth(), pt + getBitmapHeight()); 
					}
					
					src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()); 
					
					mSource = src; 
					mBounds = bds; 
					mInput = bounds;
					
					if (DEBUG && LOG.isDebugEnabled()) {
						LOG.debug("drawBitmap: init bounds, bitmap=" + bitmap 
								+ " src=" + src + " bds=" + bds + " input=" + bounds);
					}
				}
				
				canvas.drawBitmap(bitmap.get(), src, bds, mPaint); 
			}
		}
	}
	
}
