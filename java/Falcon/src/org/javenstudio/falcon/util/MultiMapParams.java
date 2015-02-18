package org.javenstudio.falcon.util;

import java.util.Iterator;
import java.util.Map;
import java.io.IOException;

public class MultiMapParams extends Params {
	
	protected final Map<String,String[]> mMap;

	public static void addParam(String name, String val, Map<String,String[]> map) {
		String[] arr = map.get(name);
		if (arr == null) {
			arr = new String[]{val};
			
		} else {
			String[] newarr = new String[arr.length+1];
			System.arraycopy(arr, 0, newarr, 0, arr.length);
			newarr[arr.length] = val;
			arr = newarr;
		}
		
		map.put(name, arr);
	}

	public MultiMapParams(Map<String,String[]> map) {
		mMap = map;
	}

	@Override
	public String get(String name) {
		String[] arr = getParams(name);
		return arr == null ? null : arr[0];
	}

	@Override
	public synchronized String[] getParams(String name) {
		return mMap.get(name);
	}

	@Override
	public synchronized Iterator<String> getParameterNamesIterator() {
		return mMap.keySet().iterator();
	}

	//public Map<String,String[]> getMap() { return mMap; }
	
	public void addParam(String name, String val) {
		addParam(name, val, mMap);
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append(getClass().getSimpleName());
		sb.append('{');
		
		try {
			boolean first = true;

			for (Map.Entry<String,String[]> entry : mMap.entrySet()) {
				String key = entry.getKey();
				String[] valarr = entry.getValue();

				for (String val : valarr) {
					if (!first) sb.append(',');
					first = false;
					
					sb.append(key);
					sb.append('=');
					
					StrHelper.partialURLEncodeVal(sb, val == null ? "" : val);
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
