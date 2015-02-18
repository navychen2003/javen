package org.javenstudio.provider.people.group;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class GroupDataSets extends AbstractDataSets<GroupItem> {
	private static final Logger LOG = Logger.getLogger(GroupDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public GroupDataSets(GroupCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<GroupItem> createDataSets(IDataSetCursorFactory<GroupItem> factory) { 
		return new GroupDataSets((GroupCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<GroupItem> createDataSet(IDataSetObject data) { 
		return new GroupDataSet(this, (GroupItem)data); 
	}
	
	public GroupDataSet getGroupDataSet(int position) { 
		return (GroupDataSet)getDataSet(position); 
	}
	
	public GroupCursor getGroupItemCursor() { 
		return (GroupCursor)getCursor(); 
	}
	
	public GroupItem getGroupItemAt(int position) { 
		GroupDataSet dataSet = getGroupDataSet(position); 
		if (dataSet != null) 
			return dataSet.getGroupItem(); 
		
		return null; 
	}
	
	public void addGroupItem(GroupItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			GroupItem item = getGroupItemAt(i);
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
