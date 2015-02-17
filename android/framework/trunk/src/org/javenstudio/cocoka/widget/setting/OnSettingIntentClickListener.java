package org.javenstudio.cocoka.widget.setting;

import android.content.Intent;

public interface OnSettingIntentClickListener {

	/**
     * Called when a setting has intent in the tree rooted at this
     * {@link SettingScreen} has been clicked.
     * 
     * @param settingScreen The {@link SettingScreen} that the
     *        setting is located in.
     * @param setting The setting that was clicked.
     * @return Whether the click was handled.
     */
	boolean onSettingIntentClick(SettingScreen settingScreen, Setting setting, Intent intent);
	
}
