package org.javenstudio.provider.task.download;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class DownloadDataSets extends AbstractDataSets<DownloadItem> {
	private static final Logger LOG = Logger.getLogger(DownloadDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public DownloadDataSets(DownloadCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<DownloadItem> createDataSets(IDataSetCursorFactory<DownloadItem> factory) { 
		return new DownloadDataSets((DownloadCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<DownloadItem> createDataSet(IDataSetObject data) { 
		return new DownloadDataSet(this, (DownloadItem)data); 
	}
	
	public DownloadDataSet getDownloadDataSet(int position) { 
		return (DownloadDataSet)getDataSet(position); 
	}
	
	public DownloadCursor getDownloadItemCursor() { 
		return (DownloadCursor)getCursor(); 
	}
	
	public DownloadItem getDownloadItemAt(int position) { 
		DownloadDataSet dataSet = getDownloadDataSet(position); 
		if (dataSet != null) 
			return dataSet.getDownloadItem(); 
		
		return null; 
	}
	
	public void addDownloadItem(DownloadItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			DownloadItem item = getDownloadItemAt(i);
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
