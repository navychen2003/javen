package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;

public class Theme {
	private static final Logger LOG = Logger.getLogger(Theme.class);

	private static final List<Theme> sThemes = new ArrayList<Theme>();
	
	public static Theme addTheme(String name, String title) { 
		if (name == null || title == null) return null;
		synchronized (sThemes) { 
			for (Theme theme : sThemes) { 
				if (theme == null) continue;
				if (name.equals(theme.getName()))
					return theme;
			}
			
			Theme theme = new Theme(name, title);
			sThemes.add(theme);
			
			if (LOG.isDebugEnabled())
				LOG.debug("addTheme: name=" + name + " title=" + title);
			
			return theme;
		}
	}
	
	public static Theme getTheme(String name) { 
		if (name == null) return null;
		synchronized (sThemes) { 
			for (Theme theme : sThemes) { 
				if (theme == null) continue;
				if (name.equals(theme.getName()))
					return theme;
			}
			return null;
		}
	}
	
	public static Theme[] getThemes() { 
		synchronized (sThemes) { 
			return sThemes.toArray(new Theme[sThemes.size()]);
		}
	}
	
	public static String getThemeName(String name) { 
		Theme theme = getTheme(name);
		if (theme == null) { 
			Theme[] themes = getThemes();
			Theme first = null;
			
			for (int i=0; themes != null && i < themes.length; i++) { 
				Theme t = themes[i];
				if (t != null) { 
					if (first == null) first = t;
					if (t.isDefault()) { 
						theme = t;
						break;
					}
				}
			}
			
			if (theme == null)
				theme = first;
		}
		return theme != null ? theme.getName() : null;
	}
	
	private final String mName;
	private final String mTitle;
	
	private boolean mDefault = false;
	
	private Theme(String name, String title) { 
		if (name == null || title == null) throw new NullPointerException();
		mName = name;
		mTitle = title;
	}
	
	public String getName() { return mName; }
	public String getTitle() { return mTitle; }
	public boolean isDefault() { return mDefault; }
	
	public void setDefault(boolean def) { 
		if (def) { 
			synchronized (sThemes) { 
				for (Theme t : sThemes) { 
					if (t != null) t.mDefault = false;
				}
			}
		}
		
		mDefault = def;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + mName + "}";
	}
	
}
