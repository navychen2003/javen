package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.javenstudio.cocoka.android.ResourceHelper;

public class ImageButton extends ImageView implements ToolBar.CheckedButton {

	protected static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };
	
	private Drawable mForeground = null; 
	private int mForegroundResource = 0; 
	protected boolean mForegroundSizeChanged = true;
	
	private boolean mCheckable = true; 
	private boolean mChecked = false; 
	private boolean mBroadcasting = false;
	
	private OnCheckedChangeListener mOnCheckedChangeListener;
	
	public ImageButton(Context context) {
		this(context, null, 0); 
	}
	
	public ImageButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0); 
	}
	
	public ImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
	}
	
	public void setForegroundColor(int color) {
        setForegroundDrawable(new ColorDrawable(color));
    }
	
	public void setForegroundResource(int resid) {
        if (resid != 0 && resid == mForegroundResource) {
            return;
        }

        Drawable d = null;
        if (resid != 0) {
            d = ResourceHelper.getResourceContext().getDrawable(resid);
        }
        setBackground(d);

        mForegroundResource = resid;
    }
	
	public void setForegroundDrawable(Drawable d) {
		if (mForeground != d) {
			if (mForeground != null) {
				mForeground.setCallback(null);
	            unscheduleDrawable(mForeground);
			}
			
			if (d != null) {
				Rect padding = new Rect(); 
	            if (d.getPadding(padding)) {
	                setPadding(padding.left, padding.top, padding.right, padding.bottom);
	            }
	            
	            d.setCallback(this);
	            if (d.isStateful()) {
	                d.setState(getDrawableState());
	            }
	            d.setVisible(getVisibility() == VISIBLE, false);
			}
			
			mForeground = d; 
			mForegroundSizeChanged = true; 
			
			requestLayout();
            invalidate();
		}
		
		mForegroundResource = 0; 
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh); 
		
		mForegroundSizeChanged = true; 
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
        if (isCheckable() && mChecked != checked) {
        	mChecked = checked;
            refreshDrawableState();
            
            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) 
                return;

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) 
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);

            mBroadcasting = false;
        }
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged(); 
		
		Drawable d = mForeground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
	}
	
	@Override
    protected boolean verifyDrawable(Drawable dr) {
        return mForeground == dr || super.verifyDrawable(dr);
    }
    
    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr == mForeground) {
            /* we invalidate the whole view in this case because it's very
             * hard to know where the drawable actually is. This is made
             * complicated because of the offsets and transformations that
             * can be applied. In theory we could get the drawable's bounds
             * and run them through the transformation and offsets, but this
             * is probably not worth the effort.
             */
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }
	
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isCheckable() && isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
	
	@Override
    public void setDrawingCacheEnabled(boolean enabled) {
    	super.setDrawingCacheEnabled(enabled); 
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mForeground != null) mForeground.setCallback(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mForeground != null) mForeground.setCallback(null);
    }
	
	@Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        Drawable d = mForeground; 
        if (d == null) {
            return; // couldn't resolve the URI
        }

        //if (d.getIntrinsicWidth() == 0 || d.getIntrinsicHeight() == 0) 
        //	return; 
        
        onDrawForeground(canvas, d); 
	}
	
	protected void onDrawForeground(Canvas canvas, Drawable foreground) {
        if (foreground != null) {
        	canvas.save(); 
        	
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            final int leftX = getLeft(); 
            final int rightX = getRight(); 
            final int topY = getTop();
            final int bottomY = getBottom(); 

            if (mForegroundSizeChanged) {
                foreground.setBounds(0, 0,  rightX - leftX, bottomY - topY);
                mForegroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
            	foreground.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                foreground.draw(canvas);
            }
            
            canvas.restore(); 
        }
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
        void onCheckedChanged(ImageButton buttonView, boolean isChecked);
    }
}
