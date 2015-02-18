package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.profile.MemberManager;

public interface IGroup extends IUser {

	public MemberManager getMemberManager() throws ErrorException;
	
}
