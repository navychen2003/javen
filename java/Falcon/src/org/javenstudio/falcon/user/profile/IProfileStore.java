package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IProfileStore {

	public NamedList<Object> loadProfile(Profile profile) 
			throws ErrorException;
	
	public void saveProfile(Profile profile, 
			NamedList<Object> items) throws ErrorException;
	
}
