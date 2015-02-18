package org.javenstudio.lightning.core.datum;

import java.util.HashSet;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.lightning.core.CoreAdminSetting;
import org.javenstudio.lightning.core.CoreSetting;

public class DatumSetting extends CoreSetting 
		implements SettingGroup.UpdateHandler {

	@SuppressWarnings("unused")
	private final Set<SettingGroup> mGroups;
	
	public DatumSetting(CoreAdminSetting setting) throws ErrorException { 
		super(setting);
		mGroups = initSetting(setting);
	}
	
	@SuppressWarnings("unused")
	private Set<SettingGroup> initSetting(CoreAdminSetting setting) 
			throws ErrorException { 
		Set<SettingGroup> groups = new HashSet<SettingGroup>();
		
		SettingCategory category = setting.createCategory("datum");
		category.setTitle("Library");
		
		if (false) {
			SettingGroup group = category.createGroup("general", this);
			group.setTitle("General");
			
			groups.add(group);
		}
		
		if (false) {
			SettingGroup group = category.createGroup("misc", this);
			group.setTitle("Misc");
			
			groups.add(group);
		}
		
		return groups;
	}

	@Override
	public boolean updateSetting(SettingGroup group, Object input)
			throws ErrorException {
		return false;
	}
	
}
