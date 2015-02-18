package org.javenstudio.provider.account.dashboard;

import android.os.Bundle;
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
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBinderBase;

public abstract class DashboardBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	private static final Logger LOG = Logger.getLogger(DashboardBinder.class);
	
	private final DashboardProvider mProvider;
	
	public DashboardBinder(DashboardProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}

	public final DashboardProvider getProvider() { 
		return mProvider; 
	}

	protected DashboardDataSets getDashboardDataSets() { 
		return getProvider().getDashboardDataSets();
	}
	
	protected OnDashboardClickListener getOnDashboardClickListener() { 
		return getProvider().getOnDashboardItemClickListener();
	}
	
	public void bindBackgroundView(IActivity activity) {}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		if (activity == null || inflater == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("bindBehindAbove: activity=" + activity);
	}
	
	@Override
	protected View inflateView(IActivity activity, 
			LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.dashboard_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.dashboard_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return DashboardAdapter.createAdapter(activity, getProvider().getDashboardDataSets(), 
				this, R.layout.dashboard_container, 
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
		if (adapter == null || !(adapter instanceof DashboardAdapter)) 
			return;
		
		DashboardAdapter dashboardAdapter = (DashboardAdapter)adapter;
		dashboardAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof DashboardAdapter)) 
			return -1;
		
		DashboardAdapter dashboardAdapter = (DashboardAdapter)adapter;
		return dashboardAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		final DashboardItem dashboardItem = (DashboardItem)item;
		return getDashboardItemViewRes(dashboardItem);
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
		final DashboardItem dashboardItem = (DashboardItem)item;
		onBindView(activity, dashboardItem, view);
		
		if (view != null && dashboardItem != null && dashboardItem.isShown() == false) {
			int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.dashboard_item_show_animation);
			if (animRes == 0) animRes = R.anim.slide_out_up;
			Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), animRes);
			view.setAnimation(ani);
			dashboardItem.setShown(true);
		}
		
		final OnDashboardClickListener listener = getOnDashboardClickListener();
		if (listener != null && view != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onDashboardClick(activity.getActivity(), dashboardItem);
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
		onRequestDownload(activity, (DashboardItem)item);
	}
	
	protected int getDashboardItemViewRes(DashboardItem item) { 
		return item.getViewRes(); 
	}
	
	protected void onBindView(IActivity activity, DashboardItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		item.bindView(activity, this, view);
	}
	
	protected void onRequestDownload(IActivity activity, DashboardItem item) {
		if (activity == null || item == null) return;
		requestDownload(activity, item.getShowImageItems());
	}
	
	protected int getUserTitleColorStateListRes() {
		return AppResources.getInstance().getColorStateListRes(
				AppResources.color.dashboard_user_title_color);
	}
	
	protected int getItemTitleColorStateListRes() {
		return AppResources.getInstance().getColorStateListRes(
				AppResources.color.dashboard_item_title_color);
	}
	
	protected int getItemHeaderViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.dashboard_item_header_background);
	}
	
	protected int getItemBodyViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.dashboard_item_body_background);
	}
	
	protected int getItemPosterViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.sectioninfo_image_background);
	}
	
}
