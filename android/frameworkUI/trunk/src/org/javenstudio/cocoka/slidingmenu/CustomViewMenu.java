package org.javenstudio.cocoka.slidingmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.slidingmenu.SlidingMenu.OnMenuVisibilityChangeListener;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public class CustomViewMenu extends ViewGroup implements SlidingMenuViews {
	private static final Logger LOG = Logger.getLogger(CustomViewMenu.class);

	private static final boolean DEBUG = SlidingMenu.DEBUG;
	
	private static final int MARGIN_THRESHOLD = 48; // dips
	private int mTouchMode = SlidingMenu.TOUCHMODE_MARGIN;

	private CustomViewContent mViewContent;

	private View mContent;
	private View mSecondaryContent;
	private int mMarginThreshold;
	private int mWidthOffset;
	private SlidingMenu.CanvasTransformer mTransformer;
	private boolean mChildrenEnabled;
	
	private SlidingMenu.OnMenuVisibilityChangeListener mVisibilityChangeListener;

	public CustomViewMenu(Context context) {
		this(context, null);
	}

	public CustomViewMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		mMarginThreshold = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				MARGIN_THRESHOLD, getResources().getDisplayMetrics());
	}

	public void setCustomViewContent(CustomViewContent customViewContent) {
		mViewContent = customViewContent;
	}

	public void setCanvasTransformer(SlidingMenu.CanvasTransformer t) {
		mTransformer = t;
	}

	public void setWidthOffset(int i) {
		mWidthOffset = i;
		requestLayout();
	}

	public int getWidthOffset() {
		return mWidthOffset;
	}
	
	public int getMenuWidth() {
		return mContent != null ? mContent.getWidth() : 0;
	}
	
	public int getMenuHeight() {
		return mContent != null ? mContent.getHeight() : 0;
	}

	public int getMenuVisibility() { 
		return mContent != null ? mContent.getVisibility() : View.GONE;
	}
	
	public int getSecondaryMenuWidth() {
		return mSecondaryContent != null ? mSecondaryContent.getWidth() : 0;
	}
	
	public int getSecondaryMenuHeight() {
		return mSecondaryContent != null ? mSecondaryContent.getHeight() : 0;
	}
	
	public int getSecondaryMenuVisibility() { 
		return mSecondaryContent != null ? mSecondaryContent.getVisibility() : View.GONE;
	}
	
	public int getContentWidth() {
		return mViewContent != null ? mViewContent.getContentWidth() : 0;
	}
	
	public int getContentHeight() {
		return mViewContent != null ? mViewContent.getContentHeight() : 0;
	}
	
	public int getContentVisibility() {
		return mViewContent != null ? mViewContent.getContentVisibility() : View.GONE;
	}
	
	public void setContent(View v, LayoutParams layoutParams) {
		if (mContent != null)
			removeView(mContent);
		mContent = v;
		addView(mContent, layoutParams);
	}

	public View getContent() {
		return mContent;
	}

	/**
	 * Sets the secondary (right) menu for use when setMode is called with SlidingMenu.LEFT_RIGHT.
	 * @param v the right menu
	 */
	public void setSecondaryContent(View v, LayoutParams layoutParams) {
		if (mSecondaryContent != null)
			removeView(mSecondaryContent);
		mSecondaryContent = v;
		addView(mSecondaryContent, layoutParams);
	}

	public View getSecondaryContent() {
		return mSecondaryContent;
	}

	public void setChildrenEnabled(boolean enabled) {
		mChildrenEnabled = enabled;
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		if (mTransformer != null) invalidate();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return !mChildrenEnabled;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return !mChildrenEnabled;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (mTransformer != null) {
			canvas.save();
			int menuVisibility = getMenuVisibility();
			int secondaryVisibilty = getSecondaryMenuVisibility();
			if (menuVisibility == View.VISIBLE) {
				mTransformer.transformCanvas(this, canvas, 
						SlidingMenu.VIEW_MENU, mViewContent.getPercentOpen());
			} else if (secondaryVisibilty == View.VISIBLE) {
				mTransformer.transformCanvas(this, canvas, 
						SlidingMenu.VIEW_SECONDARYMENU, mViewContent.getPercentOpen());
			}
			super.dispatchDraw(canvas);
			canvas.restore();
		} else
			super.dispatchDraw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int width = r - l;
		final int height = b - t;
		mContent.layout(0, 0, width-mWidthOffset, height);
		if (mSecondaryContent != null)
			mSecondaryContent.layout(0, 0, width-mWidthOffset, height);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(0, widthMeasureSpec);
		int height = getDefaultSize(0, heightMeasureSpec);
		setMeasuredDimension(width, height);
		final int contentWidth = getChildMeasureSpec(widthMeasureSpec, 0, width-mWidthOffset);
		final int contentHeight = getChildMeasureSpec(heightMeasureSpec, 0, height);
		mContent.measure(contentWidth, contentHeight);
		if (mSecondaryContent != null)
			mSecondaryContent.measure(contentWidth, contentHeight);
	}

	private int mMode;
	private boolean mFadeEnabled;
	private final Paint mFadePaint = new Paint();
	private float mScrollScale;
	private Drawable mShadowDrawable;
	private Drawable mSecondaryShadowDrawable;
	private int mShadowWidth;
	private float mFadeDegree;

	public void setMode(int mode) {
		if (mode == SlidingMenu.LEFT || mode == SlidingMenu.RIGHT) {
			if (mContent != null)
				mContent.setVisibility(View.VISIBLE);
			if (mSecondaryContent != null)
				mSecondaryContent.setVisibility(View.INVISIBLE);
		}
		mMode = mode;
	}

	public int getMode() {
		return mMode;
	}

	public void setScrollScale(float scrollScale) {
		mScrollScale = scrollScale;
	}

	public float getScrollScale() {
		return mScrollScale;
	}

	public void setShadowDrawable(Drawable shadow) {
		mShadowDrawable = shadow;
		invalidate();
	}

	public void setSecondaryShadowDrawable(Drawable shadow) {
		mSecondaryShadowDrawable = shadow;
		invalidate();
	}

	public void setShadowWidth(int width) {
		mShadowWidth = width;
		invalidate();
	}

	public void setFadeEnabled(boolean b) {
		mFadeEnabled = b;
	}

	public void setFadeDegree(float degree) {
		if (degree > 1.0f || degree < 0.0f)
			throw new IllegalStateException("The BehindFadeDegree must be between 0.0f and 1.0f");
		mFadeDegree = degree;
	}

	public int getMenuPage(int page) {
		page = (page > 1) ? 2 : ((page < 1) ? 0 : page);
		if (mMode == SlidingMenu.LEFT && page > 1) {
			return 0;
		} else if (mMode == SlidingMenu.RIGHT && page < 1) {
			return 2;
		} else {
			return page;
		}
	}

	public int getVisiblePage() {
		if (mMode == SlidingMenu.LEFT) {
			if (mContent.getVisibility() == View.VISIBLE)
				return 1;
			else
				return 0;
		} else if (mMode == SlidingMenu.RIGHT) {
			if (mSecondaryContent.getVisibility() == View.VISIBLE)
				return 2;
			else
				return 0;
		} else {
			if (mContent.getVisibility() == View.VISIBLE)
				return 1;
			else if (mSecondaryContent.getVisibility() == View.VISIBLE)
				return 2;
			else
				return 0;
		}
	}
	
	public void scrollMenuTo(View content, int x, int y) {
		int oldvis = getVisibility();
		int oldmenuvis = mContent.getVisibility();
		int oldsecondaryvis = mSecondaryContent.getVisibility();
		
		int vis = View.VISIBLE;
		if (mMode == SlidingMenu.LEFT) {
			if (x >= content.getLeft()) vis = View.INVISIBLE;
			scrollTo((int)((x + getMenuWidth())*mScrollScale), y);
			
		} else if (mMode == SlidingMenu.RIGHT) {
			if (x <= content.getLeft()) vis = View.INVISIBLE;
			scrollTo((int)(getMenuWidth() - getWidth() + 
					(x-getMenuWidth())*mScrollScale), y);
			
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			mContent.setVisibility(x >= content.getLeft() ? View.INVISIBLE : View.VISIBLE);
			mSecondaryContent.setVisibility(x <= content.getLeft() ? View.INVISIBLE : View.VISIBLE);
			
			vis = x == 0 ? View.INVISIBLE : View.VISIBLE;
			if (x <= content.getLeft()) {
				scrollTo((int)((x + getMenuWidth())*mScrollScale), y);				
			} else {
				scrollTo((int)(getMenuWidth() - getWidth() + 
						(x-getMenuWidth())*mScrollScale), y);				
			}
		}
		
		if (DEBUG && LOG.isDebugEnabled() && vis == View.INVISIBLE)
			LOG.debug("behind INVISIBLE");
		
		setVisibility(vis);
		
		SlidingMenu.OnMenuVisibilityChangeListener listener = mVisibilityChangeListener;
		if (listener != null) {
			int newvis = getVisibility();
			int newmenuvis = mContent.getVisibility();
			int newsecondaryvis = mSecondaryContent.getVisibility();
			
			if (mMode == SlidingMenu.LEFT) {
				if (newvis != oldvis) 
					listener.onMenuVisibilityChanged(newvis);
				
			} else if (mMode == SlidingMenu.RIGHT) {
				if (newvis != oldvis) 
					listener.onSecondaryMenuVisibilityChanged(newvis);
				
			} else if (mMode == SlidingMenu.LEFT_RIGHT) {
				if (newmenuvis != oldmenuvis) 
					listener.onMenuVisibilityChanged(newmenuvis);
				if (newsecondaryvis != oldsecondaryvis) 
					listener.onSecondaryMenuVisibilityChanged(newsecondaryvis);
			}
		}
	}

	public void setOnMenuVisibilityChangeListener(OnMenuVisibilityChangeListener listener) {
		mVisibilityChangeListener = listener;
	}
	
	public OnMenuVisibilityChangeListener getOnMenuVisibilityChangeListener() {
		return mVisibilityChangeListener;
	}
	
	public int getMenuLeft(View content, int page) {
		if (mMode == SlidingMenu.LEFT) {
			switch (page) {
			case 0:
				return content.getLeft() - getMenuWidth();
			case 2:
				return content.getLeft();
			}
		} else if (mMode == SlidingMenu.RIGHT) {
			switch (page) {
			case 0:
				return content.getLeft();
			case 2:
				return content.getLeft() + getMenuWidth();	
			}
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			switch (page) {
			case 0:
				return content.getLeft() - getMenuWidth();
			case 2:
				return content.getLeft() + getMenuWidth();
			}
		}
		return content.getLeft();
	}

	public int getAbsLeftBound(View content) {
		if (mMode == SlidingMenu.LEFT || mMode == SlidingMenu.LEFT_RIGHT) {
			return content.getLeft() - getMenuWidth();
		} else if (mMode == SlidingMenu.RIGHT) {
			return content.getLeft();
		}
		return 0;
	}

	public int getAbsRightBound(View content) {
		if (mMode == SlidingMenu.LEFT) {
			return content.getLeft();
		} else if (mMode == SlidingMenu.RIGHT || mMode == SlidingMenu.LEFT_RIGHT) {
			return content.getLeft() + getMenuWidth();
		}
		return 0;
	}

	public boolean marginTouchAllowed(View content, int x) {
		int left = content.getLeft();
		int right = content.getRight();
		if (mMode == SlidingMenu.LEFT) {
			return (x >= left && x <= mMarginThreshold + left);
		} else if (mMode == SlidingMenu.RIGHT) {
			return (x <= right && x >= right - mMarginThreshold);
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return (x >= left && x <= mMarginThreshold + left) || 
					(x <= right && x >= right - mMarginThreshold);
		}
		return false;
	}

	public void setTouchMode(int i) {
		mTouchMode = i;
	}

	public boolean menuOpenTouchAllowed(View content, int currPage, float x) {
		switch (mTouchMode) {
		case SlidingMenu.TOUCHMODE_FULLSCREEN:
			return true;
		case SlidingMenu.TOUCHMODE_MARGIN:
			return menuTouchInQuickReturn(content, currPage, x);
		}
		return false;
	}

	public boolean menuTouchInQuickReturn(View content, int currPage, float x) {
		if (mMode == SlidingMenu.LEFT || (mMode == SlidingMenu.LEFT_RIGHT && currPage == 0)) {
			return x >= content.getLeft();
		} else if (mMode == SlidingMenu.RIGHT || (mMode == SlidingMenu.LEFT_RIGHT && currPage == 2)) {
			return x <= content.getRight();
		}
		return false;
	}

	public boolean menuClosedSlideAllowed(float ix, float dx, int screenWidth) {
		if (mMode == SlidingMenu.LEFT) {
			return dx > 0 && ix < 10;
		} else if (mMode == SlidingMenu.RIGHT) {
			return dx < 0 && ix > (screenWidth - 10);
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return (dx > 0 && ix < 10) || (dx < 0 && ix > (screenWidth - 10));
		}
		return false;
	}

	public boolean menuOpenSlideAllowed(float ix, float dx, int screenWidth) {
		if (mMode == SlidingMenu.LEFT) {
			return dx < 0;
		} else if (mMode == SlidingMenu.RIGHT) {
			return dx > 0;
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return true;
		}
		return false;
	}

	public void drawShadow(View content, Canvas canvas, float openPercent) {
		if (mShadowDrawable == null || mShadowWidth <= 0) return;
		if (openPercent <= 0) return;
		
		View view = content;
		int left = 0, height = getHeight();
		
		if (mMode == SlidingMenu.RIGHT) {
			left = view.getRight() - mShadowWidth;
			if (mSecondaryShadowDrawable != null) {
				mSecondaryShadowDrawable.setBounds(left, 0, left + mShadowWidth, height);
				mSecondaryShadowDrawable.draw(canvas);
			}
		} else if (mMode == SlidingMenu.LEFT) {
			left = view.getLeft();
			if (mShadowDrawable != null) {
				mShadowDrawable.setBounds(left, 0, left + mShadowWidth, height);
				mShadowDrawable.draw(canvas);
			}
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			int menuVisibility = getMenuVisibility();
			int secondaryVisibility = getSecondaryMenuVisibility();
			
			if (menuVisibility == View.VISIBLE) {
				left = view.getLeft();
				if (mShadowDrawable != null) {
					mShadowDrawable.setBounds(left, 0, left + mShadowWidth, height);
					mShadowDrawable.draw(canvas);
				}
			} else if (secondaryVisibility == View.VISIBLE) {
				left = view.getRight() - mShadowWidth;
				if (mSecondaryShadowDrawable != null) {
					mSecondaryShadowDrawable.setBounds(left, 0, left + mShadowWidth, height);
					mSecondaryShadowDrawable.draw(canvas);
				}
			}
		}
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("drawShadow: left=" + left + " width=" + mShadowWidth 
					+ " mode=" + mMode + " openPercent=" + openPercent);
		}
	}

	public void drawFade(View content, Canvas canvas, float openPercent) {
		if (!mFadeEnabled) return;
		if (openPercent <= 0) return;
		
		final float alpha = (float) (mFadeDegree * 255.0f * Math.abs(openPercent));
		mFadePaint.setColor(Color.argb((int)alpha, 0, 0, 0));
		
		int left = 0, right = 0, height = getHeight();
		if (mMode == SlidingMenu.LEFT) {
			left = content.getLeft();
			right = content.getRight();
			canvas.drawRect(left, 0, right, height, mFadePaint);
			
		} else if (mMode == SlidingMenu.RIGHT) {
			left = content.getLeft();
			right = content.getRight();
			canvas.drawRect(left, 0, right, height, mFadePaint);
			
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			int menuVisibility = getMenuVisibility();
			int secondaryVisibility = getSecondaryMenuVisibility();
			
			if (menuVisibility == View.VISIBLE) {
				left = content.getLeft();
				right = content.getRight();
				canvas.drawRect(left, 0, right, height, mFadePaint);
				
			} else if (secondaryVisibility == View.VISIBLE) {
				left = content.getLeft();
				right = content.getRight();
				canvas.drawRect(left, 0, right, height, mFadePaint);
			}			
		}
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("drawFade: left=" + left + " right=" + right + " alpha=" + alpha 
					+ " mode=" + mMode + " openPercent=" + openPercent);
		}
	}
	
	private boolean mSelectorEnabled = true;
	private BitmapRef mSelectorDrawable;
	private View mSelectedView;
	
	public void drawSelector(View content, Canvas canvas, float openPercent) {
		if (!mSelectorEnabled) return;
		if (mSelectorDrawable != null && mSelectedView != null) {
			String tag = (String) mSelectedView.getTag(R.id.selected_view);
			if (tag.equals("CustomViewMenuSelectedView")) {
				canvas.save();
				int left, right, offset;
				offset = (int) (mSelectorDrawable.getWidth() * openPercent);
				if (mMode == SlidingMenu.LEFT) {
					right = content.getLeft();
					left = right - offset;
					canvas.clipRect(left, 0, right, getHeight());
					canvas.drawBitmap(mSelectorDrawable.get(), left, getSelectorTop(), null);		
				} else if (mMode == SlidingMenu.RIGHT) {
					left = content.getRight();
					right = left + offset;
					canvas.clipRect(left, 0, right, getHeight());
					canvas.drawBitmap(mSelectorDrawable.get(), right - mSelectorDrawable.getWidth(), getSelectorTop(), null);
				}
				canvas.restore();
			}
		}
	}
	
	public void setSelectorEnabled(boolean b) {
		mSelectorEnabled = b;
	}

	public void setSelectedView(View v) {
		if (mSelectedView != null) {
			mSelectedView.setTag(R.id.selected_view, null);
			mSelectedView = null;
		}
		if (v != null && v.getParent() != null) {
			mSelectedView = v;
			mSelectedView.setTag(R.id.selected_view, "CustomViewMenuSelectedView");
			invalidate();
		}
	}

	private int getSelectorTop() {
		int y = mSelectedView.getTop();
		y += (mSelectedView.getHeight() - mSelectorDrawable.getHeight()) / 2;
		return y;
	}

	public void setSelectorBitmap(BitmapRef b) {
		mSelectorDrawable = b;
		refreshDrawableState();
	}

}
