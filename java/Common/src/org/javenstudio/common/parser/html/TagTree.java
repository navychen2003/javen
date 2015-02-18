package org.javenstudio.common.parser.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.parser.ParseException;

public class TagTree {

	private static class RootTag extends Tag { 
		public RootTag(String name, String[] attrNames, String[] attrValues, 
				Tag.AttributesMatcher matcher) { 
			super(null, name, attrNames, attrValues, matcher); 
		}
	}
	
	public interface Callbacks { 
		public void onFoundTree(TagTree tree, Tag tag); 
	}
	
	private final Callbacks mCallbacks; 
	private final RootTag mRoot; 
	private final List<Tag> mTagList; 
	private final Map<String, List<Tag>> mTagMap; 
	private boolean mIgnoreNotFound = false; 
	private boolean mInited = false; 
	private Tag mNext = null; 
	
	public TagTree(Callbacks callbacks, String rootTag) { 
		this(callbacks, rootTag, null, null); 
	}
	
	public TagTree(Callbacks callbacks, String rootTag, String[] attrNames, String[] attrValues) {
		this(callbacks, rootTag, attrNames, attrValues, null); 
	}
	
	public TagTree(Callbacks callbacks, String rootTag, Tag.AttributesMatcher matcher) {
		this(callbacks, rootTag, null, null, matcher); 
	}
	
	TagTree(Callbacks callbacks, String rootTag, String[] attrNames, String[] attrValues, 
			Tag.AttributesMatcher matcher) {
		mCallbacks = callbacks; 
		mRoot = new RootTag(rootTag, attrNames, attrValues, matcher); 
		mTagList = new ArrayList<Tag>(); 
		mTagMap = new HashMap<String, List<Tag>>(); 
	}
	
	public final Tag getRootTag() { 
		return mRoot; 
	}
	
	public final void initTags() throws ParseException { 
		initTags(false); 
	}
	
	public final void initTags(boolean ignoreNotFound) throws ParseException { 
		synchronized (this) { 
			if (mInited) return; 
			addTag(mRoot); 
			mNext = mRoot; 
			mIgnoreNotFound = ignoreNotFound; 
			mInited = true; 
		}
	}
	
	private void addTag(Tag tag) throws ParseException { 
		if (tag == null) return; 
		
		for (int i=0; i < mTagList.size(); i++) { 
			Tag child = mTagList.get(i); 
			if (child == tag) 
				throw new ParseException("Tag \""+tag.getName()+"\" already existed"); 
		}
		
		List<Tag> list = mTagMap.get(tag.getName()); 
		if (list == null) 
			list = new ArrayList<Tag>(); 
		list.add(tag); 
		
		mTagList.add(tag); 
		mTagMap.put(tag.getName(), list); 
		
		Tag child = tag.getFirstChild(); 
		Tag brother = tag.getNextBrother(); 
		
		if (child != null && brother != null) 
			throw new ParseException("Tag \""+tag.getName()+"\" can only has child or brother"); 
		
		addTag(child); 
		addTag(brother); 
	}
	
	protected void foundTree(Tag tag) { 
		Callbacks callbacks = mCallbacks; 
		if (callbacks != null) 
			callbacks.onFoundTree(this, tag); 
	}
	
	public void resetMatching() { 
		mNext = mRoot; 
	}
	
	private void onMatched(Tag tag) { 
		if (tag.isLast()) { 
			mNext = null; 
			foundTree(tag); 
			return; 
		}
		
		mNext = tag.nextTag(); 
	}
	
	public void onStartTag(String name, Tag.IAttributes attributes) { 
		final Tag next = mNext; 
		final boolean ignoreNotFound = mIgnoreNotFound; 

		if (next != null) { 
			List<Tag> list = mTagMap.get(name); 
			if (list == null) { 
				if (!ignoreNotFound) resetMatching(); 
				return; 
			}
			
			for (Tag tag : list) { 
				if (tag != next) continue; 
				if (tag.match(name, attributes)) { 
					onMatched(tag); 
					break; 
				}
			}
		}
	}
	
	public void onEndTag(String name) { 
		// do nothing
	}
	
}
