package org.anybox.android.library;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import org.javenstudio.android.StorageHelper;
import org.javenstudio.cocoka.Constants;
import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.android.ModuleAppRegistry;
import org.javenstudio.cocoka.android.ResourceMap;
import org.javenstudio.cocoka.widget.setting.SettingManager;

public class MyImplements extends Implements {
	public static final String PACKAGE_NAME = MyImplements.class.getPackage().getName();
	private static final boolean DEBUG = true;
	
	public static final String INTENTFILTER_CATEGORY_MODULE = 
			"org.anybox.android.library.MODULE";
	public static final String INTENTFILTER_CATEGORY_RESOURCE = 
			"org.anybox.android.library.RESOURCE";
	
	public static final String RESOURCE_SELECTED_PREFERENCE_KEY = 
			"org.anybox.android.library.resource.selected";
	
	public static final String SETTINGMANAGER_PREFERENCES_NAME = 
			"org.anybox.android.library.preferences";
	
	static { StorageHelper.registerMimeTypes(); }
	
	public MyImplements(Application app) { 
		super(app, DEBUG);
		ResourceMap.setResourceClass(R.class); 
	}
	
	@Override 
	protected String getLogTag() { return "MyLibrary"; } 
	
	@Override 
	protected void onCreated(Bundle bundle) { 
		bundle.putString(Constants.STRINGKEY_MODULE_INTENTFILTER_CATEGORY, 
				INTENTFILTER_CATEGORY_MODULE);
		
		bundle.putString(Constants.STRINGKEY_RESOURCE_INTENTFILTER_CATEGORY, 
				INTENTFILTER_CATEGORY_RESOURCE);
		
		bundle.putString(Constants.STRINGKEY_RESOURCE_SELECTED_PREFERENCE_KEY, 
				RESOURCE_SELECTED_PREFERENCE_KEY);
		
	}
	
	private SettingManager mSettingManager = null; 
	
	public synchronized SettingManager getSettingManager() { 
		if (mSettingManager == null) { 
			mSettingManager = new SettingManager(getContext(), 
					SETTINGMANAGER_PREFERENCES_NAME); 
			//mSettingManager.setSharedPreferencesMode(
			//		Context.MODE_WORLD_WRITEABLE); 
		}
		return mSettingManager; 
	}
	
	@Override 
	public String getLocalStorageDirectory() {
		return Environment.getExternalStorageDirectory().getPath() + "/anybox";
	}
	
	@Override 
	public SharedPreferences getPreferences() { 
		return getSettingManager().getSharedPreferences(); 
	}
	
	@Override 
	protected void onRegisterModules(ModuleAppRegistry reg) { 
		super.onRegisterModules(reg);
		reg.register(MyApp.class.getName(), true); 
	}
	
	@Override 
	protected void onTerminate() {
		super.onTerminate();
		MyApp.getInstance().onTerminate();
	}
	
}
