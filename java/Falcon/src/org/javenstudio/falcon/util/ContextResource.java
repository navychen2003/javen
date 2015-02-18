package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;

public abstract class ContextResource {

	public abstract ContextLoader getContextLoader();
	
	public abstract ContextNode getNode(String path) throws ErrorException;
	public abstract ContextList getNodes(String path) throws ErrorException;
	
	public abstract String getVal(String path, boolean errIfMissing) 
			throws ErrorException;
	
	public void setVal(String path, String val) throws ErrorException {
	}
	
	public String get(String path) throws ErrorException {
		return getVal(path,true);
	}

	public String get(String path, String def) throws ErrorException {
		String val = getVal(path, false);
		if (val == null || val.length() == 0) {
			if (def != null && def.length() > 0)
				setVal(path, def);
			
			return def;
		}
		
		return val;
	}

	public int getInt(String path) throws ErrorException {
		return Integer.parseInt(getVal(path, true));
	}

	public int getInt(String path, int def) throws ErrorException {
		String val = get(path, String.valueOf(def));
		return val != null ? Integer.parseInt(val) : def;
	}

	public boolean getBool(String path) throws ErrorException {
		return Boolean.parseBoolean(getVal(path, true));
	}

	public boolean getBool(String path, boolean def) throws ErrorException {
		String val = get(path, String.valueOf(def));
		return val != null ? Boolean.parseBoolean(val) : def;
	}

	public float getFloat(String path) throws ErrorException {
		return Float.parseFloat(getVal(path, true));
	}

	public float getFloat(String path, float def) throws ErrorException {
		String val = get(path, String.valueOf(def));
		return val != null ? Float.parseFloat(val) : def;
	}

	public double getDouble(String path) throws ErrorException {
		return Double.parseDouble(getVal(path, true));
	}

	public double getDouble(String path, double def) throws ErrorException {
		String val = get(path, String.valueOf(def));
		return val != null ? Double.parseDouble(val) : def;
	}
	
}
