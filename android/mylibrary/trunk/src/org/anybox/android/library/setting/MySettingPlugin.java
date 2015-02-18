package org.anybox.android.library.setting;

import android.content.Intent;

import org.anybox.android.library.R;
import org.anybox.android.library.SettingActivity;
import org.javenstudio.android.setting.PluginDetails;
import org.javenstudio.android.setting.PluginSetting;
import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public class MySettingPlugin extends PluginSetting {

	public MySettingPlugin(SettingCategory category) {
		super(category); 
	}

	@Override
	protected Intent createIntent(String screenKey) {
		return SettingActivity.createIntent(screenKey); 
	}

	@Override
	public int getHomeIconRes() {
		return R.drawable.ic_home_plugin_dark;
	}
	
	@Override
	protected PluginDetails createPluginDetails(SettingCategory category,
			PluginManager.PackageInfo info) {
		return new MySettingPluginDetails(category, info);
	}
	
}
