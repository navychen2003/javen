package org.javenstudio.cocoka.slidingmenu;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public interface MenuInterface {

	public abstract void scrollBehindTo(int x, int y, 
			CustomViewMenu cvb, float scrollScale);
	
	public abstract int getMenuLeft(CustomViewMenu cvb, View content);
	
	public abstract int getAbsLeftBound(CustomViewMenu cvb, View content);

	public abstract int getAbsRightBound(CustomViewMenu cvb, View content);

	public abstract boolean marginTouchAllowed(View content, int x, int threshold);
	
	public abstract boolean menuOpenTouchAllowed(View content, int currPage, int x);
	
	public abstract boolean menuTouchInQuickReturn(View content, int currPage, int x);
	
	public abstract boolean menuClosedSlideAllowed(int x);
	
	public abstract boolean menuOpenSlideAllowed(int x);
	
	public abstract void drawShadow(Canvas canvas, Drawable shadow, int width);
	
	public abstract void drawFade(Canvas canvas, int alpha, 
			CustomViewMenu cvb, View content);
	
	public abstract void drawSelector(View content, Canvas canvas, float percentOpen);
	
}
