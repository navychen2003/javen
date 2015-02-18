package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;

public interface IUserData {

	public static interface Factory { 
		public IUserData create(String username) throws ErrorException;
	}
	
	public String getUserName();
	public String getUserKey();
	public String getHostKey();
	
	public int getUserFlag();
	public int getUserType();
	
	public String getAttr(String name);
	public String[] getAttrNames();
	
}
