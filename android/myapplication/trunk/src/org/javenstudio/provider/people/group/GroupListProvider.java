package org.javenstudio.provider.people.group;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.widget.SearchView;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public class GroupListProvider extends ProviderBase 
		implements ActivityHelper.SearchViewListener {

	private final GroupListBinder mBinder;
	private final GroupDataSets mDataSets;
	
	protected String mSearchTitle = null;
	protected String mQueryText = null;
	protected boolean mBindTitle = true;
	
	private OnGroupClickListener mItemClickListener = null;
	private OnGroupClickListener mViewClickListener = null;
	
	public GroupListProvider(String name, int iconRes, 
			GroupFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createGroupListBinder(this);
		mDataSets = factory.createGroupDataSets(this);
	}

	public void setOnGroupItemClickListener(OnGroupClickListener l) { mItemClickListener = l; }
	public OnGroupClickListener getOnGroupItemClickListener() { return mItemClickListener; }
	
	public void setOnGroupViewClickListener(OnGroupClickListener l) { mViewClickListener = l; }
	public OnGroupClickListener getOnGroupViewClickListener() { return mViewClickListener; }
	
	public void setSearchTitle(String title) { mSearchTitle = title; }
	public String getSearchTitle() { return mSearchTitle; }
	public boolean isSearchable() { return false; }
	
	public void setBindTitle(boolean show) { mBindTitle = show; }
	public boolean isBindTitle() { return mBindTitle; }
	
	public GroupDataSets getGroupDataSets() { return mDataSets; }
	
	@Override
	public GroupListBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public CharSequence getTitle() { 
		CharSequence queryText = isSearchable() ? mQueryText : null;
		if (queryText != null && queryText.length() > 0) {
			String title = getSearchTitle();
			if (title != null && title.length() > 0) 
				return title;
		}
		
		return super.getTitle();
	} 
	
	public CharSequence getDefaultTitle() { 
		return super.getTitle();
	}
	
	@Override
	public CharSequence getSubTitle() { 
		CharSequence queryText = isSearchable() ? mQueryText : null;
		if (queryText != null && queryText.length() > 0) 
			return queryText;
		
		return super.getSubTitle();
	}
	
	@Override
	public View getActionCustomView(final IActivity activity) { 
		final LayoutInflater inflater = LayoutInflater.from(activity.getApplicationContext());
		final View view = inflater.inflate(R.layout.actionbar_custom_item, null); 
		
		bindActionCustomView(activity, view);
		
		return view;
	}
	
	private void bindActionCustomView(IActivity activity, View customView) { 
		if (activity == null || customView == null) 
			return;
		
		TextView titleView = (TextView)customView.findViewById(R.id.actionbar_custom_item_title);
		TextView subtitleView = (TextView)customView.findViewById(R.id.actionbar_custom_item_subtitle);
		
		if (titleView != null) 
			titleView.setText(getTitle());
		
		if (subtitleView != null) {
			CharSequence subtitle = getSubTitle();
			subtitleView.setText(subtitle);
			
			if (subtitle != null && subtitle.length() > 0) 
				subtitleView.setVisibility(View.VISIBLE);
			else
				subtitleView.setVisibility(View.GONE);
		}
	}
	
	protected void bindActionTitle(IActivity activity) { 
		if (!isBindTitle()) return;
		
		final ActionHelper helper = activity.getActionHelper();
		if (helper != null) {
			View customView = helper.getCustomView();
			if (customView != null) {
				bindActionCustomView(activity, customView);
				return;
			}
			
			helper.setActionTitle(getTitle(), getSubTitle());
		}
	}
	
	//@Override
	//public void onFragmentPreCreate(IActivity activity) { 
	//	final ActionHelper helper = activity.getActionHelper();
	//	if (helper != null) {
	//		helper.setActionItems(null, null);
	//		helper.setActionTitle(null, null);
	//		helper.setHomeIcon(getHomeIconRes());
	//		helper.setHomeAsUpIndicator(getHomeAsUpIndicatorRes());
	//		helper.setCustomView(getActionCustomView(activity), 
	//				getActionCustomLayoutParams(activity));
	//	}
	//	setActionBarTitle(activity);
	//	setOrientation(activity);
	//}
	
	@Override
	public void setActionBarTitle(IActivity activity) {
		bindActionTitle(activity);
	}
	
	@Override
	public void onSearchViewOpen(IActivity activity, View view) { 
		if (view != null && view instanceof SearchView) {
			SearchView searchView = (SearchView)view;
			searchView.setQuery(mQueryText, false);
		}
	}
	
	@Override
	public void onSearchViewClose(IActivity activity, View view) {
	}
	
	@Override
	public boolean onSearchTextSubmit(IActivity activity, String query) {
		activity.getActivityHelper().hideSearchView();
		
		mQueryText = query;
		
		bindActionTitle(activity);
		activity.refreshContent(true);
		
		return true;
	}
	
	@Override
	public boolean onSearchTextChange(IActivity activity, String newText) {
		return false;
	}
	
	public String getQueryText() { return mQueryText; }
	
	@Override
	public boolean onActionHome(IActivity activity) { 
		String queryText = isSearchable() ? mQueryText : null;
		if (queryText != null && queryText.length() > 0) {
			mQueryText = null;
			
			bindActionTitle(activity);
			activity.refreshContent(false);
			return true;
		}
		
		return false; 
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getGroupDataSets().clear();
				}
			});
	}
	
	protected void postAddGroupItem(final ProviderCallback callback, final GroupItem item) { 
		if (callback == null || item == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getGroupDataSets().addGroupItem(item, false);
					callback.getController().getModel().callbackOnDataSetUpdate(item); 
				}
			});
	}
	
	protected void postNotifyChanged() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getGroupDataSets().notifyContentChanged(true);
					getGroupDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
