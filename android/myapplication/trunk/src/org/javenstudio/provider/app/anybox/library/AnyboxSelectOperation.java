package org.javenstudio.provider.app.anybox.library;

import android.view.View;

import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxLibrary;
import org.javenstudio.provider.app.anybox.AnyboxSection;
import org.javenstudio.provider.app.anybox.AnyboxSectionFolder;
import org.javenstudio.provider.library.BaseSelectOperation;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionSet;
import org.javenstudio.provider.library.SelectLibraryItem;
import org.javenstudio.provider.library.SelectSectionFileItem;
import org.javenstudio.provider.library.SelectSectionFolderItem;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.provider.library.select.SelectFolderItem;

public abstract class AnyboxSelectOperation extends BaseSelectOperation {
	private static final Logger LOG = Logger.getLogger(AnyboxSelectOperation.class);

	public AnyboxSelectOperation() {}
	
	public abstract SortType getSortType();
	public abstract FilterType getFilterType();
	
	@Override
	protected SelectLibraryItem createLibraryItem(SelectFolderItem parent, 
			ILibraryData data) {
		return new AnyboxSelectLibraryItem(this, parent, data);
	}
	
	@Override
	protected SelectSectionFolderItem createFolderItem(SelectFolderItem parent, 
			ISectionFolder data) {
		return new AnyboxSelectFolderItem(this, parent, data);
	}
	
	@Override
	protected SelectSectionFileItem createFileItem(SelectFolderItem parent, 
			ISectionData data) {
		return new AnyboxSelectFileItem(this, parent, data);
	}
	
	@Override
	protected void onFirstVisibleChanged(View view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		super.onFirstVisibleChanged(view, firstVisibleItem, 
				visibleItemCount, totalItemCount);
		
		if (firstVisibleItem + visibleItemCount >= totalItemCount) {
			SelectFolderItem folder = getCurrentFolder();
			
			int index = folder != null ? folder.getLoadIndex() : -1;
			boolean nextpage = hasNextPage(folder);
			boolean loading = isLoading();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("onFirstVisibleChanged: folder=" + folder 
						+ " loadIndex=" + index + " hasNextPage=" + nextpage 
						+ " loading=" + loading);
			}
			
			if (nextpage && !loading) 
				startLoader(folder, ReloadType.NEXTPAGE);
		}
	}
	
	@Override
	protected boolean reloadOnThread(SelectFolderItem folder, 
			ReloadType type, long reloadId) {
		if (super.reloadOnThread(folder, type, reloadId)) 
			return true;
		
		ISelectData data = folder.getData();
		if (data == null || !(data instanceof ISectionFolder))
			return false;
		
		ISectionFolder sectionFolder = (ISectionFolder)data;
		
		if (type != ReloadType.NEXTPAGE) {
			int count = 0;
			postClearData();
			
			folder.setLoadIndex(0);
			reloadDataOnThread(sectionFolder, type, reloadId);
			
			for (int i=0; i < sectionFolder.getSectionSetCount(); i++) {
				ISectionSet set = sectionFolder.getSectionSetAt(i);
				if (set != null) {
					ISectionData[] sections = set.getSections();
					postAddDataList(folder, sections);
					count += (sections != null) ? sections.length : 0;
					if (count > 0) { 
						folder.setLoadIndex(i);
						break;
					}
				}
			}
			
			if (count == 0)
				onEmptyState(folder);
			
		} else {
			ISectionSet set = null;
			int index = folder.getLoadIndex();
			
			if (index >= 0 && index < sectionFolder.getSectionSetCount()) {
				index ++;
				if (index >= 0 && index < sectionFolder.getSectionSetCount()) {
					set = sectionFolder.getSectionSetAt(index);
				}
			}
			
			if (set == null) {
				set = reloadDataOnThread(sectionFolder, type, reloadId);
				if (set != null) index = sectionFolder.getSectionSetCount() - 1;
			}
			
			if (set != null && set.getSectionFrom() > 0) {
				postAddDataList(folder, set.getSections());
				folder.setLoadIndex(index);
			}
		}
		
		return true;
	}
	
	private ISectionSet reloadDataOnThread(ISectionFolder data, 
			ReloadType type, long reloadId) {
		if (data == null) return null;
		
		SortType.Type sortType = getSortType().getSelectType();
		FilterType.Type filterType = getFilterType().getSelectType();
		
		if (data instanceof AnyboxLibrary) {
			AnyboxLibrary library = (AnyboxLibrary)data;
			if (type == ReloadType.FORCE || library.shouldReload(sortType, filterType)) {
				AnyboxLibrary.reloadSectionList(library, null, 
						reloadId, sortType, filterType);
			} else if (type == ReloadType.NEXTPAGE) {
				return AnyboxLibrary.reloadSectionListNext(library, null, 
						reloadId, filterType);
			}
		} else if (data instanceof AnyboxSectionFolder) {
			AnyboxSectionFolder folder = (AnyboxSectionFolder)data;
			if (type == ReloadType.FORCE || folder.shouldReload(sortType, filterType)) {
				AnyboxSection.reloadSectionList(folder, null, 
						reloadId, sortType, filterType);
			} else if (type == ReloadType.NEXTPAGE) {
				return AnyboxSection.reloadSectionListNext(folder, null, 
						reloadId, filterType);
			}
		}
		
		return null;
	}
	
	private boolean hasNextPage(SelectFolderItem folder) {
		if (folder == null) return false;
		
		ISelectData data = folder.getData();
		if (data == null || !(data instanceof ISectionFolder))
			return false;
		
		ISectionFolder sectionFolder = (ISectionFolder)data;
		int index = folder.getLoadIndex();
		if (index < 0) return false;
		
		if (index >= 0 && index < sectionFolder.getSectionSetCount() -1)
			return true;
		
		int count = 0;
		
		for (int i=0; i < sectionFolder.getSectionSetCount(); i++) {
			ISectionSet set = sectionFolder.getSectionSetAt(i);
			if (set != null) count += set.getSectionCount();
		}
		
		return count < sectionFolder.getTotalCount();
	}
	
}
