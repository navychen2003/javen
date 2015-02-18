package org.javenstudio.provider.library.section;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;

public class SectionGridBinder extends SectionListBinderBase 
		implements DataBinder.BinderCallback, ISectionListBinder {
	//private static final Logger LOG = Logger.getLogger(SectionGridBinder.class);

	private IActivity mActivity = null;
	private View mListView = null;
	
	public SectionGridBinder(SectionListProvider provider, 
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
		return inflater.inflate(R.layout.section_grid, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.section_grid_listview);
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
		int size = ResourceHelper.getResources().getInteger(R.integer.section_grid_column_size);
		return size > 1 ? size : 1;
	}
	
	public float getColumnSpace() { 
		return ResourceHelper.getResources().getDimension(R.dimen.section_grid_column_space);
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
					totalItemCount, lastVisibleItem);
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
		
		if (view != null && dataItem != null && dataItem.showAnimation()) {
			int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.section_grid_item_show_animation);
			if (animRes == 0) animRes = R.anim.ds_grow_fade_in_center;
			Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), animRes);
			view.setAnimation(ani);
			dataItem.setShown(true);
		}
		
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
			return SectionCategoryItem.getGridItemViewRes();
			
		} else if (item instanceof SectionFileItem) {
			return SectionFileItem.getGridItemViewRes();
			
		} else if (item instanceof SectionFolderItem) {
			return SectionFolderItem.getGridItemViewRes();
			
		} else if (item instanceof SectionEmptyItem) {
			return SectionEmptyItem.getGridItemViewRes();
		}
		
		return 0;
	}
	
	protected void onBindItemView(IActivity activity, 
			SectionListItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		
		if (item instanceof SectionCategoryItem) {
			SectionCategoryItem.bindGridItemView(activity, this, 
					(SectionCategoryItem)item, view);
			
		} else if (item instanceof SectionFileItem) {
			SectionFileItem.bindGridItemView(activity, this, 
					(SectionFileItem)item, view);
			
		} else if (item instanceof SectionFolderItem) {
			SectionFolderItem.bindGridItemView(activity, this, 
					(SectionFolderItem)item, view);
			
		} else if (item instanceof SectionEmptyItem) {
			SectionEmptyItem.bindGridItemView(activity, this, 
					(SectionEmptyItem)item, view);
		}
	}
	
	@Override
	public void updateItemView(SectionListItem item, View view,
			boolean restartSlide) {
		if (item == null || view == null) return;
		
		if (item instanceof SectionCategoryItem) {
			SectionCategoryItem.updateGridItemView(
					(SectionCategoryItem)item, view, restartSlide);
			
		} else if (item instanceof SectionFileItem) {
			SectionFileItem.updateGridItemView(
					(SectionFileItem)item, view, restartSlide);
			
		} else if (item instanceof SectionFolderItem) {
			SectionFolderItem.updateGridItemView(
					(SectionFolderItem)item, view, restartSlide);
			
		} else if (item instanceof SectionEmptyItem) {
			SectionEmptyItem.updateGridItemView(
					(SectionEmptyItem)item, view, restartSlide);
		}
	}
	
	protected void onRequestDownload(IActivity activity, SectionListItem item) {
		if (activity == null || item == null) return;
		requestDownload(activity, item.getShowImageItems(), true);
	}
	
}
