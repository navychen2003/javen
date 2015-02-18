package org.javenstudio.provider.account.space;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class SpaceDataSets extends AbstractDataSets<SpaceItem> {
	private static final Logger LOG = Logger.getLogger(SpaceDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public SpaceDataSets(SpaceCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<SpaceItem> createDataSets(IDataSetCursorFactory<SpaceItem> factory) { 
		return new SpaceDataSets((SpaceCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<SpaceItem> createDataSet(IDataSetObject data) { 
		return new SpaceDataSet(this, (SpaceItem)data); 
	}
	
	public SpaceDataSet getSpaceDataSet(int position) { 
		return (SpaceDataSet)getDataSet(position); 
	}
	
	public SpaceCursor getSpaceItemCursor() { 
		return (SpaceCursor)getCursor(); 
	}
	
	public SpaceItem getSpaceItemAt(int position) { 
		SpaceDataSet dataSet = getSpaceDataSet(position); 
		if (dataSet != null) 
			return dataSet.getSpaceItem(); 
		
		return null; 
	}
	
	public void addSpaceItem(SpaceItem... item) { 
		addDataList(item);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			SpaceItem item = getSpaceItemAt(i);
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
