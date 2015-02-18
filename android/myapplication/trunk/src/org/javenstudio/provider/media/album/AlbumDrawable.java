package org.javenstudio.provider.media.album;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.TouchListener;
import org.javenstudio.common.util.Logger;

public class AlbumDrawable extends Drawable 
		implements Drawable.Callback, TouchListener {
	private static final Logger LOG = Logger.getLogger(AlbumDrawable.class);
	
	public interface DrawableList { 
		public int getCount();
		public Drawable getDrawableAt(int index, int width, int height);
		public boolean contains(Drawable d);
	}

	private static class DrawableConf { 
		public int left, top, right, bottom;
		public int width, height;
		
		@Override
		public String toString() { 
			return "DrawableConf[(" + left + ", " + top + ") - (" 
					+ right + ", " + bottom + ")]";
		}
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	private final DrawableList mDrawables;
	private final int mWidth, mHeight;
	private final float mSpaceWidth;
	private DrawableConf[] mConfs = null;
	
	public AlbumDrawable(DrawableList drawables, 
			int width, int height, float space) { 
		mDrawables = drawables;
		mSpaceWidth = space > 0 ? space : 0;
		mWidth = width;
		mHeight = height;
		
		if (drawables == null) 
			throw new NullPointerException("DrawableList is null");
		
		if (width <= 0 || height <= 0) 
			throw new IllegalArgumentException("width or height must > 0");
		
		TouchHelper.addListener(this);
	}
	
	@Override
	public void onTouchUp(Activity activity) { 
		invalidateSelf();
	}
	
	@Override
    public void draw(Canvas canvas) {
		DrawableConf[] confs = mConfs;
		if (confs != null && confs.length > 0) { 
			for (int i=0; i < confs.length; i++) { 
				DrawableConf conf = confs[i];
				if (conf == null) continue;
				Drawable d = mDrawables.getDrawableAt(i, conf.width, conf.height);
				if (d == null) continue;
				
				//if (LOG.isDebugEnabled()) {
				//	LOG.debug("draw: id=" + getIdentity() + " drawables[" 
				//			+ i + "]=" + d + " at " + d.getBounds());
				//}
				
				d.draw(canvas);
			}
		}
	}
	
	@Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug("setBounds: id=" + getIdentity() + " rect=[(" 
    				+ left + ", " + top + ") - (" + right + ", " + bottom + ")]");
    	}
    	
    	final float width = right - left;
    	final float height = bottom - top;
    	final float space = mSpaceWidth;
    	
    	int count = mDrawables.getCount();
    	if (count < 0) count = 0;
    	if (count == 2 || count == 3) count = 4;
    	if (count > 5) count = 5;
    	
    	int smallCount = count - 1;
    	float smallSize = 0;
    	if (smallCount > 0) { 
    		smallSize = height / smallCount;
    		if (smallSize >= width) 
    			smallSize = 0;
    	}
    	
    	float bigHeight = height;
    	float bigWidth = width - smallSize;
    	
    	DrawableConf[] confs = new DrawableConf[count];
    	for (int i=0; i < confs.length; i++) { 
    		DrawableConf conf = new DrawableConf();
    		confs[i] = conf;
    		if (i == 0) { 
    			conf.left = 0;
    			conf.top = 0;
    			conf.right = (int)(bigWidth - (count > 1 ? space : 0));
    			conf.bottom = (int)bigHeight;
    			
    		} else { 
    			conf.left = (int)(bigWidth + space);
    			conf.right = (int)width;
    			
    			float ftop = ((i - 1) * smallSize);
    			float fbottom = (ftop + smallSize);
    			
    			if (i == 1) { 
    				conf.top = (int)ftop;
    				conf.bottom = (int)(fbottom - space);
    				
    			} else if (i == count - 1) { 
    				conf.top = (int)(ftop + space);
    				conf.bottom = (int)fbottom;
    				
    			} else { 
    				conf.top = (int)(ftop + space);
    				conf.bottom = (int)(fbottom - space);
    			}
    		}
    		
    		conf.width = conf.right - conf.left;
    		conf.height = conf.bottom - conf.top;
    		
    		Drawable d = mDrawables.getDrawableAt(i, conf.width, conf.height);
    		if (d != null)
    			d.setBounds(conf.left, conf.top, conf.right, conf.bottom);
    		
    		if (LOG.isDebugEnabled()) {
    			LOG.debug("setBounds: configure id=" + getIdentity() 
    					+ " drawables[" + i + "]=" + d + " conf=" + conf);
    		}
    	}
    	
    	mConfs = confs;
	}
	
	@Override 
    public void setBounds(Rect bounds) {
    	if (bounds != null) 
    		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom); 
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

	@Override
	public void invalidateDrawable(Drawable who) {
		if (mDrawables.contains(who) && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if (mDrawables.contains(who) && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		if (mDrawables.contains(who) && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
	}
	
}
