package org.javenstudio.android.app;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.graphics.FrameDrawable;
import org.javenstudio.cocoka.graphics.TouchListener;
import org.javenstudio.common.util.Logger;

public class SlideDrawable extends FrameDrawable 
		implements FrameDrawable.DrawableListener, TouchListener {
	private static final Logger LOG = Logger.getLogger(SlideDrawable.class);
	
	public interface Callback { 
		public Drawable next();
	}
	
	private boolean mDirty = false;
	
	public SlideDrawable(final Callback callback) { 
		setDuration(1500);
		setOneShot(true);
		
		Drawable frame = null;
		while ((frame = callback.next()) != null) {
			addFrame(frame, 4000);
			
			if (getNumberOfFrames() > 100) 
				break;
		}
		
		selectDrawable(0);
		setDrawableListener(this);
		
		TouchHelper.addListener(this);
	}
	
	@Override
	public void onTouchUp(Activity activity) { 
		invalidateSelf();
	}
	
	@Override
	public void onFrameChanged(Drawable current, Drawable newPrev, Drawable oldPrev) {
		if (oldPrev != null && oldPrev instanceof BitmapCache.CacheDrawable) { 
			BitmapCache.CacheDrawable bd = (BitmapCache.CacheDrawable)oldPrev;
			bd.recycle();
		}
	}
	
	@Override
	public void scheduleSelf(Runnable what, long when) {
		if (isDirty()) return;
		super.scheduleSelf(what, when);
	}
	
	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if (isDirty()) return;
		super.scheduleDrawable(who, what, when);
	}
	
	public final boolean showSlide(boolean restart) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("showSlide: id=" + getIdentity() + " restart=" + restart 
					+ " dirty=" + isDirty());
		}
		
		if (isDirty()) return false;
		setVisible(true, restart);
		
		if (isRunning() || isStarted() || !isVisible()) 
			return false;
		
		if (getNumberOfFrames() > 1) {
			setDuration(1500);
			setOneShot(true);
			
			start(0);
			return true;
		}
		
		return false;
	}
	
	public final void hideSlide() { 
		setVisible(false, false);
	}
	
	public final boolean isDirty() { return mDirty; }
	public final void setDirty() { hideSlide(); mDirty = true; }
	
}
