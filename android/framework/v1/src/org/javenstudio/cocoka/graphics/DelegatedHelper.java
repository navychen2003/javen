package org.javenstudio.cocoka.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class DelegatedHelper {

	public static interface BitmapGetter extends Recycleable {
		public Bitmap getExpectedBitmap(); 
		public int getExpectedBitmapWidth(); 
		public int getExpectedBitmapHeight(); 
		public Rect getPaddingRect(); 
	}
	
	public static DelegatedBitmap createBitmap(final BitmapGetter getter) { 
		final DelegatedBitmap bitmapImpl = new DelegatedBitmap() {
			private Rect mSource = null; 
			private RectF mBounds = null; 
			
			private int getPaddingTop() { 
				Rect rect = getter.getPaddingRect(); 
				return rect != null ? rect.top : 0; 
			}
			
			private int getPaddingLeft() { 
				Rect rect = getter.getPaddingRect(); 
				return rect != null ? rect.left : 0; 
			}
			
			private int getPaddingRight() { 
				Rect rect = getter.getPaddingRect(); 
				return rect != null ? rect.right : 0; 
			}
			
			private int getPaddingBottom() { 
				Rect rect = getter.getPaddingRect(); 
				return rect != null ? rect.bottom : 0; 
			}
			
			public int getWidth() { 
				return getter.getExpectedBitmapWidth() + getPaddingLeft() + getPaddingRight(); 
			}
			
			public int getHeight() {
				return getter.getExpectedBitmapHeight() + getPaddingTop() + getPaddingBottom(); 
			}
			
			public int getBitmapWidth() { 
				return getter.getExpectedBitmapWidth(); 
			}
			
			public int getBitmapHeight() { 
				return getter.getExpectedBitmapHeight(); 
			}
			
			public void draw(Canvas canvas, RectF bounds) {
				final Bitmap bitmap = getter.getExpectedBitmap(); 
				
				if (bitmap != null && !bitmap.isRecycled()) {
					Rect src = mSource; 
					RectF bds = mBounds; 
					
					if (src == null || bds == null ) { 
						int pt = getPaddingTop(); 
						int pb = getPaddingBottom(); 
						int pl = getPaddingLeft(); 
						int pr = getPaddingRight(); 
						
						if (bounds != null) 
							bds = new RectF(bounds.left+pl, bounds.top+pt, bounds.right-pr, bounds.bottom-pb); 
						else 
							bds = new RectF(pl, pt, pl+getBitmapWidth(), pt+getBitmapHeight()); 
						
						src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()); 
						
						mSource = src; 
						mBounds = bds; 
					} 
					canvas.drawBitmap(bitmap, src, bds, null); 
				}
			}
		}; 
		
		return bitmapImpl; 
	}
	
	public static DelegatedBitmapDrawable createDrawable(final BitmapGetter getter) {
		return new DelegatedBitmapDrawable(createBitmap(getter)) {
					public boolean isRecycled() { return getter.isRecycled(); }
					public void recycle() { getter.recycle(); }
				};
	}
	
}
