package org.javenstudio.cocoka.widget.adapter;

public interface IDataSetCursor<T extends IDataSetObject> {

	public boolean refresh(); 
	public boolean requery(); 
	
	public boolean isClosed(); 
	public void close(); 
	public int getCount(); 
	public void recycle(); 
	
	public AbstractDataSet<T> getDataSet(int position); 
	public int getDataId(int position); 
	public void setDataSet(int position, AbstractDataSet<T> data); 
	public void addDataSet(AbstractDataSet<T> data); 
	public void clear(); 
	
}
