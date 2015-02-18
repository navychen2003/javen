package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxDownloader;
import org.javenstudio.provider.app.anybox.AnyboxLibrary;
import org.javenstudio.provider.app.anybox.AnyboxSection;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.app.anybox.AnyboxSectionFolder;
import org.javenstudio.provider.app.anybox.AnyboxSectionSearch;
import org.javenstudio.provider.app.anybox.AnyboxSectionSet;
import org.javenstudio.provider.library.BaseLibraryProvider;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.ISectionSearch;
import org.javenstudio.provider.library.list.LibraryFactory;
import org.javenstudio.provider.library.section.SectionListItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxLibraryProvider extends BaseLibraryProvider {
	private static final Logger LOG = Logger.getLogger(AnyboxLibraryProvider.class);

	private final AnyboxLibrary mLibraryData;
	
	public AnyboxLibraryProvider(AnyboxAccount account, ILibraryData data,
			String name, int iconRes, LibraryFactory factory) { 
		super(account.getApp(), account, name, iconRes, factory);
		mLibraryData = (AnyboxLibrary)data;
	}
	
	public AnyboxLibrary getLibraryData() { return mLibraryData; }
	
	@Override
	public AnyboxApp getAccountApp() {
		return (AnyboxApp)super.getAccountApp();
	}
	
	@Override
	public AnyboxAccount getAccountUser() {
		return (AnyboxAccount)super.getAccountUser();
	}
	
	@Override
	public SelectOperation getSelectOperation() {
		return getAccountUser().getSelectOperation();
	}
	
	@Override
	public String getSearchText() {
		ISectionList list = getSectionList();
		if (list != null && list instanceof ISectionSearch) {
			ISectionSearch search = (ISectionSearch)list;
			return search.getQueryText();
		}
		ISectionSearch search = getLibraryData().getLastSearch();
		if (search != null) return search.getQueryText();
		return null; //getSectionSearch().getQueryText();
	}
	
	@Override
	public boolean setSearchList(IActivity activity, String query) {
		if (activity != null && query != null && query.length() > 0) {
			AnyboxSectionSearch search = getLibraryData().getSectionSearch(query);
			if (search != null) {
				if (LOG.isDebugEnabled())
					LOG.debug("setSearchList: search=" + search + " query=" + query);
				
				getSectionListDataSets().setTag(null);
				return setSectionList(activity, search, true);
			}
		}
		return false;
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		ISectionList data = getSectionList();
		if (data != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("reloadOnThread: callback=" + callback + " type=" + type 
						+ " reloadId=" + reloadId + " data=" + data);
			}
			
			if (data != getSectionListDataSets().getTag() || type == ReloadType.FORCE) {
	    		if (data instanceof AnyboxLibrary) {
	    			reloadLibraryItems(callback, type, reloadId, (AnyboxLibrary)data);
	    		} else if (data instanceof AnyboxSectionFolder){
	    			reloadSectionItems(callback, type, reloadId, (AnyboxSectionFolder)data);
	    		} else if (data instanceof AnyboxSectionSearch) {
	    			reloadSectionItems(callback, type, reloadId, (AnyboxSectionSearch)data);
	    		}
			} else if (type == ReloadType.NEXTPAGE) {
				if (data instanceof AnyboxLibrary) {
	    			nextLibraryItems(callback, type, reloadId, (AnyboxLibrary)data);
	    		} else if (data instanceof AnyboxSectionFolder){
	    			nextSectionItems(callback, type, reloadId, (AnyboxSectionFolder)data);
	    		} else if (data instanceof AnyboxSectionSearch) {
	    			nextSectionItems(callback, type, reloadId, (AnyboxSectionSearch)data);
	    		}
			} else if (type == ReloadType.DEFAULT) {
				postSetVisibleSelection(data.getFirstVisibleItem());
			}
		}
	}
	
	private void reloadLibraryItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxLibrary data) {
		SortType.Type sortType = getFactory().getSortType().getSelectType();
		FilterType.Type filterType = getFactory().getFilterType().getSelectType();
		
		if (type == ReloadType.FORCE || data.shouldReload(sortType, filterType)) {
			AnyboxLibrary.reloadSectionList(data, callback, reloadId, 
					sortType, filterType);
		}
		
		postClearDataSets(callback);
		
		int count = 0;
		for (int i=0; i < data.getSectionSetCount(); i++) {
			AnyboxSectionSet set = data.getSectionSetAt(i);
			if (set != null) {
				count += addSectionItems(callback, set.getSections(), i);
				if (count > 0) {
					getSectionListDataSets().setSectionSetIndex(i);
					break;
				}
			}
		}
		
		if (count <= 0) postAddSectionEmptyItem(callback);
		postSetVisibleSelection(data.getFirstVisibleItem());
		//postNotifyChanged(callback);
		
		getSectionListDataSets().setTag(data);
	}
	
	private void reloadSectionItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxSectionFolder data) {
		SortType.Type sortType = getFactory().getSortType().getSelectType();
		FilterType.Type filterType = getFactory().getFilterType().getSelectType();
		
		if (type == ReloadType.FORCE || data.shouldReload(sortType, filterType)) {
			AnyboxSection.reloadSectionList(data, callback, reloadId, 
					sortType, filterType);
		}
		
		postClearDataSets(callback);
		
		int count = 0;
		for (int i=0; i < data.getSectionSetCount(); i++) {
			AnyboxSectionSet set = data.getSectionSetAt(i);
			if (set != null) {
				count += addSectionItems(callback, set.getSections(), i);
				if (count > 0) {
					getSectionListDataSets().setSectionSetIndex(i);
					break;
				}
			}
		}
		
		if (count <= 0) postAddSectionEmptyItem(callback);
		postSetVisibleSelection(data.getFirstVisibleItem());
		//postNotifyChanged(callback);
		
		getSectionListDataSets().setTag(data);
	}
	
	private void reloadSectionItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxSectionSearch data) {
		SortType.Type sortType = getFactory().getSortType().getSelectType();
		FilterType.Type filterType = getFactory().getFilterType().getSelectType();
		
		if (type == ReloadType.FORCE || data.shouldReload(sortType, filterType)) {
			AnyboxSectionSearch.reloadSectionList(data, callback, reloadId, 
					sortType, filterType);
		}
		
		postClearDataSets(callback);
		
		int count = 0;
		for (int i=0; i < data.getSectionSetCount(); i++) {
			AnyboxSectionSet set = data.getSectionSetAt(i);
			if (set != null) {
				count += addSectionItems(callback, set.getSections(), i);
				if (count > 0) {
					getSectionListDataSets().setSectionSetIndex(i);
					break;
				}
			}
		}
		
		if (count <= 0) postAddSectionEmptyItem(callback);
		postSetVisibleSelection(data.getFirstVisibleItem());
		//postNotifyChanged(callback);
		
		getSectionListDataSets().setTag(data);
	}
	
	private void nextLibraryItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxLibrary data) {
		if (type == ReloadType.NEXTPAGE) {
			AnyboxSectionSet set = null;
			
			int index = getSectionListDataSets().getSectionSetIndex();
			if (index >= 0 && index < data.getSectionSetCount()) {
				index ++;
				if (index >= 0 && index < data.getSectionSetCount()) {
					set = data.getSectionSetAt(index);
				}
			}
			
			if (set == null) {
				set = AnyboxLibrary.reloadSectionListNext(data, callback, reloadId, 
						getFactory().getFilterType().getSelectType());
				if (set != null) index = data.getSectionSetCount() - 1;
			}
			
			if (set != null && set.getSectionFrom() > 0)
				addSectionItems(callback, set.getSections(), index);
		}
	}
	
	private void nextSectionItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxSectionFolder data) {
		if (type == ReloadType.NEXTPAGE) {
			AnyboxSectionSet set = null;
			
			int index = getSectionListDataSets().getSectionSetIndex();
			if (index >= 0 && index < data.getSectionSetCount()) {
				index ++;
				if (index >= 0 && index < data.getSectionSetCount()) {
					set = data.getSectionSetAt(index);
				}
			}
			
			if (set == null) {
				set = AnyboxSection.reloadSectionListNext(data, callback, reloadId, 
						getFactory().getFilterType().getSelectType());
				if (set != null) index = data.getSectionSetCount() - 1;
			}
			
			if (set != null && set.getSectionFrom() > 0)
				addSectionItems(callback, set.getSections(), index);
		}
	}
	
	private void nextSectionItems(ProviderCallback callback, 
			ReloadType type, long reloadId, AnyboxSectionSearch data) {
		if (type == ReloadType.NEXTPAGE) {
			AnyboxSectionSet set = null;
			
			int index = getSectionListDataSets().getSectionSetIndex();
			if (index >= 0 && index < data.getSectionSetCount()) {
				index ++;
				if (index >= 0 && index < data.getSectionSetCount()) {
					set = data.getSectionSetAt(index);
				}
			}
			
			if (set == null) {
				set = AnyboxSectionSearch.reloadSectionListNext(data, callback, reloadId, 
						getFactory().getFilterType().getSelectType());
				if (set != null) index = data.getSectionSetCount() - 1;
			}
			
			if (set != null && set.getSectionFrom() > 0)
				addSectionItems(callback, set.getSections(), index);
		}
	}
	
	private int addSectionItems(ProviderCallback callback, 
			AnyboxSection[] sections, int index) {
		int count = 0;
		if (sections != null) {
			ArrayList<SectionListItem> items = new ArrayList<SectionListItem>();
			
			for (AnyboxSection section : sections) {
				if (section == null) continue;
				if (LOG.isDebugEnabled())
					LOG.debug("addSectionItems: setindex=" + index + " section=" + section);
				
				addSectionItem(items, section);
				
				if (items.size() > 10) {
					postAddSectionListItem(callback, 
							items.toArray(new SectionListItem[items.size()]));
					count += items.size();
					items.clear();
				}
			}
			
			postAddSectionListItem(callback, 
					items.toArray(new SectionListItem[items.size()]));
			
			getSectionListDataSets().setSectionSetIndex(index);
			count += items.size();
		}
		return count;
	}
	
	protected void addSectionItem(List<SectionListItem> items, AnyboxSection data) {
		if (items == null || data == null) return;
		
		if (data instanceof AnyboxSectionFolder) {
			items.add(createFolderItem((AnyboxSectionFolder)data));
		} else if (data instanceof AnyboxSectionFile) {
			items.add(createFileItem((AnyboxSectionFile)data));
		}
	}
	
	protected SectionListItem createFolderItem(AnyboxSectionFolder data) {
		return new AnyboxSectionFolderItem(this, data);
	}
	
	protected SectionListItem createFileItem(AnyboxSectionFile data) {
		return new AnyboxSectionFileItem(this, data);
	}
	
	@Override
	public boolean onActionDownload(IActivity activity, ISectionData[] items) { 
		if (activity == null || items == null) return false;
		if (items != null && items.length > 0) {
			if (LOG.isDebugEnabled()) LOG.debug("onActionDownload: itemCount=" + items.length);
			ArrayList<IDownloadable> files = new ArrayList<IDownloadable>();
			for (ISectionData data : items) {
				if (data != null && data instanceof IDownloadable)
					files.add((IDownloadable)data);
			}
			return AnyboxDownloader.download(activity.getActivity(), getAccountUser(), 
					files.toArray(new IDownloadable[files.size()]));
		}
		return false; 
	}
	
	@Override
	public void onUploadButtonClick(IActivity activity) {
		if (activity == null) return;
		ISectionList list = getSectionList();
		if (list != null && list instanceof ISectionFolder) {
			getSelectOperation().showSelectDialog(activity.getActivity(), 
					new AnyboxSelectUpload(getAccountUser(), (ISectionFolder)list));
		}
	}
	
	@Override
	public void onCreateButtonClick(IActivity activity) {
		if (activity == null) return;
		
		ISectionFolder folder = null;
		ISectionList list = getSectionList();
		
		if (list != null && list instanceof ISectionFolder) 
			folder = (ISectionFolder)list;
		
		getSelectCreate().showSelectDialog(activity, folder);
	}
	
	@Override
	public void onScanButtonClick(IActivity activity) {
		if (activity == null) return;
		
		ISectionFolder folder = null;
		ISectionList list = getSectionList();
		
		if (list != null && list instanceof ISectionFolder) 
			folder = (ISectionFolder)list;
		
		getSelectScan().showSelectDialog(activity, folder);
	}
	
	protected AnyboxSelectCreate getSelectCreate() {
		return new AnyboxSelectCreate(getAccountUser());
	}
	
	protected AnyboxSelectScan getSelectScan() {
		return new AnyboxSelectScan(getAccountUser());
	}
	
}
