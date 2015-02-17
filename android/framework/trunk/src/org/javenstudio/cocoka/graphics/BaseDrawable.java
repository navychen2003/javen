package org.javenstudio.cocoka.graphics;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.app.Activity;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.ImageDrawable;
import org.javenstudio.common.util.Logger;

public abstract class BaseDrawable extends Drawable 
		implements ImageDrawable, TouchListener {
	private static final Logger LOG = Logger.getLogger(BaseDrawable.class);
	protected static boolean DEBUG = false;

	public interface OnDrawableInvalidateListener { 
		public void onDrawableInvalidateSelf(Drawable d);
	}
	
	private WeakReference<View> mParentViewRef = null; 
	private WeakReference<Drawable> mParentDrawableRef = null; 
	private OnDrawableInvalidateListener mOnInvalidateListener;
	
	public BaseDrawable() {} 
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	@Override
	public void onDrawableBinded(View view) { 
		if (view != null) 
			setParentView(view);
	}
	
	private static boolean hasDrawableCallback(Drawable d) { 
		try {
			Method method = d.getClass().getMethod("getCallback");
			return method.invoke(d, (Object[])null) != null;
		} catch (Throwable e) { 
			if (DEBUG && LOG.isDebugEnabled()) 
				LOG.debug("getCallback error: " + e, e);
			
			return true;
		}
	}
	
	boolean hasCallback() { 
		if (hasDrawableCallback(this)) 
			return true;
		
		Drawable parent = getParentDrawable();
		if (parent != null) { 
			if (parent instanceof BaseDrawable)
				return ((BaseDrawable)parent).hasCallback();
			
			return hasDrawableCallback(parent);
		}
		
		return true;
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
	public void onTouchUp(Activity activity) { 
		invalidateSelf();
		
		View view = getParentView();
		if (view != null) 
			view.invalidate();
	}
	
	@Override 
	public void invalidateSelf() { 
		if (DEBUG && LOG.isDebugEnabled()) LOG.debug("invalidateSelf: drawable=" + this);
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
	}
	
	@Override
    public void setColorFilter(ColorFilter cf) {
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" 
				+ Integer.toHexString(System.identityHashCode(this)) 
				+ "-" + getIdentity() + "{}";
	}
	
}
