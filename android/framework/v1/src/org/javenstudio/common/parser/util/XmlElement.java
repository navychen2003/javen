package org.javenstudio.common.parser.util;

import java.util.HashMap;
import java.util.Map;

public class XmlElement {

	private final XmlElement mParent; 
	private final String mLocalName; 
	private final Map<String, String> mAttributes; 
	private final int mStartLength; 
	
	public XmlElement(XmlElement parent, String name, int start) { 
		mParent = parent; 
		mLocalName = name; 
		mStartLength = start; 
		mAttributes = new HashMap<String, String>(); 
	}
	
	public XmlElement getParent() { 
		return mParent; 
	}
	
	public synchronized String[] getAttributeNames() { 
		return mAttributes.keySet().toArray(new String[0]); 
	}
	
	public synchronized String getAttribute(String name) { 
		return mAttributes.get(name); 
	}
	
	public synchronized void addAttribute(String name, String value) { 
		mAttributes.put(name, value); 
	}
	
	public String getLocalName() { 
		return mLocalName; 
	}
	
	public int getStartLength() { 
		return mStartLength; 
	}
	
	@Override 
	public String toString() { 
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append(getClass().getSimpleName()); 
		sbuf.append(":<"); 
		sbuf.append(mLocalName); 
		String[] names = mAttributes.keySet().toArray(new String[0]); 
		for (int i=0; names != null && i < names.length; i++) { 
			String name = names[i]; 
			String value = mAttributes.get(name); 
			sbuf.append(" "); 
			sbuf.append(name); 
			sbuf.append("=\""); 
			sbuf.append(value); 
			sbuf.append("\""); 
		}
		sbuf.append(">"); 
		return sbuf.toString(); 
	}
	
}
