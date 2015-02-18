package org.javenstudio.provider.library.list;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class LibraryListDataSets extends AbstractDataSets<LibraryListItem> {
	private static final Logger LOG = Logger.getLogger(LibraryListDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public LibraryListDataSets() {
		this(new LibraryListCursorFactory());
	}
	
	public LibraryListDataSets(LibraryListCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<LibraryListItem> createDataSets(IDataSetCursorFactory<LibraryListItem> factory) { 
		return new LibraryListDataSets((LibraryListCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<LibraryListItem> createDataSet(IDataSetObject data) { 
		return new LibraryListDataSet(this, (LibraryListItem)data); 
	}
	
	public LibraryListDataSet getLibraryListDataSet(int position) { 
		return (LibraryListDataSet)getDataSet(position); 
	}
	
	public LibraryListCursor getLibraryListItemCursor() { 
		return (LibraryListCursor)getCursor(); 
	}
	
	public LibraryListItem getLibraryListItemAt(int position) { 
		LibraryListDataSet dataSet = getLibraryListDataSet(position); 
		if (dataSet != null) 
			return dataSet.getLibraryListItem(); 
		
		return null; 
	}
	
	public void addLibraryListItem(LibraryListItem... items) { 
		addDataList(items);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			LibraryListItem item = getLibraryListItemAt(i);
			if (item != null) 
				item.onRemove();
		}
		
		mFirstVisibleItem = -1;
		super.clear();
	}
	
	final void setFirstVisibleItem(int item) { 
		if (LOG.isDebugEnabled())
			LOG.debug("setFirstVisibleItem: firstItem=" + item);
		
		mFirstVisibleItem = item; 
	}
	
	final int getFirstVisibleItem() { return mFirstVisibleItem; }
	
}
