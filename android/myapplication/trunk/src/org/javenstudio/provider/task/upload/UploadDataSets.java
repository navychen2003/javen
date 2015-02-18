package org.javenstudio.provider.task.upload;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class UploadDataSets extends AbstractDataSets<UploadItem> {
	private static final Logger LOG = Logger.getLogger(UploadDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public UploadDataSets(UploadCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<UploadItem> createDataSets(IDataSetCursorFactory<UploadItem> factory) { 
		return new UploadDataSets((UploadCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<UploadItem> createDataSet(IDataSetObject data) { 
		return new UploadDataSet(this, (UploadItem)data); 
	}
	
	public UploadDataSet getUploadDataSet(int position) { 
		return (UploadDataSet)getDataSet(position); 
	}
	
	public UploadCursor getUploadItemCursor() { 
		return (UploadCursor)getCursor(); 
	}
	
	public UploadItem getUploadItemAt(int position) { 
		UploadDataSet dataSet = getUploadDataSet(position); 
		if (dataSet != null) 
			return dataSet.getUploadItem(); 
		
		return null; 
	}
	
	public void addUploadItem(UploadItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			UploadItem item = getUploadItemAt(i);
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
