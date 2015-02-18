package org.javenstudio.common.parser.html;

public class ContentField {

	private final ContentTable mTable; 
	private final TagTree mTree; 
	private final String mName; 
	
	TagElement mFoundTag = null; 
	
	public ContentField(ContentTable table, String name, TagTree tree) { 
		mTable = table; 
		mTree = tree; 
		mName = name; 
	}
	
	public final ContentTable getTable() { 
		return mTable; 
	}
	
	public final TagTree getTagTree() { 
		return mTree; 
	}
	
	public final String getName() { 
		return mName; 
	}
	
}
