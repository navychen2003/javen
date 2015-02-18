package org.javenstudio.falcon.setting.user;

import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.raptor.conf.Configuration;

public class UserCategory {

	static UserCategory newNormalUser(UserManager manager) {
		Configuration conf = manager.getStore().getConfiguration();
		UserCategory category = new UserCategory(IUser.NORMAL, IUser.USER);
		category.setFreeSpace(conf.getLong("user.category.normaluser.freespace", IUser.NORMAL_USER_FREESPACE));
		category.setTitle("Normal User");
		return category;
	}
	
	static UserCategory newPublicGroup(UserManager manager) {
		Configuration conf = manager.getStore().getConfiguration();
		UserCategory category = new UserCategory(IUser.PUBLIC, IUser.GROUP);
		category.setFreeSpace(conf.getLong("user.category.publicgroup.freespace", IUser.PUBLIC_GROUP_FREESPACE));
		category.setMaxMembers(conf.getInt("user.category.privategroup.maxmembers", IUser.PUBLIC_GROUP_MAXMEMBERS));
		category.setTitle("Public Group");
		return category;
	}
	
	static UserCategory newPrivateGroup(UserManager manager) {
		Configuration conf = manager.getStore().getConfiguration();
		UserCategory category = new UserCategory(IUser.PRIVATE, IUser.GROUP);
		category.setFreeSpace(conf.getLong("user.category.privategroup.freespace", IUser.PRIVATE_GROUP_FREESPACE));
		category.setMaxMembers(conf.getInt("user.category.privategroup.maxmembers", IUser.PRIVATE_GROUP_MAXMEMBERS));
		category.setTitle("Private Group");
		return category;
	}
	
	static UserCategory newAttachUser(UserManager manager) {
		Configuration conf = manager.getStore().getConfiguration();
		UserCategory category = new UserCategory(IUser.ATTACHUSER, IUser.USER);
		category.setFreeSpace(conf.getLong("user.category.attachuser.freespace", IUser.ATTACH_USER_FREESPACE));
		category.setTitle("Attach User");
		return category;
	}
	
	private final String mName;
	private final String mType;
	
	private String mTitle = null;
	private String mDesc = null;
	
	private int mMaxMembers = 0;
	private long mFreeSpace = 0;
	
	public UserCategory(String name, String type) {
		if (name == null || type == null) throw new NullPointerException();
		mName = name.toLowerCase();
		mType = type.toLowerCase();
	}
	
	public String getName() { return mName; }
	public String getType() { return mType; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String val) { mTitle = val; }
	
	public String getDescription() { return mDesc; }
	public void setDescription(String text) { mDesc = text; }
	
	public int getMaxMembers() { return mMaxMembers; }
	public void setMaxMembers(int val) { if (val >= 0) mMaxMembers = val; }
	
	public long getFreeSpace() { return mFreeSpace; }
	public void setFreeSpace(long val) { if (val >= 0) mFreeSpace = val; }
	
	public long getFreeSpaceInMB() {
		return mFreeSpace / (1024 * 1024);
	}
	
	public void setFreeSpaceInMB(String str) {
		if (str != null && str.length() > 0) {
			try {
				long num = Long.valueOf(str);
				if (num >= 0) setFreeSpace(num * 1024 * 1024);
			} catch (Throwable e) {
			}
		}
	}
	
	public void setFreeSpace(String str) {
		if (str != null && str.length() > 0) {
			try {
				long num = Long.valueOf(str);
				if (num >= 0) setFreeSpace(num);
			} catch (Throwable e) {
			}
		}
	}
	
	public void setMaxMembers(String str) {
		if (str != null && str.length() > 0) {
			try {
				int num = Integer.valueOf(str);
				if (num >= 0) setMaxMembers(num);
			} catch (Throwable e) {
			}
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + mName 
				+ ",type=" + mType + ",maxMembers=" + mMaxMembers 
				+ ",freeSpace=" + mFreeSpace + "}";
	}
	
}
