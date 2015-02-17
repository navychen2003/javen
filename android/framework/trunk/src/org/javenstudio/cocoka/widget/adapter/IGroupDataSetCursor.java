package org.javenstudio.cocoka.widget.adapter;

public interface IGroupDataSetCursor<T extends IExpandableObject, E extends IExpandableObject> {

	public boolean requery(); 
	public boolean isClosed(); 
	public void close(); 
	public boolean closeGroupDataSet(int groupPosition); 
	
	public int getGroupCount(); 
	public long getGroupId(int groupPosition); 
	public AbstractGroupDataSet<T, E> getGroupDataSet(int groupPosition);
	public boolean hasStableIds();
	
}
