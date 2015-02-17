package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ProgressImageView extends ImageView {

	private Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG); 
	private RectF mProgressRect = null; 
	private float mProgressRectPercent = 0.0f; 
	private float mProgressPercent = 0.0f; 
	private float mStartAngle = -90.0f; 
	
	public ProgressImageView(Context context) {
		super(context); 
		initView(); 
	}
	
	public ProgressImageView(Context context, AttributeSet attrs) {
		super(context, attrs); 
		initView(); 
	}
	
	public ProgressImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		initView(); 
	}
	
	protected void initView() {
		mProgressPaint.setColor(Color.BLUE); 
	}
	
	public void setProgressColor(int color) {
		mProgressPaint.setColor(color); 
	}
	
	public void setProgressAlpha(int alpha) {
		mProgressPaint.setAlpha(alpha); 
	}
	
	public void setProgress(float percent) {
		mProgressPercent = percent; 
	}
	
	public float getProgress() {
		return mProgressPercent; 
	}
	
	public void setStartAngle(float angle) {
		mStartAngle = angle; 
	}
	
	public void setProgressRect(float left, float top, float right, float bottom) {
		mProgressRect = new RectF(left, top, right, bottom); 
	}
	
	public void setProgressRect(float percentSize) {
		mProgressRect = null; 
		mProgressRectPercent = percentSize; 
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec); 
	}
	
	@Override 
    protected void onDraw(Canvas canvas) {
		float percent = mProgressPercent; 
		if (percent > 0) {
			if (percent > 1.0f) percent = 1.0f; 

			float sweepAngle = percent * 360.0f; 
			
			if (mProgressRect == null) {
				final float width = getWidth(); 
				final float height = getHeight(); 
				
				if (mProgressRectPercent > 0) {
					float rPercent = mProgressRectPercent < 1.0f ? mProgressRectPercent : 1.0f; 
					float pWidth = width * rPercent; 
					float pHeight = height * rPercent; 
					
					float left = (width - pWidth) / 2.0f; 
					float right = left + pWidth; 
					float top = (height - pHeight) / 2.0f; 
					float bottom = top + pHeight; 
					
					mProgressRect = new RectF(left, top, right, bottom); 
					
				} else 
					mProgressRect = new RectF(0, 0, width, height); 
			}
			
			canvas.drawArc(mProgressRect, mStartAngle, sweepAngle, true, mProgressPaint); 
		}
		
        super.onDraw(canvas);
	}
	
}
