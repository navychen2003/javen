package org.javenstudio.android.setting;

import android.content.Intent;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public abstract class AboutSetting extends AboutSettingBase {

	public AboutSetting(SettingCategory category) {
		super(category); 
		
		String screenKey = category.getKey() + ".about"; 
		
		int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_about);
		if (iconRes == 0) iconRes = R.drawable.ic_setting_about_selector;
		
		setKey(screenKey); 
		setTitle(R.string.setting_about_title);
		setIcon(iconRes); 
		
		setIntent(createIntent(screenKey));
	}
	
	protected abstract Intent createIntent(String screenKey);
	
	@Override
	protected Setting createVersionSetting(SettingCategory category) {
		return null;
	}

	@Override
	protected Setting createAgreementSetting(SettingCategory category) {
		return null;
	}

	@Override
	protected Setting createPrivacySetting(SettingCategory category) {
		return null;
	}

	@Override
	protected Setting createTermsSetting(SettingCategory category) {
		return null;
	}

	@Override
	protected Setting createLawSetting(SettingCategory category) {
		return null;
	}
	
}
