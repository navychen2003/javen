package org.javenstudio.falcon.user.device;

import java.util.Comparator;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.Setting;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.setting.SettingSelect;
import org.javenstudio.falcon.setting.Theme;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.IParams;

public class WebDeviceType extends DeviceType {
	private static final Logger LOG = Logger.getLogger(WebDeviceType.class);

	public static final String DEV_WEB = "web";
	public static final String DEV_WEB_TITLE = "Web Browser";
	
	private String mLanguage = null;
	private String mTheme = null;
	
	public WebDeviceType(DeviceManager manager) { 
		super(manager, DEV_WEB, DEV_WEB_TITLE);
	}
	
	@Override
	public WebDevice newDevice(String key, String name, String ver) 
			throws ErrorException {
		return new WebDevice(this, key, name, ver);
	}
	
	private static final String THEME_NAME = "theme";
	private static final String LANGUAGE_NAME = "language";
	
	public String getTheme() { return mTheme; }
	
	public void setTheme(String name) { 
		mTheme = Theme.getThemeName(name);
	}
	
	public String getLanguage() { return mLanguage; }
	
	public void setLanguage(String lang) { 
		mLanguage = Strings.getInstance().getLanguage(lang); 
	}
	
	@Override
	protected boolean loadDeviceSetting(String name, 
			Object val) throws ErrorException { 
		if (name.equals(LANGUAGE_NAME)) { 
			String value = SettingConf.toString(val);
			if (value != null && value.length() > 0) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadDeviceSetting: language=" + value);
				
				setLanguage(value);
				return true;
			}
		} else if (name.equals(THEME_NAME)) { 
			String value = SettingConf.toString(val);
			if (value != null && value.length() > 0) { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadDeviceSetting: theme=" + value);
				
				setTheme(value);
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected SettingGroup initDeviceSetting(SettingCategory category, 
			SettingGroup.UpdateHandler handler) throws ErrorException { 
		SettingGroup group = category.createGroup(getName(), handler);
		group.setTitle(getTitle());
		
		SettingSelect language = new SettingSelect(category.getManager(), LANGUAGE_NAME) { 
				public String getValue() { return getLanguage(); }
				public void setValue(String value) { setLanguage(value); }
			};
		language.setTitle("Language");
		language.setDescription("Help us translate into your language.");
		
		if (language != null) {
			String[] values = Strings.getInstance().getLanguages();
			
			for (int i=0; values != null && i < values.length; i++) { 
				String value = values[i];
				String title = Strings.getInstance().getResourceName(value);
				if (value != null && title != null) 
					language.addOption(value, title);
			}
			
			if (mLanguage == null)
				mLanguage = Strings.getInstance().getDefaultLanguage();
			
			language.setSorter(new Comparator<SettingSelect.Option>() {
					@Override
					public int compare(SettingSelect.Option o1, SettingSelect.Option o2) {
						String value1 = o1 != null ? o1.getValue() : null;
						String value2 = o2 != null ? o2.getValue() : null;
						
						if (value1 == null || value2 == null) { 
							if (value1 == null && value2 == null) return 0;
							if (value1 == null) return -1;
							return 1;
						} else
							return value1.compareTo(value2);
					}
				});
		}
		
		SettingSelect theme = new SettingSelect(category.getManager(), THEME_NAME) { 
				public String getValue() { return getTheme(); }
				public void setValue(String value) { setTheme(value); }
			};
		theme.setTitle("Theme");
		theme.setDescription("Change your current theme.");
		
		if (theme != null) {
			Theme[] themes = Theme.getThemes();
			
			for (int i=0; themes != null && i < themes.length; i++) { 
				Theme t = themes[i];
				if (t == null) continue;
				String name = t.getName();
				String title = t.getTitle();
				if (name != null && title != null) {
					theme.addOption(name, title);
					if (mTheme == null && t.isDefault())
						mTheme = t.getName();
				}
			}
			
			theme.setSorter(new Comparator<SettingSelect.Option>() {
					@Override
					public int compare(SettingSelect.Option o1, SettingSelect.Option o2) {
						String value1 = o1 != null ? o1.getValue() : null;
						String value2 = o2 != null ? o2.getValue() : null;
						
						if (value1 == null || value2 == null) { 
							if (value1 == null && value2 == null) return 0;
							if (value1 == null) return -1;
							return 1;
						} else
							return value1.compareTo(value2);
					}
				});
		}
		
		group.addSetting(language);
		group.addSetting(theme);
		
		return group;
	}
	
	@Override
	protected boolean updateDeviceSetting(SettingGroup group, 
			IParams params) throws ErrorException { 
		if (group == null || params == null)
			throw new NullPointerException();
		
		IUserClient client = UserHelper.checkUserClient(params, null);
		WebUserClient webclient = client != null && client instanceof WebUserClient ? 
				(WebUserClient)client : null;
		
		String input_language = params.getParam(LANGUAGE_NAME);
		String input_theme = params.getParam(THEME_NAME);
		boolean changed = false;
		
		if (input_language != null) { 
			if (input_language.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Language cannot be empty");
			}
			
			String language = getLanguage();
			if (!input_language.equals(language)) { 
				setLanguage(input_language);
				changed = true;
				
				if (webclient != null)
					webclient.setLanguage(input_language);
				
				Setting setting = group.getSetting(LANGUAGE_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateDeviceSetting: setting 'language' changed to '" 
							+ input_language + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Language"), input_language));
			}
		}
		
		if (input_theme != null) { 
			if (input_theme.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Theme cannot be empty");
			}
			
			String theme = getTheme();
			if (!input_theme.equals(theme)) { 
				setTheme(input_theme);
				changed = true;
				
				if (webclient != null)
					webclient.setTheme(input_theme);
				
				Setting setting = group.getSetting(THEME_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateDeviceSetting: setting 'theme' changed to '" 
							+ input_theme + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Theme"), input_theme));
			}
		}
		
		return changed;
	}
	
}
