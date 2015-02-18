package org.javenstudio.provider.account.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.app.R;
import org.javenstudio.provider.ProviderBinderBase;

public class AccountListBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(AccountListBinder.class);

	private final AccountListProvider mProvider;
	
	public AccountListBinder(AccountListProvider provider) {
		mProvider = provider;
	}

	public final AccountListProvider getProvider() { return mProvider; }

	protected AccountListDataSets getAccountListDataSets() { 
		return getProvider().getAccountListDataSets();
	}
	
	protected OnAccountListClickListener getOnAccountListClickListener() { 
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
		
		return AccountListAdapter.createAdapter(activity, 
				mProvider.getAccountListDataSets(), this, R.layout.provider_container, 
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
		if (adapter == null || !(adapter instanceof AccountListAdapter)) 
			return;
		
		AccountListAdapter accountAdapter = (AccountListAdapter)adapter;
		accountAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof AccountListAdapter)) 
			return -1;
		
		AccountListAdapter accountAdapter = (AccountListAdapter)adapter;
		return accountAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final AccountListItem accountListItem = (AccountListItem)item;
		return getAccountListItemViewRes(accountListItem);
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
		final AccountListItem accountListItem = (AccountListItem)item;
		onBindViews(activity, accountListItem, view);
		
		//if (view != null && accountListItem != null && accountListItem.isShown() == false) {
		//	Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), R.anim.slide_out_up);
		//	view.setAnimation(ani);
		//	accountListItem.setShown(true);
		//}
		
		final OnAccountListClickListener listener = getOnAccountListClickListener();
		if (listener != null && view != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onAccountClick(activity.getActivity(), accountListItem);
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
		onRequestDownload(activity, (AccountListItem)item);
	}
	
	protected int getAccountListItemViewRes(AccountListItem item) { 
		return item.getViewRes(); 
	}
	
	protected void onBindViews(IActivity activity, AccountListItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		item.bindViews(activity, view);
	}
	
	protected void onRequestDownload(IActivity activity, AccountListItem item) {
		if (activity == null || item == null) return;
		//requestDownload(activity, item.getShowImageItems());
	}
	
}
