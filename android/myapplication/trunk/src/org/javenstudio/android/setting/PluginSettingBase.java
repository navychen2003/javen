package org.javenstudio.android.setting;

import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public abstract class PluginSettingBase extends SettingScreen {

	public PluginSettingBase(SettingCategory category) {
		super(category.getSettingManager(), null); 
		
		setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					initPluginScreen(); 
					return true;
				}
			});
	}
	
	protected final void initPluginScreen() { 
		removeAll(false);
		
		initPluginScreen(this); 
		
		addSetting(createSettingCategory(null));
	}
	
	protected void initPluginScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".plugin"); 
		
		PluginManager.PackageInfo[] packages = 
				ResourceHelper.getModuleManager().getModulePackages();
		
		for (int i=0; packages != null && i < packages.length; i++) { 
			PluginManager.PackageInfo info = packages[i]; 
			if (info == null) continue; 
			
			PluginDetails setting = createPluginDetails(category, info);
			
			category.addSetting(setting);
		}
		
		screen.addSetting(category); 
	}
	
	protected abstract PluginDetails createPluginDetails(SettingCategory category, 
			PluginManager.PackageInfo info);
	
}
