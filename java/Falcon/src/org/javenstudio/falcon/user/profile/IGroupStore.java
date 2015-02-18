package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IGroupStore {

	public NamedList<Object> loadGroupList(GroupManager manager) 
			throws ErrorException;
	
	public void saveGroupList(GroupManager manager, 
			NamedList<Object> items) throws ErrorException;
	
	public NamedList<Object> loadInviteList(GroupManager manager) 
			throws ErrorException;
	
	public void saveInviteList(GroupManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
