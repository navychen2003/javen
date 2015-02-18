package org.javenstudio.android.data;

import org.javenstudio.android.app.SelectManager;

public interface IData extends SelectManager.SelectData {

    public String getName();
    public DataApp getDataApp();
	public DataPath getDataPath();
	
	public long getVersion();
	public long getIdentity();
	public String getLocation();
	
}
