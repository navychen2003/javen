package org.javenstudio.provider.people.group;

public class GroupFactory {

	public GroupDataSets createGroupDataSets(GroupListProvider p) { 
		return new GroupDataSets(new GroupCursorFactory());
	}
	
	public GroupListBinder createGroupListBinder(GroupListProvider p) { 
		return new GroupListBinder(p);
	}
	
}
