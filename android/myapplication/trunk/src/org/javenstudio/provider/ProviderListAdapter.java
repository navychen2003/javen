package org.javenstudio.provider;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;

public class ProviderListAdapter extends PagerAdapter {
	private static final Logger LOG = Logger.getLogger(ProviderListAdapter.class);

	private final ProviderList mProvider;
	private final ProviderActionItem[] mItems;
	private final IActivity mActivity;
	
	public ProviderListAdapter(ProviderList provider, 
			IActivity activity, ProviderActionItem[] items) {
		if (provider == null || items == null || activity == null) 
			throw new NullPointerException();
		mProvider = provider;
		mActivity = activity;
		mItems = items;
	}
	
	public ProviderList getProvider() { return mProvider; }
	public ProviderActionItem[] getActionItems() { return mItems; }
	public IActivity getActivity() { return mActivity; }
	
	@Override
	public int getCount() {
		return mItems.length;
	}

	public ProviderActionItem getItemAt(int position) {
		if (position >= 0 && position < mItems.length)
			return mItems[position];
		return null;
	}
	
	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public Object instantiateItem(View container, int position) {
		if (LOG.isDebugEnabled()) { 
			LOG.debug("instantiateItem: container=" + container 
					+ " position=" + position);
		}
		
		if (position < 0) position = 0;
		else if (position >= mItems.length) position = mItems.length -1;
		
		ProviderActionItem item = mItems[position];
		Provider p = item != null ? item.getProvider() : null;
		if (p != null) {
			ProviderBinder binder = p.getBinder();
			if (binder != null) {
				LayoutInflater inflater = LayoutInflater.from(getActivity().getActivity());
				ViewGroup pager = (ViewGroup)container;
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("instantiateItem: position=" + position 
							+ " item=" + item + " provider=" + p + " binder=" + binder);
				}
				
				View view = binder.bindView(getActivity(), inflater, pager, null);
				pager.addView(view);
				
				return view;
			}
		}
		
		return null;
	}
	
	@Override
	public void destroyItem(View container, int position, Object object) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("destroyItem: container=" + container 
					+ " position=" + position + " object=" + object);
		}
		((ViewPager) container).removeView((View) object);
	}
	
}
