package org.javenstudio.android.setting;

import android.util.AttributeSet;

import org.javenstudio.cocoka.widget.setting.SettingManager;
import org.javenstudio.cocoka.widget.setting.SettingScreen;

public class SettingScreenBase extends SettingScreen {

	public SettingScreenBase(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs); 
    }
	
	protected String getListNewValue(Object newValue) { 
		if (newValue != null) { 
			if (newValue instanceof CharSequence) 
				return newValue.toString(); 
			
			if (newValue instanceof Object[]) { 
				Object[] values = (Object[])newValue; 
				if (values != null && values.length > 0) { 
					Object value = values[0]; 
					if (value != null) 
						return value.toString();
				}
			}
			
			return newValue.toString();
		}
		return null;
	}
	
	protected String getStringNewValue(Object newValue) { 
		try { 
			if (newValue != null) 
				return newValue.toString();
		} catch (Exception e) { 
			return null;
		}
		return null;
	}
	
	protected int getIntNewValue(Object newValue) { 
		try { 
			if (newValue != null) 
				return Integer.valueOf(newValue.toString());
		} catch (Exception e) { 
			return -1;
		}
		return 0;
	}
	
	//public static Intent createIntent(String screenKey) { 
	//	return AppResources.getInstance().createSettingIntent(screenKey);
	//}
	
}
