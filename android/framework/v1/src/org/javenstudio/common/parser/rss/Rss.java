package org.javenstudio.common.parser.rss;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.Node;

public class Rss extends Node {
	public final static String ROOTNAME = "rss"; 

	public class Channel extends Node { 
		public final static String NAME = "channel"; 
		
		Channel(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(Image.NAME)) { 
				return new Image(this); 
				
			} else if (name.equals(Item.NAME)) { 
				return new Item(this); 
				
			} else 
				return new Node(this, name); 
		}
		
		public final Item[] getItems() { 
			ArrayList<Item> items = new ArrayList<Item>(); 
			for (int i=0; i < getChildCount(); i++) { 
				Node node = getChildAt(i); 
				if (node != null && node instanceof Item) 
					items.add((Item)node); 
			}
			return items.toArray(new Item[items.size()]); 
		}
	}
	
	public class Image extends Node { 
		public final static String NAME = "image"; 
		
		Image(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			return new Node(this, name); 
		}
	}
	
	public class Item extends Node { 
		public final static String NAME = "item"; 
		
		Item(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			return new Node(this, name); 
		}
	}
	
	private final Channel mChannel; 
	
	public Rss() { 
		super(null, ROOTNAME); 
		
		mChannel = new Channel(this); 
	}
	
	@Override 
	public synchronized Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		if (name.equals(Channel.NAME)) 
			return mChannel; 
		
		return null; 
	}
	
	public Channel getChannel() { 
		return mChannel; 
	}
	
	public static class Handler extends AbstractHandler { 
		private final Rss mEntity = new Rss(); 
		public Handler() {} 
		public Rss getEntity() { return mEntity; }
		protected Node getRootNode() { return getEntity(); }
	}
	
}
