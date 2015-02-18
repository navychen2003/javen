package org.javenstudio.falcon.datum;

public abstract class StoreInfo {

	private final String mScheme;
	private final String mName;
	private final String mLocation;
	
	public StoreInfo(String scheme, String name, String location) { 
		if (scheme == null || name == null || location == null)
			throw new NullPointerException();
		mScheme = scheme;
		mName = name;
		mLocation = location;
	}
	
	public String getScheme() { return mScheme; }
	public String getName() { return mName; }
	public String getStoreUri() { return mLocation; }
	
	public abstract long getUsedSpace();
	public abstract long getUsableSpace();
	public abstract long getCapacitySpace();
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof StoreInfo)) return false;
		StoreInfo other = (StoreInfo)obj;
		return this.mName.equals(other.mName);
	}
	
	@Override
	public String toString() { 
		return mName;
	}
	
}
