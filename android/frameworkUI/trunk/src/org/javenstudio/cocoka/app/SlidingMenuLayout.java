package org.javenstudio.cocoka.app;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SlidingMenuLayout extends FrameLayout {

	public enum Status { COLLAPSED, EXPANDED, COLLAPSING, EXPANDING }
	private Status mStatus = Status.COLLAPSED;
	
	private boolean mResetLayout = false;
    private long mStartTimeMillis = -1;
    private int mDuration = 0;
    private int mInvalidateMode = 0;
	
	protected int mWidthMeasureSpec = -1;
	protected int mHeightMeasureSpec = -1;
	
	protected int mWidthMeasured = -1;
	protected int mHeightMeasured = -1;
	
    public SlidingMenuLayout(Context context) {
        super(context);
    }
    
    public SlidingMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    	
    	mWidthMeasureSpec = widthMeasureSpec;
    	mHeightMeasureSpec = heightMeasureSpec;
    	
    	mWidthMeasured = getMeasuredWidth();
    	mHeightMeasured = getMeasuredHeight();
    	
    	int width = mWidthMeasured;
    	int height = mHeightMeasured;
    	
    	switch (mStatus) { 
    	case COLLAPSED: 
    	case EXPANDED: {
        	if (mStatus == Status.COLLAPSED) 
        		height = 0;
        	
    		break;
    	}
    	case COLLAPSING: 
    	case EXPANDING: {
    		if (mInvalidateMode == WHAT_REQUESTLAYOUT) {
    			height = nextHeight(height);
                mHandler.sendEmptyMessage(WHAT_REQUESTLAYOUT);
            } else if (mStatus == Status.EXPANDING && mResetLayout) { 
            	mResetLayout = false;
            	height = 0;
            }
    		break;
    	}}
    	
    	setMeasuredDimension(
    		getDefaultSize(width, widthMeasureSpec), 
    		getDefaultSize(height, heightMeasureSpec));
    }
    
    private int nextHeight(int height) { 
    	if (mStartTimeMillis >= 0 && mDuration > 0) {
    		float elapsed = SystemClock.uptimeMillis() - mStartTimeMillis;
            float normalized = (float)(elapsed) / mDuration;
            boolean done = normalized >= 1.0f;
            normalized = Math.min(normalized, 1.0f);
            
            if (mStatus == Status.COLLAPSING) { 
            	height = (int)(mHeightMeasured * (1 - normalized * normalized));
            	if (done) { 
            		mStatus = Status.COLLAPSED;
            		mInvalidateMode = 0;
            		mStartTimeMillis = -1;
            	}
            	
            } else { 
            	height = (int)(mHeightMeasured * normalized * normalized);
            	if (done) { 
            		mStatus = Status.EXPANDED;
            		mInvalidateMode = 0;
            		mStartTimeMillis = -1;
            	}
            }
        }
    	
    	return height;
    }
    
    private void handleInvalidate() { 
    	mInvalidateMode = WHAT_INVALIDATE;
    	invalidate();
    }
    
    private void handleRequestLayout() { 
    	mInvalidateMode = WHAT_REQUESTLAYOUT;
		invalidate();
		requestLayout();
    }
    
    private static final int WHAT_INVALIDATE = 1;
    private static final int WHAT_REQUESTLAYOUT = 2;
    
    private final Handler mHandler = new Handler() { 
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) { 
    		case WHAT_INVALIDATE: 
    			handleInvalidate();
    			break;
    		case WHAT_REQUESTLAYOUT: 
    			handleRequestLayout();
    			break;
    		}
    	}
    };
    
    public Status getStatus() { return mStatus; }
    
    private static final int DURATION_MAX = 1600;
    
    public void collapse(boolean reset) { 
    	collapse(-1, reset);
    }
    
    public void collapse(int duration, boolean reset) { 
    	toggle(Status.COLLAPSING, -1, WHAT_REQUESTLAYOUT, reset);
    }
    
    public void expand(boolean reset) { 
    	expand(-1, reset);
    }
    
    public void expand(int duration, boolean reset) { 
    	toggle(Status.EXPANDING, -1, WHAT_REQUESTLAYOUT, reset);
    }
    
    private void toggle(Status status, int duration, int what, boolean reset) {
    	if (mWidthMeasured == -1 || mHeightMeasured == -1) 
    		return;
    	
    	if (mStatus == Status.COLLAPSING || mStatus == Status.EXPANDING) 
    		return;
    	
    	if (mStatus == Status.COLLAPSED && status == Status.COLLAPSING)
    		return;
    	
    	if (mStatus == Status.EXPANDED && status == Status.EXPANDING)
    		return;
    	
    	if (duration < 0) { 
    		duration = 200 * getChildCount();
        	if (duration > DURATION_MAX) 
        		duration = DURATION_MAX;
    	}
    	
    	mStatus = status;
    	mStartTimeMillis = SystemClock.uptimeMillis();
    	mDuration = duration;
    	mResetLayout = reset;
    	
    	mHandler.sendEmptyMessage(what);
    }
    
}
