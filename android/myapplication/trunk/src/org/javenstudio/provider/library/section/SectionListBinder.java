package org.javenstudio.provider.library.section;

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
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;

public class SectionListBinder extends SectionListBinderBase 
		implements DataBinder.BinderCallback, ISectionListBinder {
	//private static final Logger LOG = Logger.getLogger(SectionListBinder.class);

	private IActivity mActivity = null;
	private View mListView = null;
	
	public SectionListBinder(SectionListProvider provider, 
			SectionListFactory factory) {
		super(provider, factory);
	}

	protected SectionListDataSets getSectionListDataSets() { 
		return getDataSets(); //getProvider().getSectionListDataSets();
	}
	
	protected OnSectionListClickListener getOnSectionListClickListener() { 
		return getProvider().getOnItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, 
			LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.section_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.section_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return SectionListAdapter.createAdapter(activity, 
				getSectionListDataSets(), this, R.layout.section_container, 
				getColumnSize());
	}
	
	@Override
	public void bindListView(final IActivity activity, View view, 
			final ListAdapter adapter) { 
		super.bindListView(activity, view, adapter);
		mActivity = activity;
		mListView = view;
	}
	
	@Override
	public View getBindedListView() { return mListView; }
	
	@Override
	public IActivity getBindedActivity() { return mActivity; }
	
	@Override
	public int getColumnSize() { 
		int size = 0; //activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	public float getColumnSpace() { 
		return ResourceHelper.getResources().getDimension(R.dimen.section_list_column_space);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, 
			ListAdapter adapter, AbsListView view, int firstVisibleItem, 
			int visibleItemCount, int totalItemCount) { 
		super.onFirstVisibleChanged(activity, adapter, view, 
				firstVisibleItem, visibleItemCount, totalItemCount);
		
		if (adapter == null || !(adapter instanceof SectionListAdapter)) 
			return;
		
		SectionListAdapter accountAdapter = (SectionListAdapter)adapter;
		accountAdapter.onFirstVisibleChanged(this, view, 
				firstVisibleItem, visibleItemCount, totalItemCount);
		
		// load next page
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem > 0 && accountAdapter.getCount() > 0 && 
			shouldLoadNextPage(firstVisibleItem, visibleItemCount, totalItemCount)) { 
			ProviderCallback callback = (ProviderCallback)activity.getCallback();
			callback.getController().getModel().loadNextPage(callback, 
					accountAdapter.getCount(), lastVisibleItem);
		}
	}
	
	protected boolean shouldLoadNextPage(int firstVisibleItem, 
			int visibleItemCount, int totalItemCount) {
		return getProvider().getSectionListDataSets().getCount() < 
				getProvider().getSectionListTotalCount();
	}
	
	//@Override
	//protected int getFirstVisibleItem(ListAdapter adapter) { 
	//	if (adapter == null || !(adapter instanceof SectionListAdapter)) 
	//		return -1;
	//	
	//	SectionListAdapter accountAdapter = (SectionListAdapter)adapter;
	//	return accountAdapter.getFirstVisibleItem();
	//}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final SectionListItem dataItem = (SectionListItem)item;
		return getSectionListItemViewRes(dataItem);
	}
	
	@Override
	public void addItemView(IActivity activity, DataBinderItem item, 
			ViewGroup container, View view, int index, int count) { 
		if (index > 0) { 
			container.addView(new View(activity.getActivity()), 
					new LinearLayout.LayoutParams((int)getColumnSpace(), 
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
		final SectionListItem dataItem = (SectionListItem)item;
		onBindItemView(activity, dataItem, view);
		
		//if (view != null && dataItem != null && dataItem.isShown() == false) {
		//	Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), R.anim.slide_out_up);
		//	view.setAnimation(ani);
		//	dataItem.setShown(true);
		//}
		
		final OnSectionListClickListener listener = getOnSectionListClickListener();
		if (listener != null && view != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onSectionClick(activity.getActivity(), dataItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		updateItemView((SectionListItem)item, view, false);
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {
		onRequestDownload(activity, (SectionListItem)item);
	}
	
	protected int getSectionListItemViewRes(SectionListItem item) {
		if (item instanceof SectionCategoryItem) {
			return SectionCategoryItem.getListItemViewRes();
			
		} else if (item instanceof SectionFileItem) {
			return SectionFileItem.getListItemViewRes();
			
		} else if (item instanceof SectionFolderItem) {
			return SectionFolderItem.getListItemViewRes();
			
		} else if (item instanceof SectionEmptyItem) {
			return SectionEmptyItem.getListItemViewRes();
		}
		
		return 0;
	}
	
	protected void onBindItemView(IActivity activity, 
			SectionListItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		
		if (item instanceof SectionCategoryItem) {
			SectionCategoryItem.bindListItemView(activity, this, 
					(SectionCategoryItem)item, view);
			
		} else if (item instanceof SectionFileItem) {
			SectionFileItem.bindListItemView(activity, this, 
					(SectionFileItem)item, view);
			
		} else if (item instanceof SectionFolderItem) {
			SectionFolderItem.bindListItemView(activity, this, 
					(SectionFolderItem)item, view);
			
		} else if (item instanceof SectionEmptyItem) {
			SectionEmptyItem.bindListItemView(activity, this, 
					(SectionEmptyItem)item, view);
		}
	}
	
	@Override
	public void updateItemView(SectionListItem item, View view,
			boolean restartSlide) {
		if (item == null || view == null) return;
		
		if (item instanceof SectionCategoryItem) {
			SectionCategoryItem.updateListItemView(
					(SectionCategoryItem)item, view, restartSlide);
			
		} else if (item instanceof SectionFileItem) {
			SectionFileItem.updateListItemView(
					(SectionFileItem)item, view, restartSlide);
			
		} else if (item instanceof SectionFolderItem) {
			SectionFolderItem.updateListItemView(
					(SectionFolderItem)item, view, restartSlide);
			
		} else if (item instanceof SectionEmptyItem) {
			SectionEmptyItem.updateListItemView(
					(SectionEmptyItem)item, view, restartSlide);
		}
	}
	
	protected void onRequestDownload(IActivity activity, SectionListItem item) {
		if (activity == null || item == null) return;
		requestDownload(activity, item.getShowImageItems(), true);
	}
	
}
