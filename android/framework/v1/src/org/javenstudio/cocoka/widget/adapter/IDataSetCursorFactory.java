package org.javenstudio.cocoka.widget.adapter;

public interface IDataSetCursorFactory<T extends IDataSetObject> {

	public IDataSetCursor<T> create(); 
	
}
