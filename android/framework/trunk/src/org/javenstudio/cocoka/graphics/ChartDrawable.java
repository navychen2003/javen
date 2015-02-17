package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.javenstudio.cocoka.net.metrics.IMetricsFormater;
import org.javenstudio.cocoka.net.metrics.IProgressUpdater;
import org.javenstudio.cocoka.net.metrics.MetricsContext;
import org.javenstudio.common.util.Logger;

public class ChartDrawable extends BaseDrawable implements IProgressUpdater {
	private static final Logger LOG = Logger.getLogger(ChartDrawable.class);

	public static final int CHART_PIE = 1;
	public static final int CHART_HISTOGRAM = 2;
	
	private final Paint mBelowPaint = new Paint();
	private final Paint mAbovePaint = new Paint();
	private final int mWidth, mHeight;
	private RectF mRectF = null;
	private int mChartMode = CHART_PIE;
	private float mStartAngle = 0.0f;
	private float mPercent = 0.0f;
	
	public ChartDrawable() {
		this(-1, -1, CHART_PIE);
	}
	
	public ChartDrawable(int mode) {
		this(-1, -1, mode);
	}
	
	public ChartDrawable(int width, int height) {
		this(width, height, CHART_PIE);
	}
	
	public ChartDrawable(int width, int height, int mode) {
		mWidth = width > 0 && height > 0 ? width : -1;
		mHeight = width > 0 && height > 0 ? height : -1;
		
		mBelowPaint.setColor(Color.BLACK);
		mBelowPaint.setStyle(Paint.Style.STROKE);
		mBelowPaint.setStrokeWidth(4.0f);
		
		mAbovePaint.setColor(Color.BLACK);
		mAbovePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mAbovePaint.setStrokeWidth(4.0f);
		
		switch (mode) {
		case CHART_PIE:
		case CHART_HISTOGRAM:
			mChartMode = mode;
			break;
		}
	}
	
	public Paint getBelowPaint() { return mBelowPaint; }
	public Paint getAbovePaint() { return mAbovePaint; }
	
	public float getStartAngle() { return mStartAngle; }
	public void setStartAngle(float angle) { mStartAngle = angle; }
	
	public float getPercent() { return mPercent; }
	public void setPercent(float percent) { mPercent = percent; }
	
	public int getChartMode() { return mChartMode; }
	public void setChartMode(int mode) { mChartMode = mode; }
	
	@Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	mRectF = new RectF(left, top, right, bottom);
    	
    	if (DEBUG && LOG.isDebugEnabled()) 
			LOG.debug("setBounds: rect=" + mRectF);
	}
	
	@Override 
    public void setBounds(Rect bounds) {
    	if (bounds != null) 
    		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom); 
    }
	
	@Override
	public void draw(Canvas canvas) {
		RectF rectF = mRectF;
		if (rectF != null && mChartMode == CHART_PIE) {
			float width = rectF.right - rectF.left;
			float height = rectF.bottom - rectF.top;
			
			float cx = (rectF.left + rectF.right) / 2.0f;
			float cy = (rectF.top + rectF.bottom) / 2.0f;
			
			float radius = width / 2.0f;
			if (height < width) radius = height / 2.0f;
			
			float strokeWidth = mAbovePaint.getStrokeWidth() / 2.0f;
			
			RectF rf = new RectF(cx-radius+strokeWidth, cy-radius+strokeWidth, 
					cx+radius-strokeWidth, cy+radius-strokeWidth);
			
			radius -= mBelowPaint.getStrokeWidth() / 2.0f;
			
			float percent = mPercent;
			if (percent < 0) percent = 0;
			else if (percent > 100) percent = 100;
			
			float startAngle = mStartAngle;
			float sweepAngle = 3.6f * percent;
			
			if (DEBUG && LOG.isDebugEnabled()) {
				LOG.debug("draw: pie chart: rect=" + rectF + " cx=" + cx + " cy=" + cy 
						+ " radius=" + radius + " arcRect=" + rf + " startAngle=" + startAngle 
						+ " sweepAngle=" + sweepAngle + " percent=" + percent);
			}
			
			canvas.drawCircle(cx, cy, radius, mBelowPaint);
			canvas.drawArc(rf, startAngle, sweepAngle, true, mAbovePaint);
			
		} else if (rectF != null && mChartMode == CHART_HISTOGRAM) {
			float width = rectF.right - rectF.left;
			//float height = rectF.bottom - rectF.top;
			
			float percent = mPercent;
			if (percent < 0) percent = 0;
			else if (percent > 100) percent = 100;
			float sweepWidth = width * percent / 100.0f;
			
			RectF rf = new RectF(rectF.left, rectF.top, rectF.left+sweepWidth, rectF.bottom);
			
			if (DEBUG && LOG.isDebugEnabled()) {
				LOG.debug("draw: histogram chart: rect=" + rectF + " rf=" + rf 
						+ " percent=" + percent);
			}
			
			canvas.drawRect(rectF, mBelowPaint);
			canvas.drawRect(rf, mAbovePaint);
		}
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
	public void setMetricsFormater(IMetricsFormater formater) {
	}

	@Override
	public boolean updateMetrics(MetricsContext context) {
		return false;
	}

	@Override
	public void setProgress(float p) {
		setPercent(100.0f * p);
	}

	@Override
	public void setProgressInformation(String text) {
	}

	@Override
	public void refreshProgress() {
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
	
}
