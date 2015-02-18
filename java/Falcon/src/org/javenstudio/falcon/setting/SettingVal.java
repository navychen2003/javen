package org.javenstudio.falcon.setting;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class SettingVal {

	private final Map<String,Object> mValues = 
			new HashMap<String,Object>();
	
	public SettingVal() {}
	
	private static String toString(Object obj) { 
		if (obj != null) { 
			if (obj instanceof String)
				return (String)obj;
			else
				return obj.toString();
		}
		return null;
	}
	
	private static Number toNumber(Object obj) { 
		if (obj != null) { 
			if (obj instanceof Number)
				return (Number)obj;
			else
				return 0;
		}
		return null;
	}
	
	public final String getString(String key, String def) { 
		if (key == null) return def;
		synchronized (mValues) { 
			if (mValues.containsKey(key))
				return toString(mValues.get(key));
			else
				return def;
		}
	}
	
	public final void setString(String key, String val) {
		if (key == null) return;
		synchronized (mValues) { 
			if (val == null)
				mValues.remove(key);
			else
				mValues.put(key, val);
		}
	}
	
	public final Number getNumber(String key, Number def) { 
		if (key == null) return def;
		synchronized (mValues) { 
			if (mValues.containsKey(key))
				return toNumber(mValues.get(key));
			else
				return def;
		}
	}
	
	public final void setNumber(String key, Number val) {
		if (key == null) return;
		synchronized (mValues) { 
			if (val == null)
				mValues.remove(key);
			else
				mValues.put(key, val);
		}
	}
	
	public final int size() { 
		synchronized (mValues) { 
			return mValues.size();
		}
	}
	
	public void clear() { 
		synchronized (mValues) { 
			mValues.clear();
		}
	}
	
	public static void loadSettingVal(SettingVal item, 
			NamedList<Object> listItem) { 
		if (item == null || listItem == null) return;
		
		for (int i=0; i < listItem.size(); i++) { 
			String name = listItem.getName(i);
			Object value = listItem.getVal(i);
			
			if (name != null && value != null) { 
				if (value instanceof String)
					item.setString(name, (String)value);
				else if (value instanceof Number)
					item.setNumber(name, (Number)value);
			}
		}
	}
	
	public static NamedList<Object> toNamedList(SettingVal item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		synchronized (item.mValues) { 
			for (Map.Entry<String, Object> entry : item.mValues.entrySet()) { 
				String key = entry.getKey();
				Object val = entry.getValue();
				
				if (key != null && val != null)
					info.add(key, val);
			}
		}
		
		return info;
	}
	
}
