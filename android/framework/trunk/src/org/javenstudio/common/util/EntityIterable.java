package org.javenstudio.common.util;

import java.util.Iterator;

public interface EntityIterable<E> extends Iterator<E> {

	public void close(); 
	public boolean isClosed(); 
	
	public int getCount(); 
	public int getPosition(); 
	public E entityAt(int position); 
	public E next(); 
	
	public boolean isFirst(); 
	public boolean isLast(); 
	public boolean hasNext(); 
	
}
