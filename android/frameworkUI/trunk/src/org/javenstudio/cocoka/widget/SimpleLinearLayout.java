package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class SimpleLinearLayout extends LinearLayout {
	//private static final Logger LOG = Logger.getLogger(RegisterLayout.class);

	public static interface OnDrawListener {
		public void onDraw(Canvas canvas);
	}
	
	public interface CanvasTransformer {
		public void transformCanvas(Canvas canvas, View view);
	}
	
	private OnDrawListener mOnDrawListener = null;
	public void setOnDrawListener(OnDrawListener listener) { mOnDrawListener = listener; }
	
	private CanvasTransformer mTransformer = null;
	public void setCanvasTransformer(CanvasTransformer t) { mTransformer = t; }
	
    public SimpleLinearLayout(Context context) {
        super(context);
    }

    public SimpleLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SimpleLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
    @Override
    protected void dispatchDraw(Canvas canvas) {
    	CanvasTransformer transformer = mTransformer;
    	if (transformer != null) {
			canvas.save();
			transformer.transformCanvas(canvas, this);
			super.dispatchDraw(canvas);
			canvas.restore();
		} else
			super.dispatchDraw(canvas);
    	
    	OnDrawListener listener = mOnDrawListener;
    	if (listener != null) listener.onDraw(canvas);
    }
    
}
