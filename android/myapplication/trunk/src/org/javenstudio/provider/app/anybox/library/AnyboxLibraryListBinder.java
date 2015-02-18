package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.list.LibraryFactory;
import org.javenstudio.provider.library.section.SectionListBinder;

public class AnyboxLibraryListBinder extends SectionListBinder {
	private static final Logger LOG = Logger.getLogger(AnyboxLibraryListBinder.class);

	public AnyboxLibraryListBinder(AnyboxLibraryProvider provider, 
			LibraryFactory factory) {
		super(provider, factory);
	}
	
	@Override
	protected boolean shouldLoadNextPage(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int fileCount = getProvider().getSectionListDataSets().getFileCount();
		int folderCount = getProvider().getSectionListDataSets().getFolderCount();
		int categoryCount = getProvider().getSectionListDataSets().getCategoryCount();
		int emptyCount = getProvider().getSectionListDataSets().getEmptyCount();
		int count = getProvider().getSectionListDataSets().getCount();
		int totalCount = getProvider().getSectionListTotalCount();
		
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		boolean result = (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 1) && 
				(count - categoryCount - emptyCount) < totalCount && 
				(fileCount + folderCount) < totalCount;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("shouldLoadNextPage: count=" + count 
					+ " fileCount=" + fileCount + " folderCount=" + folderCount 
					+ " categoryCount=" + categoryCount + " emptyCount=" + emptyCount 
					+ " totalCount=" + totalCount + " firstVisibleItem=" + firstVisibleItem 
					+ " visibleItemCount=" + visibleItemCount + " totalItemCount=" + totalItemCount
					+ " result=" + result);
		}
		
		return result;
	}
	
	@Override
	protected boolean isFooterViewEnabled(IActivity activity) { 
		if (getProvider().getSectionList().isRecycleBin() || 
			getProvider().getSectionList().isSearchResult()) { 
			return false;
		}
		return super.isFooterViewEnabled(activity); 
	}
	
}
