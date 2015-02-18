package org.javenstudio.provider.media;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.widget.SearchView;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.media.photo.PhotoFactory;
import org.javenstudio.provider.media.photo.PhotoSource;

public abstract class PhotoProvider extends ProviderBase 
		implements ActivityHelper.SearchViewListener {

	protected final PhotoSource mSource;
	protected String mSearchTitle = null;
	protected String mQueryText = null;
	protected String mQueryTag = null;
	protected boolean mBindTitle = true;
	
	private final Object mTagsLock = new Object();
	private String[] mPopularTags = null;
	
	public PhotoProvider(String name, int iconRes, 
			PhotoSet set, PhotoFactory factory) { 
		super(name, iconRes);
		mSource = factory.createPhotoSource(this, set);
	}
	
	public PhotoSource getSource() { return mSource; }
	public PhotoSet getPhotoSet() { return mSource.getPhotoSet(); }
	
	public SelectMode.Callback getSelectCallback() { return mSource; }
	public boolean isSearchable() { return getPhotoSet().isSearchable(); }
	
	public void setSearchTitle(String title) { mSearchTitle = title; }
	public String getSearchTitle() { return mSearchTitle; }
	
	public void setBindTitle(boolean show) { mBindTitle = show; }
	public boolean isBindTitle() { return mBindTitle; }
	
	public String[] getPhotoTags() { 
		synchronized (mTagsLock) { 
			if (mPopularTags == null) { 
				mPopularTags = getPhotoSet().getPhotoTags();
				if (mPopularTags == null) 
					mPopularTags = new String[0];
			}
			return mPopularTags;
		}
	}
	
	public int getPhotoTagCount() { 
		String[] tags = getPhotoTags();
		return tags != null ? tags.length : 0;
	}
	
	@Override
	public final ProviderBinder getBinder() { 
		return mSource.getBinder();
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
		
		String queryTag = mQueryTag;
		if (queryTag != null && queryTag.length() > 0) 
			return queryTag;
		
		return super.getSubTitle();
	}
	
	@Override
	public View getActionCustomView(final IActivity activity) { 
		final LayoutInflater inflater = LayoutInflater.from(activity.getApplicationContext());
		final View view;
		
		if (getPhotoTagCount() > 0) { 
			view = inflater.inflate(R.layout.actionbar_custom_spinner_item, null); 
			bindActionCustomView(activity, view);
			
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new PhotoPopup(PhotoProvider.this, activity).showTagSelect(v);
					}
				});
		} else { 
			view = inflater.inflate(R.layout.actionbar_custom_item, null); 
			bindActionCustomView(activity, view);
		}
		
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
	
	@Override
	public boolean onActionHome(IActivity activity) { 
		String queryText = isSearchable() ? mQueryText : null;
		if (queryText != null && queryText.length() > 0) {
			mSource.clearPhotoList();
			mQueryText = null;
			
			bindActionTitle(activity);
			activity.refreshContent(false);
			return true;
		}
		
		return false; 
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
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		callback.clearParams();
		
		String queryText = isSearchable() ? mQueryText : null;
		if (queryText != null && queryText.length() > 0) {
			callback.setParam(ReloadCallback.PARAM_QUERYTEXT, queryText);
		
		} else {
			String queryTag = mQueryTag;
			if (queryTag != null && queryTag.length() > 0) 
				callback.setParam(ReloadCallback.PARAM_QUERYTAG, queryTag);
		}
		
		mSource.reloadData(callback, type, reloadId);
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
		
		mSource.clearPhotoList();
		mQueryText = query;
		
		bindActionTitle(activity);
		activity.refreshContent(true);
		
		return true;
	}
	
	@Override
	public boolean onSearchTextChange(IActivity activity, String newText) {
		return false;
	}
	
	public boolean onQueryTagSubmit(IActivity activity, String tag) {
		activity.getActivityHelper().hideSearchView();
		
		mSource.clearPhotoList();
		mQueryText = null;
		mQueryTag = tag;
		
		bindActionTitle(activity);
		activity.refreshContent(false);
		
		return true;
	}
	
}
