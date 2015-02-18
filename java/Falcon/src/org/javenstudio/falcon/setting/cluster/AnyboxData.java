package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import org.javenstudio.falcon.util.INamedValues;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class AnyboxData {

	private final JSONObject mData;
	
	public AnyboxData(JSONObject data) {
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public String[] getNames() {
		synchronized (mData) {
			Iterator<?> it = mData.keys();
			ArrayList<String> list = new ArrayList<String>();
			if (it != null) {
				while (it.hasNext()) {
					String name = (String)it.next();
					list.add(name);
				}
			}
			return list.toArray(new String[list.size()]);
		}
	}
	
	public boolean has(String name) {
		if (name == null || name.length() == 0)
			return false;
		
		return mData.has(name);
	}
	
	public AnyboxData get(String name) throws IOException {
		if (name == null || name.length() == 0)
			return null;
		
		try {
			JSONObject val = mData.has(name) ? mData.getJSONObject(name) : null;
			if (val != null)
				return new AnyboxData(val);
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
		
		return null;
	}
	
	public AnyboxData[] getArray(String name) throws IOException {
		if (name == null || name.length() == 0)
			return null;
		
		try {
			JSONArray arr = mData.has(name) ? mData.getJSONArray(name) : null;
			if (arr != null) {
				ArrayList<AnyboxData> list = new ArrayList<AnyboxData>();
				
				for (int i=0; i < arr.length(); i++) {
					JSONObject val = arr.getJSONObject(i);
					if (val != null)
						list.add(new AnyboxData(val));
				}
				
				return list.toArray(new AnyboxData[list.size()]);
			}
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
		
		return null;
	}
	
	public String getString(String name) throws IOException {
		if (name == null || name.length() == 0)
			return null;
		
		try {
			return mData.has(name) ? mData.getString(name) : null;
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	public int getInt(String name, int def) throws IOException {
		if (name == null || name.length() == 0)
			return def;
		
		try {
			return mData.has(name) ? mData.getInt(name) : def;
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	public long getLong(String name, long def) throws IOException {
		if (name == null || name.length() == 0)
			return def;
		
		try {
			return mData.has(name) ? mData.getLong(name) : def;
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	public double getDouble(String name, double def) throws IOException {
		if (name == null || name.length() == 0)
			return def;
		
		try {
			return mData.has(name) ? mData.getDouble(name) : def;
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	public boolean getBool(String name, boolean def) throws IOException {
		if (name == null || name.length() == 0)
			return def;
		
		try {
			return mData.has(name) ? mData.getBoolean(name) : def;
		} catch (Throwable e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{data=" + mData + "}";
	}
	
	public void copyTo(INamedValues<Object> values) {
		if (values == null) throw new NullPointerException();
		copyTo(mData, values);
	}
	
	private static void copyTo(JSONObject data, INamedValues<Object> values) {
		if (data == null || values == null) return;
		synchronized (data) {
			Iterator<?> it = data.keys();
			if (it == null) return;
			
			while (it.hasNext()) {
				String name = (String)it.next();
				if (name == null) continue;
				
				Object value = toNamedList(data.get(name));
				values.add(name, value);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static Object toNamedList(Object value) {
		if (value == null) return null;
		
		if (value instanceof JSONObject) {
			NamedList<Object> list = new NamedMap<Object>();
            copyTo((JSONObject)value, list);
            return list;
            
        } else if (value instanceof JSONArray) {
        	ArrayList<Object> list = new ArrayList<Object>();
        	JSONArray arr = ((JSONArray) value);
        	for (int i=0; i < arr.length(); i++) {
        		Object val = toNamedList(arr.get(i));
        		list.add(val);
        	}
        	return list.toArray(new Object[list.size()]);
        	
        } else if (value instanceof Map) {
        	NamedList<Object> list = new NamedMap<Object>();
        	Iterator it = ((Map)value).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String key = (String)e.getKey();
                Object val = e.getValue();
                list.add(key, toNamedList(val));
            }
            return list;
            
        } else if (value instanceof Collection) {
        	ArrayList<Object> list = new ArrayList<Object>();
        	Iterator it = ((Collection)value).iterator();
            while (it.hasNext()) {
            	Object val = toNamedList(it.next());
            	list.add(val);
            }
            return list.toArray(new Object[list.size()]);
            
        } else if (value.getClass().isArray()) {
        	ArrayList<Object> list = new ArrayList<Object>();
        	int length = Array.getLength(value);
        	for (int i=0; i < length; i++) {
        		Object val = toNamedList(Array.get(value, i));
        		list.add(val);
        	}
        	return list.toArray(new Object[list.size()]);
        	
        } else if (value instanceof Number) {
            return value;
            
        } else if (value instanceof Boolean) {
            return value;
            
        } else if (value instanceof JSONString) {
            String val;
            try {
                val = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                val = value.toString();
            }
            return val;
            
        } else {
        	return value.toString();
        }
	}
	
}
