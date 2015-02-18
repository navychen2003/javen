package org.javenstudio.common.parser.html;

import org.javenstudio.common.parser.ParseException;

public class TagTraveller implements TagTree.Callbacks {

	private final HTMLHandler mAnalyzer; 
	private TagTree mBeginTree = null; 
	private TagTree mEndTree = null; 
	private TagTree mContentTree = null; 
	private boolean mBeginFound = false; 
	private boolean mEndFound = false; 
	private boolean mInited = false; 
	
	TagTraveller(HTMLHandler a) { 
		mAnalyzer = a; 
	}
	
	TagTree newBeginTree(String rootTag, String[] attrNames, String[] attrValues, 
			Tag.AttributesMatcher matcher) { 
		synchronized (this) { 
			if (!mInited) { 
				mBeginTree = new TagTree(this, rootTag, attrNames, attrValues, matcher); 
				return mBeginTree; 
			} else 
				return null; 
		}
	}
	
	TagTree newEndTree(String rootTag, String[] attrNames, String[] attrValues, 
			Tag.AttributesMatcher matcher) { 
		synchronized (this) { 
			if (!mInited) { 
				mEndTree = new TagTree(this, rootTag, attrNames, attrValues, matcher); 
				return mEndTree; 
			} else 
				return null; 
		}
	}
	
	TagTree newContentTree(String rootTag, String[] attrNames, String[] attrValues, 
			Tag.AttributesMatcher matcher) { 
		synchronized (this) { 
			if (!mInited) { 
				mContentTree = new TagTree(this, rootTag, attrNames, attrValues, matcher); 
				return mContentTree; 
			} else 
				return null; 
		}
	}
	
	public void init() throws ParseException { 
		if (mInited) return; 
		
		mBeginFound = true; 
		mEndFound = false; 
		
		TagTree tree = mBeginTree; 
		if(tree != null) { 
			tree.initTags(); 
			mBeginFound = false; 
		}
		
		tree = mEndTree; 
		if(tree != null) { 
			tree.initTags(); 
			mEndFound = false; 
		}
		
		tree = mContentTree; 
		if(tree != null) 
			tree.initTags(); 
		
		mInited = true; 
	}
	
	public boolean isBeginFound() { return mBeginFound; } 
	public boolean isEndFound() { return mEndFound; } 
	
	@Override 
	public void onFoundTree(TagTree tree, Tag tag) { 
		if (tree == mContentTree) { 
			mAnalyzer.onContentTreeFound(); 
			tree.resetMatching(); 
			
		} else if (tree == mBeginTree) { 
			mBeginFound = true; 
			
		} else if (tree == mEndTree) { 
			mEndFound = true; 
			
		}
	}
	
	public void startTag(String name, Tag.IAttributes attributes) { 
		if (!mBeginFound) { 
			TagTree tree = mBeginTree; 
			if (tree != null)
				tree.onStartTag(name, attributes); 
		}
		
		if (mBeginFound && !mEndFound) { 
			TagTree tree = mEndTree; 
			if (tree != null)
				tree.onStartTag(name, attributes); 
		}
		
		if (mBeginFound && !mEndFound) { 
			TagTree tree = mContentTree; 
			if (tree != null) 
				tree.onStartTag(name, attributes); 
		}
	}
	
	public void endTag(String name) { 
		if (!mBeginFound) { 
			TagTree tree = mBeginTree; 
			if (tree != null)
				tree.onEndTag(name); 
		}
		
		if (mBeginFound && !mEndFound) { 
			TagTree tree = mEndTree; 
			if (tree != null)
				tree.onEndTag(name); 
		}
		
		if (mBeginFound && !mEndFound) { 
			TagTree tree = mContentTree; 
			if (tree != null) 
				tree.onEndTag(name); 
		}
	}
	
}
