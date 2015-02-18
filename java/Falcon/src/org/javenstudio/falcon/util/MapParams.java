package org.javenstudio.falcon.util;

import java.util.Iterator;
import java.util.Map;
import java.io.IOException;


public class MapParams extends Params {
	
	protected final Map<String,String> mMap;

	public MapParams(Map<String,String> map) {
		mMap = map;
	}

	@Override
	public String get(String name) {
		return mMap.get(name);
	}

	@Override
	public String[] getParams(String name) {
		String val = mMap.get(name);
		return val == null ? null : new String[]{val};
	}

	@Override
	public Iterator<String> getParameterNamesIterator() {
		return mMap.keySet().iterator();
	}

	public Map<String,String> getMap() { return mMap; }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("MapParams{");
		
		try {
			boolean first = true;

			for (Map.Entry<String,String> entry : mMap.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (!first) sb.append(',');
				first = false;
				
				sb.append(key);
				sb.append('=');
				
				StrHelper.partialURLEncodeVal(sb, val == null ? "" : val);
			}
		} catch (IOException e) { 
			// can't happen
			throw new RuntimeException(e);
		}

		sb.append("}");
		return sb.toString();
	}
	
}
