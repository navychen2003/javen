package org.javenstudio.provider.account.dashboard;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class DashboardDataSets extends AbstractDataSets<DashboardItem> {
	private static final Logger LOG = Logger.getLogger(DashboardDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public DashboardDataSets(DashboardCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<DashboardItem> createDataSets(IDataSetCursorFactory<DashboardItem> factory) { 
		return new DashboardDataSets((DashboardCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<DashboardItem> createDataSet(IDataSetObject data) { 
		return new DashboardDataSet(this, (DashboardItem)data); 
	}
	
	public DashboardDataSet getDashboardDataSet(int position) { 
		return (DashboardDataSet)getDataSet(position); 
	}
	
	public DashboardCursor getDashboardItemCursor() { 
		return (DashboardCursor)getCursor(); 
	}
	
	public DashboardItem getDashboardItemAt(int position) { 
		DashboardDataSet dataSet = getDashboardDataSet(position); 
		if (dataSet != null) 
			return dataSet.getDashboardItem(); 
		
		return null; 
	}
	
	public void addDashboardItem(DashboardItem... item) { 
		addDataList(item);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			DashboardItem item = getDashboardItemAt(i);
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
