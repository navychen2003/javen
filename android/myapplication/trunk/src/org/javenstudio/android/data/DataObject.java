package org.javenstudio.android.data;

import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class DataObject implements Comparable<DataObject>, IData {

	private final DataPath mPath;
    private final long mIdentity = ResourceHelper.getIdentity();
    protected long mDataVersion;
	
    public DataObject(DataPath path, long version) { 
    	path.setObject(this);
        mPath = path;
        mDataVersion = version;
    }
    
    public final DataPath getDataPath() { return mPath; }
    public final long getIdentity() { return mIdentity; }
    
    public final long getVersion() { return mDataVersion; }
    public long reloadData(ReloadCallback callback, ReloadType type) { return 0; }
    
    public abstract String getName();
    public abstract DataApp getDataApp();
    
    public String getLocation() { return getDataPath().toString(); }
    
	@Override
	public int compareTo(DataObject another) {
		return getDataPath().compareTo(another.getDataPath());
	}
	
	@Override
	public boolean equals(Object o) { 
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass())
			return false;
		
		DataObject other = (DataObject)o;
		return getDataPath().equals(other.getDataPath());
	}
	
    @Override
    public String toString() { 
    	return getClass().getSimpleName() + "{path=" + getDataPath() + "}";
    }
	
}
