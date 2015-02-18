package org.javenstudio.falcon.datum.index;

import org.javenstudio.falcon.datum.ISectionSort;
import org.javenstudio.falcon.datum.data.SqSort;

final class SearchSort implements ISectionSort {

	private final String mName;
	private final String mTitle;
	
	public SearchSort(String name, String title) { 
		if (name == null) throw new NullPointerException();
		mName = name;
		mTitle = title;
	}
	
	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getTitle() {
		return mTitle;
	}

	@Override
	public boolean equals(Object o) { 
		if (o == this) return true;
		if (o == null || !(o instanceof SqSort)) return false;
		
		SqSort other = (SqSort)o;
		return this.getName().equals(other.getName());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() + "}";
	}
	
}
