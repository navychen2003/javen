package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IFriendStore {

	public NamedList<Object> loadFriendList(FriendManager manager) 
			throws ErrorException;
	
	public void saveFriendList(FriendManager manager, 
			NamedList<Object> items) throws ErrorException;
	
	public NamedList<Object> loadInviteList(FriendManager manager) 
			throws ErrorException;
	
	public void saveInviteList(FriendManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
