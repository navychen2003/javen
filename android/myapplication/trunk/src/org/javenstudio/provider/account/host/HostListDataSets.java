package org.javenstudio.provider.account.host;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class HostListDataSets extends AbstractDataSets<HostListItem> {
	private static final Logger LOG = Logger.getLogger(HostListDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public HostListDataSets() {
		this(new HostListCursorFactory());
	}
	
	public HostListDataSets(HostListCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<HostListItem> createDataSets(IDataSetCursorFactory<HostListItem> factory) { 
		return new HostListDataSets((HostListCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<HostListItem> createDataSet(IDataSetObject data) { 
		return new HostListDataSet(this, (HostListItem)data); 
	}
	
	public HostListDataSet getHostListDataSet(int position) { 
		return (HostListDataSet)getDataSet(position); 
	}
	
	public HostListCursor getHostListItemCursor() { 
		return (HostListCursor)getCursor(); 
	}
	
	public HostListItem getHostListItemAt(int position) { 
		HostListDataSet dataSet = getHostListDataSet(position); 
		if (dataSet != null) 
			return dataSet.getHostListItem(); 
		
		return null; 
	}
	
	public void addHostListItem(HostListItem... items) { 
		addDataList(items);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			HostListItem item = getHostListItemAt(i);
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
