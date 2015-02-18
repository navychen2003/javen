package org.javenstudio.cocoka.widget.adapter;

public interface IGroupDataSetCursorFactory<T extends IExpandableObject, E extends IExpandableObject> {

	public IGroupDataSetCursor<T, E> createGroupCursor(); 
	public IChildDataSetCursor<T, E> createChildCursor(AbstractGroupDataSet<T, E> groupDataSet); 
	
}
