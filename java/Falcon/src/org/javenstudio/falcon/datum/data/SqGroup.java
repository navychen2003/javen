package org.javenstudio.falcon.datum.data;

import org.javenstudio.falcon.datum.ISectionGroup;

public class SqGroup implements ISectionGroup {

	private final String mName;
	private final String mTitle;
	
	public SqGroup(String name, String title) { 
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
		if (o == null || !(o instanceof SqGroup)) return false;
		
		SqGroup other = (SqGroup)o;
		return this.getName().equals(other.getName());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() + "}";
	}
	
}
