package org.javenstudio.provider.media.photo;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class PhotoDataSets extends AbstractDataSets<PhotoItem> {
	private static final Logger LOG = Logger.getLogger(PhotoDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public PhotoDataSets(PhotoCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<PhotoItem> createDataSets(IDataSetCursorFactory<PhotoItem> factory) { 
		return new PhotoDataSets((PhotoCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<PhotoItem> createDataSet(IDataSetObject data) { 
		return new PhotoDataSet(this, (PhotoItem)data); 
	}
	
	public PhotoDataSet getPhotoDataSet(int position) { 
		return (PhotoDataSet)getDataSet(position); 
	}
	
	public PhotoCursor getPhotoItemCursor() { 
		return (PhotoCursor)getCursor(); 
	}
	
	public PhotoItem getPhotoItemAt(int position) { 
		PhotoDataSet dataSet = getPhotoDataSet(position); 
		if (dataSet != null) 
			return dataSet.getPhotoItem(); 
		
		return null; 
	}
	
	public void addPhotoItem(PhotoItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			PhotoItem item = getPhotoItemAt(i);
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
