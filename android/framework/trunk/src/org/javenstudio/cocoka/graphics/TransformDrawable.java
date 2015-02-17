package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class TransformDrawable extends Drawable implements Drawable.Callback {
	private static final Logger LOG = Logger.getLogger(TransformDrawable.class);
	private static final boolean DEBUG = BaseDrawable.DEBUG;
	
	private static final int MOVESTEP = 1;
	private static final long DELAYMILLIS = 400;
	
	private static enum Direction {
		NONE, TOP, TOPRIGHT, RIGHT, RIGHTBOTTOM, BOTTOM, LEFTBOTTOM, LEFT, LEFTTOP
	}
	
	private static enum State {
		STOP, START, MOVING, WAITING
	}
	
	private final Drawable mDrawable; 
	private final int mWidth, mHeight;
	
	private Drawable mBackground = null; 
	private Rect mDrawableBounds = null;
	private State mState = null;
	private Direction mDirection = null;
	
	public TransformDrawable(Drawable d) { 
		this(d, 0, 0);
	}
	
	public TransformDrawable(Drawable d, int width, int height) { 
		mDrawable = d; 
		mWidth = width;
		mHeight = height;
		
		if (mDrawable != null)
			mDrawable.setCallback(this);
	}
	
	public void setBackground(Drawable d) { 
		mBackground = d; 
		
		if (mBackground != null)
			mBackground.setCallback(this);
	}
	
	public void startTransforming() {
		//if (DEBUG && LOG.isDebugEnabled())
		//	LOG.debug("startTransforming: start");
		
		if (mState == null || mState == State.STOP) {
			mState = State.START;
			transformDelay(0);
		}
	}
	
	public void stopTransforming() {
		//if (DEBUG && LOG.isDebugEnabled())
		//	LOG.debug("stopTransforming: stop");
		
		mState = State.STOP;
	}
	
	private void requestTransform() {
		transformDelay(DELAYMILLIS);
	}
	
	private void transformDelay(long millis) {
		if (mState != State.MOVING && mState != State.START) return;
		mState = State.WAITING;
		
		//if (DEBUG && LOG.isDebugEnabled())
		//	LOG.debug("transformDelay: delay=" + millis);
		
		if (millis <= 0) {
			transform();
			return;
		}
		
		ResourceHelper.getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					transform();
				}
			}, millis);
	}
	
	private void transform() {
		mState = State.MOVING;
		invalidateSelf();
	}
	
	@Override
	public void invalidateSelf() {
		if (DEBUG && LOG.isDebugEnabled())
			LOG.debug("invalidateSelf: state=" + mState + " dir=" + mDirection);
		
		super.invalidateSelf();
	}
	
	private Rect getMoveBounds(Drawable d, Rect bounds) {
		if (d == null || bounds == null) return bounds;
		
		Rect bds = getBounds();
		if (bds != null && !bds.equals(bounds)) {
			int left = bounds.left;
			int right = bounds.right;
			int top = bounds.top;
			int bottom = bounds.bottom;
			
			if (left < bds.left) {
				if (right > bds.right) {
					if (top < bds.top) {
						if (bottom > bds.bottom) {
							bounds = newBounds(bounds, bds);
						} else {
							bounds = newBoundsBottom(bounds, bds);
						}
					} else {
						bounds = newBoundsTop(bounds, bds);
					}
				} else {
					if (top < bds.top) {
						if (bottom > bds.bottom) {
							bounds = newBoundsRight(bounds, bds);
						} else {
							bounds = newBoundsRightBottom(bounds, bds);
						}
					} else {
						bounds = newBoundsRightTop(bounds, bds);
					}
				}
			} else {
				if (top < bds.top) {
					if (bottom > bds.bottom) {
						bounds = newBoundsLeft(bounds, bds);
					} else {
						bounds = newBoundsLeftBottom(bounds, bds);
					}
				} else {
					bounds = newBoundsLeftTop(bounds, bds);
				}
			}
		}
		
		return bounds;
	}
	
	private Rect nextBounds(Rect bounds, Rect bds, 
			Direction... acceptDirs) {
		if (bounds == null || bds == null) 
			return bounds;
		
		Direction dir = mDirection;
		if (dir != null) {
			if (acceptDirs != null && acceptDirs.length > 0) {
				boolean found = false;
				
				for (Direction acceptDir : acceptDirs) {
					if (acceptDir == dir) {
						found = true; break;
					}
				}
				
				if (found == false) 
					dir = null;
			}
		}
		
		if (dir == null || dir == Direction.NONE) {
			int num = (int)(System.currentTimeMillis() % 8);
			switch (num) {
			case 0:
				dir = Direction.TOP;
				break;
			case 1:
				dir = Direction.TOPRIGHT;
				break;
			case 2:
				dir = Direction.RIGHT;
				break;
			case 3:
				dir = Direction.RIGHTBOTTOM;
				break;
			case 4:
				dir = Direction.BOTTOM;
				break;
			case 5:
				dir = Direction.LEFTBOTTOM;
				break;
			case 6:
				dir = Direction.LEFT;
				break;
			case 7:
				dir = Direction.LEFTTOP;
				break;
			default:
				dir = Direction.NONE;
				break;
			}
			
			mDirection = dir;
		}
		
		if (dir != null) {
			if (acceptDirs != null && acceptDirs.length > 0) {
				boolean found = false;
				
				for (Direction acceptDir : acceptDirs) {
					if (acceptDir == dir) {
						found = true; break;
					}
				}
				
				if (found == false) 
					return bounds;
			}
		}
		
		int left = bounds.left;
		int right = bounds.right;
		int top = bounds.top;
		int bottom = bounds.bottom;
		
		switch (dir) {
		case TOP:
			bounds = new Rect(left, top-MOVESTEP, right, bottom-MOVESTEP);
			break;
		case TOPRIGHT:
			bounds = new Rect(left+MOVESTEP, top-MOVESTEP, right+MOVESTEP, bottom-MOVESTEP);
			break;
		case RIGHT:
			bounds = new Rect(left+MOVESTEP, top, right+MOVESTEP, bottom);
			break;
		case RIGHTBOTTOM:
			bounds = new Rect(left+MOVESTEP, top+MOVESTEP, right+MOVESTEP, bottom+MOVESTEP);
			break;
		case BOTTOM:
			bounds = new Rect(left, top+MOVESTEP, right, bottom+MOVESTEP);
			break;
		case LEFTBOTTOM:
			bounds = new Rect(left-MOVESTEP, top+MOVESTEP, right-MOVESTEP, bottom+MOVESTEP);
			break;
		case LEFT:
			bounds = new Rect(left-MOVESTEP, top, right-MOVESTEP, bottom);
			break;
		case LEFTTOP:
			bounds = new Rect(left-MOVESTEP, top-MOVESTEP, right-MOVESTEP, bottom-MOVESTEP);
			break;
		default:
			break;
		}
		
		return bounds;
	}
	
	private Rect newBoundsRightTop(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.TOP, Direction.TOPRIGHT, Direction.RIGHT);
	}
	
	private Rect newBoundsRightBottom(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.RIGHT, Direction.RIGHTBOTTOM, Direction.BOTTOM);
	}
	
	private Rect newBoundsLeftTop(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.LEFT, Direction.LEFTTOP, Direction.TOP);
	}
	
	private Rect newBoundsLeftBottom(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.BOTTOM, Direction.LEFTBOTTOM, Direction.LEFT);
	}
	
	private Rect newBoundsRight(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.TOP, Direction.TOPRIGHT, Direction.RIGHT, Direction.RIGHTBOTTOM, Direction.BOTTOM);
	}
	
	private Rect newBoundsLeft(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.BOTTOM, Direction.LEFTBOTTOM, Direction.LEFT, Direction.LEFTTOP, Direction.TOP);
	}
	
	private Rect newBoundsBottom(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.RIGHT, Direction.RIGHTBOTTOM, Direction.BOTTOM, Direction.LEFTBOTTOM, Direction.LEFT);
	}
	
	private Rect newBoundsTop(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds, Direction.LEFT, Direction.LEFTTOP, Direction.TOP, Direction.TOPRIGHT, Direction.RIGHT);
	}
	
	private Rect newBounds(Rect bounds, Rect bds) {
		return nextBounds(bounds, bds);
	}
	
	@Override
	public void invalidateDrawable(Drawable who) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		if ((who == mDrawable || who == mBackground) && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
	}
	
	@Override
    public void draw(Canvas canvas) {
		Drawable background = mBackground; 
		if (background != null) 
			background.draw(canvas); 
		
		if (mDrawable != null) {
			if (mState == State.MOVING) {
				Rect db = mDrawableBounds;
				Rect bounds = getMoveBounds(mDrawable, db);
				
				if (bounds != null && !bounds.equals(db)) {
					mDrawableBounds = bounds;
					mDrawable.setBounds(bounds);
				}
			}
			mDrawable.draw(canvas); 
		}
		
		requestTransform();
	}
	
	@Override
    public void setBounds(int left, int top, int right, int bottom) {
    	super.setBounds(left, top, right, bottom); 
    	
    	if (mDrawable != null) {
    		int width = mDrawable.getIntrinsicWidth();
    		int height = mDrawable.getIntrinsicHeight();
    		
    		int boundWidth = right - left;
    		int boundHeight = bottom - top;
    		
    		if (width > boundWidth && height > boundHeight) {
    			left = (boundWidth - width) / 2;
    			top = (boundHeight - height) / 2;
    			right = left + width;
    			bottom = top + height;
    		}
    		
    		mDrawableBounds = new Rect(left, top, right, bottom);
    		mDrawable.setBounds(left, top, right, bottom); 
    		
    		//if (DEBUG && LOG.isDebugEnabled()) 
    		//	LOG.debug("setBounds: drawableBounds=" + mDrawableBounds);
    	}
    	
    	Drawable d = mBackground; 
		if (d != null) 
			d.setBounds(left, top, right, bottom); 
	}
	
	@Override 
    public void setBounds(Rect bounds) {
    	if (bounds != null) 
    		setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom); 
    }
	
	@Override
    public int getOpacity() {
		if (mDrawable != null) 
			return mDrawable.getOpacity(); 
		
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    	if (mDrawable != null) 
    		mDrawable.setAlpha(alpha); 
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    	if (mDrawable != null) 
    		mDrawable.setColorFilter(cf); 
    }

    @Override
    public int getIntrinsicWidth() {
    	if (mWidth > 0) 
    		return mWidth;
    	
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicWidth();
    	
    	return 0; 
    }

    @Override
    public int getIntrinsicHeight() {
    	if (mHeight > 0) 
    		return mHeight;
    	
    	if (mDrawable != null) 
    		return mDrawable.getIntrinsicHeight();
    	
    	return 0; 
    }

    @Override
    public int getMinimumWidth() {
    	if (mWidth > 0) 
    		return mWidth;
    	
    	if (mDrawable != null) 
    		return mDrawable.getMinimumWidth();
    	
    	return 0; 
    }

    @Override
    public int getMinimumHeight() {
    	if (mHeight > 0) 
    		return mHeight;
    	
    	if (mDrawable != null) 
    		return mDrawable.getMinimumHeight();
    	
    	return 0; 
    }
	
}
