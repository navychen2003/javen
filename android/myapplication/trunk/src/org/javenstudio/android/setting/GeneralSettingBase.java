package org.javenstudio.android.setting;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Log;

public abstract class GeneralSettingBase extends SettingScreenBase {

	public GeneralSettingBase(SettingCategory category) {
		super(category.getSettingManager(), null); 
		
		setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					initSettingScreen(); 
					return true;
				}
			});
	}
	
	protected final void initSettingScreen() { 
		removeAll(false);
		
		initNotificationScreen(this); 
		initMiscScreen(this); 
		
		addSetting(createSettingCategory(null));
	}
	
	protected abstract Setting createLogDebugSetting(SettingCategory category); 
	protected abstract Setting createOrientationSensorSetting(SettingCategory category); 
	protected abstract Setting createShowWarningSetting(SettingCategory category); 
	protected abstract Setting createNotifyTrafficSetting(SettingCategory category); 
	protected abstract Setting createAutoFetchSetting(SettingCategory category);
	
	protected void initMiscScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".misc"); 
		
		category.addSetting(createLogDebugSetting(category));
		
		screen.addSetting(category);
	}
	
	protected void initNotificationScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".notify"); 
		
		category.addSetting(createOrientationSensorSetting(category));
		//category.addSetting(createShowWarningSetting(category));
		category.addSetting(createNotifyTrafficSetting(category));
		category.addSetting(createAutoFetchSetting(category));
		
		screen.addSetting(category);
	}
	
	public static synchronized boolean getLogDebug() { 
		return Log.getLogDebug();
	}
	
	public static synchronized boolean setLogDebug(boolean debug) { 
		Log.setLogDebug(debug);
		return true;
	}
	
	private static final String ORIENTATIONSENSOR_PREFERENCE_KEY = "general.orientationsensor";
	private static int sEnableOrientationSensor = -1;
	
	public static synchronized boolean getOrientationSensor() { 
		if (sEnableOrientationSensor == 1) return true;
		if (sEnableOrientationSensor == 0) return false;
		
		boolean value = ResourceHelper.getPreferenceBoolean(ORIENTATIONSENSOR_PREFERENCE_KEY, true);
		sEnableOrientationSensor = value ? 1 : 0;
		
		return value;
	}
	
	public static synchronized boolean setOrientationSensor(boolean value) { 
		if (ResourceHelper.setPreference(ORIENTATIONSENSOR_PREFERENCE_KEY, value)) { 
			sEnableOrientationSensor = value ? 1 : 0;
			return true;
		}
		return false;
	}
	
	private static final String HIDEWARNING_PREFERENCE_KEY = "general.showwarning";
	private static int sHideWarning = -1;
	
	public static synchronized boolean getShowWarning() { 
		if (sHideWarning == 1) return true;
		if (sHideWarning == 0) return false;
		
		boolean value = ResourceHelper.getPreferenceBoolean(HIDEWARNING_PREFERENCE_KEY, true);
		sHideWarning = value ? 1 : 0;
		
		return value;
	}
	
	public static synchronized boolean setShowWarning(boolean value) { 
		if (ResourceHelper.setPreference(HIDEWARNING_PREFERENCE_KEY, value)) { 
			sHideWarning = value ? 1 : 0;
			return true;
		}
		return false;
	}
	
	private static final String NOTIFYTRAFFIC_PREFERENCE_KEY = "general.notifytraffic";
	private static int sNotifyTraffic = -1;
	
	public static synchronized boolean getNotifyTraffic() { 
		if (sNotifyTraffic == 1) return true;
		if (sNotifyTraffic == 0) return false;
		
		boolean value = ResourceHelper.getPreferenceBoolean(NOTIFYTRAFFIC_PREFERENCE_KEY, false);
		sNotifyTraffic = value ? 1 : 0;
		
		return value;
	}
	
	public static synchronized boolean setNotifyTraffic(boolean value) { 
		if (ResourceHelper.setPreference(NOTIFYTRAFFIC_PREFERENCE_KEY, value)) { 
			sNotifyTraffic = value ? 1 : 0;
			return true;
		}
		return false;
	}
	
	private static final String AUTOFETCH_PREFERENCE_KEY = "general.autofetch";
	private static String sAutoFetch = null;
	
	private static final String AUTOFETCH_ALLOW_VALUE = "1";
	private static final String AUTOFETCH_CONFIRM_VALUE = "2";
	private static final String AUTOFETCH_DISABLED_VALUE = "3";
	
	public static final int AUTOFETCH_ALLOW = 1;
	public static final int AUTOFETCH_CONFIRM = 2;
	public static final int AUTOFETCH_DISABLED = 3;
	
	public static int getAutoFetchType() { 
		String value = getAutoFetchTypeValue();
		if (value != null) { 
			if (value.equals(AUTOFETCH_ALLOW_VALUE)) return AUTOFETCH_ALLOW;
			if (value.equals(AUTOFETCH_CONFIRM_VALUE)) return AUTOFETCH_CONFIRM;
			if (value.equals(AUTOFETCH_DISABLED_VALUE)) return AUTOFETCH_DISABLED;
		}
		
		return AUTOFETCH_CONFIRM;
	}
	
	public static synchronized String getAutoFetchTypeValue() { 
		if (sAutoFetch != null) return sAutoFetch;
		
		String value = ResourceHelper.getPreferenceString(AUTOFETCH_PREFERENCE_KEY, 
				AUTOFETCH_ALLOW_VALUE);
		sAutoFetch = value;
		
		return value;
	}
	
	public static synchronized boolean setAutoFetchTypeValue(String value) { 
		if (value == null || value.length() == 0) 
			return false;
		
		if (value.equals(AUTOFETCH_ALLOW_VALUE) || 
			value.equals(AUTOFETCH_CONFIRM_VALUE) || 
			value.equals(AUTOFETCH_DISABLED_VALUE)) { 
			
			if (ResourceHelper.setPreference(AUTOFETCH_PREFERENCE_KEY, value)) { 
				sAutoFetch = value;
				return true;
			}
		}
		
		return false;
	}
	
}
