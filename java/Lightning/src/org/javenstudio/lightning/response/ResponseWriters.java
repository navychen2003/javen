package org.javenstudio.lightning.response;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.lightning.response.writer.JSONResponseWriter;
import org.javenstudio.lightning.response.writer.RawResponseWriter;
import org.javenstudio.lightning.response.writer.SecretJSONResponseWriter;
import org.javenstudio.lightning.response.writer.SecretXMLResponseWriter;
import org.javenstudio.lightning.response.writer.XMLResponseWriter;

public class ResponseWriters {
	static Logger LOG = Logger.getLogger(ResponseWriters.class);

	public static final String STANDARD_TYPE = "standard";
	
	private static final Map<String, ResponseWriter> DEFAULT_WRITERS;
	static {
		Map<String, ResponseWriter> map = new HashMap<String, ResponseWriter>();
		map.put(XMLResponseWriter.TYPE, new XMLResponseWriter());
		map.put(JSONResponseWriter.TYPE, new JSONResponseWriter());
		map.put(RawResponseWriter.TYPE, new RawResponseWriter());
		map.put(SecretXMLResponseWriter.TYPE, new SecretXMLResponseWriter());
		map.put(SecretJSONResponseWriter.TYPE, new SecretJSONResponseWriter());
		map.put(STANDARD_TYPE, map.get(XMLResponseWriter.TYPE));
		DEFAULT_WRITERS = Collections.unmodifiableMap(map);
	}
	
	public static ResponseWriter getDefaultWriter(String type) { 
		ResponseWriter writer = DEFAULT_WRITERS.get(type);
		if (writer == null) 
			writer = DEFAULT_WRITERS.get(STANDARD_TYPE);
		
		return writer;
	}
	
	private final Map<String, ResponseWriter> mWriters = 
			new HashMap<String, ResponseWriter>();
	
	public synchronized void registerWriter(String type, ResponseWriter writer) { 
		if (type == null || writer == null) 
			throw new NullPointerException();
		
		mWriters.put(type, writer);
	}
	
	private ResponseWriter getStandardWriter() { 
		ResponseWriter writer = mWriters.get(STANDARD_TYPE);
		if (writer == null) 
			writer = DEFAULT_WRITERS.get(STANDARD_TYPE);
		
		return writer;
	}
	
	public synchronized ResponseWriter getWriter(String type) { 
		ResponseWriter writer = mWriters.get(type);
		if (writer == null) {
			writer = DEFAULT_WRITERS.get(type);
		
			if (writer == null) {
				writer = getStandardWriter();
				
				if (writer != null && LOG.isDebugEnabled()) {
					LOG.debug("ResponseWriter: " + type + " not found, use " 
							+ writer.getClass().getName());
				}
			}
		}
		
		return writer;
	}
	
}
