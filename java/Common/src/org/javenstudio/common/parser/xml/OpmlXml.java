package org.javenstudio.common.parser.xml;

import org.javenstudio.common.parser.util.Node;

public class OpmlXml extends Node {
	public final static String ROOTNAME = "opml"; 

	public class Head extends Node { 
		public final static String NAME = "head"; 
		
		Head(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			return new Node(this, name); 
		}
	}
	
	public class Body extends Node { 
		public final static String NAME = "body"; 
		
		Body(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(Outline.NAME)) 
				return new Outline(this); 
			
			return null; 
		}
		
		public int getOutlineCount() { 
			return getChildCount(); 
		}
		
		public Outline getOutlineAt(int index) { 
			Node node = getChildAt(index); 
			if (node != null && node instanceof Outline) 
				return (Outline)node; 
			else 
				return null; 
		}
	}
	
	public class Outline extends Node { 
		public final static String NAME = "outline"; 
		
		Outline(Node parent) { 
			super(parent, NAME); 
		}
		
		@Override 
		public Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(Outline.NAME)) 
				return new Outline(this); 
			
			return null; 
		}
		
		public int getOutlineCount() { 
			return getChildCount(); 
		}
		
		public Outline getOutlineAt(int index) { 
			Node node = getChildAt(index); 
			if (node != null && node instanceof Outline) 
				return (Outline)node; 
			else 
				return null; 
		}
	}
	
	private final Head mHead; 
	private final Body mBody; 
	
	private OpmlXml() { 
		super(null, ROOTNAME); 
		
		mHead = new Head(this); 
		mBody = new Body(this); 
	}
	
	@Override 
	public Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		if (name.equals(Head.NAME)) 
			return mHead; 
		
		if (name.equals(Body.NAME)) 
			return mBody; 
		
		return null; 
	}
	
	public Head getHead() { return mHead; }
	public Body getBody() { return mBody; }
	
	public static class Handler extends AbstractHandler { 
		private final OpmlXml mEntity = new OpmlXml(); 
		public Handler() {} 
		public OpmlXml getEntity() { return mEntity; }
		protected Node getRootNode() { return getEntity(); }
	}
	
}
