package org.javenstudio.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.cocoka.app.RefreshGridView;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.common.util.Logger;

public abstract class ProviderBinderBase extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {
	private static final Logger LOG = Logger.getLogger(ProviderBinderBase.class);

	public abstract Provider getProvider();
	
	protected abstract View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container);
	protected abstract View findListView(IActivity activity, View rootView);
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
	}
	
	@Override
	public final View bindView(final IActivity activity, LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) { 
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		View rootView = inflateView(activity, inflater, container);
		View view = findListView(activity, rootView);
		bindListView(activity, view, createListAdapter(activity));
		
		return rootView;
	}
	
	public void onPullToRefresh(IRefreshView refreshView) {}
	public void onReleaseToRefresh(IRefreshView refreshView) {}
	public void onPullReset(IRefreshView refreshView) {}
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {}
	
	public int getHomeAsUpIndicatorMenuRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.icon_homeasup_menu_indicator);
	}
	
	public int getHomeAsUpIndicatorBackRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.icon_homeasup_back_indicator);
	}
	
	public void bindListView(final IActivity activity, View view, final ListAdapter adapter) { 
		if (activity == null || view == null || adapter == null) 
			return;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindListView: binder=" + this + " view=" + view + " adapter=" + adapter);
		
		AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
	    		private int mFirstVisibleItem = -1;
	    		private ListAdapter mAdapter = adapter;
	    		
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					TouchHelper.onScrollStateChanged(activity, view, scrollState);
				}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem != mFirstVisibleItem) { 
						mFirstVisibleItem = firstVisibleItem;
						onFirstVisibleChanged(activity, mAdapter, view, 
								firstVisibleItem, visibleItemCount, totalItemCount);
					}
				}
	        };
		
		if (view instanceof RefreshListView) { 
			RefreshListView listView = (RefreshListView)view;
			activity.getActivityHelper().setRefreshView(listView);
			
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
			listView.setOnScrollListener(scrollListener);
			listView.disableOverscrollGlowEdge();
			
			listView.setOnRefreshListener(activity.getActivityHelper()
					.createListRefreshListener(activity, this));
			listView.setOnPullEventListener(activity.getActivityHelper()
					.createListPullListener(activity, this));
			
			int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
			
		} else if (view instanceof ListView) { 
			ListView listView = (ListView)view;
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
	        RefreshListView.disableOverscrollGlowEdge(listView);
	        
	        int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
	        
		} else if (view instanceof RefreshGridView) { 
			RefreshGridView listView = (RefreshGridView)view;
			activity.getActivityHelper().setRefreshView(listView);
			
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
			listView.setOnScrollListener(scrollListener);
			listView.disableOverscrollGlowEdge();
			
			listView.setOnRefreshListener(activity.getActivityHelper()
					.createGridRefreshListener(activity, this));
			listView.setOnPullEventListener(activity.getActivityHelper()
					.createGridPullListener(activity, this));
			
			int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
			
		} else if (view instanceof GridView) { 
			GridView listView = (GridView)view;
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
	        RefreshListView.disableOverscrollGlowEdge(listView);
	        
	        int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
		}
	}
	
	public static ListAdapter getListViewAdapter(View view) {
		if (view == null) return null;
		
		if (view instanceof RefreshListView) { 
			RefreshListView listView = (RefreshListView)view;
			ListAdapter adapter = listView.getRefreshableView().getAdapter();
			if (adapter != null && adapter instanceof HeaderViewListAdapter)
				adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
			
			return adapter;
			
		} else if (view instanceof ListView) { 
			ListView listView = (ListView)view;
			ListAdapter adapter = listView.getAdapter();
			if (adapter != null && adapter instanceof HeaderViewListAdapter)
				adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
			
			return adapter;
			
		} else if (view instanceof RefreshGridView) { 
			RefreshGridView listView = (RefreshGridView)view;
			ListAdapter adapter = listView.getRefreshableView().getAdapter();
			if (adapter != null && adapter instanceof HeaderViewListAdapter)
				adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
			
			return adapter;
			
		} else if (view instanceof GridView) { 
			GridView listView = (GridView)view;
			ListAdapter adapter = listView.getAdapter();
			if (adapter != null && adapter instanceof HeaderViewListAdapter)
				adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
			
			return adapter;
		}
		
		return null;
	}
	
	public static void setListViewSelection(View view, int position) {
		if (view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("setListViewSelection: view=" + view + " position=" + position);
		
		if (view instanceof RefreshListView) { 
			RefreshListView listView = (RefreshListView)view;
			ListAdapter adapter = listView.getRefreshableView().getAdapter();
			if (adapter != null) {
				if (position >= 0 && position < adapter.getCount())
					listView.setSelection(position);
			}
			
		} else if (view instanceof ListView) { 
			ListView listView = (ListView)view;
			ListAdapter adapter = listView.getAdapter();
			if (adapter != null) {
				if (position >= 0 && position < adapter.getCount())
					listView.setSelection(position);
			}
			
		} else if (view instanceof RefreshGridView) { 
			RefreshGridView listView = (RefreshGridView)view;
			ListAdapter adapter = listView.getRefreshableView().getAdapter();
			if (adapter != null) {
				if (position >= 0 && position < adapter.getCount())
					listView.setSelection(position);
			}
			
		} else if (view instanceof GridView) { 
			GridView listView = (GridView)view;
			ListAdapter adapter = listView.getAdapter();
			if (adapter != null) {
				if (position >= 0 && position < adapter.getCount())
					listView.setSelection(position);
			}
		}
	}
	
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
	}
	
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		return -1;
	}
	
	public abstract ListAdapter createListAdapter(IActivity activity);
	
	protected AdapterView.OnItemClickListener getOnItemClickListener(
			IActivity activity, ListAdapter adapter) { 
		return null;
	}
	
}
