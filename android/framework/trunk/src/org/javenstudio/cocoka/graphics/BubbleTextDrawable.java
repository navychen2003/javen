package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.text.TextPaint;

public class BubbleTextDrawable extends BaseDrawable 
		implements Drawable.Callback {

	protected Drawable mDrawable = null; 
	protected Drawable mBackground = null; 
	protected Drawable mForeground = null; 
	protected Paint mPaint = null; 
	protected Paint mTextPaint = null; 
	protected String mText = null; 
	protected RectF mRect = null; 
	protected float mRadiusX = 0.0f; 
	protected float mRadiusY = 0.0f; 
	protected float mTextX = 0.0f; 
	protected float mTextY = 0.0f; 
	protected float mStrokeWidth = 0.0f;
	protected int mPaintStrokeColor = 0; 
	protected int mPaintColor = 0; 
	protected boolean mShouldMeasure = false; 
	
	public BubbleTextDrawable(Drawable d) {
		this(d, null); 
	}
	
    public BubbleTextDrawable(Drawable d, String text) {
        mDrawable = d; 
        
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED); 
        mPaint.setStyle(Style.FILL); 
        
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.YELLOW); 
        mTextPaint.setTypeface(Typeface.create(mTextPaint.getTypeface(), Typeface.BOLD)); 
        
        mStrokeWidth = 2.0f;
        mPaintStrokeColor = Color.YELLOW; 
        mPaintColor = Color.RED; 
        
        if (mDrawable != null) { 
        	mDrawable.setCallback(this);
        	
        	if (mDrawable instanceof BaseDrawable)
        		((BaseDrawable)mDrawable).setParentDrawable(this); 
        }
        
        setText(text); 
        setTextSize(14); 
    }

    public final Drawable getDrawable() { 
    	return mDrawable;
    }
    
    public void setDrawable(Drawable d) {
    	mDrawable = d; 
    	
    	if (d != null) 
    		d.setBounds(getBounds()); 
    	
    	if (d != null && d instanceof BaseDrawable) 
        	((BaseDrawable)d).setParentDrawable(this); 
    }
    
    public final Drawable getBackground() { 
    	return mBackground;
    }
    
    public void setBackground(Drawable d) {
    	mBackground = d; 
    	
    	if (d != null) 
    		d.setBounds(getBounds()); 
    	
    	if (d != null && d instanceof BaseDrawable) 
        	((BaseDrawable)d).setParentDrawable(this); 
    }
    
    public final Drawable getForeground() { 
    	return mForeground;
    }
    
    public void setForeground(Drawable d) {
    	mForeground = d; 
    	
    	if (d != null) 
    		d.setBounds(getBounds()); 
    	
    	if (d != null && d instanceof BaseDrawable) 
        	((BaseDrawable)d).setParentDrawable(this); 
    }
    
    @Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	
    	Drawable drawable = mDrawable; 
    	if (drawable != null) 
    		drawable.setBounds(left, top, right, bottom); 
    	
    	drawable = mBackground; 
    	if (drawable != null) 
    		drawable.setBounds(left, top, right, bottom); 
    	
    	drawable = mForeground; 
    	if (drawable != null) 
    		drawable.setBounds(left, top, right, bottom); 
    }
    
    public void setTextSize(float size) {
    	mTextPaint.setTextSize(size); 
    	requestMeasure(); 
    }
    
    public void setTypeface(Typeface font) {
    	if (font != null) {
    		mTextPaint.setTypeface(font); 
    		requestMeasure(); 
    	}
    }
    
    public void setTypefaceStyle(int style) {
    	setTypeface(Typeface.create(mTextPaint.getTypeface(), style)); 
    }
    
    public void setTextColor(int color) {
    	mTextPaint.setColor(color); 
    	mPaintStrokeColor = color; 
    }
    
    public void setTextBackground(int color) {
    	mPaint.setColor(color); 
    	mPaintColor = color; 
    }
    
    public void setStrokeWidth(float width) {
    	if (width >= 0 && mStrokeWidth != width) {
	    	mStrokeWidth = width; 
	    	requestMeasure(); 
    	}
    }
    
    public void setText(String text) {
    	mText = text; 
    	requestMeasure(); 
    }
    
    public String getText() {
    	return mText; 
    }
    
    public void requestMeasure() {
    	mShouldMeasure = true; 
    }
    
    protected int getMeasuredWidth() { 
    	Rect bounds = getBounds();
    	if (bounds != null) { 
    		int width = bounds.right - bounds.left;
    		if (width > 0) return width;
    		if (width < 0) return width * (-1);
    	}
    	return getIntrinsicWidth(); 
    }
    
    protected int getMeasuredHeight() { 
    	Rect bounds = getBounds();
    	if (bounds != null) { 
    		int height = bounds.bottom - bounds.top;
    		if (height > 0) return height;
    		if (height < 0) return height * (-1);
    	}
    	return getIntrinsicHeight(); 
    }
    
    protected void measure() {
    	final float width = getMeasuredWidth(); 
    	final float height = getMeasuredHeight(); 
    	
    	if (width <= 0 || height <= 0 || mText == null || mText.length() == 0) {
    		mRect = null; 
    		return; 
    	}
    	
    	FontMetrics fm = mTextPaint.getFontMetrics(); 
    	float textHeight = (float)Math.ceil(fm.descent - fm.ascent); 
    	
    	float[] widths = new float[mText.length()];
		mTextPaint.getTextWidths(mText, widths);
		
		float textWidth = 0; 
		float minw = 0; 
		for (int i=0; i < widths.length; i++) {
			float w = (float)Math.ceil(widths[i]); 
			textWidth += w; 
			if (minw <= 0 || minw > w)
				minw = w; 
		}
		
		float rectWidth = textWidth + minw; 
		float rectHeight = textHeight * 1.2f;
		float textOffsetX = minw / 2; 
		
		if (rectWidth < rectHeight) {
			rectWidth = rectHeight; 
			textOffsetX = (rectWidth - textWidth) / 2; 
		}
		
		float offsetY = mStrokeWidth > 0 ? mStrokeWidth : 0; 
		float offsetX = - offsetY; 
		
		float left = offsetX + width - rectWidth;
		float right = left + rectWidth; 
		float top = offsetY; 
		float bottom = top + rectHeight; 
		
		if (left < 0) left = 0; 
		if (bottom > height) bottom = height; 
		
		mRect = new RectF(left, top, right, bottom); 
		
		mRadiusX = mRadiusY = mRect.height() / 2; 
		
		mTextX = left + textOffsetX; 
		mTextY = offsetY + textHeight * 0.9f; 
		
		mShouldMeasure = false; 
    }
    
    @Override
    public void draw(Canvas canvas) {
    	if (mShouldMeasure) 
    		measure(); 
    	
    	onDrawBackground(canvas, mBackground); 
    	onDraw(canvas, mDrawable); 
    	onDrawForeground(canvas, mForeground); 
    	onDrawText(canvas); 
    }
    
    protected void onDraw(Canvas canvas, Drawable d) {
    	if (d != null) 
    		d.draw(canvas); 
    }
    
    protected void onDrawBackground(Canvas canvas, Drawable d) {
    	if (d != null) 
    		d.draw(canvas); 
    }
    
    protected void onDrawForeground(Canvas canvas, Drawable d) {
    	if (d != null) 
    		d.draw(canvas); 
    }
    
    protected void onDrawText(Canvas canvas) {
        final String text = mText; 
        final RectF rect = mRect; 
        
        if (rect != null) {
        	mPaint.setColor(mPaintColor); 
        	mPaint.setStyle(Style.FILL); 
	    	canvas.drawRoundRect(rect, mRadiusX, mRadiusY, mPaint); 
        
	    	if (mStrokeWidth > 0) {
		    	mPaint.setColor(mPaintStrokeColor); 
	        	mPaint.setStyle(Style.STROKE); 
	        	mPaint.setStrokeWidth(mStrokeWidth);
		    	canvas.drawRoundRect(rect, mRadiusX, mRadiusY, mPaint); 
	    	}
	    	
	        if (text != null && text.length() > 0) 
	        	canvas.drawText(text, mTextX, mTextY, mTextPaint); 
        }
    }

    @Override
    public int getOpacity() {
    	Drawable drawable = mDrawable; 
    	if (drawable != null) 
    		return drawable.getOpacity(); 
    	
    	return PixelFormat.TRANSLUCENT;
    }
    
    @Override
    public void setAlpha(int alpha) {
    	Drawable drawable = mDrawable; 
    	if (drawable != null) 
    		drawable.setAlpha(alpha); 
    }
    
    @Override
    public void setColorFilter(ColorFilter cf) {
    	Drawable drawable = mDrawable; 
    	if (drawable != null) 
    		drawable.setColorFilter(cf); 
    }
    
    @Override
    public int getIntrinsicWidth() {
    	Drawable drawable = mDrawable; 
    	if (drawable != null)
    		return drawable.getIntrinsicWidth(); 
    	
    	return 0; 
    }
    
    @Override
    public int getIntrinsicHeight() {
    	Drawable drawable = mDrawable; 
    	if (drawable != null)
    		return drawable.getIntrinsicHeight(); 
    	
    	return 0; 
    }
    
    @Override
    public int getMinimumWidth() {
    	Drawable drawable = mDrawable; 
    	if (drawable != null)
    		return drawable.getMinimumWidth(); 
    	
    	return 0; 
    }
    
    @Override
    public int getMinimumHeight() {
    	Drawable drawable = mDrawable; 
    	if (drawable != null)
    		return drawable.getMinimumHeight(); 
    	
    	return 0; 
    }
    
    @Override
    public boolean isStateful() {
    	return true; 
    }
    
    @Override
    public boolean setState(final int[] stateSet) {
    	boolean result = super.setState(stateSet); 
    	
    	Drawable drawable = mDrawable; 
    	if (drawable != null && drawable.isStateful()) {
    		if (drawable.setState(stateSet)) 
    			result = true; 
    	}
    	
    	drawable = mBackground; 
    	if (drawable != null && drawable.isStateful()) {
    		if (drawable.setState(stateSet)) 
    			result = true; 
    	}
    	
    	drawable = mForeground; 
    	if (drawable != null && drawable.isStateful()) {
    		if (drawable.setState(stateSet)) 
    			result = true; 
    	}
    	
    	return result; 
    }
    
	@Override
	public void invalidateDrawable(Drawable who) {
		if (who == mDrawable && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if (who == mDrawable && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		if (who == mDrawable && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
	}
    
}
