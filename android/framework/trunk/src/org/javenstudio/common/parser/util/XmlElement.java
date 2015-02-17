package org.javenstudio.common.parser.util;

import java.util.ArrayList;

public class XmlElement {

	private static final int MIN_CAPACITY_INCREMENT = 8;
	private static final int MIN_CAPACITY = 8;
	
	private final XmlElement mParent; 
	private final String mLocalName; 
	private final int mStartLength; 
	
	private String[] mAttrNames = null; 
	private String[] mAttrValues = null;
	private int mAttrSize = 0;
	
	public XmlElement(XmlElement parent, String name, int start) { 
		mParent = parent; 
		mLocalName = name; 
		mStartLength = start; 
	}
	
	public XmlElement getParent() { return mParent; }
	
	@SuppressWarnings("unused")
	private final String[] getAttributeNames() { 
		if (mAttrNames == null) 
			return null;
		
		ArrayList<String> names = new ArrayList<String>();
		
		for (int i=0; i < mAttrSize; i++) { 
			String attrName = mAttrNames[i];
			names.add(attrName);
		}
		
		return names.toArray(new String[names.size()]);
	}
	
	public final String getAttribute(String name) { 
		if (name == null || mAttrNames == null) 
			return null;
		
		for (int i=0; i < mAttrSize; i++) { 
			String attrName = mAttrNames[i];
			String attrValue = mAttrValues[i];
			
			if (name.equals(attrName)) 
				return attrValue;
		}
		
		return null;
	}
	
	@SuppressWarnings("unused")
	private final String[] getAttributes(String name) { 
		if (name == null || mAttrNames == null) 
			return null;
		
		ArrayList<String> values = new ArrayList<String>();
		
		for (int i=0; i < mAttrSize; i++) { 
			String attrName = mAttrNames[i];
			String attrValue = mAttrValues[i];
			
			if (name.equals(attrName)) 
				values.add(attrValue);
		}
		
		return values.toArray(new String[values.size()]);
	}
	
	public void addAttribute(String name, String value) { 
		if (name == null || value == null) 
			return;
		
		if (mAttrNames != null) { 
			if (mAttrSize == mAttrNames.length) {
				String[] attrNames = new String[mAttrSize + MIN_CAPACITY_INCREMENT];
				String[] attrValues = new String[mAttrSize + MIN_CAPACITY_INCREMENT];
			
				System.arraycopy(mAttrNames, 0, attrNames, 0, mAttrSize);
				System.arraycopy(mAttrValues, 0, attrValues, 0, mAttrSize);
			
				mAttrNames = attrNames;
				mAttrValues = attrValues;
			}
		} else { 
			mAttrNames = new String[MIN_CAPACITY];
			mAttrValues = new String[MIN_CAPACITY];
		}
		
		mAttrNames[mAttrSize] = name;
		mAttrValues[mAttrSize] = value;
		mAttrSize ++;
	}
	
	public final int getAttributeSize() { return mAttrSize; }
	
	public final String getAttributeName(int index) { 
		return index >= 0 && index < mAttrSize ? mAttrNames[index] : null;
	}
	
	public final String getAttributeValue(int index) { 
		return index >= 0 && index < mAttrSize ? mAttrValues[index] : null;
	}
	
	public final String getLocalName() { return mLocalName; }
	
	public final int getStartLength() { return mStartLength; }
	
	@Override 
	public String toString() { 
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append(getClass().getSimpleName()); 
		sbuf.append(":<"); 
		sbuf.append(mLocalName); 
		for (int i=0; i < mAttrSize; i++) { 
			String name = mAttrNames[i]; 
			String value = mAttrValues[i]; 
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
