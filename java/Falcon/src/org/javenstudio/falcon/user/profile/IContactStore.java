package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IContactStore {

	public NamedList<Object> loadContactList(ContactManager manager) 
			throws ErrorException;
	
	public void saveContactList(ContactManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
