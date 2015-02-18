package org.javenstudio.falcon.util;

import java.util.Map;

public class SimpleParams extends ModifiableParams {

	public SimpleParams(Map<String,String[]> params) { 
		super(params);
	}
	
	@Override
	public String get(String name) {
		String[] arr = getParams(name);
		if (arr == null) 
			return null;
		
		String s = arr[0];
		if (s.length() == 0) 
			return null;  // screen out blank parameters
		
		return s;
	}
	
}
