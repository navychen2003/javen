package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class FrameDrawable extends AnimationDrawable {
	private static final Logger LOG = Logger.getLogger(FrameDrawable.class);
	private static final boolean DEBUG = BaseDrawable.DEBUG;
	
    //private static final int TRANSITION_STARTING = 0;
    //private static final int TRANSITION_RUNNING = 1;
    //private static final int TRANSITION_NONE = 2;
	
    private static enum TransitionState { NONE, RUNNING, STOP }
    
    public interface InvalidateListener { 
    	public void onInvalidateDrawable(Drawable who);
    }
    
    public interface DrawableListener { 
    	public void onFrameChanged(Drawable current, Drawable newPrev, Drawable oldPrev);
    }
    
    private Drawable mPrev = null;
    private InvalidateListener mInvalidateListener = null;
    private DrawableListener mDrawableListener = null;
    private TransitionState mTransitionState = TransitionState.NONE;
    private long mStartTimeMillis = 0;
    private int mDuration = 0;
    private int mAlpha = 255;
    
	public FrameDrawable() {}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	public void setInvalidateListener(InvalidateListener l) { 
		mInvalidateListener = l;
	}
	
	public void setDrawableListener(DrawableListener l) { 
		mDrawableListener = l;
	}
	
	public void setDuration(int duration) { 
		if (duration > 0) 
			mDuration = duration; 
		else 
			mDuration = 0;
	}
	
	public void resetFrame(int alpha, boolean stopAni) { 
		if (alpha < 0) alpha = 0;
		if (alpha > 255) alpha = 255;
		
		mAlpha = alpha;
		mPrev = null;
		mTransitionState = stopAni ? 
				TransitionState.STOP : TransitionState.NONE;
	}
	
	@Override
    public void draw(Canvas canvas) {
		final Drawable current = getCurrent();
		final Drawable prev = mPrev;
		
        boolean done = true;

        if (current != prev && mDuration > 0 && mTransitionState == TransitionState.NONE) {
        	mStartTimeMillis = SystemClock.uptimeMillis();
            mTransitionState = TransitionState.RUNNING;
            mAlpha = 0;
            done = false;
        }
        
        switch (mTransitionState) {
            case RUNNING:
                if (mStartTimeMillis >= 0) {
                    float normalized = (float)
                            (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                    done = normalized >= 1.0f;
                    normalized = Math.min(normalized, 1.0f);
                    mAlpha = (int) (255 * normalized);
                }
                break;
            case STOP: 
            	mAlpha = 255;
            	break;
            default: 
            	break;
        }
        
		if (done) {
			mPrev = current;
			mTransitionState = TransitionState.NONE;
		}
		
		if (prev != null && prev != current) 
			prev.draw(canvas);
		
        if (current != null) {
        	current.setAlpha(mAlpha);
        	current.draw(canvas);
        	current.setAlpha(0xFF);
        }
        
        if (!done)
        	invalidateSelf();
        
        if (done && prev != mPrev) { 
        	if (DEBUG && LOG.isDebugEnabled()) { 
        		LOG.debug("FrameDrawable(" + getIdentity() + ").onFrameChanged: " 
    					+ getDebugInfo());
        	}
        	
        	DrawableListener listener = mDrawableListener;
        	if (listener != null) 
        		listener.onFrameChanged(current, mPrev, prev);
        }
	}
	
	@Override
	public void invalidateDrawable(Drawable who) {
		if (!isVisible()) return;
		
		InvalidateListener listener = mInvalidateListener;
		if (listener != null) 
			listener.onInvalidateDrawable(who);
		
		if (mTransitionState == TransitionState.RUNNING) 
			return;
		
		super.invalidateDrawable(who);
	}
	
	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if (!isVisible()) return;
		super.scheduleDrawable(who, what, when);
	}
	
	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		super.unscheduleDrawable(who, what);
	}
	
	private boolean mStarted = false;
	private int mScheduleTimes = 0;
	
	private final Runnable mStarter = new Runnable() {
			@Override
			public void run() {
				synchronized (FrameDrawable.this) {
					if (isVisible()) start();
					mStarted = false;
				}
			}
		};
	
	public final boolean isStarted() { return mStarted; }
	public final boolean isScheduled() { return mScheduleTimes > 0; }
		
	public synchronized final void start(final int delayMillis) { 
		if (isRunning() || !isVisible() || isStarted() || isScheduled()) 
			return;
		
		if (delayMillis > 0) {
			mStarted = true;
			ResourceHelper.getHandler().postDelayed(mStarter, delayMillis);
		} else 
			start();
	}
	
	@Override
	public final boolean setVisible(boolean visible, boolean restart) {
		boolean changed = super.setVisible(visible, restart);
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").setVisible(" 
					+ visible + "," + restart + "): " + " changed=" + changed 
					+ getDebugInfo());
		}
		
		return changed;
	}
	
	@Override
	public synchronized final void start() {
		if (!isVisible() || isScheduled()) return;
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").start: " 
					+ getDebugInfo());
		}
		
		super.start();
	}
	
	@Override
	public synchronized final void stop() {
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").stop: " 
					+ getDebugInfo());
		}
		
		super.stop();
	}
	
	@Override
	public synchronized final void run() {
		if (!isVisible()) return;
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").run: " 
					+ getDebugInfo());
		}
		
		mScheduleTimes --;
		if (mScheduleTimes < 0) mScheduleTimes = 0;
		
		super.run();
	}
	
	@Override
	public synchronized void scheduleSelf(Runnable what, long when) {
		if (!isVisible() || isScheduled()) return;
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").scheduleSelf: " 
					+ " when=" + when + " uptime=" + SystemClock.uptimeMillis() 
					+ getDebugInfo());
		}
		
		if (mScheduleTimes < 0) mScheduleTimes = 0;
		mScheduleTimes ++;
		
		super.scheduleSelf(what, when);
	}
	
	@Override
    public synchronized void unscheduleSelf(Runnable what) {
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("FrameDrawable(" + getIdentity() + ").unscheduleSelf: " 
					+ getDebugInfo());
		}
		
		super.unscheduleSelf(what);
		
		mScheduleTimes --;
		if (mScheduleTimes < 0) mScheduleTimes = 0;
	}
	
	@Override
	public final void addFrame(Drawable frame, int duration) {
		if (DEBUG && LOG.isDebugEnabled()) {
			String frameInfo = " duration=" + duration;
			
			if (frame != null && frame instanceof BaseDrawable) 
				frameInfo += " frameId=" + ((BaseDrawable)frame).getIdentity();
			
			LOG.debug("FrameDrawable(" + getIdentity() + ").addFrame: " 
					+ frameInfo + getDebugInfo());
		}
		
		super.addFrame(frame, duration);
	}
	
	private String getDebugInfo() { 
		String currentInfo = " frameNum=" + getNumberOfFrames();
		
		Drawable d = getCurrent();
		if (d != null && d instanceof BaseDrawable) 
			currentInfo += " currentId=" + ((BaseDrawable)d).getIdentity();
		
		return " visible=" + isVisible() + " running=" + isRunning() 
				+ " schedules=" + mScheduleTimes
				//+ " oneShot=" + isOneShot() + " started=" + isStarted() 
				+ currentInfo;
		
	}
	
}
