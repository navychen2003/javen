package org.anybox.android.library.setting;

import org.javenstudio.cocoka.android.ModuleAppSetting;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public abstract class MySettingBase implements ModuleAppSetting {

	protected String mSettingKey = null; 
	
	public final String getSettingKey() { 
		return mSettingKey; 
	}
	
	protected abstract SettingScreen createSettingScreen(SettingCategory category);
	
	@Override 
	public final Setting createSetting(SettingCategory category) { 
		final SettingScreen screen = createSettingScreen(category);
		mSettingKey = screen.getKey();
		
		screen.setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					if (screen.getSettingCount() == 0)
						initSettingScreen(screen);
					return true;
				}
			});
		
		return screen; 
	}
	
	protected final void initSettingScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".library"); 
		
		screen.addSetting(category);
	}
	
}
