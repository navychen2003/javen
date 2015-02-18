package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageManager;
import org.javenstudio.falcon.publication.PublicationManager;
import org.javenstudio.falcon.user.profile.Preference;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.ILockable;

public interface IUser {
	
	public static enum ShowType {
		DEFAULT, ALL, INFO
	}
	
	public static final String SYSTEM = "system";
	
	public static final String USER = "user";
	public static final String GROUP = "group";
	
	public static final String ADMINISTRATOR = "administrator";
	public static final String MANAGER = "manager";
	
	public static final String NORMAL = "normal";
	public static final String PUBLIC = "public";
	public static final String PRIVATE = "private";
	public static final String ATTACHUSER = "attachuser";
	
	public static final String ENABLED = "enabled";
	public static final String READONLY = "readonly";
	public static final String DISABLED = "disabled";
	
	public static final long NORMAL_USER_FREESPACE = 5l * 1024 * 1024 * 1024; // 5G
	public static final long PUBLIC_GROUP_FREESPACE = 10l * 1024 * 1024 * 1024; // 10G
	public static final long PRIVATE_GROUP_FREESPACE = 2l * 1024 * 1024 * 1024; // 2G
	public static final long ATTACH_USER_FREESPACE = 500l * 1024 * 1024 * 1024; // 500G
	
	public static final int PUBLIC_GROUP_MAXMEMBERS = 0;
	public static final int PRIVATE_GROUP_MAXMEMBERS = 100;
	
	public static final int TYPE_USER = 0;
	public static final int TYPE_GROUP = 1;
	public static final int TYPE_NAMED_USER = 2;
	public static final int TYPE_NAMED_GROUP = 3;
	
	public static final int FLAG_ENABLED = 0;
	public static final int FLAG_READONLY = 1;
	public static final int FLAG_DISABLED = 2;
	
	public static final int NAME_NICKNAME = 0;
	public static final int NAME_USERKEY = 1;
	public static final int NAME_EMAIL = 2;
	
	public static final class Util {
		
		public static String typeOfUser(IUser user) 
				throws ErrorException { 
			if (user == null) return null;
			if (user instanceof IGroup) return GROUP;
			if (user instanceof IMember) { 
				IMember member = (IMember)user;
				if (member.isAdministrator())
					return ADMINISTRATOR;
				else if (member.isManager())
					return MANAGER;
				else
					return NORMAL;
			}
			return USER;
		}
		
		public static String stringOfFlag(int flag) throws ErrorException {
			switch (flag) {
			case FLAG_ENABLED: 
				return ENABLED;
			case FLAG_READONLY:
				return READONLY;
			case FLAG_DISABLED:
				return DISABLED;
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Wrong user flag value: " + flag);
			}
		}
		
		public static int parseFlag(String str) throws ErrorException { 
			if (str != null) { 
				if (str.equalsIgnoreCase(ENABLED))
					return FLAG_ENABLED;
				else if (str.equalsIgnoreCase(READONLY))
					return FLAG_READONLY;
				else if (str.equalsIgnoreCase(DISABLED))
					return FLAG_DISABLED;
			}
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong user flag string: " + str);
		}
	}
	
	public UserManager getUserManager();
	public ILockable.Lock getLock();
	
	public DataManager getDataManager() throws ErrorException;;
	public MessageManager getMessageManager() throws ErrorException;
	public PublicationManager getPublicationManager() throws ErrorException;
	
	public Profile getProfile() throws ErrorException;
	public Preference getPreference() throws ErrorException;
	
	public IInviteSet getInvites() throws ErrorException;
	public IMessageSet getMessages() throws ErrorException;
	
	public String getUserName();
	public String getUserEmail();
	public String getUserId();
	public String getUserKey();
	public int getUserFlag();
	public int getUserType();
	
	public long getUsedSpace();
	public long getUsableSpace();
	public long getPurchasedSpace();
	public long getFreeSpace();
	public long getCapacitySpace();
	
	public long getObtainTime();
	public long getModifiedTime();
	public long getAccessTime();
	
	public boolean isClosed();
	public void close();
	
}
