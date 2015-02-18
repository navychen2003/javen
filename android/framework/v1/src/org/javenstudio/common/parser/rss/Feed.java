package org.javenstudio.common.parser.rss;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.Node;

public class Feed extends Node {
	public final static String ROOTNAME = "feed"; 

	public class Entry extends Node { 
		public final static String NAME = "entry"; 
		
		Entry(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			return new Node(this, name); 
		}
	}
	
	@Override 
	public synchronized Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		if (name.equals(Entry.NAME)) 
			return new Entry(this); 
		
		return null; 
	}
	
	public final Entry[] getItems() { 
		ArrayList<Entry> items = new ArrayList<Entry>(); 
		for (int i=0; i < getChildCount(); i++) { 
			Node node = getChildAt(i); 
			if (node != null && node instanceof Entry) 
				items.add((Entry)node); 
		}
		return items.toArray(new Entry[items.size()]); 
	}
	
	public Feed() { super(null, ROOTNAME); } 
	
	public static class Handler extends AbstractHandler { 
		private final Feed mEntity = new Feed(); 
		public Handler() {} 
		public Feed getEntity() { return mEntity; }
		protected Node getRootNode() { return getEntity(); }
	}
	
}
