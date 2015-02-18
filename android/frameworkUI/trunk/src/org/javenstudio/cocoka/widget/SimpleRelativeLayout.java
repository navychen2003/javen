package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class SimpleRelativeLayout extends RelativeLayout {

	public static interface OnTouchEventListener {
		public void onDispatchTouchEvent(SimpleRelativeLayout view, MotionEvent ev);
	}
	
	private OnTouchEventListener mOnTouchEventListener = null;
	public void setOnTouchEventListener(OnTouchEventListener l) { mOnTouchEventListener = l; }
	
	public SimpleRelativeLayout(Context context) {
        super(context);
    }
    
    public SimpleRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
    	OnTouchEventListener listener = mOnTouchEventListener;
    	if (listener != null) listener.onDispatchTouchEvent(this, ev);
    	return super.dispatchTouchEvent(ev);
    }
    
}
