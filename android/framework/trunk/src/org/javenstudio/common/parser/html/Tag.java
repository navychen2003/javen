package org.javenstudio.common.parser.html;

import java.util.HashMap;
import java.util.Map;

public class Tag {

	public interface IAttributes { 
		public int getLength(); 
		public String getLocalName(int index); 
		public String getValue(int index); 
	}
	
	public interface AttributesMatcher { 
		public boolean matchAttributes(IAttributes attributes); 
	}
	
	private final Tag mParent; 
	private final String mName; 
	private final Map<String, String> mAttrs; 
	private final AttributesMatcher mAttrsMatcher; 
	private Tag mPrevBrother = null; 
	private Tag mNextBrother = null; 
	private Tag mFirstChild = null; 
	
	Tag(Tag parent, String name, String[] attrNames, String[] attrValues, 
			AttributesMatcher matcher) { 
		mParent = parent; 
		mName = name.toLowerCase(); 
		mAttrs = new HashMap<String, String>(); 
		mAttrsMatcher = matcher; 
		
		if (attrNames != null && attrValues != null) { 
			for (int i=0; i < attrNames.length && i < attrValues.length; i++) { 
				String attrName = attrNames[i]; 
				String attrValue = attrValues[i]; 
				if (attrName != null && attrName.length() > 0 && 
					attrValue != null && attrValue.length() > 0) {
					mAttrs.put(attrName.toLowerCase(), attrValue); 
				}
			}
		}
	}
	
	public final Tag newChild(String name) { 
		return newChild(name, null, null); 
	}
	
	public final Tag newChild(String name, String[] attrNames, String[] attrValues) { 
		return newChild(name, attrNames, attrValues, null); 
	}
	
	public final Tag newChild(String name, AttributesMatcher matcher) { 
		return newChild(name, null, null, matcher); 
	}
	
	private final Tag newChild(String name, String[] attrNames, String[] attrValues, 
			AttributesMatcher matcher) { 
		Tag tag = new Tag(this, name, attrNames, attrValues, matcher); 
		addChild(tag); 
		return tag; 
	}
	
	public final Tag getRoot() { 
		if (mParent == null) return this; 
		else return mParent.getRoot(); 
	}
	
	public final Tag getParent() { 
		return mParent; 
	}
	
	public final String getName() { 
		return mName; 
	}
	
	public final Tag getFirstChild() { 
		return mFirstChild; 
	}
	
	public final Tag getPrevBrother() { 
		return mPrevBrother; 
	}
	
	public final Tag getNextBrother() { 
		return mNextBrother; 
	}
	
	private final void addChild(Tag tag) { 
		if (tag == null || tag == this || tag.getParent() != this) 
			return; 
		
		synchronized (this) { 
			if (mFirstChild == null) { 
				mFirstChild = tag; 
				tag.mPrevBrother = null; 
				tag.mNextBrother = null; 
				return; 
			}
			
			Tag lastChild = mFirstChild; 
			while (lastChild.mNextBrother != null) 
				lastChild = lastChild.mNextBrother; 
			
			lastChild.mNextBrother = tag; 
			tag.mPrevBrother = lastChild; 
			tag.mNextBrother = null; 
		}
	}
	
	public Tag nextTag() { 
		Tag child = mFirstChild; 
		if (child != null) 
			return child; 
		
		return mNextBrother; 
	}
	
	public boolean isRoot() { 
		return mParent == null; 
	}
	
	public boolean isLast() { 
		return mFirstChild == null && mNextBrother == null; 
	}
	
	public final boolean match(String localName, IAttributes attributes) { 
		//if (!mName.equals(localName)) // checked at TagTree
		//	return false; 
		
		return mAttrsMatcher != null ? 
				mAttrsMatcher.matchAttributes(attributes) : 
					matchAttributes(attributes); 
	}
	
	protected boolean matchAttributes(IAttributes attributes) { 
		if (mAttrs.size() > 0) { 
			if (attributes == null) return false; 
			
			int count = 0; 
			for (int i=0; i < attributes.getLength(); i++) { 
				String name = attributes.getLocalName(i); 
				String value = attributes.getValue(i); 
				if (name == null) continue; 
				
				String key = name.toLowerCase(); 
				String val = mAttrs.get(key); 
				
				if (val != null && val.equals(value)) { 
					count ++; 
					if (count >= mAttrs.size()) 
						return true; 
				}
			}
			
			return count == mAttrs.size(); 
		}
		
		return true; 
	}
	
	@Override 
	public String toString() { 
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append(getClass().getSimpleName()); 
		sbuf.append(":<"); 
		sbuf.append(mName); 
		String[] names = mAttrs.keySet().toArray(new String[0]); 
		for (int i=0; names != null && i < names.length; i++) { 
			String name = names[i]; 
			String value = mAttrs.get(name); 
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
