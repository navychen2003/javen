package org.javenstudio.provider.people.group;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class GroupCursor extends ListDataSetCursor<GroupItem> {

	public GroupCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<GroupItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof GroupDataSet) 
			addGroupDataSet((GroupDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<GroupItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof GroupDataSet) 
			addGroupDataSet((GroupDataSet)data); 
	}
	
	private void addGroupDataSet(GroupDataSet dataSet) { 
		if (dataSet != null) { 
			GroupItem data = dataSet.getGroupItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
