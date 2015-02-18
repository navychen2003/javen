package org.javenstudio.falcon.setting.cluster;

import org.javenstudio.falcon.user.IMember;

public interface IAuthInfo {

	public IMember getUser();
	public IAttachUserInfo getAttachUser();
	public IHostInfo getAttachHost();
	
}
