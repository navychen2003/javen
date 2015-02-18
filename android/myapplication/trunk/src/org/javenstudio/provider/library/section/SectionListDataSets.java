package org.javenstudio.provider.library.section;

import org.javenstudio.android.app.SelectManager;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.IVisibleData;

public class SectionListDataSets extends AbstractDataSets<SectionListItem> {
	private static final Logger LOG = Logger.getLogger(SectionListDataSets.class);
	
	private final SectionListProvider mProvider;
	private final SelectManager mSelectManager;
	
	private int mFileCount = 0;
	private int mFolderCount = 0;
	private int mCategoryCount = 0;
	private int mEmptyCount = 0;
	private int mSectionSetIndex = 0;
	
	public SectionListDataSets(SectionListProvider provider) {
		this(provider, new SectionListCursorFactory());
	}
	
	public SectionListDataSets(SectionListProvider provider, SectionListCursorFactory factory) {
		super(factory); 
		mProvider = provider;
		mSelectManager = factory.createSelectManager();
	}
	
	public SectionListProvider getProvider() {
		return mProvider;
	}
	
	public SelectManager getSelectManager() {
		return mSelectManager;
	}
	
	@Override 
	protected AbstractDataSets<SectionListItem> createDataSets(IDataSetCursorFactory<SectionListItem> factory) { 
		return new SectionListDataSets(getProvider(), (SectionListCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<SectionListItem> createDataSet(IDataSetObject data) { 
		return new SectionListDataSet(this, (SectionListItem)data); 
	}
	
	@Override 
	protected void onDataSetPreAdd(AbstractDataSet<SectionListItem> dataSet) {
		if (dataSet == null) return;
		
		SectionListItem item = dataSet.getData();
		if (item != null) {
			if (item instanceof SectionFolderItem) {
				boolean addCategory = true;
				if (getCount() > 0) {
					SectionListItem preItem = getSectionListItemAt(getCount() -1);
					if (preItem != null && (preItem instanceof SectionFolderItem || preItem instanceof SectionCategoryItem))
						addCategory = false;
				}
				if (addCategory) {
					addFolderCategory(item);
				}
			} else if (item instanceof SectionFileItem) {
				boolean addCategory = true;
				if (getCount() > 0) {
					SectionListItem preItem = getSectionListItemAt(getCount() -1);
					if (preItem != null && (preItem instanceof SectionFileItem || preItem instanceof SectionCategoryItem))
						addCategory = false;
				}
				if (addCategory) {
					addFileCategory(item);
				}
			}
		}
	}
	
	private void addFolderCategory(SectionListItem item) {
		int columnSize = getProvider().getBinder().getColumnSize();
		if (columnSize > 1 && getCount() > 0) {
			int count = columnSize - (getCount() % columnSize);
			if (count > 0 && count < columnSize) {
				for (int i=0; i < count; i++) {
					addEmptyItem(item);
				}
			}
		}
		for (int i=0; i < columnSize; i++) {
			if (i == 0) {
				addCategoryItem(item, true);
			} else {
				addEmptyItem(item);
			}
		}
	}
	
	private void addFileCategory(SectionListItem item) {
		int columnSize = getProvider().getBinder().getColumnSize();
		if (columnSize > 1 && getCount() > 0) {
			int count = columnSize - (getCount() % columnSize);
			if (count > 0 && count < columnSize) {
				for (int i=0; i < count; i++) {
					addEmptyItem(item);
				}
			}
		}
		for (int i=0; i < columnSize; i++) {
			if (i == 0) {
				addCategoryItem(item, false);
			} else {
				addEmptyItem(item);
			}
		}
	}
	
	private void addCategoryItem(SectionListItem item, boolean isFolder) {
		ISectionList folder = item.getProvider().getSectionList();
		if (folder != null) {
			SectionCategoryItem categoryItem = isFolder ? 
					new SectionCategoryItem(item.getProvider(), folder.getFolderCategory()) : 
					new SectionCategoryItem(item.getProvider(), folder.getFileCategory());
			addData(categoryItem, false);
			mCategoryCount ++;
		} else {
			addEmptyItem(item);
		}
	}
	
	private void addEmptyItem(SectionListItem item) {
		SectionEmptyItem emptyItem = new SectionEmptyItem(item.getProvider());
		addData(emptyItem, false);
		mEmptyCount ++;
	}
	
	public SectionListDataSet getSectionListDataSet(int position) { 
		return (SectionListDataSet)getDataSet(position); 
	}
	
	public SectionListCursor getSectionListItemCursor() { 
		return (SectionListCursor)getCursor(); 
	}
	
	public SectionListItem getSectionListItemAt(int position) { 
		SectionListDataSet dataSet = getSectionListDataSet(position); 
		if (dataSet != null) 
			return dataSet.getSectionListItem(); 
		
		return null; 
	}
	
	public void addSectionListItem(SectionListItem... items) { 
		addDataList(items);
		if (items != null) {
			for (SectionListItem item : items) {
				if (item == null) continue;
				if (item instanceof SectionFolderItem)
					mFolderCount ++;
				else if (item instanceof SectionFileItem)
					mFileCount ++;
				else if (item instanceof SectionCategoryItem)
					mCategoryCount ++;
			}
		}
	}
	
	private IVisibleData findFirstVisibleData(int fromPos) {
		if (fromPos < 0) fromPos = 0;
		
		for (int i=fromPos; i < getCount(); i++) {
			SectionListItem item = getSectionListItemAt(i);
			if (item == null) continue;
			
			if (item instanceof SectionFileItem) {
				SectionFileItem fileItem = (SectionFileItem)item;
				return fileItem.getSectionData();
				
			} else if (item instanceof SectionFolderItem) {
				SectionFolderItem folderItem = (SectionFolderItem)item;
				return folderItem.getSectionData();
				
			} else if (item instanceof SectionCategoryItem) {
				SectionCategoryItem categoryItem = (SectionCategoryItem)item;
				return categoryItem.getData();
			}
		}
		
		return null;
	}
	
	public IVisibleData setFirstVisibleItem(int item) {
		IVisibleData data = item > 0 ? findFirstVisibleData(item -1) : null;
		if (data == null) return data;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("setFirstVisibleItem: item=" + item + " data=" + data);
		
		getProvider().setFirstVisibleItem(data);
		return data;
	}
	
	public int getFirstVisibleItem() {
		IVisibleData data = getProvider().getFirstVisibleItem();
		int item = findVisiblePosition(data, false) + 1;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("getFirstVisibleItem: item=" + item + " data=" + data);
		
		return item;
	}
	
	public int findVisiblePosition(IVisibleData data, boolean categoryOp) {
		if (data != null) {
			SectionListItem posItem = null;
			int pos = -1;
			
			for (int i=0; i < getCount(); i++) {
				SectionListItem item = getSectionListItemAt(i);
				if (item == null) continue;
				
				if (item instanceof SectionFileItem) {
					SectionFileItem fileItem = (SectionFileItem)item;
					if (fileItem.getSectionData() == data) {
						pos = i; posItem = item;
						break;
					}
				} else if (item instanceof SectionFolderItem) {
					SectionFolderItem folderItem = (SectionFolderItem)item;
					if (folderItem.getSectionData() == data) {
						pos = i; posItem = item;
						break;
					}
				} else if (item instanceof SectionCategoryItem) {
					SectionCategoryItem categoryItem = (SectionCategoryItem)item;
					if (categoryItem.getData() == data) {
						pos = i; posItem = item;
						break;
					}
				}
			}
			
			if (categoryOp && posItem != null && 
				!(posItem instanceof SectionCategoryItem) && pos > 0) {
				for (int i=pos-1; i >= 0; i--) {
					SectionListItem item = getSectionListItemAt(i);
					if (item == null) continue;
					
					if (item instanceof SectionFileItem) {
						break;
					} else if (item instanceof SectionFolderItem) {
						break;
					} else if (item instanceof SectionCategoryItem) {
						pos = i; posItem = item;
						break;
					} else {
						continue;
					}
				}
			}
			
			return pos;
		}
		
		return -1;
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			SectionListItem item = getSectionListItemAt(i);
			if (item != null) 
				item.onRemove();
		}
		
		mFileCount = 0;
		mFolderCount = 0;
		mCategoryCount = 0;
		mEmptyCount = 0;
		mSectionSetIndex = 0;
		mSelectManager.clearSelectedItems();
		
		super.clear();
	}
	
	public int getFileCount() { return mFileCount; }
	public int getFolderCount() { return mFolderCount; }
	public int getCategoryCount() { return mCategoryCount; }
	public int getEmptyCount() { return mEmptyCount; }
	
	public int getSectionSetIndex() { return mSectionSetIndex; }
	public void setSectionSetIndex(int idx) { mSectionSetIndex = idx; }
	
}
