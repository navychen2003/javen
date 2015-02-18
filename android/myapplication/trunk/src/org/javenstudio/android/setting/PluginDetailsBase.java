package org.javenstudio.android.setting;

import android.content.Intent;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public abstract class PluginDetailsBase extends SettingScreen {

	private final PluginManager.PackageInfo mInfo;
	
	public PluginDetailsBase(SettingCategory category, 
			PluginManager.PackageInfo info) {
		super(category.getSettingManager(), null); 
		
		mInfo = info;
		
		setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					initPluginScreen(); 
					return true;
				}
			});
	}
	
	public final PluginManager.PackageInfo getPackageInfo() { 
		return mInfo;
	}
	
	protected final void initPluginScreen() { 
		removeAll(false);
		
		initPluginScreen(this); 
		
		addSetting(createSettingCategory(null));
	}
	
	protected void initPluginScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".info"); 
		
		category.addSetting(createPluginInfoSetting(category));
		category.addSetting(createPluginOpenSetting(category));
		
		screen.addSetting(category);
	}
	
	protected Setting createPluginInfoSetting(SettingCategory category) { 
		PluginManager.PackageInfo info = getPackageInfo();
		
		PluginInfo setting = new PluginInfo(category.getSettingManager());
    	setting.setKey(category.getKey() + ".info"); 
    	setting.setTitle(info.getTitle()); 
    	setting.setSubTitle(info.getVersionName());
    	setting.setSummary(info.getSummary());
    	setting.setIcon(info.getIcon()); 
		
		//setting.setCheckable(true); 
		setting.setSelectable(false); 
		//setting.setChangeCheckedOnClick(false); 
		//setting.setOnSettingChangeListener(mOnCheckBoxSettingChangeListener);
		//setting.setRadio(false);
		
		return setting;
	}
	
	protected Setting createPluginOpenSetting(SettingCategory category) { 
		PluginManager.PackageInfo info = getPackageInfo();
		if (info.getResolveInfo() == null) 
			return null;
		
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(info.getComponentName());
		
		Setting setting = category.createActionSetting(
				category.getKey() + ".open", R.string.label_action_about, 
				0, 0, intent);
		
		//setting.setCheckable(true); 
		setting.setSelectable(true); 
		//setting.setChangeCheckedOnClick(false); 
		//setting.setOnSettingChangeListener(mOnCheckBoxSettingChangeListener);
		//setting.setRadio(false);
		
		return setting;
	}
	
}
