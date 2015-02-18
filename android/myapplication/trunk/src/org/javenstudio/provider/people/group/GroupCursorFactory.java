package org.javenstudio.provider.people.group;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class GroupCursorFactory implements IDataSetCursorFactory<GroupItem> {

	public GroupCursorFactory() {}
	
	public GroupCursor createCursor() { 
		return new GroupCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<GroupItem> create() { 
		return createCursor(); 
	}
	
}
