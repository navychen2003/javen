package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IMemberStore {

	public NamedList<Object> loadMemberList(MemberManager manager) 
			throws ErrorException;
	
	public void saveMemberList(MemberManager manager, 
			NamedList<Object> items) throws ErrorException;
	
	public NamedList<Object> loadInviteList(MemberManager manager) 
			throws ErrorException;
	
	public void saveInviteList(MemberManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
