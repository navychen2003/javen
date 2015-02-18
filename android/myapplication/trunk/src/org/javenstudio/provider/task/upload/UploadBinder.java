package org.javenstudio.provider.task.upload;

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

public class UploadBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(UploadBinder.class);
	
	private final UploadProvider mProvider;
	
	public UploadBinder(UploadProvider provider) {
		mProvider = provider;
	}

	public final UploadProvider getProvider() { return mProvider; }

	@Override
	protected View inflateView(IActivity activity, 
			LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.upload_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.upload_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return UploadAdapter.createAdapter(activity, 
				mProvider.getUploadDataSets(), this, R.layout.upload_container, 
				getColumnSize(activity));
	}
	
	public int getColumnSize(IActivity activity) { 
		//int size = activity.getResources().getInteger(R.integer.list_column_size);
		return 1; //size > 1 ? size : 1;
	}
	
	public float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, 
			ListAdapter adapter, AbsListView view, int firstVisibleItem, 
			int visibleItemCount, int totalItemCount) { 
		if (adapter == null || !(adapter instanceof UploadAdapter)) 
			return;
		
		UploadAdapter uploadAdapter = (UploadAdapter)adapter;
		uploadAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof UploadAdapter)) 
			return -1;
		
		UploadAdapter uploadAdapter = (UploadAdapter)adapter;
		return uploadAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final UploadItem uploadItem = (UploadItem)item;
		return uploadItem.getViewRes();
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
		final UploadItem uploadItem = (UploadItem)item;
		uploadItem.bindView(activity, this, view);
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		final UploadItem uploadItem = (UploadItem)item;
		uploadItem.updateView(view, true);
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {
		final UploadItem uploadItem = (UploadItem)item;
		uploadItem.onViewBinded(activity);
	}
	
}
