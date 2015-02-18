package org.javenstudio.provider.library.list;

import java.util.ArrayList;

import android.view.View;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.ActionExecutor;
import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.cocoka.widget.SearchView;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.section.SectionListItem;
import org.javenstudio.provider.library.section.SectionListProvider;
import org.javenstudio.provider.library.select.SelectOperation;

public abstract class LibraryProvider extends SectionListProvider {
	private static final Logger LOG = Logger.getLogger(LibraryProvider.class);

	public LibraryProvider(AccountApp app, AccountUser account, 
			String name, int iconRes, LibraryFactory factory) { 
		super(app, account, name, iconRes, factory);
	}
	
	@Override
	public LibraryFactory getFactory() {
		return (LibraryFactory)super.getFactory();
	}
	
	public abstract ILibraryData getLibraryData();
	public abstract SelectOperation getSelectOperation();
	
	public abstract String getSearchText();
	public abstract boolean setSearchList(IActivity activity, String query);
	
	public abstract boolean setSectionList(IActivity activity, 
			ISectionList data, boolean refreshContent);
	
	public ActivityHelper.SearchViewListener getSearchViewListener() {
		return new ActivityHelper.SearchViewListener() {
				@Override
				public void onSearchViewOpen(IActivity activity, View view) { 
					if (view != null && view instanceof SearchView) {
						SearchView searchView = (SearchView)view;
						searchView.setQuery(getSearchText(), false);
					}
					LibraryProvider.this.onSearchViewOpen(activity, view);
				}
				
				@Override
				public void onSearchViewClose(IActivity activity, View view) { 
					LibraryProvider.this.onSearchViewClose(activity, view);
				}
				
				@Override
				public boolean onSearchTextSubmit(IActivity activity, String query) {
					activity.getActivityHelper().hideSearchView();
					return setSearchList(activity, query);
				}
				
				@Override
				public boolean onSearchTextChange(IActivity activity, String newText) {
					return false;
				}
			};
	}
	
	public void onSearchViewOpen(IActivity activity, View view) {}
	public void onSearchViewClose(IActivity activity, View view) {}
	
	@Override
	public void startSelectMode(IActivity activity, SectionListItem item) {
		if (activity == null || activity.isDestroyed()) return;
		if (LOG.isDebugEnabled())
			LOG.debug("startSelectMode: activity=" + activity + " item=" + item);
		
		ActionHelper helper = activity.getActionHelper();
		if (helper != null && !helper.isSelectMode()) { 
			SelectMode.Callback callback = getSelectCallback();
			if (callback != null) {
				SelectMode mode = getFactory().createSelectMode(activity);
				callback.getSelectManager().clearSelectedItems();
				if (helper.startSelectMode(mode, callback)) {
					if (item != null) item.onItemSelect(activity, true);
				}
			}
		}
	}
	
	public SelectMode.Callback getSelectCallback() {
		return new SelectMode.AbstractCallback() {
				@Override
				public SelectManager getSelectManager() {
					return getSectionListDataSets().getSelectManager();
				}
				
				@Override
				public void onSelectChanged(SelectMode mode) {
					if (mode != null) 
						mode.setTitle(""+getSelectManager().getSelectedCount());
				}
				
				@Override
				public void onCustomViewBinded(SelectMode mode, View view) {
					getBinder().onActionModeViewBinded(mode, view);
				}
				
				@Override
				public boolean isActionEnabled(DataAction action) {
					if (action == DataAction.DELETE || action == DataAction.MOVE || 
						action == DataAction.SHARE || action == DataAction.DOWNLOAD) {
						return true;
					}
					return false;
				}
				
				@Override
				public boolean handleAction(SelectMode mode, DataAction action, 
						SelectManager.SelectData[] items) {
					if (action == DataAction.DOWNLOAD) {
						IActivity activity = mode != null ? 
								mode.getActionHelper().getIActivity() : null;
						if (activity != null && items != null) {
							ArrayList<ISectionData> files = new ArrayList<ISectionData>();
							
							for (SelectManager.SelectData data : items) {
								if (data != null && data instanceof ISectionData) {
									ISectionData section = (ISectionData)data;
									if (section != null && !section.isFolder())
										files.add(section);
								}
							}
							
							onActionDownload(activity, 
									files.toArray(new ISectionData[files.size()]));
						}
						mode.getActionHelper().finishSelectMode();
						return true;
					}
					return false;
				}
				
				@Override
				public void executeAction(DataAction action, SelectManager.SelectData item, 
						ActionExecutor.ProgressListener listener) throws DataException {
				}
			};
	}
	
	public boolean refreshContent(IActivity activity, boolean forceReload) {
		if (activity != null && activity instanceof IProviderActivity) {
			getSectionListDataSets().clear();
			getSectionListDataSets().setTag(null);
			if (forceReload) getSectionList().clearSectionSet();
			
			IProviderActivity menuactivity = (IProviderActivity)activity;
			menuactivity.setContentProvider(menuactivity.getCurrentProvider(), true);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onActionButtonClick(IActivity activity, int action) {
		if (activity == null || activity.isDestroyed()) return;
		if (LOG.isDebugEnabled()) LOG.debug("onActionButtonClick: action=" + action);
		
		if (action == ACTION_UPLOAD) {
			onUploadButtonClick(activity);
		} else if (action == ACTION_CREATE) {
			onCreateButtonClick(activity);
		} else if (action == ACTION_SCAN) {
			onScanButtonClick(activity);
		}
	}
	
	public void onUploadButtonClick(IActivity activity) {}
	public void onCreateButtonClick(IActivity activity) {}
	public void onScanButtonClick(IActivity activity) {}
	
	public boolean onActionDownload(IActivity activity, ISectionData[] files) { 
		return false; 
	}
	
}
