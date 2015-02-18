package org.javenstudio.cocoka.data;

public interface IMediaDetails extends LoadCallback {

	public void add(String name, CharSequence value);
	public void add(int nameRes, CharSequence value);
	
	public void clear();
	public int getCount();
	
}
