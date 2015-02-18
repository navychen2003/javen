package org.javenstudio.common.parser.html;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.XmlElement;

public class TagElement extends XmlElement implements Tag.IAttributes {

	private final HTMLHandler mAnalyzer; 
	private ArrayList<String> mNames = null; 
	
	public TagElement(HTMLHandler a, String name, int start) { 
		super(null, name, start); 
		
		mAnalyzer = a; 
	}
	
	public final HTMLHandler getAnalyzer() { 
		return mAnalyzer; 
	}
	
	@Override 
	public synchronized void addAttribute(String name, String value) { 
		super.addAttribute(name, value); 
		
		if (name != null) { 
			if (mNames == null) mNames = new ArrayList<String>(); 
			mNames.add(name); 
		}
	}
	
	@Override 
	public synchronized int getLength() { 
		if (mNames == null) return 0; 
		return mNames.size(); 
	}
	
	@Override 
	public synchronized String getLocalName(int index) { 
		if (mNames == null) return null; 
		return index >= 0 && index < mNames.size() ? mNames.get(index) : null; 
	}
	
	@Override 
	public synchronized String getValue(int index) { 
		String name = getLocalName(index); 
		return name != null ? getAttribute(name) : null; 
	}
	
	@Override 
	public boolean equals(Object obj) { 
		if (obj == this) return true; 
		if (obj == null || !(obj instanceof TagElement)) 
			return false; 
		
		TagElement e = (TagElement)obj; 
		if (e.getStartLength() != getStartLength()) 
			return false; 
		
		String thisName = getLocalName(); 
		String thatName = e.getLocalName(); 
		
		if (thisName == null) { 
			if (thatName != null) return false; 
		} else if (thatName == null) 
			return false; 
		
		if (!thisName.equals(thatName)) 
			return false; 
		
		for (int i=0; i < getLength(); i++) { 
			String name = getLocalName(i); 
			String value1 = getAttribute(name); 
			String value2 = getAttribute(name); 
			
			if (value1 == null) { 
				if (value2 != null) return false; 
			} else if (value2 == null) 
				return false; 
			
			if (!value1.equals(value2)) 
				return false; 
		}
		
		return true; 
	}
	
}
