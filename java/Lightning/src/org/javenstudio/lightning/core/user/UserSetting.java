package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.user.UserSettingHelper;
import org.javenstudio.lightning.core.CoreAdminSetting;
import org.javenstudio.lightning.core.CoreSetting;

public class UserSetting extends CoreSetting {

	private final UserSettingHelper mHelper;
	
	public UserSetting(CoreAdminSetting setting) throws ErrorException { 
		super(setting);
		mHelper = new UserSettingHelper(setting);
	}
	
	@Override
	protected void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
		mHelper.loadSetting(categoryName, groupName, name, val);
	}
	
}
