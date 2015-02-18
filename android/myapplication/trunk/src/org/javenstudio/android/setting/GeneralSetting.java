package org.javenstudio.android.setting;

import android.content.Intent;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.widget.setting.CheckBoxSetting;
import org.javenstudio.cocoka.widget.setting.ListSetting;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public abstract class GeneralSetting extends GeneralSettingBase {

	public GeneralSetting(SettingCategory category) {
		super(category); 
		
		String screenKey = category.getKey() + ".general"; 
		
		int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_general);
		if (iconRes == 0) iconRes = R.drawable.ic_setting_general_selector;
		
		setKey(screenKey); 
		setTitle(R.string.setting_general_title);
		setIcon(iconRes); 
		
		setIntent(createIntent(screenKey));
	}
	
	protected abstract Intent createIntent(String screenKey);
	
	@Override
	protected Setting createLogDebugSetting(SettingCategory category) { 
		final CheckBoxSetting setting = category.createCheckBoxSetting(
				category.getKey() + ".logdebug", 
				R.string.setting_logdebug_title);
		
		setting.setCheckable(true);
		setting.setSelectable(true);
		setting.setChangeCheckedOnClick(true);
		setting.setChecked(getLogDebug());
		
		setting.setOnGetSettingViewListener(new Setting.OnGetSettingViewListener() {
				@Override
				public void onGetSettingView(Setting s) {
					setting.setChecked(getLogDebug());
				}
			});
		
		setting.setOnSettingChangeListener(new Setting.OnSettingChangeListener() {
				@Override
				public boolean onSettingChange(Setting s, Object newValue) {
					if (newValue != null && newValue instanceof Boolean) {
						if (setLogDebug(((Boolean)newValue).booleanValue())) { 
							//setting.setFlag(SettingActivity.FLAG_CHANGED);
							return true;
						}
					}
					return false;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createOrientationSensorSetting(SettingCategory category) { 
		final CheckBoxSetting setting = category.createCheckBoxSetting(
				category.getKey() + ".orientationsensor", 
				R.string.setting_orientationsensor_title);
		
		setting.setCheckable(true);
		setting.setSelectable(true);
		setting.setChangeCheckedOnClick(true);
		setting.setChecked(getOrientationSensor());
		
		setting.setOnGetSettingViewListener(new Setting.OnGetSettingViewListener() {
				@Override
				public void onGetSettingView(Setting s) {
					setting.setChecked(getOrientationSensor());
				}
			});
		
		setting.setOnSettingChangeListener(new Setting.OnSettingChangeListener() {
				@Override
				public boolean onSettingChange(Setting s, Object newValue) {
					if (newValue != null && newValue instanceof Boolean) {
						if (setOrientationSensor(((Boolean)newValue).booleanValue())) { 
							//setting.setFlag(SettingActivity.FLAG_CHANGED);
							return true;
						}
					}
					return false;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createShowWarningSetting(SettingCategory category) { 
		final CheckBoxSetting setting = category.createCheckBoxSetting(
				category.getKey() + ".showwarning", 
				R.string.setting_showwarning_title);
		
		setting.setCheckable(true);
		setting.setSelectable(true);
		setting.setChangeCheckedOnClick(true);
		setting.setChecked(getShowWarning());
		
		setting.setOnGetSettingViewListener(new Setting.OnGetSettingViewListener() {
				@Override
				public void onGetSettingView(Setting s) {
					setting.setChecked(getShowWarning());
				}
			});
		
		setting.setOnSettingChangeListener(new Setting.OnSettingChangeListener() {
				@Override
				public boolean onSettingChange(Setting s, Object newValue) {
					if (newValue != null && newValue instanceof Boolean) {
						if (setShowWarning(((Boolean)newValue).booleanValue())) { 
							//setting.setFlag(SettingActivity.FLAG_CHANGED);
							return true;
						}
					}
					return false;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createNotifyTrafficSetting(SettingCategory category) { 
		final CheckBoxSetting setting = category.createCheckBoxSetting(
				category.getKey() + ".notifytraffic", 
				R.string.setting_notifytraffic_title);
		
		setting.setCheckable(true);
		setting.setSelectable(true);
		setting.setChangeCheckedOnClick(true);
		setting.setChecked(getNotifyTraffic());
		
		setting.setOnGetSettingViewListener(new Setting.OnGetSettingViewListener() {
				@Override
				public void onGetSettingView(Setting s) {
					setting.setChecked(getNotifyTraffic());
				}
			});
		
		setting.setOnSettingChangeListener(new Setting.OnSettingChangeListener() {
				@Override
				public boolean onSettingChange(Setting s, Object newValue) {
					if (newValue != null && newValue instanceof Boolean) {
						if (setNotifyTraffic(((Boolean)newValue).booleanValue())) { 
							//setting.setFlag(SettingActivity.FLAG_CHANGED);
							return true;
						}
					}
					return false;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createAutoFetchSetting(SettingCategory category) { 
		String screenKey = category.getKey() + ".autofetch"; 
		
		final ListSetting list = category.createListSetting(
				screenKey, R.string.setting_autofetch_title, 0);
		
		list.setEntries(
				R.array.setting_autofetch_entries, 
				R.array.setting_autofetch_values, 
				R.array.setting_autofetch_summaries);
		
		list.setCheckedValue(getAutoFetchTypeValue());
		list.setIntent(createIntent(screenKey));
		list.setHomeIconRes(getHomeIconRes());
		
		list.setOnGetSettingViewListener(new Setting.OnGetSettingViewListener() {
				@Override
				public void onGetSettingView(Setting setting) {
					list.setCheckedValue(getAutoFetchTypeValue());
				}
			});
		
		list.setOnSettingChangeListener(new Setting.OnSettingChangeListener() {
				@Override
				public boolean onSettingChange(Setting s, Object newValue) {
					String value = getStringNewValue(getListNewValue(newValue)); 
					if (value != null) { 
						if (setAutoFetchTypeValue(value)) { 
							//list.setFlag(SettingActivity.FLAG_CHANGED);
							return true;
						}
					}
					return false;
				}
			});
		
		return list;
	}
	
}
