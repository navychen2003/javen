package org.javenstudio.falcon.user;

import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.util.StringUtils;

public final class UserName implements IUserName {

	private final String mUserName;
	private final String mName;
	private final String mHostKey;
	private final String mDomain;
	
	private UserName(String username, 
			String name, String hostkey, String domain) {
		if (username == null || name == null) 
			throw new NullPointerException();
		mUserName = username;
		mName = name;
		mHostKey = hostkey;
		mDomain = domain;
	}
	
	public String getUserName() { return mUserName; }
	public String getName() { return mName; }
	public String getHostKey() { return mHostKey; }
	public String getDomain() { return mDomain; }
	
	@Override
	public String toString() {
		return "UserName{username=" + mUserName + ",name=" + mName 
				+ ",hostkey=" + mHostKey + ",domain=" + mDomain + "}";
	}
	
	public static UserName parse(IHostCluster cluster, String fullname) {
		fullname = StringUtils.trim(fullname);
		
		if (fullname == null || fullname.length() == 0)
			return null;
		
		fullname = fullname.toLowerCase();
		
		String name = fullname;
		String hostkey = null;
		String domain = null;
		
		int pos = fullname.indexOf('@');
		if (pos > 0) {
			name = fullname.substring(0, pos);
			domain = fullname.substring(pos+1);
		}
		
		int pos2 = name.indexOf('#');
		if (pos2 > 0) {
			hostkey = name.substring(pos2+1);
			name = name.substring(0, pos2);
		}
		
		String username = name;
		if (domain != null && domain.length() > 0) {
			if (cluster == null || !domain.equals(cluster.getMailDomain()))
				username = name + '@' + domain;
		}
		
		return new UserName(username, name, hostkey, domain);
	}
	
}
