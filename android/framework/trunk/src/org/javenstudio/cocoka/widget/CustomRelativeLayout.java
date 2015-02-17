package org.javenstudio.cocoka.widget;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * This custom View is the list item for the activity, and serves two purposes:
 * 1.  It's a container to store message metadata (e.g. the ids of the message, mailbox, & account)
 * 2.  It handles internal clicks such as the checkbox or the favorite star
 */
public class CustomRelativeLayout extends RelativeLayout implements CustomLayout.LayoutAdapter {

	private CustomLayout mCustomLayout; 
	
    public CustomRelativeLayout(Context context) {
        super(context);
        initialize(); 
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(); 
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(); 
    }
	
    protected void initialize() { 
    	mCustomLayout = new CustomLayout(this); 
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    	super.onLayout(changed, l, t, r, b); 
    	
    	mCustomLayout.onLayout(changed, l, t, r, b); 
    }
	
    public void setHandleSwipe(boolean handle) { 
    	mCustomLayout.setHandleSwipe(handle); 
    }
    
    @Override
    public void onInitClickArea(List<CustomLayout.ClickArea> areas) { 
    	// do nothing
    }
    
    /**
     * Overriding this method allows us to "catch" clicks in the checkbox or star
     * and process them accordingly.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mCustomLayout.onTouchEvent(event);

        if (handled) {
            postInvalidate();
        } else {
            handled = super.onTouchEvent(event);
        }

        return handled;
    }
    
    @Override
    public boolean handleClick() { 
    	return false; 
    }
    
    @Override
    public boolean handleSwipeFromLeftToRight() { 
    	return false; 
    }
    
    @Override
    public boolean handleSwipeFromRightToLeft() { 
    	return false; 
    }
    
    @Override
    public boolean handleSwipeFromTopToBottom() { 
    	return false; 
    }
    
    @Override
    public boolean handleSwipeFromBottomToTop() { 
    	return false; 
    }
    
}
