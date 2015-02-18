package org.javenstudio.falcon.setting.cluster;

import org.javenstudio.falcon.user.IUserName;

public interface IHostUserName extends IUserName {

	public IHostInfo getHostNode();
	public IHostUserData getHostUserData();
	
}
