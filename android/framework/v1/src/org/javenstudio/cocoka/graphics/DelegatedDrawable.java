package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class DelegatedDrawable extends Drawable implements Delegatable {

	private final Drawable mDrawable; 
	private Drawable mBackground = null; 
	private Object mOwner = null; 
	
	public DelegatedDrawable(Drawable d) { 
		mDrawable = d; 
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
    	
    	if (mDrawable != null) 
    		mDrawable.setBounds(left, top, right, bottom); 
    	
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
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicWidth();
    	
    	return 0; 
    }

    @Override
    public int getIntrinsicHeight() {
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicHeight();
    	
    	return 0; 
    }

    @Override
    public int getMinimumWidth() {
    	if (mDrawable != null) 
    		return mDrawable.getMinimumWidth();
    	
    	return 0; 
    }

    @Override
    public int getMinimumHeight() {
    	if (mDrawable != null) 
    		return mDrawable.getMinimumHeight();
    	
    	return 0; 
    }
	
}
