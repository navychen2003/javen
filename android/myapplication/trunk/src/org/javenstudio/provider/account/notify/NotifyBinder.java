package org.javenstudio.provider.account.notify;

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

public abstract class NotifyBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(NotifyBinder.class);
	
	private final NotifyProvider mProvider;
	
	public NotifyBinder(NotifyProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}

	public final NotifyProvider getProvider() { 
		return mProvider; 
	}

	protected NotifyDataSets getNotifyDataSets() { 
		return getProvider().getNotifyDataSets();
	}
	
	protected OnNotifyClickListener getOnNotifyClickListener() { 
		return getProvider().getOnNotifyItemClickListener();
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
		
		return NotifyAdapter.createAdapter(activity, getProvider().getNotifyDataSets(), 
				this, R.layout.notify_container, 
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
		if (adapter == null || !(adapter instanceof NotifyAdapter)) 
			return;
		
		NotifyAdapter notifyAdapter = (NotifyAdapter)adapter;
		notifyAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof NotifyAdapter)) 
			return -1;
		
		NotifyAdapter notifyAdapter = (NotifyAdapter)adapter;
		return notifyAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final NotifyItem notifyItem = (NotifyItem)item;
		return getNotifyItemViewRes(notifyItem);
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
		final NotifyItem notifyItem = (NotifyItem)item;
		onBindView(notifyItem, view);
		
		final OnNotifyClickListener listener = getOnNotifyClickListener();
		if (listener != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onNotifyClick(activity.getActivity(), notifyItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {}
	
	protected int getNotifyItemViewRes(NotifyItem item) { 
		return item.getViewRes(); 
	}
	
	protected void onBindView(NotifyItem item) { 
		if (item == null) return;
		onBindView(item, item.getBindView());
	}
	
	protected void onBindView(NotifyItem item, View view) { 
		if (item == null) return;
		item.bindView(view);
	}
	
}
