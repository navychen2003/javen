package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class DelegatedBitmapDrawable extends BaseDrawable implements Recycleable, Delegatable {
	
    private DelegatedBitmap mBitmap;
    private RectF mBounds = null; 
    private Drawable mBackground = null; 
    private Drawable mForeground = null; 
    private Object mOwner = null; 

    public DelegatedBitmapDrawable(DelegatedBitmap bitmap) {
        mBitmap = bitmap;
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
		
		RectF bounds = mBounds; 
		if (d != null && bounds != null) 
			d.setBounds((int)bounds.left, (int)bounds.top, (int)bounds.right, (int)bounds.bottom); 
		
		if (d != null && d instanceof BaseDrawable) 
        	((BaseDrawable)d).setParentDrawable(this); 
	}
    
    public void setForeground(Drawable d) { 
		mForeground = d; 
		
		RectF bounds = mBounds; 
		if (d != null && bounds != null) 
			d.setBounds((int)bounds.left, (int)bounds.top, (int)bounds.right, (int)bounds.bottom); 
		
		if (d != null && d instanceof BaseDrawable) 
        	((BaseDrawable)d).setParentDrawable(this); 
	}
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds); 
    }
    
    @Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	
    	mBounds = new RectF(left, top, right, bottom); 
    	
    	Drawable d = mBackground; 
		if (d != null) 
			d.setBounds(left, top, right, bottom); 
		
		d = mForeground; 
		if (d != null) 
			d.setBounds(left, top, right, bottom); 
    }

    @Override 
    public void setBounds(Rect bounds) {
    	if (bounds != null) 
    		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom); 
    }
    
    public void setBoundsF(RectF bounds) {
    	mBounds = bounds; 
    	
    	Drawable d = mBackground; 
		if (d != null && bounds != null) 
			d.setBounds((int)bounds.left, (int)bounds.top, (int)bounds.right, (int)bounds.bottom); 
		
		d = mForeground; 
		if (d != null && bounds != null) 
			d.setBounds((int)bounds.left, (int)bounds.top, (int)bounds.right, (int)bounds.bottom); 
    }
    
    public RectF getBoundsF() {
    	return mBounds; 
    }
    
    @Override
    public void draw(Canvas canvas) {
    	Drawable d = mBackground; 
		if (d != null) 
			d.draw(canvas); 
		
    	if (mBitmap != null) 
    		mBitmap.draw(canvas, mBounds); 
    	
    	//super.draw(canvas); 
    	
    	d = mForeground; 
		if (d != null) 
			d.draw(canvas); 
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    	//super.setAlpha(alpha); 
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    	//super.setColorFilter(cf); 
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getMinimumHeight() {
        return mBitmap.getHeight();
    }
    
    @Override
    public boolean isRecycled() {
    	return true; 
    }
    
    @Override
    public void recycle() {
    	
    }
}
