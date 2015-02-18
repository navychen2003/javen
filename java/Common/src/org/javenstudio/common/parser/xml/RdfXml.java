package org.javenstudio.common.parser.xml;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.Node;

public class RdfXml extends Node {
	public final static String ROOTNAME = "rdf:rdf"; 
	
	public class Item extends Node { 
		public final static String NAME = "item"; 
		
		Item(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			return new Node(this, name); 
		}
	}

	private RdfXml() { 
		super(null, ROOTNAME); 
	}
	
	@Override 
	public Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		if (name.equals(Item.NAME)) 
			return new Item(this); 
		
		return null; 
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
	
	public static class Handler extends AbstractHandler { 
		private final RdfXml mEntity = new RdfXml(); 
		public Handler() {} 
		public RdfXml getEntity() { return mEntity; }
		protected Node getRootNode() { return getEntity(); }
	}
	
}
