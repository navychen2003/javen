package org.javenstudio.provider.library.list;

import java.util.ArrayList;

import android.content.Context;
import android.widget.SpinnerAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.IActionAdapterFactory;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionItem;
import org.javenstudio.provider.ProviderList;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.ISectionSearch;

public class LibrariesProvider extends ProviderList {
	private static final Logger LOG = Logger.getLogger(LibrariesProvider.class);

	public LibrariesProvider(String name, int iconRes) { 
		super(name, iconRes);
	}
	
	@Override
	public IActionAdapterFactory getActionAdapterFactory(IActivity activity) {
		return mFactory;
	}
	
	private final IActionAdapterFactory mFactory = 
		new IActionAdapterFactory() {
			@Override
			public SpinnerAdapter createActionAdapter(Context context,
					ActionItem[] items) {
				return new LibraryActionAdapter(context, items);
			}
		};
	
	@Override
	public void onActionItemsInited(IActionBar actionBar, ActionItem[] items) {
		if (actionBar == null || items == null) return;
		
		LibraryProvider provider = (LibraryProvider)getSelectProvider();
		ISectionList sectionList = provider != null ? provider.getSectionList() : null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onActionItemsInited: actionBar=" + actionBar 
					+ " sectionList=" + sectionList);
		}
		
		for (int i=0; items != null && i < items.length; i++) {
			ActionItem item = items[i];
			if (item != null && item instanceof LibraryActionItem) {
				LibraryActionItem actionItem = (LibraryActionItem)item;
				ISectionList folder = actionItem.getSectionList();
				if (sectionList != null && sectionList == folder) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("onActionItemsInited: setSelectedNavigationItem: position=" 
								+ i + " item=" + item);
					}
					actionBar.setSelectedNavigationItem(i);
					return;
				}
			}
		}
	}
		
	@Override
	protected ProviderActionItem[] createActionItems(final IActivity activity) {
		ProviderActionItem[] items = super.createActionItems(activity);
		if (LOG.isDebugEnabled()) {
			LOG.debug("createActionItems: activity=" + activity 
					+ " provider=" + this);
		}
		
		ArrayList<ProviderActionItem> itemList = new ArrayList<ProviderActionItem>();
		
		LibraryProvider provider = (LibraryProvider)getSelectProvider();
		ISectionList sectionList = provider != null ? provider.getSectionList() : null;
		
		if (sectionList != null && sectionList instanceof ISectionFolder) {
			if (!(sectionList instanceof ILibraryData) && 
				!(sectionList instanceof ISectionSearch)) {
				ISectionFolder folder = (ISectionFolder)sectionList;
				boolean folderCategoryAdded = false;
				
				while (folder != null) {
					LibraryActionItem item = createLibraryActionItem(activity, provider, folder);
					if (item != null) {
						if (!folderCategoryAdded) {
							itemList.add(createFolderActionCategory(activity));
							folderCategoryAdded = true;
						}
						itemList.add(item);
					}
					
					folder = folder.getParent();
					if (folder == null || folder instanceof ILibraryData)
						break;
				}
			}
		} else if (sectionList != null && sectionList instanceof ISectionSearch) {
			if (!(sectionList instanceof ILibraryData) && 
				!(sectionList instanceof ISectionFolder)) {
				
				ISectionSearch search = (ISectionSearch)sectionList;
				ISectionSearch[] searches = search.getSearches();
				boolean searchCategoryAdded = false;
				int searchIndex = 0;
				
				while (search != null) {
					LibraryActionItem item = createLibraryActionItem(activity, provider, search);
					if (item != null) {
						if (!searchCategoryAdded) {
							itemList.add(createSearchActionCategory(activity));
							searchCategoryAdded = true;
						}
						itemList.add(item);
					}
					search = null;
					
					for (int i=searchIndex; searches != null && i < searches.length; i++) {
						ISectionSearch s = searches[i];
						if (s != null && s != sectionList) {
							searchIndex = i + 1;
							search = s; break;
						}
					}
					
					if (search == null || search instanceof ILibraryData)
						break;
				}
			}
		}
		
		if (items != null) {
			boolean libraryCategoryAdded = false;
			for (ProviderActionItem item : items) {
				if (item != null) { 
					if (!libraryCategoryAdded) {
						itemList.add(createLibraryActionCategory(activity));
						libraryCategoryAdded = true;
					}
					itemList.add(item);
				}
			}
		}
		
		return itemList.toArray(new ProviderActionItem[itemList.size()]);
	}
	
	@Override
	protected ProviderActionItem createActionItem(final IActivity activity, 
			final Provider item) {
		LibraryProvider library = (LibraryProvider)item;
		return createLibraryActionItem(activity, library, library.getLibraryData());
	}
	
	protected LibraryActionItem createLibraryActionItem(final IActivity activity, 
			final LibraryProvider item, final ISectionList folder) {
		return new LibraryActionItem(this, item, folder, activity);
	}
	
	protected LibraryActionCategory createLibraryActionCategory(final IActivity activity) {
		return new LibraryActionCategory(this, 
				ResourceHelper.getResources().getString(R.string.library_category_label));
	}
	
	protected LibraryActionCategory createFolderActionCategory(final IActivity activity) {
		return new LibraryActionCategory(this, 
				ResourceHelper.getResources().getString(R.string.folder_category_label));
	}
	
	protected LibraryActionCategory createSearchActionCategory(final IActivity activity) {
		return new LibraryActionCategory(this, 
				ResourceHelper.getResources().getString(R.string.search_category_label));
	}
	
}
