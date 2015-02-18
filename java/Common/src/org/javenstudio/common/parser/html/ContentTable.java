package org.javenstudio.common.parser.html;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.parser.ParseException;

public abstract class ContentTable {

	private final ContentHandler mHandler; 
	private final Map<String, ContentField> mFields; 
	private final String[] mFieldNames; 
	
	public ContentTable(ContentHandler handler) throws ParseException { 
		mHandler = handler; 
		mFields = new HashMap<String, ContentField>(); 
		
		onInitFields(); 
		mFieldNames = mFields.keySet().toArray(new String[0]); 
		
		for (int i=0; mFieldNames != null && i < mFieldNames.length; i++) { 
			String name = mFieldNames[i]; 
			ContentField field = getField(name); 
			if (field != null) { 
				TagTree tree = field.getTagTree(); 
				if (tree != null) 
					tree.initTags(true); 
			}
		}
	}
	
	public boolean isDebug() { return false; }
	
	public final ContentHandler getContentHandler() { 
		return mHandler; 
	}
	
	public abstract void onInitFields(); 
	
	public void onFoundField(ContentField field, TagElement e, int endLength) { 
		// do nothing 
	}
	
	public final TagTree newTagTree(String rootTag) { 
		return newTagTree(rootTag, null, null); 
	}
	
	public final TagTree newTagTree(String rootTag, String[] attrNames, String[] attrValues) { 
		return new TagTree(getContentHandler(), rootTag, attrNames, attrValues); 
	}
	
	public final TagTree newTagTree(String rootTag, Tag.AttributesMatcher matcher) { 
		return new TagTree(getContentHandler(), rootTag, matcher); 
	}
	
	public final void addField(ContentField field) { 
		if (field == null || field.getTable() != this) 
			return; 
		
		mFields.put(field.getName(), field); 
	}
	
	public final String[] getFieldNames() { 
		return mFieldNames; 
	}
	
	public final ContentField getField(String name) { 
		return name != null ? mFields.get(name) : null; 
	}
	
}
