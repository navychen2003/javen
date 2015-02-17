package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class SimpleWrapLayout extends ViewGroup {

	public static class LayoutParams extends ViewGroup.LayoutParams {
		public final int mHorizontalSpacing;
		public final int mVerticalSpacing;

		public LayoutParams(int horizontalSpacing, int verticalSpacing) {
			super(0, 0);
			mHorizontalSpacing = horizontalSpacing;
			mVerticalSpacing = verticalSpacing;
		}
	}

	private int mLineHeight = 0;
	
	public SimpleWrapLayout(Context context) {
		super(context);
	}

	public SimpleWrapLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		assert (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED);
		
		final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		final int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom(); 
		
		final int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST); 
		
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec); 
		final int heightSpec = heightMode == MeasureSpec.AT_MOST ? 
				MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST) : 
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED); 
		
		int layoutWidth = width; 
		int layoutHeight = height; 
		int xpos = getPaddingLeft();
		int ypos = getPaddingTop();
		int lineHeight = 0;
		
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams)child.getLayoutParams();
				final int childWidthMeasureSpec = widthSpec; 
				final int childHeightMeasureSpec = heightSpec; 
				
				child.measure(childWidthMeasureSpec, childHeightMeasureSpec); 
				
				final int childw = child.getMeasuredWidth();
				lineHeight = Math.max(lineHeight, child.getMeasuredHeight() + lp.mVerticalSpacing);
				if (xpos + childw > width) {
					xpos = getPaddingLeft();
					ypos += lineHeight;
				}
				
				xpos += childw + lp.mHorizontalSpacing;
			}
		}
		
		mLineHeight = lineHeight;
		if (heightMode == MeasureSpec.UNSPECIFIED) {
			layoutHeight = ypos + lineHeight;
		} else if (heightMode == MeasureSpec.AT_MOST) {
			if (ypos + lineHeight < height) {
				layoutHeight = ypos + lineHeight;
			}
		}
		
		setMeasuredDimension(layoutWidth, layoutHeight);
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(1, 1);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
		return lp != null && lp instanceof LayoutParams; 
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int count = getChildCount();
		final int width = right - left;
		
		int xpos = getPaddingLeft();
		int ypos = getPaddingTop();
		
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final int childw = child.getMeasuredWidth();
				final int childh = child.getMeasuredHeight();
				
				final LayoutParams lp = (LayoutParams)child.getLayoutParams();
				
				if (xpos + childw > width) {
					xpos = getPaddingLeft();
					ypos += mLineHeight;
				}
				
				child.layout(xpos, ypos, xpos + childw, ypos + childh);
				
				xpos += childw + lp.mHorizontalSpacing;
			}
		}
	}
	
}
