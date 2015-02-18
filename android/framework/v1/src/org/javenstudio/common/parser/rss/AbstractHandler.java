package org.javenstudio.common.parser.rss;

import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.parser.util.XmlHandler;

public abstract class AbstractHandler extends XmlHandler {

	AbstractHandler() {} 
	
	@Override 
	protected String normalizeTagName(String localName, String qName) { 
		return qName != null ? qName.toLowerCase() : qName; 
	}
	
	@Override 
	protected String normalizeAttribueName(String name) { 
		return name != null ? name.toLowerCase() : name; 
	}
	
	@Override 
	public synchronized String getString(int startLength, int endLength) { 
		return ParseUtils.trim(super.getString(startLength, endLength)); 
	}
	
}
