package org.javenstudio.common.parser.util;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {

	private final JsonHandler mHandler; 
	
	public JsonParser(JsonHandler handler) { 
		mHandler = handler; 
	}
	
	public void parse(String source) throws JSONException { 
		if (source == null || source.length() == 0) 
			return; 
		
		JSONObject json = new JSONObject(source); 
		travelJSONObject(json); 
	}
	
	private void travelJSONObject(JSONObject json) throws JSONException { 
		if (json == null) return; 
		
		Iterator<?> keys = json.keys(); 
		while (keys.hasNext()) {
            String key = (String)keys.next();
            Object val = json.get(key); 
            if (key == null) 
            	continue; 
            
            travelObject(key, val); 
		}
	}
	
	private void travelObject(String key, Object obj) throws JSONException { 
		if (key == null || obj == null) return; 
		
		mHandler.handleNodeBegin(key); 
		
		if (obj instanceof JSONObject) { 
			travelJSONObject((JSONObject)obj); 
			
		} else if (obj instanceof JSONArray) { 
			travelJSONArray(key, (JSONArray)obj); 
			
		} else 
			mHandler.handleNodeValue(key, obj); 
		
		mHandler.handleNodeEnd(key); 
	}
	
	private void travelJSONArray(String key, JSONArray jsonArr) throws JSONException { 
		if (key == null || jsonArr == null) return; 
		
		for (int i=0; i < jsonArr.length(); i++) { 
			travelObject(key, jsonArr.get(i)); 
		}
	}
	
}
