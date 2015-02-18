package org.javenstudio.provider;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.common.util.Logger;

public abstract class ProviderListBinder extends DataBinder 
		implements ProviderBinder {
	private static final Logger LOG = Logger.getLogger(ProviderListBinder.class);

	private static final String STATE_CURRENTITEM = "ProviderListBinder.ViewPager.CurrentItem";
	
	private ProviderListAdapter mAdapter = null;
	private ViewPager mPager = null;
	private int mRestoreCurrentItem = -1;
	
	public abstract ProviderList getProvider();
	
	public ProviderListAdapter getAdapter() { return mAdapter; }
	public ViewPager getViewPager() { return mPager; }
	
	public boolean onItemSelected(IActivity activity, ProviderActionItem item) {
		if (activity == null || item == null) return false;
		if (LOG.isDebugEnabled()) 
			LOG.debug("onItemSelected: activity=" + activity + " item=" + item);
		
		ProviderListAdapter adapter = mAdapter;
		ViewPager pager = mPager;
		
		if (adapter != null && pager != null) {
			ProviderActionItem[] items = adapter.getActionItems();
			
			for (int i=0; items != null && i < items.length; i++) {
				ProviderActionItem actionItem = items[i];
				if (actionItem == item) {
					if (i != pager.getCurrentItem()) pager.setCurrentItem(i);
					return true;
				}
			}
		}
		
		return false;
	}
	
	//public void onDataSetChanged(IActivity activity, Object data) {
	//	ProviderListAdapter adapter = mAdapter;
	//	ViewPager pager = mPager;
	//	
	//	if (adapter != null && pager != null) {
	//		ProviderActionItem[] items = adapter.getActionItems();
	//		
	//		for (int i=0; items != null && i < items.length; i++) {
	//			ProviderActionItem actionItem = items[i];
	//			Provider p = actionItem != null ? actionItem.getProvider() : null;
	//			if (p != null) p.onDataSetChanged(activity, data);
	//		}
	//	}
	//}
	
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		ProviderListAdapter adapter = mAdapter;
		ViewPager pager = mPager;
		
		if (adapter != null && pager != null) {
			ProviderActionItem[] items = adapter.getActionItems();
			
			for (int i=0; items != null && i < items.length; i++) {
				ProviderActionItem actionItem = items[i];
				Provider p = actionItem != null ? actionItem.getProvider() : null;
				if (p != null) p.reloadOnThread(callback, type, reloadId);
			}
		}
	}
	
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState == null) return;
		
		ProviderListAdapter adapter = mAdapter;
		ViewPager pager = mPager;
		
		if (adapter != null && pager != null) {
			mRestoreCurrentItem = pager.getCurrentItem();
			savedInstanceState.putInt(STATE_CURRENTITEM, pager.getCurrentItem());
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("onSaveInstanceState: savedInstanceState=" + savedInstanceState);
	}
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("onRestoreInstanceState: savedInstanceState=" + savedInstanceState);
		
		int currentItem = savedInstanceState.getInt(STATE_CURRENTITEM, -1);
		if (currentItem >= 0)
			mRestoreCurrentItem = currentItem;
	}
	
	protected ViewPager.OnPageChangeListener getPageChangeListener(IActivity activity) {
		return null;
	}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
	}
	
	@Override
	public final View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		onRestoreInstanceState(savedInstanceState);
		View view = inflateView(activity, inflater, container);
		
		ViewPager pager = findViewPager(activity, view);
		ProviderListAdapter adapter = getProvider().getPagerAdapter(activity);
		if (pager != null && adapter != null) {
			pager.setAdapter(adapter);
			pager.setOnPageChangeListener(getPageChangeListener(activity));
			
			int currentItem = mRestoreCurrentItem;
			//if (currentItem < 0) currentItem = getProvider().getActionSelectIndex();
			if (currentItem >= 0 && currentItem < adapter.getCount() && 
				currentItem != pager.getCurrentItem()) {
				pager.setCurrentItem(currentItem);
			}
			
			ProviderActionItem item = adapter.getItemAt(pager.getCurrentItem());
			if (item != null) {
				Provider provider = item.getProvider();
				if (provider != null) provider.onPagerItemSelected(activity);
			}
			
			mAdapter = adapter;
			mPager = pager;
		}
		
		return view;
	}

	public Provider getCurrentProvider() {
		ProviderListAdapter adapter = mAdapter;
		ViewPager pager = mPager;
		
		if (adapter != null && pager != null) {
			ProviderActionItem item = adapter.getItemAt(pager.getCurrentItem());
			if (item != null) return item.getProvider();
		}
		
		return null;
	}
	
	protected View inflateView(IActivity activity, LayoutInflater inflater, 
			ViewGroup container) { 
		return inflater.inflate(R.layout.provider_pager, container, false);
	}
	
	protected ViewPager findViewPager(IActivity activity, View rootView) { 
		return (ViewPager)rootView.findViewById(R.id.provider_viewpager);
	}
	
}
