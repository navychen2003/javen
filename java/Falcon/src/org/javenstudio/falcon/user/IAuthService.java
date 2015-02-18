package org.javenstudio.falcon.user;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public interface IAuthService {

	public IUserData addUser(String username, String userkey, String hostkey, 
			String password, int flag, int type, Map<String,String> attrs) throws ErrorException;
	
	public IUserData authUser(String username, String password) throws ErrorException;
	public IUserData updateUser(String username, String hostkey, String password, int flag, 
			Map<String,String> attrs) throws ErrorException;
	
	public IUserData searchUser(String username) throws ErrorException;
	public IUserData removeUser(String username) throws ErrorException;
	
	public INameData searchName(String name) throws ErrorException;
	public INameData updateName(String name, String value, String hostkey, int flag, 
			Map<String,String> attrs) throws ErrorException;
	public INameData removeName(String name) throws ErrorException;
	
	public void close();
	
}
