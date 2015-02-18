package org.javenstudio.android.setting;

import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public abstract class AboutSettingBase extends SettingScreenBase {

	public AboutSettingBase(SettingCategory category) {
		super(category.getSettingManager(), null); 
		
		setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					initSettingScreen(); 
					return true;
				}
			});
	}
	
	protected final void initSettingScreen() { 
		removeAll(false);
		
		initVersionScreen(this); 
		initLicenseScreen(this); 
		
		addSetting(createSettingCategory(null));
	}
	
	protected abstract Setting createVersionSetting(SettingCategory category); 
	protected abstract Setting createAgreementSetting(SettingCategory category); 
	protected abstract Setting createPrivacySetting(SettingCategory category); 
	protected abstract Setting createTermsSetting(SettingCategory category); 
	protected abstract Setting createLawSetting(SettingCategory category); 
	
	protected void initVersionScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".version"); 
		
		category.addSetting(createVersionSetting(category));
		
		screen.addSetting(category);
	}
	
	protected void initLicenseScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".license"); 
		
		category.addSetting(createAgreementSetting(category));
		category.addSetting(createPrivacySetting(category));
		category.addSetting(createTermsSetting(category));
		category.addSetting(createLawSetting(category));
		
		screen.addSetting(category);
	}
	
}
