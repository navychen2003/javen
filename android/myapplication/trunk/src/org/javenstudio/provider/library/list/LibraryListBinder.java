package org.javenstudio.provider.library.list;

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

public class LibraryListBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(LibraryListBinder.class);

	private final LibraryListProvider mProvider;
	
	public LibraryListBinder(LibraryListProvider provider) {
		mProvider = provider;
	}

	public final LibraryListProvider getProvider() { return mProvider; }

	protected LibraryListDataSets getLibraryListDataSets() { 
		return getProvider().getLibraryListDataSets();
	}
	
	protected OnLibraryListClickListener getOnLibraryListClickListener() { 
		return getProvider().getOnItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, 
			LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return LibraryListAdapter.createAdapter(activity, 
				mProvider.getLibraryListDataSets(), this, R.layout.provider_container, 
				getColumnSize(activity));
	}
	
	protected int getColumnSize(IActivity activity) { 
		int size = 0; //activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, 
			ListAdapter adapter, AbsListView view, int firstVisibleItem, 
			int visibleItemCount, int totalItemCount) { 
		if (adapter == null || !(adapter instanceof LibraryListAdapter)) 
			return;
		
		LibraryListAdapter accountAdapter = (LibraryListAdapter)adapter;
		accountAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof LibraryListAdapter)) 
			return -1;
		
		LibraryListAdapter accountAdapter = (LibraryListAdapter)adapter;
		return accountAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final LibraryListItem dataItem = (LibraryListItem)item;
		return getLibraryItemViewRes(dataItem);
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
		final LibraryListItem dataItem = (LibraryListItem)item;
		onBindView(activity, dataItem, view);
		
		//if (view != null && dataItem != null && dataItem.isShown() == false) {
		//	Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), R.anim.slide_out_up);
		//	view.setAnimation(ani);
		//	dataItem.setShown(true);
		//}
		
		final OnLibraryListClickListener listener = getOnLibraryListClickListener();
		if (listener != null && view != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onLibraryClick(activity.getActivity(), dataItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {
		onRequestDownload(activity, (LibraryListItem)item);
	}
	
	protected int getLibraryItemViewRes(LibraryListItem item) { 
		return item.getViewRes(); 
	}
	
	protected void onBindView(IActivity activity, LibraryListItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		item.bindView(activity, view);
	}
	
	protected void onRequestDownload(IActivity activity, LibraryListItem item) {
		if (activity == null || item == null) return;
		//requestDownload(activity, item.getShowImageItems());
	}
	
}
