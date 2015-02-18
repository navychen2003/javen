package org.javenstudio.cocoka.graphics;

import java.lang.ref.WeakReference;

import org.javenstudio.cocoka.util.ImageDrawable;

import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class BaseDrawable extends Drawable implements ImageDrawable {

	public interface OnDrawableInvalidateListener { 
		public void onDrawableInvalidateSelf(Drawable d);
	}
	
	private WeakReference<View> mParentViewRef = null; 
	private WeakReference<Drawable> mParentDrawableRef = null; 
	private OnDrawableInvalidateListener mOnInvalidateListener;
	
	public BaseDrawable() {} 
	
	@Override
	public void onDrawableBinded(View view) { 
		if (view != null) 
			setParentView(view);
	}
	
	public final void setParentDrawable(Drawable d) { 
		mParentDrawableRef = null; 
		if (d != null && d != this) 
			mParentDrawableRef = new WeakReference<Drawable>(d); 
	}
	
	public final Drawable getParentDrawable() { 
		WeakReference<Drawable> ref = mParentDrawableRef; 
		return ref != null ? ref.get() : null; 
	}
	
	public final void setParentView(View view) {
		mParentViewRef = null; 
		if (view != null) 
			mParentViewRef = new WeakReference<View>(view); 
	}
	
	public final View getParentView() { 
		WeakReference<View> ref = mParentViewRef; 
		return ref != null ? ref.get() : null; 
	}
	
	public final void setOnDrawableInvalidateListener(OnDrawableInvalidateListener listener) { 
		mOnInvalidateListener = listener;
	}
	
	public final OnDrawableInvalidateListener getOnDrawableInvalidateListener() { 
		return mOnInvalidateListener;
	}
	
	@Override 
	public void invalidateSelf() { 
		super.invalidateSelf(); 
		
		Drawable d = getParentDrawable(); 
		if (d != null) 
			d.invalidateSelf(); 
		
		OnDrawableInvalidateListener listener = getOnDrawableInvalidateListener();
		if (listener != null) 
			listener.onDrawableInvalidateSelf(this);
	}
	
	@Override 
	public void scheduleSelf(Runnable what, long when) { 
		super.scheduleSelf(what, when); 
		
		Drawable d = getParentDrawable(); 
		if (d != null) 
			d.scheduleSelf(what, when); 
	}
	
	@Override 
	public void unscheduleSelf(Runnable what) { 
		super.unscheduleSelf(what); 
		
		Drawable d = getParentDrawable(); 
		if (d != null) 
			d.unscheduleSelf(what); 
	}
	
	public void requestLayoutSelf() { 
		View view = getParentView(); 
		if (view != null) { 
			view.requestLayout(); 
			return; 
		}
		
		Drawable d = getParentDrawable(); 
		if (d != null && d instanceof BaseDrawable) { 
			((BaseDrawable)d).requestLayoutSelf(); 
		}
	}
	
	public void refreshSelf() { 
		invalidateSelf(); 
		requestLayoutSelf(); 
	}
	
	public void invalidateView() {
		View view = getParentView(); 
		if (view != null) 
			view.invalidate(); 
	}
	
	public void postInvalidateView() {
		View view = getParentView(); 
		if (view != null) 
			view.postInvalidate(); 
	}
	
	public void requestLayoutView() {
		View view = getParentView(); 
		if (view != null) 
			view.requestLayout(); 
	}
	
	@Override
    public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
	@Override
    public void setAlpha(int alpha) {
		// do nothing
	}
	
	@Override
    public void setColorFilter(ColorFilter cf) {
		// do nothing
	}
	
}
