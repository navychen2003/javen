package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.net.metrics.IMetricsFormater;
import org.javenstudio.cocoka.net.metrics.IProgressUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;

public class ProgressDrawable extends BubbleTextDrawable implements IProgressUpdater {
	
	private float mProgress = 0.0f; 
	private IMetricsFormater mFormater = null; 
	private RectF mRectBg = null; 
	private Paint mRectPaint = null; 
	
	public ProgressDrawable(Drawable d) {
		super(d); 
		
		mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mRectPaint.setColor(Color.DKGRAY); 
		mRectPaint.setStyle(Style.FILL); 
		mRectPaint.setAlpha(128); 
		
		setProgressColor(Color.GREEN); 
		setProgressAlpha(192); 
		
		setTextColor(Color.WHITE); 
		setTypefaceStyle(Typeface.NORMAL); 
		setTextSize(10); 
	}
	
	@Override 
	public void setMetricsFormater(IMetricsFormater formater) {
		mFormater = formater; 
	}
	
	@Override 
	public boolean updateMetrics(MetricsContext context) {
		if (context != null) {
			IMetricsFormater formater = mFormater; 
			if (formater != null) { 
				formater.format(this, context); 
				
				requestMeasure(); 
				postInvalidate(); 
				
				return true; 
			}
		}
		return false; 
	}
	
	@Override 
	public void invalidate() { 
		invalidateView(); 
	}
	
	@Override 
	public void postInvalidate() { 
		postInvalidateView(); 
	}
	
	@Override 
	public void requestLayout() { 
		requestLayoutView(); 
	}
	
	@Override 
	public void refreshProgress() { 
		
	}
	
	@Override 
	public void setProgressInformation(String text) {
		setText(text); 
	}
	
	@Override 
	public void setProgress(float p) {
		mProgress = p; 
	}
	
	public void setProgressColor(int color) {
		mPaint.setColor(color); 
	}
	
	public void setProgressAlpha(int alpha) {
		mPaint.setAlpha(alpha); 
	}
	
	@Override 
	protected void measure() {
		final float width = getIntrinsicWidth(); 
    	final float height = getIntrinsicHeight(); 
    	
    	mRect = null; 
    	mRectBg = null; 
    	
    	mTextX = mTextY = 0; 
    	
    	if (width <= 0 || height <= 0) 
    		return; 
    	
    	if (mProgress < 0) mProgress = 0; 
    	else if (mProgress > 1) mProgress = 1; 
    	
    	boolean hasText = false; 
    	
    	if (mText != null && mText.length() > 0) { 
	    	//FontMetrics fm = mTextPaint.getFontMetrics(); 
	    	//float textHeight = (float)Math.ceil(fm.descent - fm.ascent); 
	    	
	    	//float[] widths = new float[mText.length()];
			//mTextPaint.getTextWidths(mText, widths);
			
			//float textWidth = 0; 
			//for (int i=0; i < widths.length; i++) {
			//	float w = (float)Math.ceil(widths[i]); 
			//	textWidth += w; 
			//}
			
			mTextX = 2; 
			mTextY = height - 5; 
			
			if (mTextY < 0) mTextY = 0; 
			
			hasText = true; 
    	}
		
    	if (hasText || (mProgress >= 0 && mProgress <= 1)) { 
    		float left = 0.0f; 
    		float top = height - 18; 
    		float bottom = height; 
    		float right = (float)width * mProgress; 
    		
    		if (top < 0) top = 0; 
    		
    		if (right > 0) 
    			mRect = new RectF(left, top, right, bottom); 
    		
    		if (hasText || mRect != null) 
    			mRectBg = new RectF(left, top, width, bottom); 
    	}
    	
		mShouldMeasure = false; 
	}
	
	@Override 
	protected void onDrawText(Canvas canvas) {
		final String text = mText; 
		final RectF rect = mRect; 
		final RectF rectbg = mRectBg; 
		
		if (rectbg != null) 
			canvas.drawRect(rectbg, mRectPaint); 
		
		if (rect != null) 
			canvas.drawRect(rect, mPaint); 
		
		if (text != null && text.length() > 0) 
        	canvas.drawText(text, mTextX, mTextY, mTextPaint); 
	}
	
}
