package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.user.profile.ContactManager;
import org.javenstudio.falcon.user.profile.FriendManager;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.user.profile.HistoryManager;

public interface IMember extends IUser {

	public DeviceManager getDeviceManager() throws ErrorException;
	public ContactManager getContactManager() throws ErrorException;
	public FriendManager getFriendManager() throws ErrorException;
	public GroupManager getGroupManager() throws ErrorException;
	public HistoryManager getHistoryManager() throws ErrorException;
	
	public IGroup[] getGroups(String role) throws ErrorException;
	
	public IUserClient[] getClients() throws ErrorException;
	public IUserClient getClient(String token, 
			IUserClient.Factory factory) throws ErrorException;
	
	public boolean isAdministrator() throws ErrorException;
	public boolean isManager() throws ErrorException;
	
}
