package org.javenstudio.falcon.setting.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserManager;

public class UserCategoryManager {
	private static final Logger LOG = Logger.getLogger(UserCategoryManager.class);
	
	private final UserManager mManager;
	private final List<String> mNames = new ArrayList<String>();
	
	private final Map<String,UserCategory> mCategories = 
			new HashMap<String,UserCategory>();
	
	public UserCategoryManager(UserManager manager) {
		if (manager == null) throw new NullPointerException();
		mManager = manager;
		
		addCategory(UserCategory.newNormalUser(manager));
		addCategory(UserCategory.newPublicGroup(manager));
		addCategory(UserCategory.newPrivateGroup(manager));
		addCategory(UserCategory.newAttachUser(manager));
	}
	
	public UserManager getManager() { return mManager; }
	
	public void addCategory(UserCategory category) {
		if (category == null) return;
		
		synchronized (mCategories) {
			final String name = category.getName();
			if (mCategories.containsKey(name)) {
				UserCategory old = mCategories.get(name);
				if (old == category) return;
				
				if (LOG.isWarnEnabled())
					LOG.warn("addCategory: replace existed: " + old);
				
				mCategories.put(name, category);
				return;
			}
			
			mCategories.put(name, category);
			mNames.add(name);
		}
	}
	
	public UserCategory getCategory(String name) {
		if (name == null) return null;
		
		synchronized (mCategories) {
			return mCategories.get(name);
		}
	}
	
	public String[] getCategoryNames() {
		synchronized (mCategories) {
			return mNames.toArray(new String[mNames.size()]);
		}
	}
	
	public long getFreeSpace(IUser user) {
		if (user == null) return 0;
		
		try {
			UserCategory category = getCategory(user.getPreference().getCategory());
			if (category != null) 
				return category.getFreeSpace();
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("getFreeSpace: error: " + e, e);
		}
		
		return 0;
	}
	
	public void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
	}
	
}
