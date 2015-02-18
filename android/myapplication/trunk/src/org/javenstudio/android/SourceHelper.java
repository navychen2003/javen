package org.javenstudio.android;

import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.android.ResourceHelper;

public class SourceHelper {

	static final String[] sDomains = new String[] { 
			"com", "net", "org", "gov", "edu", "mil", "biz", "name", "info", 
			"mobi", "pro", "travel", "museum", "int", "aero", "post", "rec", "asia", 
			"co", "mn"
		};
	
	static boolean isFirstDomain(String name) { 
		for (String domain : sDomains) { 
			if (domain.equalsIgnoreCase(name)) 
				return true;
		}
		
		return false;
	}
	
	static boolean isDigitalDomain(String name) { 
		if (name != null && name.length() > 0) { 
			for (int i=0; i < name.length(); i++) { 
				char chr = name.charAt(i);
				if (chr < '0' || chr > '9') 
					return false;
			}
			return true;
		}
		return false;
	}
    
    public static String toSourceName(String name) { 
    	if (name != null) { 
    		String[] tokens = name.split("\\.");
			if (tokens != null) { 
				String sitename = "";
				int count = 0;
				boolean alldigital = true;
				
				for (int i=tokens.length-1; i >= 0; i--) { 
					String token = tokens[i];
					if (token == null) continue;
					
					if (!isDigitalDomain(token)) 
						alldigital = false;
					
					if (sitename.length() > 0) 
						sitename = "." + sitename;
					sitename = token + sitename;
					count ++;
					
					if ((count == 1 && token.length() <= 2) || isFirstDomain(token)) 
						continue;
					
					if (count >= 2 && !alldigital) 
						break;
				}
				
				if (sitename.length() > 0 && !alldigital) 
					name = sitename;
			}
    	}
    	
    	return name;
    }
    
    static class SourceItem { 
    	public final String mName;
    	public final int mIconRes;
    	
    	public SourceItem(String name, int iconRes) { 
    		mName = name;
    		mIconRes = iconRes;
    	}
    	
    	public String getName() { return mName; }
    	public int getIconRes() { return mIconRes; }
    }
    
    private static final Map<String, SourceItem> sSources = 
    		new HashMap<String, SourceItem>();
    
    public static void addSource(String name, int iconRes) { 
    	if (name == null || name.length() == 0) 
    		return;
    	
    	synchronized (sSources) { 
    		name = name.toLowerCase();
    		sSources.put(name, new SourceItem(name, iconRes));
    	}
    }
    
    public static int getSourceIconRes(String name, int def) { 
    	if (name == null || name.length() == 0) 
    		return def;
    	
    	synchronized (sSources) { 
    		name = name.toLowerCase();
    		SourceItem item = sSources.get(name);
    		if (item != null) 
    			return item.getIconRes();
    		
    		return def;
    	}
    }
    
    public static Drawable getSourceIcon(String name) { 
    	return getSourceIcon(name, 0);
    }
    
    public static Drawable getSourceIcon(String name, int def) { 
    	int iconRes = getSourceIconRes(name, def);
		if (iconRes != 0) 
			return ResourceHelper.getResources().getDrawable(iconRes);
		
		return null; 
    }
    
}
