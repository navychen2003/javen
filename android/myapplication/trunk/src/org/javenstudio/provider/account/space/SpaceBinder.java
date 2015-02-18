package org.javenstudio.provider.account.space;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.provider.ProviderBinderBase;

public abstract class SpaceBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(SpaceBinder.class);
	
	private final SpaceProvider mProvider;
	
	public SpaceBinder(SpaceProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}

	public final SpaceProvider getProvider() { 
		return mProvider; 
	}

	protected SpaceDataSets getSpaceDataSets() { 
		return getProvider().getSpaceDataSets();
	}
	
	protected OnSpaceClickListener getOnSpaceClickListener() { 
		return getProvider().getOnSpaceItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, 
			LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.storagespace, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.storagespace_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return SpaceAdapter.createAdapter(activity, getProvider().getSpaceDataSets(), 
				this, R.layout.storagespace_container, 
				getColumnSize(activity));
	}
	
	protected int getColumnSize(IActivity activity) { 
		//int size = activity.getResources().getInteger(R.integer.list_column_size);
		return 1; //size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, 
			ListAdapter adapter, AbsListView view, int firstVisibleItem, 
			int visibleItemCount, int totalItemCount) { 
		if (adapter == null || !(adapter instanceof SpaceAdapter)) 
			return;
		
		SpaceAdapter spaceAdapter = (SpaceAdapter)adapter;
		spaceAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof SpaceAdapter)) 
			return -1;
		
		SpaceAdapter spaceAdapter = (SpaceAdapter)adapter;
		return spaceAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final SpaceItem spaceItem = (SpaceItem)item;
		return spaceItem.getViewRes();
	}
	
	@Override
	public void addItemView(IActivity activity, DataBinderItem item, 
			ViewGroup container, View view, int index, int count) { 
		if (index > 0) { 
			container.addView(new View(activity.getActivity()), 
					new LinearLayout.LayoutParams((int)getColumnSpace(activity), 
							LinearLayout.LayoutParams.MATCH_PARENT));
		}
		
		if (view == null) 
			view = new View(activity.getActivity());
		
		container.addView(view, 
				new LinearLayout.LayoutParams(0, 
						LinearLayout.LayoutParams.WRAP_CONTENT, 1));
	}
	
	@Override
	public final void bindItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		final SpaceItem spaceItem = (SpaceItem)item;
		spaceItem.bindView(view);
		
		final OnSpaceClickListener listener = getOnSpaceClickListener();
		if (listener != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onSpaceClick(activity.getActivity(), spaceItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		final SpaceItem spaceItem = (SpaceItem)item;
		spaceItem.bindView(view);
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {
		final SpaceItem spaceItem = (SpaceItem)item;
		spaceItem.onViewBinded(activity);
	}
	
}
