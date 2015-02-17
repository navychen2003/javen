package org.javenstudio.cocoka.graphics;

import org.javenstudio.common.util.Logger;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class DelegatedDrawable extends BaseDrawable 
		implements Delegatable, Drawable.Callback {
	private static final Logger LOG = Logger.getLogger(DelegatedDrawable.class);
	private static final boolean DEBUG = BaseDrawable.DEBUG;

	private final Drawable mDrawable; 
	private final int mWidth, mHeight;
	private Drawable mBackground = null; 
	private Object mOwner = null; 
	private int mPadding = 0;
	
	public DelegatedDrawable(Drawable d) { 
		this(d, 0, 0, 0);
	}
	
	public DelegatedDrawable(Drawable d, int width, int height) { 
		this(d, width, height, 0);
	}
	
	public DelegatedDrawable(Drawable d, int width, int height, int padding) { 
		mDrawable = d; 
		mWidth = width;
		mHeight = height;
		mPadding = padding;
		
		if (mDrawable != null) 
			mDrawable.setCallback(this);
	}
	
	@Override
	public void setOwner(Object owner) { 
		mOwner = owner; 
	}
	
	@Override
	public Object getOwner() { 
		return mOwner; 
	}
	
	public void setBackground(Drawable d) { 
		mBackground = d; 
	}
	
	@Override
    public void draw(Canvas canvas) {
		Drawable d = mBackground; 
		if (d != null) 
			d.draw(canvas); 
		
		if (mDrawable != null) 
			mDrawable.draw(canvas); 
	}
	
	@Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	
    	if (mDrawable != null) {
    		int imageWidth = mDrawable.getIntrinsicWidth();
    		int imageHeight = mDrawable.getIntrinsicHeight();;
    		
    		int padding = mPadding;
    		int width = right - left;
    		int height = bottom - top;
    		
    		int dleft = left;
    		int dtop = top;
    		int dright = right;
    		int dbottom = bottom;
    		
    		if (imageWidth > width) {
    			int delta = (imageWidth - width) / 2;
    			dleft -= delta;
    			dright += delta;
    		}
    		
    		if (imageHeight > height) {
    			int delta = (imageHeight - height) / 2;
    			dtop -= delta;
    			dbottom += delta;
    		}
    		
    		if (padding > 0 && padding < width/2 && padding < height/2) {
    			dleft += padding;
    			dright -= padding;
    			dtop += padding;
    			dbottom -= padding;
    		}
    		
    		if (DEBUG && LOG.isDebugEnabled()) { 
    			LOG.debug("setBounds: imageSize=" + imageWidth + " x " + imageHeight 
    					+ " bounds=(" + left + "," + top + " - " + right + "," + bottom + ")"
    					+ " imageBds=(" + dleft + "," + dtop + " - " + dright + "," + dbottom + ")");
    		}
    		
    		mDrawable.setBounds(dleft, dtop, dright, dbottom); 
    	}
    	
    	Drawable d = mBackground; 
		if (d != null) 
			d.setBounds(left, top, right, bottom); 
	}
	
	@Override 
    public void setBounds(Rect bounds) {
    	if (bounds != null) 
    		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom); 
    }
	
	@Override
    public int getOpacity() {
		if (mDrawable != null) 
			return mDrawable.getOpacity(); 
		
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    	if (mDrawable != null) 
    		mDrawable.setAlpha(alpha); 
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    	if (mDrawable != null) 
    		mDrawable.setColorFilter(cf); 
    }

    @Override
    public int getIntrinsicWidth() {
    	if (mWidth > 0) 
    		return mWidth;
    	
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicWidth();
    	
    	return 0; 
    }

    @Override
    public int getIntrinsicHeight() {
    	if (mHeight > 0) 
    		return mHeight;
    	
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicHeight();
    	
    	return 0; 
    }

    @Override
    public int getMinimumWidth() {
    	if (mWidth > 0) 
    		return mWidth;
    	
    	if (mDrawable != null) 
    		return mDrawable.getMinimumWidth();
    	
    	return 0; 
    }

    @Override
    public int getMinimumHeight() {
    	if (mHeight > 0) 
    		return mHeight;
    	
    	if (mDrawable != null) 
    		return mDrawable.getMinimumHeight();
    	
    	return 0; 
    }

	@Override
	public void invalidateDrawable(Drawable who) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
	}
	
}
