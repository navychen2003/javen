package org.javenstudio.cocoka.database;

public interface SQLiteCursorObserver {

	public void notifyDataSetChanged(); 
	public boolean requery(); 
	
}
