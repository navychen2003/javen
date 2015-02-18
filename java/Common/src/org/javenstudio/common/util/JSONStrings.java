package org.javenstudio.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONStrings extends Strings.AbstractLoader {
	private static final Logger LOG = Logger.getLogger(JSONStrings.class);
	
	public JSONStrings(String path) {
		super(path);
	}

	@Override
	protected Map<String, String> loadResource(File file) {
		if (file == null || !file.exists() || !file.isFile())
			return null;
		
		if (LOG.isInfoEnabled())
			LOG.info("loadResource: " + file);
		
		StringBuilder sbuf = new StringBuilder();
		String line = null;
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(fis, "UTF-8"));
			
			while ((line = reader.readLine()) != null) { 
				sbuf.append(line);
				sbuf.append('\n');
			}
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("loadResource: " + file + " error: " + e, e);
			
			return null;
		} finally { 
			try {
				if (fis != null)
					fis.close();
			} catch (Throwable ex) { 
			}
		}
		
		if (sbuf.length() == 0)
			return null;
		
		JSONObject json = new JSONObject(sbuf.toString());
		JSONArray names = json.names();
		
		Map<String, String> map = new HashMap<String, String>();
		
		for (int i=0; names != null && i < names.length(); i++) { 
			String name = names.getString(i);
			String value = json.getString(name);
			
			if (name != null && value != null)
				map.put(name, value);
		}
		
		return map;
	}

}
