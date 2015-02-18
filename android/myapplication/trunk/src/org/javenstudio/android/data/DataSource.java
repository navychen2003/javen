package org.javenstudio.android.data;

public abstract class DataSource {

	public abstract String getPrefix();
	public abstract DataObject getDataObject(DataPath path);
	
    public DataObject getDataObject(String path) { 
    	return getDataObject(DataPath.fromString(path));
    }
    
    @Override
    public boolean equals(Object obj) { 
    	if (obj == this) return true;
    	if (obj == null || !(obj instanceof DataSource))
    		return false;
    	
    	DataSource other = (DataSource)obj;
    	return this.getPrefix().equals(other.getPrefix());
    }
    
}
