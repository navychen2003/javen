package org.javenstudio.cocoka.widget;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.view.MotionEvent;

public class CustomLayout {
	
	public static interface OnClickListener { 
		public void onAreaClicked(ClickArea area); 
	}
	
	public static interface LayoutAdapter { 
		public void onInitClickArea(List<CustomLayout.ClickArea> areas); 
		public boolean handleClick(); 
		public boolean handleSwipeFromLeftToRight(); 
		public boolean handleSwipeFromRightToLeft(); 
		public boolean handleSwipeFromTopToBottom(); 
		public boolean handleSwipeFromBottomToTop(); 
	}
	
	public static class ClickArea { 
		private final Rect mRect; 
		private final OnClickListener mListener; 
		
		public ClickArea(Rect rect, OnClickListener l) { 
			mRect = rect; 
			mListener = l; 
		}
		
		public boolean contains(int touchX, int touchY) { 
			Rect r = mRect; 
			if (r != null) 
				return r.contains(touchX, touchY); 
			else 
				return false; 
		}
		
		final void onClicked() { 
			OnClickListener l = mListener; 
			if (l != null) 
				l.onAreaClicked(this); 
		}
	}
	
	private static final int SWIPE_FROM_LEFT_TO_RIGHT = 1; 
	private static final int SWIPE_FROM_RIGHT_TO_LEFT = 2; 
	private static final int SWIPE_FROM_TOP_TO_BOTTOM = 3; 
	private static final int SWIPE_FROM_BOTTOM_TO_TOP = 4; 
	
	private final ArrayList<ClickArea> mClickAreas = new ArrayList<ClickArea>(); 
	private ClickArea mTouchedArea = null; 
	
	private int mTouchSlop = 0;
	//private int mMaximumVelocity = 0;
	private int mTouchState = 0; 
	private int mFirstTouchX = -1; 
	private int mFirstTouchY = -1; 
	private boolean mDownEvent = false; 
	private boolean mHandleSwipe = false; 
	
	private final LayoutAdapter mLayout; 
	
    public CustomLayout(LayoutAdapter layout) {
        mLayout = layout; 
        initialize(); 
    }

    protected void initialize() { 
    	//final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = 15; //configuration.getScaledTouchSlop();
        //mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }
    
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    	synchronized (mClickAreas) { 
    		mClickAreas.clear(); 
    		onInitClickArea(mClickAreas); 
    	}
    }
	
    public void setHandleSwipe(boolean handle) { 
    	mHandleSwipe = handle; 
    }
    
    protected void onInitClickArea(List<ClickArea> areas) { 
    	mLayout.onInitClickArea(areas); 
    }
    
    private ClickArea findTouchedArea(int touchX, int touchY) { 
    	synchronized (mClickAreas) { 
    		for (ClickArea area : mClickAreas) { 
    			if (area.contains(touchX, touchY)) 
    				return area; 
    		}
    	}
    	return null; 
    }
    
    /**
     * Overriding this method allows us to "catch" clicks in the checkbox or star
     * and process them accordingly.
     */
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        
        final int touchX = (int) event.getX(); 
        final int touchY = (int) event.getY(); 

        final int touchSlop = mTouchSlop; 
        final boolean handleSwipe = mHandleSwipe; 
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownEvent = true;
                mTouchState = 0; 
                mFirstTouchX = touchX; 
                mFirstTouchY = touchY; 
                mTouchedArea = findTouchedArea(touchX, touchY); 
                if (mTouchedArea != null) 
                	handled = true; 
                else if (handleSwipe && touchSlop > 0) 
                	handled = true; 
                break;

            case MotionEvent.ACTION_CANCEL:
                mDownEvent = false;
                mTouchState = 0; 
                break;

            case MotionEvent.ACTION_UP:
                if (mDownEvent) {
                    ClickArea area = mTouchedArea; 
                    if (area != null && area.contains(touchX, touchY)) { 
                    	area.onClicked(); 
                    	handled = true; 
                    	
                    } else { 
                    	switch (mTouchState) { 
                    	case SWIPE_FROM_LEFT_TO_RIGHT: 
                    		handled = handleSwipeFromLeftToRight(); 
                    		break; 
                    	case SWIPE_FROM_RIGHT_TO_LEFT: 
                    		handled = handleSwipeFromRightToLeft(); 
                    		break; 
                    	case SWIPE_FROM_TOP_TO_BOTTOM: 
                    		handled = handleSwipeFromTopToBottom(); 
                    		break; 
                    	case SWIPE_FROM_BOTTOM_TO_TOP: 
                    		handled = handleSwipeFromBottomToTop(); 
                    		break; 
                    	}
                    }
                    
                    if (!handled) 
                    	handled = handleClick(); 
                }
                mDownEvent = false; 
                mFirstTouchX = -1; 
                mFirstTouchY = -1; 
                break;
                
            case MotionEvent.ACTION_MOVE:
            	if (touchSlop > 0 && handleSwipe) { 
            		/*
                     * Locally do absolute value. mLastMotionX is set to the y value
                     * of the down event.
                     */
                    final int xDiff = (int) Math.abs(touchX - mFirstTouchX);
                    final int yDiff = (int) Math.abs(touchY - mFirstTouchY);

                    boolean xMoved = xDiff > touchSlop;
                    boolean yMoved = yDiff > touchSlop;
                	
                    if (xMoved || yMoved) { 
                    	// means the finger path pitch is smaller than 45deg so we assume the user want to scroll X axis
                        if (xDiff > yDiff) {
                        	if (touchX > mFirstTouchX) 
                        		mTouchState = SWIPE_FROM_LEFT_TO_RIGHT; 
                        	else 
                        		mTouchState = SWIPE_FROM_RIGHT_TO_LEFT; 
                        } else { 
                        	if (touchY > mFirstTouchY) 
                        		mTouchState = SWIPE_FROM_TOP_TO_BOTTOM; 
                        	else 
                        		mTouchState = SWIPE_FROM_BOTTOM_TO_TOP; 
                        }
                    }
            	}
                break;
        }

        return handled;
    }
    
    protected boolean handleClick() { 
    	return mLayout.handleClick(); 
    }
    
    protected boolean handleSwipeFromLeftToRight() { 
    	return mLayout.handleSwipeFromLeftToRight(); 
    }
    
    protected boolean handleSwipeFromRightToLeft() { 
    	return mLayout.handleSwipeFromRightToLeft(); 
    }
    
    protected boolean handleSwipeFromTopToBottom() { 
    	return mLayout.handleSwipeFromTopToBottom(); 
    }
    
    protected boolean handleSwipeFromBottomToTop() { 
    	return mLayout.handleSwipeFromBottomToTop(); 
    }
    
}
