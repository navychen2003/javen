package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.Shape;
import android.graphics.Rect;

@SuppressWarnings({"unused"})
public class Highlights {
	public static final int TYPE_SIMPLE = 1;
	public static final int TYPE_STROKE = 2;

	public static class DrawableSetting {
		public int mColorAlpha = 255; 
		public int mStrokeAlpha = 255; 
		public int mStrokeWidth = 2; 
		public Integer mColor = null; 
		public Integer mStrokeColor = null; 
		public Drawable mDrawable = null; 
		public Shape mShape = null; 
		public Shape mStrokeShape = null; 
	}
	
	private Context mContext; 
	private DrawableSetting mNormal = null; 
	private DrawableSetting mFocused = null; 
	private DrawableSetting mChecked = null; 
	private DrawableSetting mPressed = null; 
	
	public Highlights(Context context) {
		mContext = context; 
	}
	
	public void setChecked(DrawableSetting setting) {
		mChecked = setting; 
	}
	
	public void setPressed(DrawableSetting setting) {
		mPressed = setting; 
	}
	
	public void setFocused(DrawableSetting setting) {
		mFocused = setting; 
	}
	
	public void setNormal(DrawableSetting setting) {
		mNormal = setting; 
	}
	
	private void addStateDrawable(HighlightsDrawable drawable, int width, int height, int[] state, DrawableSetting setting) {
		if (drawable == null || state == null || setting == null) 
			return; 
		
		Drawable d = setting.mDrawable; 
		Integer color = setting.mColor; 
		Integer strokeColor = setting.mStrokeColor; 
		
		if (d == null && color != null) { 
			if (strokeColor != null) 
				d = createStrokeDrawable(setting, width, height); 
			else
				d = createSimpleDrawable(setting, width, height); 
		}
		
		if (d != null) 
			drawable.addState(state, d); 
	}
	
	public Drawable getDrawable(int width, int height) {
		HighlightsDrawable drawable = new HighlightsDrawable();
		
		int[] stateEmpty = new int[]{}; 
		int[] stateEnabled = new int[]{ android.R.attr.state_enabled }; 
		int[] stateFocused = new int[]{ android.R.attr.state_focused }; 
		int[] statePressed = new int[]{ android.R.attr.state_pressed };
		int[] stateChecked = new int[]{ android.R.attr.state_checked };
		int[] stateWindowFocused = new int[]{ android.R.attr.state_window_focused };
		
		addStateDrawable(drawable, width, height, stateChecked, mChecked); 
		addStateDrawable(drawable, width, height, statePressed, mPressed); 
		addStateDrawable(drawable, width, height, stateFocused, mFocused); 
		addStateDrawable(drawable, width, height, stateEmpty, mNormal); 
		
		return drawable; 
	}
	
	public static class HighlightsDrawable extends StateListDrawable {
		public HighlightsDrawable() {}
		
		@Override
		public void invalidateSelf() {
			super.invalidateSelf(); 
		}
	}
	
	public static class MultiShapDrawable extends ShapeDrawable {
		private Drawable draw2 = null; 
		
		public MultiShapDrawable(Drawable draw2) {
			initDraw2(draw2); 
		}
		
		private void initDraw2(Drawable draw2) {
			this.draw2 = draw2; 
			if (draw2 == null) 
				throw new java.lang.IllegalArgumentException("draw2 is null"); 
		}
		
		public void draw(Canvas canvas) {
			super.draw(canvas);
			if (draw2 != null) draw2.draw(canvas); 
		}
		
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom); 
			if (draw2 != null) draw2.setBounds(left, top, right, bottom); 
		}
		
		public void setBounds(Rect bounds) {
			super.setBounds(bounds); 
			if (draw2 != null) draw2.setBounds(bounds);
		}
	}
	
	private Drawable createStrokeDrawable(DrawableSetting setting, int width, int height) {
		if (setting == null) return null; 
		
		Shape shape = setting.mShape; 
		Shape strokeShape = setting.mStrokeShape; 
		if (strokeShape == null) 
			strokeShape = shape; 
		
		int color = setting.mColor != null ? setting.mColor.intValue() : 0; 
		int alpha = setting.mColorAlpha; 
		
		int strokeColor = setting.mStrokeColor != null ? setting.mStrokeColor.intValue() : 0; 
		int strokeAlpha = setting.mStrokeAlpha; 
		int strokeWidth = setting.mStrokeWidth; 
		
		ShapeDrawable draw2 = new ShapeDrawable(); 
		if (strokeShape != null) 
			draw2.setShape(strokeShape); 
		
		draw2.getPaint().setColor(strokeColor); 
		draw2.getPaint().setStyle(Paint.Style.STROKE); 
		draw2.getPaint().setStrokeWidth(strokeWidth); 
		draw2.getPaint().setAlpha(strokeAlpha); 
		
		draw2.setIntrinsicWidth(width); 
		draw2.setIntrinsicHeight(height); 
		
		MultiShapDrawable draw = new MultiShapDrawable(draw2); 
		if (shape != null) 
			draw.setShape(shape); 
		
		draw.getPaint().setColor(color); 
		draw.getPaint().setStyle(Paint.Style.FILL); 
		draw.getPaint().setAlpha(alpha); 

		draw.setIntrinsicWidth(width); 
		draw.setIntrinsicHeight(height); 
		
		return draw; 
	}
	
	private Drawable createSimpleDrawable(DrawableSetting setting, int width, int height) {
		if (setting == null) return null; 
		
		Shape shape = setting.mShape; 
		int color = setting.mColor != null ? setting.mColor.intValue() : 0; 
		int alpha = setting.mColorAlpha; 
		
		ShapeDrawable draw = new ShapeDrawable(); 
		if (shape != null)
			draw.setShape(shape); 
		
		draw.getPaint().setColor(color); 
		draw.getPaint().setStyle(Paint.Style.FILL); 
		draw.getPaint().setAlpha(alpha); 
		
		draw.setIntrinsicWidth(width); 
		draw.setIntrinsicHeight(height); 
		
		return draw; 
	}
	
	private Drawable createColorDrawable(int color) {
		return new ColorDrawable(color); 
	}
	
}