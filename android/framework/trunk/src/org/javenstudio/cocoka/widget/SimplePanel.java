package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.android.ResourceHelper;

//@SuppressWarnings({"unused"})
public class SimplePanel extends ViewGroup {
	
	protected View mContent = null; 
	protected boolean mRequestMeasure = false; 
	
	public SimplePanel(Context context) {
		this(context, null, 0);
	}

    public SimplePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public SimplePanel(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle); 
    }
    
    @Override
    protected void onFinishInflate() {
    	// do nothing
    }
    
    public View setContentView(int resid) {
    	if (resid == 0) return null; 
    	
    	View view = ResourceHelper.getResourceContext().inflateView(resid, this, false); 
    	
    	return setContentView(view); 
    }
    
    public View setContentView(View view) {
    	final View content = mContent; 
    	
    	if (view != content && view != null) {
    		if (content != null) {
    			removeView(content); 
    			onViewRemoved(content); 
    		}
    		
    		addView(view); 
    		mContent = view; 
    		
    		onViewAdded(view); 
    		
    		requestLayout(); 
    		invalidate(); 
    	}
    	
    	return view; 
    }
    
    protected void onViewRemoved(View view) { }
    
    protected void onViewAdded(View view) { }
    
    public View getContentView() {
    	return mContent; 
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("cannot have UNSPECIFIED dimensions");
        }

        final View content = mContent; 
        
        if (content != null) {
        	measureChild(content, widthMeasureSpec, heightMeasureSpec); 
        	
        	widthSpecSize = measureWidth(widthSpecSize, content.getMeasuredWidth()); 
        	heightSpecSize = measureHeight(heightSpecSize, content.getMeasuredHeight()); 
        	
        } else {
        	widthSpecSize = 0; 
        	heightSpecSize = 0; 
        }
        
        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }
    
    protected int measureWidth(int widthSpecSize, int contentWidth) {
    	return contentWidth; 
    }
    
    protected int measureHeight(int heightSpecSize, int contentHeight) {
    	return contentHeight; 
    }
    
    protected void onMeasureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    	//int widthSpecMode = MeasureSpec.getMode(parentWidthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(parentWidthMeasureSpec);

        //int heightSpecMode = MeasureSpec.getMode(parentHeightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(parentHeightMeasureSpec);
        
        LayoutParams lp = (LayoutParams)child.getLayoutParams(); 
    	if (lp != null && lp.fillscreen) {
    		if (lp.width < 0 || lp.width < widthSpecSize) 
    			lp.width = widthSpecSize; 
    		if (lp.height < 0 || lp.height < heightSpecSize) 
    			lp.height = heightSpecSize; 
    	}
    }
    
    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    	onMeasureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec); 
    	super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec); 
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final View content = mContent;
        
        if (content != null) {
	        //int contentWidth = content.getMeasuredWidth(); 
	        //int contentHeight = content.getMeasuredHeight();
	        
	        content.layout(0, 0, r-l, b-t); 
        }
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
    	final long drawingTime = getDrawingTime();
    	final View content = mContent; 
    	
    	if (content != null) {
    		drawChild(canvas, content, drawingTime); 
    	}
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	return super.onInterceptTouchEvent(event); 
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	return super.onTouchEvent(event); 
    }
    
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SimplePanel.LayoutParams(getContext(), attrs);
    }
    
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
    	return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
    }
    
    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof SimplePanel.LayoutParams;
    }
    
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public boolean fillscreen = false;

        /**
         * {@inheritDoc}
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}
