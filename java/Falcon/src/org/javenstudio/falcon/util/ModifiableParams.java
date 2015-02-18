package org.javenstudio.falcon.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;

/**
 * This class is similar to MultiMapParams except you can edit the 
 * parameters after it is initialized.  It has helper functions to set/add
 * integer and boolean param values.
 * 
 */
public class ModifiableParams extends Params {

	private final Map<String, String[]> mValues;
  
	public ModifiableParams() {
		// LinkedHashMap so params show up in CGI in the same order as they are entered
		mValues = new LinkedHashMap<String, String[]>();
	}

	/** 
	 * Constructs a new ModifiableParams directly using the provided 
	 * Map&lt;String,String[]&gt; 
	 */
	public ModifiableParams(Map<String,String[]> v) {
		if (v == null) throw new NullPointerException();
		mValues = new LinkedHashMap<String, String[]>(v);
	}

	/** Constructs a new ModifiableParams, copying values from an existing Params */
	public ModifiableParams(Params params) throws ErrorException {
		mValues = new LinkedHashMap<String, String[]>();
		if (params != null) 
			add(params);
	}

	/**
	 * Replace any existing parameter with the given name. 
	 * if val==null remove key from params completely.
	 */
	public ModifiableParams set(String name, String ... val) {
		if (val == null || (val.length == 1 && val[0] == null)) 
			mValues.remove(name);
		else 
			mValues.put(name, val);
		
		return this;
	}
  
	public ModifiableParams set(String name, int val) {
		set(name, String.valueOf(val));
		return this;
	}
  
	public ModifiableParams set(String name, boolean val) {
		set(name, String.valueOf(val));
		return this;
	}

	/**
	 * Add the given values to any existing name
	 * @param name Key
	 * @param val Array of value(s) added to the name. NOTE: If val is null 
	 *     or a member of val is null, then a corresponding null reference 
	 *     will be included when a get method is called on the key later.
	 *  @return this
	 */
	public ModifiableParams add(String name, String... val) {
		String[] old = mValues.put(name, val);
		if (old != null) {
			if (val == null || val.length < 1) {
				String[] both = new String[old.length+1];
				System.arraycopy(old, 0, both, 0, old.length);
				both[old.length] = null;
				mValues.put(name, both);
				
			} else {
				String[] both = new String[old.length+val.length];
				System.arraycopy(old, 0, both, 0, old.length);
				System.arraycopy(val, 0, both, old.length, val.length);
				mValues.put(name, both);
			}
		}
		return this;
	}

	public void add(Params params) throws ErrorException {
		if (params == null) return;
		Iterator<String> names = params.getParameterNamesIterator();
		while (names.hasNext()) {
			String name = names.next();
			set(name, params.getParams(name));
		}
	}
  
	/** remove a field at the given name */
	public String[] remove(String name) {
		return mValues.remove( name );
	}
  
	/** clear all parameters */
	public void clear() {
		mValues.clear();
	}
  
	/** 
	 * remove the given value for the given name
	 * 
	 * @return true if the item was removed, false if null or not present
	 */
	public boolean remove(String name, String value) {
		String[] tmp = mValues.get(name);
		if (tmp == null) 
			return false;
		
		for (int i=0; i < tmp.length; i++) {
			if (tmp[i].equals(value)) {
				String[] tmp2 = new String[tmp.length-1];
				if (tmp2.length == 0) {
					tmp2 = null;
					remove(name);
				} else {
					System.arraycopy(tmp, 0, tmp2, 0, i);
					System.arraycopy(tmp, i+1, tmp2, i, tmp.length-i-1);
					set(name, tmp2);
				}
				
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String get(String param) {
		String[] v = mValues.get(param);
		if (v != null && v.length > 0) 
			return v[0];
		
		return null;
	}

	@Override
	public Iterator<String> getParameterNamesIterator() {
		return mValues.keySet().iterator();
	}
  
	public String[] getParameterNames() {
		return mValues.keySet().toArray(new String[mValues.size()]);
	}

	@Override
	public String[] getParams(String param) {
		return mValues.get(param);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append(getClass().getSimpleName());
		sb.append('{');
		
		try {
			boolean first = true;

			for (Map.Entry<String,String[]> entry : mValues.entrySet()) {
				String key = entry.getKey();
				String[] valarr = entry.getValue();
				
				for (String val : valarr) {
					if (!first) sb.append('&');
					first = false;
					sb.append(key);
					sb.append('=');
					
					if (val != null) 
						sb.append(URLEncoder.encode(val, "UTF-8"));
				}
			}
		} catch (IOException e) { 
			// can't happen
			throw new RuntimeException(e); 
		}

		sb.append('}');
		return sb.toString();
	}
	
}
