package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

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
	
}
