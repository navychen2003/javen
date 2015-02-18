package org.javenstudio.provider.library.select;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class SelectListDataSets extends AbstractDataSets<SelectListItem> {
	private static final Logger LOG = Logger.getLogger(SelectListDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public SelectListDataSets() {
		this(new SelectListCursorFactory());
	}
	
	public SelectListDataSets(SelectListCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<SelectListItem> createDataSets(IDataSetCursorFactory<SelectListItem> factory) { 
		return new SelectListDataSets((SelectListCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<SelectListItem> createDataSet(IDataSetObject data) { 
		return new SelectListDataSet(this, (SelectListItem)data); 
	}
	
	public SelectListDataSet getSelectListDataSet(int position) { 
		return (SelectListDataSet)getDataSet(position); 
	}
	
	public SelectListCursor getSelectListItemCursor() { 
		return (SelectListCursor)getCursor(); 
	}
	
	public SelectListItem getSelectListItemAt(int position) { 
		SelectListDataSet dataSet = getSelectListDataSet(position); 
		if (dataSet != null) 
			return dataSet.getSelectListItem(); 
		
		return null; 
	}
	
	public void addSelectListItem(SelectListItem... items) { 
		addDataList(items);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			SelectListItem item = getSelectListItemAt(i);
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
