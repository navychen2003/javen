package org.javenstudio.provider.account.notify;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class NotifyDataSets extends AbstractDataSets<NotifyItem> {
	private static final Logger LOG = Logger.getLogger(NotifyDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public NotifyDataSets(NotifyCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<NotifyItem> createDataSets(IDataSetCursorFactory<NotifyItem> factory) { 
		return new NotifyDataSets((NotifyCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<NotifyItem> createDataSet(IDataSetObject data) { 
		return new NotifyDataSet(this, (NotifyItem)data); 
	}
	
	public NotifyDataSet getNotifyDataSet(int position) { 
		return (NotifyDataSet)getDataSet(position); 
	}
	
	public NotifyCursor getNotifyItemCursor() { 
		return (NotifyCursor)getCursor(); 
	}
	
	public NotifyItem getNotifyItemAt(int position) { 
		NotifyDataSet dataSet = getNotifyDataSet(position); 
		if (dataSet != null) 
			return dataSet.getNotifyItem(); 
		
		return null; 
	}
	
	public void addNotifyItem(NotifyItem... item) { 
		addDataList(item);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			NotifyItem item = getNotifyItemAt(i);
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
