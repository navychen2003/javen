package org.anybox.android.library.setting;

import android.content.Intent;

import org.anybox.android.library.R;
import org.anybox.android.library.SettingActivity;
import org.javenstudio.android.setting.GeneralSetting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public class MySettingGeneral extends GeneralSetting {

	public MySettingGeneral(SettingCategory category) {
		super(category); 
	}

	@Override
	protected Intent createIntent(String screenKey) {
		return SettingActivity.createIntent(screenKey); 
	}
	
	@Override
	public int getHomeIconRes() {
		return R.drawable.ic_home_setting_dark;
	}
	
}
