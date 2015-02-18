package org.javenstudio.common.util;

public interface EntityCursor<E> extends EntityIterable<E> {

	public boolean requery(); 
	public boolean isClosed(); 
	public void close(); 
	
	public int getCount(); 
	public int getPosition(); 
	public E entityAt(int position); 
	public E next(); 
	
	public boolean move(int offset);
	public boolean moveToPosition(int position);
	public boolean moveToFirst();
	public boolean moveToLast();
	public boolean moveToNext();
	public boolean moveToPrevious();
	
	public boolean isFirst(); 
	public boolean isLast(); 
	public boolean hasNext(); 
	
}
