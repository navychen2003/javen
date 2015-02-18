package org.javenstudio.falcon.setting.cluster;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserName;

public interface IHostCluster {

	public String getClusterId();
	public String getDomain();
	public String getMailDomain();
	public String getSecret();
	
	public IHostNode getHostByKey(String key);
	public IHostNode[] getNamedHosts();
	public IHostNode[] getHosts();
	public int getHostCount();
	
	public void scanHosts(IScanListener listener) throws ErrorException;
	public void removeHost(IHostNode node, String reason) throws ErrorException;
	public void addHost(IHostNode node) throws ErrorException;
	
	public IHostList getHostListByHash(int hash);
	public IHostList getHostListByName(String name) throws ErrorException;
	public IHostUserName getHostUserName(IUserName uname) throws ErrorException;
	public IUserName parseUserName(String name) throws ErrorException;
	
	public IHostUserData searchUser(String username) throws ErrorException;
	public IHostNameData searchName(String name) throws ErrorException;
	
	public IHostUserData addUser(String username, String userkey, String hostkey, 
			String password, int flag, int type, Map<String,String> attrs) throws ErrorException;
	public IHostUserData updateUser(String username, String hostkey, String password, 
			int flag, Map<String,String> attrs) throws ErrorException;
	public IHostUserData removeUser(String username) throws ErrorException;
	
	public IHostNameData updateName(String name, String value, String hostkey, 
			int flag, Map<String,String> attrs, String oldname) throws ErrorException;
	public IHostNameData removeName(String name) throws ErrorException;
	
	public void close();
	
}
