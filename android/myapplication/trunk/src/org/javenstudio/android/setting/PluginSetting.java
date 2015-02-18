package org.javenstudio.android.setting;

import android.content.Intent;

import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;

public abstract class PluginSetting extends PluginSettingBase {

	public PluginSetting(SettingCategory category) {
		super(category); 
		
		String screenKey = category.getKey() + ".plugin"; 
		
		int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_plugin);
		if (iconRes == 0) iconRes = R.drawable.ic_setting_general_selector;
		
		setKey(screenKey); 
		setTitle(R.string.setting_plugin_title);
		setIcon(iconRes); 
		
		setIntent(createIntent(screenKey));
	}
	
	protected abstract Intent createIntent(String screenKey);
	
}
