package org.javenstudio.android.setting;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public class TaskSetting extends SettingScreen 
		implements Setting.OnSettingClickListener {

	public TaskSetting(SettingCategory category) {
		super(category.getSettingManager(), null);
		
		String screenKey = category.getKey() + ".task"; 
		
		int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_task);
		if (iconRes == 0) iconRes = R.drawable.ic_setting_task_selector;
		
		setKey(screenKey); 
		setTitle(R.string.setting_task_title);
		setIcon(iconRes); 
		
		setOnSettingClickListener(this);
	}
	
	public boolean onSettingClick(Setting setting) {
		return true;
	}
	
}
