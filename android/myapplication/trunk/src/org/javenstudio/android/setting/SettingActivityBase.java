package org.javenstudio.android.setting;

import android.os.Bundle;

import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Logger;

public abstract class SettingActivityBase extends SettingActivityImpl {
	private static final Logger LOG = Logger.getLogger(SettingActivityBase.class);
	
	private SettingScreen mCurrentScreen = null;
	
	@Override
    protected void doOnCreate(Bundle savedInstanceState) {
        super.doOnCreate(savedInstanceState);
        
        initSettings(); 
	}
	
	@Override
	public synchronized SettingScreen getCurrentSettingScreen() {
		if (mCurrentScreen == null) {
			SettingScreen screen = getSettingManager().getSettingScreen(); 
			
			if (screen != null) { 
				Setting setting = screen.findSetting(getIntentSettingScreenKey());
				if (setting != null && setting instanceof SettingScreen)
					screen = (SettingScreen)setting;
			}
			
			if (screen != null)
				screen.setOnSettingIntentClickListener(this); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getCurrentSettingScreen: screen=" + screen);
			
			mCurrentScreen = screen;
		}
		
		return mCurrentScreen; 
	}
	
	protected final void initSettings() { 
		if (getRootSettingScreen() != null) return;
		if (LOG.isDebugEnabled()) LOG.debug("initSettings");
		
        final SettingScreen screen = createSettingScreen("settings"); 
        
        if (screen != null) { 
        	SettingCategory category = screen.createSettingCategory(
        			screen.getKey() + ".account"); 
        	
        	category.addSetting(createAccountSetting(category));
        	
        	screen.addSetting(category, false); 
        }
        
        if (screen != null) { 
        	SettingCategory category = screen.createSettingCategory(
        			screen.getKey() + ".general"); 
        	
        	category.addSetting(createGeneralSetting(category));
        	
        	//ModuleApp[] apps = ResourceHelper.getModuleApps(); 
        	//for (int i=0; apps != null && i < apps.length; i++) { 
        	//	ModuleApp app = apps[i]; 
        	//	ModuleAppSetting appSetting = app != null ? app.getAppSetting() : null; 
        	//	if (appSetting == null) continue; 
        	//	
        	//	category.addSetting(appSetting.createSetting(category));
        	//}
        	
        	Setting pluginSetting = createPluginSetting(category); 
        	if (pluginSetting != null) 
        		category.addSetting(pluginSetting);
        	
        	Setting taskSetting = createTaskSetting(category); 
        	if (taskSetting != null) 
        		category.addSetting(taskSetting);
        	
	        screen.addSetting(category); 
        }
        
        //if (screen != null) { 
        //	SettingCategory category = screen.createSettingCategory(
        //			screen.getKey() + ".help"); 
        //	
        //	category.addSetting(createHelpSetting(category));
	    //    
	    //    screen.addSetting(category); 
        //}
        
        if (screen != null) { 
        	SettingCategory category = screen.createSettingCategory(
        			screen.getKey() + ".about"); 
        	
        	category.addSetting(createAboutSetting(category)); 
	        
	        screen.addSetting(category); 
        }
        
        setRootSettingScreen(screen); 
	}

	protected SettingScreen createSettingScreen(String key) {
		SettingScreen screen = getSettingManager().createSettingScreen(key); 
		//screen.setTitle(R.string.app_setting_name); 
		return screen; 
	}

	protected abstract Setting createGeneralSetting(SettingCategory category);
	protected abstract Setting createPluginSetting(SettingCategory category);
	protected abstract Setting createTaskSetting(SettingCategory category);
	
	protected abstract Setting createAccountSetting(SettingCategory category);
	protected abstract Setting createAboutSetting(SettingCategory category);
	
}
