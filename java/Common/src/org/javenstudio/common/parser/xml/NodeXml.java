package org.javenstudio.common.parser.xml;

import org.javenstudio.common.parser.util.Node;

public class NodeXml extends Node {

	@Override 
	public Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		return new Node(this, name); 
	}
	
	private NodeXml(String rootName) { super(null, rootName); } 
	
	public static class Handler extends AbstractHandler { 
		private final NodeXml mEntity; 
		public Handler(String rootName) { mEntity = new NodeXml(rootName); } 
		public NodeXml getEntity() { return mEntity; }
		protected Node getRootNode() { return getEntity(); }
	}
	
}
