package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;

public interface PicasaSource {

	public DataApp getDataApp();
	public String getTopSetLocation();
	
	public SystemUser getAccount();
	public int getSourceIconRes();
	
	public long reloadData(final ReloadCallback callback, 
    		ReloadType type);
	
}
