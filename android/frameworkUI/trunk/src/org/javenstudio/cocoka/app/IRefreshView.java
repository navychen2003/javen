package org.javenstudio.cocoka.app;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public interface IRefreshView {
	
	public static interface RefreshListener {
		public void onPullToRefresh(IRefreshView refreshView);
		public void onReleaseToRefresh(IRefreshView refreshView);
		public void onPullReset(IRefreshView refreshView);
		public void onTouchEvent(IRefreshView refreshView, MotionEvent event);
	}
	
	public void setTextColor(int color);
	public void setTextColor(ColorStateList color);
	
	public void setSubTextColor(int color);
	public void setSubTextColor(ColorStateList color);
	
	public void setPullLabel(CharSequence pullLabel);
	public void setRefreshingLabel(CharSequence refreshingLabel);
	public void setReleaseLabel(CharSequence releaseLabel);
	public void setLastUpdatedLabel(CharSequence label);
	
	public void setLoadingDrawable(Drawable drawable);
	
	public void onRefreshComplete();
	public void setPullToRefreshEnabled(boolean enable);
	public void setRefreshing();
	public void setRefreshing(boolean... params);
	
	public boolean isPullToRefreshEnabled();
	public boolean isPullToRefreshOverScrollEnabled();
	public boolean isRefreshing();
	
}
