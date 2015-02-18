package org.anybox.android.library.setting;

import android.app.Activity;

import org.anybox.android.library.MyApp;
import org.anybox.android.library.R;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingManager;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public class MySetting extends MySettingBase {

	public static final String SCREENKEY = MySetting.class.getName();
	
	public static void actionSetting(Activity fromActivity) { 
		//SettingActivity.actionSetting(fromActivity, SCREENKEY);
	}
	
	private final MyApp mApp; 
	
	protected class MySettingScreen extends SettingScreen { 
		public MySettingScreen(SettingManager manager) {
	    	super(manager); 
	    }
	}
	
	public MySetting(MyApp app) { 
		mApp = app;
	}
	
	public final MyApp getApp() { 
		return mApp;
	}
	
	@Override 
	protected SettingScreen createSettingScreen(SettingCategory category) { 
		String screenKey = SCREENKEY; //category.getKey() + ".reader"; 
		
		SettingScreen screen = new MySettingScreen(category.getSettingManager()); 
		screen.setKey(screenKey); 
		screen.setTitle(R.string.app_name); 
		screen.setIcon(R.drawable.ic_home_anybox_dark); 
		//screen.setIntent(SettingActivity.createIntent(screenKey));
		
		return screen; 
	}
	
}
