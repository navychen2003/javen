package org.javenstudio.common.parser.util;

public abstract class JsonHandler {

	public static final String CONTENT_NAME = "content"; 
	
	private Node mCurrent = null; 
	
	public JsonHandler() {}
	
	protected abstract Node getRootNode(); 
	
	protected final void handleNodeBegin(String name) { 
		if (name == null) return; 
		
		Node current = mCurrent; 
		if (current == null) { 
			Node root = getRootNode(); 
			if (root != null && name.equals(root.getName())) { 
				mCurrent = root; 
				current = root; 
			}
		} else { 
			Node node = current.newChildNode(name); 
			if (node != null) { 
				current.addChild(node); 
				mCurrent = node; 
			}
		}
	}
	
	protected final void handleNodeEnd(String name) { 
		if (name == null) return; 
		
		Node node = mCurrent; 
		if (node == null) return; 
		
		if (name.equals(node.getName())) { 
			if (node.getParent() != null) 
				mCurrent = node.getParent(); 
			
			node.onNodeEnded(); 
		}
	}
	
	protected final void handleNodeValue(String name, Object value) { 
		if (name == null) return; 
		
		Node node = mCurrent; 
		if (node == null) return; 

		if (name.equals(node.getName())) { 
			setNodeValue(node, value); 
			
		} else 
			setNodeAttribute(node, name, value); 
	}
	
	protected void setNodeValue(Node node, Object value) { 
		if (node == null || value == null) 
			return; 
		
		if (node.getChildCount() == 0) { 
			String str = objectToString(value); 
			
			node.setValue(str); 
		}
	}
	
	protected void setNodeAttribute(Node node, String name, Object value) { 
		if (node == null || name == null || value == null) 
			return; 
		
		String str = objectToString(value); 
		
		if (node.getChildCount() == 0 && node.getValue() == null && name.equals(CONTENT_NAME)) { 
			node.setValue(str); 
			return; 
		}
		
		node.addAttribute(name, str); 
	}
	
	protected String objectToString(Object value) { 
		return value instanceof String ? (String)value : value.toString(); 
	}
	
}
