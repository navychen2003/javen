package org.javenstudio.cocoka.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;

import org.javenstudio.common.util.Logger;

public class RefreshGridView extends PullToRefreshGridView implements IRefreshView {
	private static final Logger LOG = Logger.getLogger(RefreshGridView.class);
	
	public static abstract class OnRefreshListener 
			implements PullToRefreshBase.OnRefreshListener<GridView> { 
		@Override
		public final void onRefresh(PullToRefreshBase<GridView> refreshView, boolean... params) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("onRefresh: refreshView=" + refreshView + " doScroll=" + params);
			onRefresh((RefreshGridView)refreshView, params);
		}
		
		@Override
		public final void onDispatchTouchEvent(PullToRefreshBase<GridView> refreshView, MotionEvent event) {
			onTouchEvent((RefreshGridView)refreshView, event);
		}
		
		public abstract void onRefresh(RefreshGridView refreshView, boolean... params);
		public abstract void onTouchEvent(RefreshGridView refreshView, MotionEvent event);
	}
	
	public static abstract class OnPullEventListener 
			implements PullToRefreshBase.OnPullEventListener<GridView> {
		@Override
		public void onPullEvent(PullToRefreshBase<GridView> refreshView, 
				PullToRefreshBase.State state, PullToRefreshBase.Mode direction) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("onPullEvent: refreshView=" + refreshView + " state=" + state 
						+ " direction=" + direction);
			}
			
			if (state == PullToRefreshBase.State.PULL_TO_REFRESH)
				onPullToRefresh((RefreshGridView)refreshView);
			else if (state == PullToRefreshBase.State.RELEASE_TO_REFRESH)
				onReleaseToRefresh((RefreshGridView)refreshView);
			else if (state == PullToRefreshBase.State.RESET)
				onPullReset((RefreshGridView)refreshView);
		}
		
		public void onPullToRefresh(RefreshGridView refreshView) {}
		public void onReleaseToRefresh(RefreshGridView refreshView) {}
		public void onPullReset(RefreshGridView refreshView) {}
	}
	
	public RefreshGridView(Context context) {
		super(context);
	}

	public RefreshGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RefreshGridView(Context context, Mode mode) {
		super(context, mode);
	}

	public RefreshGridView(Context context, Mode mode, AnimationStyle style) {
		super(context, mode, style);
	}
	
	@SuppressWarnings("deprecation")
	public void setLastUpdatedLabel(CharSequence label) {
		if (LOG.isDebugEnabled()) LOG.debug("setLastUpdatedLabel: label=" + label);
		super.setLastUpdatedLabel(label);
	}
	
	@SuppressWarnings("deprecation")
	public void setPullToRefreshEnabled(boolean enable) {
		if (LOG.isDebugEnabled()) LOG.debug("setPullToRefreshEnabled: enable=" + enable);
		super.setPullToRefreshEnabled(enable);
	}
	
	//public void addHeaderView(View v) {
	//	getRefreshableView().addHeaderView(v);
	//}
	
	//public void addHeaderView(View v, Object data, boolean isSelectable) {
	//	getRefreshableView().addHeaderView(v, data, isSelectable);
	//}
	
	//public int getHeaderViewsCount() {
	//	return getRefreshableView().getHeaderViewsCount();
	//}
	
	//public boolean removeHeaderView(View v) {
	//	return getRefreshableView().removeHeaderView(v);
	//}
	
	public void disableOverscrollGlowEdge() {
		RefreshListView.disableOverscrollGlowEdge(getRefreshableView());
	}
	
}
