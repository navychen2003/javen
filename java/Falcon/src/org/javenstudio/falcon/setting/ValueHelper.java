package org.javenstudio.falcon.setting;

import org.javenstudio.falcon.util.NamedList;

public class ValueHelper {

	public static String getString(NamedList<Object> item, String name) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toString(val);
		}
		return null;
	}
	
	public static String toString(Object val) { 
		if (val != null) { 
			if (val instanceof String) 
				return (String)val;
			//if (val instanceof CharSequence)
				return val.toString();
		}
		return null;
	}
	
	public static int getInt(NamedList<Object> item, String name) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toInt(val);
		}
		return 0;
	}
	
	public static int toInt(Object val) { 
		if (val != null) { 
			if (val instanceof Number) 
				return ((Number)val).intValue();

			try {
				return Integer.parseInt(val.toString());
			} catch (Throwable e) {
			}
		}
		return 0;
	}
	
	public static long getLong(NamedList<Object> item, String name) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toLong(val);
		}
		return 0;
	}
	
	public static long toLong(Object val) { 
		if (val != null) { 
			if (val instanceof Number) 
				return ((Number)val).longValue();

			try {
				return Long.parseLong(val.toString());
			} catch (Throwable e) {
			}
		}
		return 0;
	}
	
	public static float getFloat(NamedList<Object> item, String name) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toFloat(val);
		}
		return 0;
	}
	
	public static float toFloat(Object val) { 
		if (val != null) { 
			if (val instanceof Number) 
				return ((Number)val).floatValue();

			try {
				return Float.parseFloat(val.toString());
			} catch (Throwable e) {
			}
		}
		return 0;
	}
	
	public static double getDouble(NamedList<Object> item, String name) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toDouble(val);
		}
		return 0;
	}
	
	public static double toDouble(Object val) { 
		if (val != null) { 
			if (val instanceof Number) 
				return ((Number)val).doubleValue();

			try {
				return Double.parseDouble(val.toString());
			} catch (Throwable e) {
			}
		}
		return 0;
	}
	
	public static boolean getBool(NamedList<Object> item, String name) { 
		return getBool(item, name, false);
	}
	
	public static boolean getBool(NamedList<Object> item, String name, boolean def) { 
		if (item != null && name != null) { 
			Object val = item.get(name);
			return toBool(val, def);
		}
		return def;
	}
	
	public static boolean toBool(Object val) { 
		return toBool(val, false);
	}
	
	public static boolean toBool(Object val, boolean def) { 
		if (val != null) { 
			if (val instanceof Boolean) 
				return ((Boolean)val).booleanValue();

			try {
				return Boolean.parseBoolean(val.toString());
			} catch (Throwable e) {
			}
		}
		return def;
	}
	
	public static byte[] toBytes(Object val) {
		return toBytes(val, null);
	}
	
	public static byte[] toBytes(Object val, byte[] def) {
		if (val != null) { 
			if (val instanceof byte[]) 
				return (byte[])val;
		}
		return def;
	}
	
}
