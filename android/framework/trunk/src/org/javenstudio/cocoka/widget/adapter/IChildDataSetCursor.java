package org.javenstudio.cocoka.widget.adapter;

public interface IChildDataSetCursor<T extends IExpandableObject, E extends IExpandableObject> {

	public boolean requery(); 
	public boolean isClosed(); 
	public void close(); 
	
	public int getChildrenCount();
	public long getChildId(int childPosition); 
	public AbstractChildDataSet<T, E> getChildDataSet(int childPosition);
	public boolean isChildSelectable(int childPosition);
	
}
