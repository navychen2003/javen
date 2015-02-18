package org.javenstudio.falcon.setting.user;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.setting.SettingManager;
import org.javenstudio.falcon.setting.SettingText;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.util.IParams;

public class UserSettingHelper implements SettingGroup.UpdateHandler {
	private static final Logger LOG = Logger.getLogger(UserSettingHelper.class);

	private static final String USER_NAME = "user";
	private static final String FREESPACE_NAME = "freespace";
	private static final String MAXMEMBERS_NAME = "maxmembers";
	
	private static class UCGroup {
		private final SettingGroup mGroup;
		private final UserCategory mCategory;
		
		public UCGroup(SettingGroup group, UserCategory category) {
			if (group == null || category == null) throw new NullPointerException();
			mGroup = group;
			mCategory = category;
		}
		
		@SuppressWarnings("unused")
		public SettingGroup getGroup() { return mGroup; }
		public UserCategory getCategory() { return mCategory; }
	}
	
	private final SettingManager mManager;
	private final Map<String,UCGroup> mGroups;
	
	public UserSettingHelper(SettingManager manager) throws ErrorException {
		if (manager == null) throw new NullPointerException();
		mManager = manager;
		mGroups = initSetting(manager);
	}
	
	public SettingManager getManager() { return mManager; }
	
	private Map<String,UCGroup> initSetting(SettingManager setting) 
			throws ErrorException { 
		Map<String,UCGroup> groups = new HashMap<String,UCGroup>();
		
		SettingCategory category = setting.createCategory(USER_NAME);
		category.setTitle("User");
		
		UserCategoryManager manager = UserManager.getInstance().getCategoryManager();
		String[] names = manager.getCategoryNames();
		if (names != null) {
			for (String name : names) {
				UserCategory uc = manager.getCategory(name);
				if (uc == null) continue;
				
				SettingGroup group = category.createGroup(uc.getName(), this);
				group.setTitle(uc.getTitle());
				group.setDescription(uc.getDescription());
				
				initGroup(group, uc);
				groups.put(group.getName(), new UCGroup(group, uc));
			}
		}
		
		return groups;
	}
	
	private void initGroup(SettingGroup group, final UserCategory category) {
		if (group == null || category == null) return;
		
		if (category.getType().equals(IUser.GROUP)) {
			SettingText freeSpace = new SettingText(getManager(), FREESPACE_NAME) { 
					public String getValue() { return ""+category.getFreeSpaceInMB(); }
					public void setValue(String value) { category.setFreeSpaceInMB(value); }
				};
			freeSpace.setTitle("Free Space(MB)");
			freeSpace.setDescription("free space in MB for this category of group.");
			
			SettingText maxMembers = new SettingText(getManager(), MAXMEMBERS_NAME) { 
					public String getValue() { return ""+category.getMaxMembers(); }
					public void setValue(String value) { category.setMaxMembers(value); }
				};
			maxMembers.setTitle("Max Members");
			maxMembers.setDescription("max members for this category of group.");
			
			group.addSetting(freeSpace);
			group.addSetting(maxMembers);
			
		} else {
			SettingText freeSpace = new SettingText(getManager(), FREESPACE_NAME) { 
					public String getValue() { return ""+category.getFreeSpaceInMB(); }
					public void setValue(String value) { category.setFreeSpaceInMB(value); }
				};
			freeSpace.setTitle("Free Space(MB)");
			freeSpace.setDescription("free space in MB for this category of user.");
			
			group.addSetting(freeSpace);
		}
	}
	
	@Override
	public synchronized boolean updateSetting(SettingGroup group, 
			Object input) throws ErrorException {
		if (group == null || input == null)
			throw new NullPointerException();
		
		UCGroup ucg = mGroups.get(group.getName());
		if (ucg == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Update failed with wrong handler");
		}
		
		if (!(input instanceof IParams)) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Update failed with wrong input");
		}
		
		IParams req = (IParams)input;
		String input_freeSpace = req.getParam(FREESPACE_NAME);
		String input_maxMembers = req.getParam(MAXMEMBERS_NAME);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateSetting: group=" + group.getName() 
					+ " inputFreeSpace=" + input_freeSpace 
					+ " inputMaxMembers=" + input_maxMembers);
		}
		
		ucg.getCategory().setFreeSpaceInMB(input_freeSpace);
		ucg.getCategory().setMaxMembers(input_maxMembers);
		
		return true;
	}
	
	public synchronized void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
		if (categoryName.equals(USER_NAME)) { 
			UCGroup group = mGroups.get(groupName);
			if (group != null) {
				if (name.equals(FREESPACE_NAME)) { 
					String value = SettingConf.toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: freeSpace=" + value);
						
						group.getCategory().setFreeSpaceInMB(value);
					}
				} else if (name.equals(MAXMEMBERS_NAME)) {
					String value = SettingConf.toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: maxMembers=" + value);
						
						group.getCategory().setMaxMembers(value);
					}
				}
			}
		}
	}
	
}
