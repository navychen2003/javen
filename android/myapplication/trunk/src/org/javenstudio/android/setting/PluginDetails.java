package org.javenstudio.android.setting;

import android.content.Intent;

import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public abstract class PluginDetails extends PluginDetailsBase {

	public PluginDetails(SettingCategory category, 
			PluginManager.PackageInfo info) {
		super(category, info); 
		
		String screenKey = "plugin." + info.getComponentName().getPackageName(); 
		
		setKey(screenKey); 
		setTitle(info.getTitle());
		setIcon(info.getSmallIcon()); 
		
		setIntent(createIntent(screenKey));
	}
	
	protected abstract Intent createIntent(String screenKey);
	
}
