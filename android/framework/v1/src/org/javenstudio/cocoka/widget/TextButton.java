package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextButton extends TextView implements ToolBar.CheckedButton {

	private static final float CORNER_RADIUS = 8.0f;
	
	private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };
	
	private boolean mCheckable = true; 
	private boolean mChecked = false; 
	private boolean mBroadcasting = false;
	
	private RectF mRect = null;
	private Paint mPaint = null;
	private float mCornerRadius = CORNER_RADIUS;
	
	private OnCheckedChangeListener mOnCheckedChangeListener;
	
	public TextButton(Context context) {
		this(context, null, 0); 
	}
	
	public TextButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0); 
	}
	
	public TextButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
	}
	
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
	private void initTextgroundPaint() {
		if (mPaint != null) 
			return; 
		
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GRAY); 
        mPaint.setAlpha(192); 
	}
	
	private void initTextgroundRect() {
		if (mRect != null) return; 
		
        final Layout layout = getLayout();
        if (layout == null) 
        	return; 
        
        initTextgroundPaint(); 
        
        final float left = getCompoundPaddingLeft();
        final float top = getExtendedPaddingTop();
        final float right = left + layout.getLineRight(0); 
        final float bottom = top + layout.getLineBottom(0);
        
        final CharSequence text = getText(); 
        if (mPaint.getAlpha() > 0 && text != null && text.length() > 0) 
	        mRect = new RectF(left, top, right, bottom); 
    }
    
	public void setTextground(boolean enabled) { 
		if (!enabled) { 
			mRect = null; mPaint = null; 
			
		}
	}
	
    public void setTextgroundColor(int c) {
    	initTextgroundPaint(); 
    	
    	mPaint.setColor(c); 
    }
    
    public void setTextgroundAlpha(int n) {
    	initTextgroundPaint(); 
    	
    	mPaint.setAlpha(n); 
    }
    
    public void setTextgroundCornerRadius(float value) {
    	mCornerRadius = value; 
    }
    
    @Override 
	public boolean isCheckable() { 
		return mCheckable; 
	} 
	
	@Override 
	public void setCheckable(boolean enable) {
		mCheckable = enable; 
	}
	
	@Override 
	public boolean isChecked() { 
		return mChecked; 
	} 
	
	@Override 
	public void toggle() { 
		if (isCheckable()) 
			setChecked(!isChecked()); 
	}
	
	@Override 
	public void setChecked(boolean checked) {
        if (mCheckable && mChecked != checked) {
        	mChecked = checked;
            refreshDrawableState();
            
            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
	}
	
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mCheckable && isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
	
	@Override
    public void setDrawingCacheEnabled(boolean enabled) {
    	super.setDrawingCacheEnabled(enabled); 
    }
	

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }
	
	/**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    public static interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        void onCheckedChanged(TextButton buttonView, boolean isChecked);
    }
    
    protected void onDrawBackground(Canvas canvas) {
    	
    }
    
    protected void onDrawForeground(Canvas canvas) {
    	
    }
    
    @Override
    public void draw(Canvas canvas) {
    	onDrawBackground(canvas); 
        
        final Paint paint = mPaint; 
        if (paint != null) { 
        	initTextgroundRect(); 
        	
        	final RectF rect = mRect;
        	if (rect != null) 
        		canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, paint); 
        }
        
        super.draw(canvas); 
        onDrawForeground(canvas); 
    }
    
}
