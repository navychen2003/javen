package org.javenstudio.cocoka.app;

import android.view.GestureDetector;
import android.view.MotionEvent;

import org.javenstudio.common.util.Logger;

public class SlidingGestureListener implements GestureDetector.OnGestureListener {
	private static final Logger LOG = Logger.getLogger(SlidingGestureListener.class);

	public boolean onDown(MotionEvent e) { return false; }
	public void onShowPress(MotionEvent e) {}
	public boolean onSingleTapUp(MotionEvent e) { return false; }
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
	public void onLongPress(MotionEvent e) {}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2,
			float velocityX, float velocityY) {
		if (e1 == null || e2 == null) 
			return false;
		
		float deltaX = e2.getX() - e1.getX();
		float deltaY = e2.getY() - e1.getY();
		
		if (deltaY < 0) deltaY = -deltaY;
		if (velocityY < 0) velocityY = -velocityY;
		
		if ((deltaX > 300 && deltaX > 3 * deltaY) || 
			(velocityX > 3000 && velocityX > 5 * velocityY)) {
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("onFlingToRight: deltaX=" + deltaX + " deltaY=" + deltaY 
						+ " velocityX=" + velocityX + " velocityY=" + velocityY);
			}
			
			if (onFlingToRight()) 
				return true;
		}
		
		return false;
	}
	
	protected boolean onFlingToRight() { return false; }
	
}
