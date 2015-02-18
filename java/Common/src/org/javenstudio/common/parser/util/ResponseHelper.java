package org.javenstudio.common.parser.util;

import org.javenstudio.common.util.Logger;

public class ResponseHelper {
	private static Logger LOG = Logger.getLogger(ResponseHelper.class);

	public static class XmlResponseHandler extends XmlHandler {
		private final ResponseImpl mEntity; 
		
		public XmlResponseHandler() { 
			mEntity = new ResponseImpl(); 
		}
		
		public ResponseImpl getEntity() { return mEntity; }
		
		@Override 
		protected Node getRootNode() { 
			return mEntity; 
		}
		
		@Override 
		protected String normalizeTagName(String localName, String qName) { 
			return qName != null ? qName.toLowerCase() : qName; 
		}
		
		@Override 
		protected String normalizeAttribueName(String name) { 
			return name != null ? name.toLowerCase() : name; 
		}
		
		@Override 
		public String getString(int startLength, int endLength) { 
			return ParseUtils.trim(super.getString(startLength, endLength)); 
		}
	}
	
	public static Response parseXml(String content) { 
		if (content == null || content.length() == 0) 
			return null; 
		
		try { 
			XmlResponseHandler handler = new XmlResponseHandler(); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			Response response =  handler.getEntity(); 
			if (LOG.isDebugEnabled()) 
				LOG.debug("Protocol: parsed response: "+response); 
			
			return response; 
			
		} catch (Exception e) { 
			LOG.error("Protocol: parse error", e); 
		}
		
		return null; 
	}
	
	public static class JsonResponseHandler extends JsonHandler {
		private final ResponseImpl mEntity; 
		
		public JsonResponseHandler() { 
			mEntity = new ResponseImpl(); 
		}
		
		public Response getEntity() { return mEntity; }
		
		@Override 
		protected Node getRootNode() { 
			return mEntity; 
		}
	}
	
	public static Response parseJson(String content) { 
		if (content == null || content.length() == 0) 
			return null; 
		
		try { 
			JsonResponseHandler handler = new JsonResponseHandler(); 
			JsonParser parser = new JsonParser(handler); 
			parser.parse(content); 
			
			Response response =  handler.getEntity(); 
			if (LOG.isDebugEnabled()) 
				LOG.debug("Protocol: parsed response: "+response); 
			
			return response; 
			
		} catch (Exception e) { 
			LOG.error("Protocol: parse error", e); 
		}
		
		return null; 
	}
	
}
