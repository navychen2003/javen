package org.javenstudio.provider.media.album;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class AlbumDataSets extends AbstractDataSets<AlbumItem> {
	private static final Logger LOG = Logger.getLogger(AlbumDataSets.class);

	private int mFirstVisibleItem = -1;
	
	public AlbumDataSets(AlbumCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<AlbumItem> createDataSets(IDataSetCursorFactory<AlbumItem> factory) { 
		return new AlbumDataSets((AlbumCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<AlbumItem> createDataSet(IDataSetObject data) { 
		return new AlbumDataSet(this, (AlbumItem)data); 
	}
	
	public AlbumDataSet getAlbumDataSet(int position) { 
		return (AlbumDataSet)getDataSet(position); 
	}
	
	public AlbumCursor getAlbumItemCursor() { 
		return (AlbumCursor)getCursor(); 
	}
	
	public AlbumItem getAlbumItemAt(int position) { 
		AlbumDataSet dataSet = getAlbumDataSet(position); 
		if (dataSet != null) 
			return dataSet.getAlbumItem(); 
		
		return null; 
	}
	
	public void addAlbumItem(AlbumItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			AlbumItem item = getAlbumItemAt(i);
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
