package org.javenstudio.cocoka.app;

import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.javenstudio.common.util.Logger;

public class RefreshListView extends PullToRefreshListView implements IRefreshView {
	private static final Logger LOG = Logger.getLogger(RefreshListView.class);
	
	public static abstract class OnRefreshListener 
			implements PullToRefreshBase.OnRefreshListener<ListView> { 
		@Override
		public final void onRefresh(PullToRefreshBase<ListView> refreshView, boolean... params) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("onRefresh: refreshView=" + refreshView + " doScroll=" + params);
			onRefresh((RefreshListView)refreshView, params);
		}

		@Override
		public final void onDispatchTouchEvent(PullToRefreshBase<ListView> refreshView, MotionEvent event) {
			onTouchEvent((RefreshListView)refreshView, event);
		}
		
		public abstract void onRefresh(RefreshListView refreshView, boolean... params);
		public abstract void onTouchEvent(RefreshListView refreshView, MotionEvent event);
	}
	
	public static abstract class OnPullEventListener 
			implements PullToRefreshBase.OnPullEventListener<ListView> {
		@Override
		public void onPullEvent(PullToRefreshBase<ListView> refreshView, 
				PullToRefreshBase.State state, PullToRefreshBase.Mode direction) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("onPullEvent: refreshView=" + refreshView + " state=" + state 
						+ " direction=" + direction);
			}
			
			if (state == PullToRefreshBase.State.PULL_TO_REFRESH)
				onPullToRefresh((RefreshListView)refreshView);
			else if (state == PullToRefreshBase.State.RELEASE_TO_REFRESH)
				onReleaseToRefresh((RefreshListView)refreshView);
			else if (state == PullToRefreshBase.State.RESET)
				onPullReset((RefreshListView)refreshView);
		}
		
		public void onPullToRefresh(RefreshListView refreshView) {}
		public void onReleaseToRefresh(RefreshListView refreshView) {}
		public void onPullReset(RefreshListView refreshView) {}
	}
	
	public RefreshListView(Context context) {
		super(context);
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RefreshListView(Context context, Mode mode) {
		super(context, mode);
	}

	public RefreshListView(Context context, Mode mode, AnimationStyle style) {
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
	
	public void addHeaderView(View v) {
		getRefreshableView().addHeaderView(v);
	}
	
	public void addHeaderView(View v, Object data, boolean isSelectable) {
		getRefreshableView().addHeaderView(v, data, isSelectable);
	}
	
	public int getHeaderViewsCount() {
		return getRefreshableView().getHeaderViewsCount();
	}
	
	public boolean removeHeaderView(View v) {
		return getRefreshableView().removeHeaderView(v);
	}
	
	public void setOverscrollHeader(Drawable header) {
		getRefreshableView().setOverscrollHeader(header);
	}
	
	public void setOverscrollFooter(Drawable footer) {
		getRefreshableView().setOverscrollFooter(footer);
	}
	
	public void disableOverscrollGlowEdge() {
		RefreshListView.disableOverscrollGlowEdge(getRefreshableView());
	}
	
	public static void disableOverscrollGlowEdge(AbsListView listview) {
		if (listview == null) return;
		try {
			Class<?> c = (Class<?>) Class.forName(AbsListView.class.getName());
			Field egtField = c.getDeclaredField("mEdgeGlowTop");
			Field egbBottom = c.getDeclaredField("mEdgeGlowBottom");
			egtField.setAccessible(true);
			egbBottom.setAccessible(true);
			Object egtObject = egtField.get(listview); 
			Object egbObject = egbBottom.get(listview);
			
			Class<?> cc = (Class<?>) Class.forName(egtObject.getClass().getName());
			Field mGlow = cc.getDeclaredField("mGlow");
			mGlow.setAccessible(true);
			mGlow.set(egtObject,new ColorDrawable(Color.TRANSPARENT));
			mGlow.set(egbObject,new ColorDrawable(Color.TRANSPARENT));
			
			Field mEdge = cc.getDeclaredField("mEdge");
			mEdge.setAccessible(true);
			mEdge.set(egtObject,new ColorDrawable(Color.TRANSPARENT));
			mEdge.set(egbObject,new ColorDrawable(Color.TRANSPARENT));
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("disableOverscrollGlowEdge: error: " + e, e);
		}
	}
	
}
