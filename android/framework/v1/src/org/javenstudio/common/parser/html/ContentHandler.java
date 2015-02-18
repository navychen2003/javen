package org.javenstudio.common.parser.html;

import org.javenstudio.common.util.Logger;

public class ContentHandler implements TagTree.Callbacks {
	static final Logger LOG = Logger.getLogger(ContentHandler.class);
	
	private final ContentTable mTable; 
	private TagElement mCurrent = null; 
	
	public ContentHandler(ContentTableFactory factory) { 
		mTable = factory.createContentTable(this); 
	}
	
	public final ContentTable getContentTable() { 
		return mTable; 
	}
	
	public void handleStart(TagElement e) { }
	public void handleElementStart(TagElement e) { handleTagStart(e); }
	public void handleElementEnd(TagElement e, int endLength) { handleTagEnd(e, endLength); }
	public void handleEnd(TagElement e) { }
	
	@Override 
	public synchronized void onFoundTree(TagTree tree, Tag tag) { 
		if (tree == null) return; 
		
		final String[] names = mTable.getFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) { 
			ContentField field = mTable.getField(names[i]); 
			if (field == null) continue; 
			
			if (tree == field.getTagTree() && field.mFoundTag == null) 
				field.mFoundTag = mCurrent; 
		}
	}
	
	protected synchronized final void handleTagStart(TagElement e) { 
		final String localName = e != null ? e.getLocalName() : null; 
		if (localName == null) return; 
		
		mCurrent = e; 
		if (LOG.isDebugEnabled() && mTable.isDebug())
			LOG.debug("ContentHandler.handleTagStart: " + e);
		
		final String[] names = mTable.getFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) { 
			ContentField field = mTable.getField(names[i]); 
			if (field == null) continue; 
			
			TagTree tree = field.getTagTree(); 
			if (tree != null) 
				tree.onStartTag(localName, e); 
		}
	}
	
	protected synchronized final void handleTagEnd(TagElement e, int endLength) { 
		final String localName = e != null ? e.getLocalName() : null; 
		if (localName == null) return; 
		
		if (LOG.isDebugEnabled() && mTable.isDebug())
			LOG.debug("ContentHandler.handleTagEnd: " + e);
		
		final String[] names = mTable.getFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) { 
			ContentField field = mTable.getField(names[i]); 
			if (field == null) continue; 
			
			TagElement foundTag = field.mFoundTag; 
			if (foundTag != null && foundTag.equals(e)) { 
				onFoundField(field, e, endLength); 
				field.mFoundTag = null; 
			}
			
			TagTree tree = field.getTagTree(); 
			if (tree != null) 
				tree.onEndTag(localName); 
		}
		
		mCurrent = null; 
	}
	
	protected void onFoundField(ContentField field, TagElement e, int endLength) { 
		mTable.onFoundField(field, e, endLength); 
	}
	
}
