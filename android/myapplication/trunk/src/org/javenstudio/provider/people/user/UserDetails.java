package org.javenstudio.provider.people.user;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;

public class UserDetails extends UserAction {
	private static final Logger LOG = Logger.getLogger(UserDetails.class);

	public static interface DetailsItem { 
		public View getView(Context context, View convertView, boolean dropDown);
	}
	
	private final List<DetailsItem> mItems = new ArrayList<DetailsItem>();
	private ListAdapter mAdapter = null;
	
	public UserDetails(UserItem item, String name) { 
		this(item, name, 0);
	}
	
	public UserDetails(UserItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}
	
	public void clearDetailsItems() { 
		synchronized (mItems) { 
			mItems.clear();
			mAdapter = null;
		}
	}
	
	public void addDetailsItem(DetailsItem item) { 
		if (item == null) return;
		
		synchronized (mItems) { 
			for (DetailsItem di : mItems) { 
				if (di == item) return;
			}
			
			mItems.add(item);
			mAdapter = null;
		}
	}
	
	public DetailsItem[] getDetailsItems() { 
		synchronized (mItems) {
			DetailsItem[] items = mItems.toArray(new DetailsItem[mItems.size()]);
			return items;
		}
	}
	
	@Override
	public ListAdapter getAdapter(IActivity activity) { 
		return getDetailsAdapter(false);
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
		if (type == ReloadType.FORCE) 
			postUpdateViews();
	}
	
	public void postUpdateViews() {
		if (LOG.isDebugEnabled()) LOG.debug("postUpdateViews: action=" + this);
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					bindListViewDefault(getTabItem().getListView(), 
							getDetailsAdapter(true));
				}
			});
	}
	
	private ListAdapter getDetailsAdapter(boolean createNew) {
		synchronized (mItems) {
			ListAdapter adapter = mAdapter;
			if (adapter != null && !createNew) return adapter;
			
			adapter = new DetailsAdapter(ResourceHelper.getContext(), getDetailsItems());
			mAdapter = adapter;
			
			return adapter;
		}
	}
	
	public static class DetailsAdapter extends ArrayAdapter<DetailsItem> {
		public DetailsAdapter(Context context, DetailsItem[] items) {
			super(context, 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, false);
		}
		
		@Override
	    public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, true);
		}
		
		@Override
	    public boolean isEnabled(int position) {
	        return false;
	    }
		
		@Override
	    public int getItemViewType(int position) {
	        return position;
	    }

		@Override
	    public int getViewTypeCount() {
			int count = getCount();
	        return count > 0 ? count : 1;
	    }
		
		protected View getView(int position, View convertView, boolean dropDown) {
			final DetailsItem item = getItem(position);
			
			return item.getView(getContext(), convertView, dropDown);
		}
	}
	
}
