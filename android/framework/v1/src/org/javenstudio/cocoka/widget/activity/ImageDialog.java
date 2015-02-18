package org.javenstudio.cocoka.widget.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock; 
import android.widget.ImageView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.util.ImageBindable;
import org.javenstudio.cocoka.util.ImageDrawable;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.ImageRefreshable;
import org.javenstudio.cocoka.widget.SimpleDialog;

//@SuppressWarnings({"unused"})
public class ImageDialog extends SimpleDialog implements ImageBindable, ImageRefreshable {
	
	public interface ImageBinder { 
		public void bindImage(String name, ImageFile file, View view); 
	}
	
	public interface OnDialogDismissListener { 
		public void onDialogDismiss(); 
	}
	
	private ImageView mImageView = null; 
	private int mImageViewId = 0; 
	private Drawable mDrawable = null; 
	private ImageFile mFile = null; 
	private OnDialogDismissListener mDismissListener = null; 
	
	private boolean mTouchEventDown = false; 
	private long mTouchEventDownTime = 0; 
	private int mTouchSlop = 0;
	private float mLastMotionX = 0.0f; 
	private float mLastMotionY = 0.0f; 
	
	public ImageDialog(Context context, int style, int contentView, int imageView) {
		super(context, style, contentView); 
		
		mImageViewId = imageView; 
		
		setCanceledOnTouchOutside(true); 
	}
	
	public void setImageBinder(String[] names, int[] resources, final ImageBinder binder) { 
		if (names == null || resources == null || binder == null) 
			return; 
		
		synchronized (this) { 
			setViewBinder(names, resources, new ViewBinder() {
					public void bindView(String name, View view) { 
						ImageFile file = mFile; 
						if (file != null) 
							binder.bindImage(name, file, view); 
					}
				}); 
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		
		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
		
        mImageView = (ImageView)findViewById(mImageViewId); 
	}
	
	public void setOnDialogDismissListener(OnDialogDismissListener listener) { 
		mDismissListener = listener; 
	}
	
	public void onDismissDialog() {
		OnDialogDismissListener listener = mDismissListener; 
		if (listener != null) 
			listener.onDialogDismiss(); 
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (super.onKeyDown(keyCode, event))
			return true; 
		
		return false; 
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction(); 
		final float xf = event.getX(); 
		final float yf = event.getY(); 

		switch (action) {
		case MotionEvent.ACTION_DOWN: 
			mTouchEventDown = true; 
			mTouchEventDownTime = SystemClock.elapsedRealtime(); 
			mLastMotionX = xf; 
			mLastMotionY = yf; 
			break; 
			
		case MotionEvent.ACTION_MOVE: 
			/*
             * Locally do absolute value. mLastMotionX is set to the y value
             * of the down event.
             */
            final int xDiff = (int) Math.abs(xf - mLastMotionX);
            final int yDiff = (int) Math.abs(yf - mLastMotionY);
 
            final int touchSlop = mTouchSlop;
            boolean xMoved = xDiff > touchSlop;
            boolean yMoved = yDiff > touchSlop;
            
            if (xMoved || yMoved) { 
            	mTouchEventDown = false; 
            }
			break; 
			
		case MotionEvent.ACTION_UP: 
			if (mTouchEventDown) {
				mTouchEventDown = false; 
				
				final long current = SystemClock.elapsedRealtime(); 
				final long elapsed = current - mTouchEventDownTime; 
				
				if (elapsed > 0 && elapsed < 500) 
					onTouchPressed(event); 
				else if (elapsed > 2000) 
					onLongPressedImage(); 
				
				return true; 
			}
			break; 
		}
		
		return super.onTouchEvent(event); 
	}
	
	private void onTouchPressed(MotionEvent event) {
		ImageView view = mImageView; 
		if (view == null) 
			return; 
		
		//final int action = event.getAction(); 
		final float xf = event.getX(); 
		final float yf = event.getY(); 
		
		final int left = view.getLeft(); 
		final int right = view.getRight(); 
		final int top = view.getTop(); 
		final int bottom = view.getBottom(); 
		
		//final int middleX = (left + right) / 2; 
		//final int middleY = (top + bottom) / 2; 
		
		final int width = (right - left); 
		final int height = (bottom - top); 
		
		int offsetX = width / 3; 
		int offsetY = height / 3; 
		
		final int tLeft = left + offsetX; 
		final int tRight = right - offsetX; 
		final int tTop = top; 
		final int tBottom = top + offsetY; 
		
		final int lLeft = left; 
		final int lRight = left + offsetX; 
		final int lTop = top + offsetY; 
		final int lBottom = bottom - offsetY; 
		
		final int rLeft = right - offsetX; 
		final int rRight = right; 
		final int rTop = top + offsetY; 
		final int rBottom = bottom - offsetY; 
		
		final int bLeft = left + offsetX; 
		final int bRight = right - offsetX; 
		final int bTop = bottom - offsetY; 
		final int bBottom = bottom; 
		
		final int x = (int)xf; 
		final int y = (int)yf; 
		
		final Rect tRect = new Rect(tLeft, tTop, tRight, tBottom); 
		final Rect lRect = new Rect(lLeft, lTop, lRight, lBottom); 
		final Rect rRect = new Rect(rLeft, rTop, rRight, rBottom); 
		final Rect bRect = new Rect(bLeft, bTop, bRight, bBottom); 
		
		if (tRect.contains(x, y)) {
			onShowPrev(); 
			
		} else if (lRect.contains(x, y)) {
			onShowPrev(); 
			
		} else if (rRect.contains(x, y)) {
			onShowNext(); 
			
		} else if (bRect.contains(x, y)) {
			onShowNext(); 
			
		}
	}
	
	protected void onShowNext() {}
	protected void onShowPrev() {}
	protected void onLongPressedImage() {}
	
	@Override 
	public void bindImage(ImageFile file, Drawable d) { 
		if (file == null) return; 
		
		setImageFile(file); 
		setImageDrawable(d); 
	}
	
	@Override 
	public boolean refreshDrawable(Drawable d) { 
		bindViews(); 
		setImageDrawable(d); 
		
		return true; 
	}
	
	private synchronized void setImageFile(ImageFile file) { 
		if (file == null || file == mFile) 
			return; 
		
		mFile = file; 
		
		bindViews(); 
	}
	
	protected synchronized void setImageDrawable(Drawable d) {
		if (d == null || d == mDrawable) 
			return; 

		ImageView view = mImageView; 
		if (view != null) {
			view.setImageDrawable(d); 
			if (d instanceof ImageDrawable) 
				((ImageDrawable)d).onDrawableBinded(view);
			
			//Drawable old = mDrawable; 
			mDrawable = d; 
			view.requestLayout(); 
		}
	}
	
	protected synchronized void setImageLocation(String location) {
		if (location == null || location.length() == 0) 
			return; 
		
		ImageView view = mImageView; 
		if (view != null) {
			view.setImageURI(Uri.parse(location)); 

			//Drawable old = mDrawable; 
			mDrawable = null; 
		}
	}
}
