package org.javenstudio.provider.library;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderList;
import org.javenstudio.provider.library.list.LibraryFactory;
import org.javenstudio.provider.library.list.LibraryProvider;

public abstract class BaseLibraryProvider extends LibraryProvider {
	private static final Logger LOG = Logger.getLogger(BaseLibraryProvider.class);

	private ISectionList mSectionList = null;
	//private IVisibleData mFirstVisibleItem = null;
	
	public BaseLibraryProvider(AccountApp app, AccountUser account, 
			String name, int iconRes, LibraryFactory factory) { 
		super(app, account, name, iconRes, factory);
	}
	
	private boolean setSectionList(ISectionList data) {
		if (data != null && data.getLibrary() == getLibraryData()) {
			if (data == mSectionList) return false;
			mSectionList = data;
			return true;
		}
		return false;
	}
	
	@Override
	public ISectionList getSectionList() { 
		return mSectionList != null ? mSectionList : getLibraryData(); 
	}
	
	@Override
	public CharSequence getTitle() {
		return getSectionList().getName(); //super.getTitle(activity);
	}
	
	@Override
	public CharSequence getSubTitle() {
		//ISectionList data = getSectionList();
		//if (data != getLibraryData()) return getLibraryData().getName();
		return null; //super.getSubTitle();
	}
	
	@Override
	public CharSequence getDropdownTitle() { 
		return getLibraryData().getName(); 
	}
	
	@Override
	public boolean onActionHome(IActivity activity) { 
		if (changeParent(activity, true)) return true;
		return super.onActionHome(activity); 
	}
	
	@Override
	public boolean onBackPressed(IActivity activity) { 
		if (changeParent(activity, true)) return true;
		return super.onBackPressed(activity);
	}
	
	public boolean changeParent(IActivity activity, boolean refreshContent) {
		ISectionList data = getSectionList();
		if (data != null && data != getLibraryData()) {
			ISectionFolder parent = data.getParent();
			if (parent != null) 
				return setSectionList(activity, parent, refreshContent);
		}
		return false;
	}
	
	@Override
	public boolean setSectionList(IActivity activity, ISectionList data, 
			boolean refreshContent) {
		if (activity == null || data == null) return false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setSectionList: activity=" + activity + " data=" + data 
					+ " refreshContent=" + refreshContent);
		}
		
		//mFirstVisibleItem = data.getFirstVisibleItem();
		if (setSectionList(data)) {
			if (refreshContent) {
				postClearDataSets(null);
				
				if (activity instanceof IProviderActivity) {
					IProviderActivity pa = (IProviderActivity)activity;
					Provider provider = pa.getCurrentProvider();
					if (provider != null && provider instanceof ProviderList) {
						ProviderList providerList = (ProviderList)provider;
						providerList.resetActionItems();
					}
					pa.setContentProvider(provider, true);
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public int getSectionListTotalCount() { 
		ISectionList data = getSectionList();
		if (data != null) return data.getTotalCount();
		return 0; 
	}
	
	@Override
	public void setFirstVisibleItem(IVisibleData item) {
		if (item != null) {
			ISectionList data = getSectionList();
			if (data != null && item.getParent() == data) 
				data.setFirstVisibleItem(item);
		}
	}
	
	@Override
	public IVisibleData getFirstVisibleItem() { 
		ISectionList data = getSectionList();
		if (data != null) return data.getFirstVisibleItem();
		return null; 
	}
	
	//public void postSetVisibleSelection() {
	//	postSetVisibleSelection(mFirstVisibleItem);
	//}
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		ISectionList data = getSectionList();
		if (data != null) {
			long requestTime = data.getRefreshTime();
			return AppResources.getInstance().formatRefreshTime(requestTime);
		}
		return null;
	}
	
}
