package org.javenstudio.common.parser.util;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.javenstudio.common.parser.TagHandler;

public abstract class XmlHandler implements TagHandler {

	private final Stack<XmlElement> mElements; 
	private final StringBuilder mBuffer; 
	private Node mCurrent = null; 
	
	public XmlHandler() {
		mBuffer = new StringBuilder(); 
		mElements = new Stack<XmlElement>(); 
	}
	
	@Override 
	public void handleStartDocument() throws SAXException { 
		// do nothing
	}
	
	@Override 
	public void handleEndDocument() { 
		// do nothing
	}
	
	protected String normalizeTagName(String localName, String qName) { 
		return qName; 
	}
	
	protected String normalizeAttribueName(String name) { 
		return name; 
	}
	
	@Override 
	public final void handleStartTag(String localName, String qName, Attributes attributes) { 
		final String tagName = normalizeTagName(localName, qName); 
		final XmlElement parent = !mElements.empty() ? mElements.peek() : null; 
		final int startLength = mBuffer.length(); 
		final XmlElement e = new XmlElement(parent, tagName, startLength); 
		
		for (int i=0; attributes != null && i < attributes.getLength(); i++) { 
			String name = attributes.getLocalName(i); 
			String value = attributes.getValue(i); 
			
			if (name != null && value != null) 
				e.addAttribute(normalizeAttribueName(name), value); 
		}
		
		mElements.push(e); 
		handleElementBegin(e); 
	}
	
	@Override 
	public final void handleEndTag(String localName, String qName) { 
		final String tagName = normalizeTagName(localName, qName); 
		final int endLength = mBuffer.length(); 
		final XmlElement e = !mElements.empty() ? mElements.pop() : null; 
		
		if (e != null) { 
			if (tagName != null && tagName.equals(e.getLocalName())) { 
				handleElementEnd(e, endLength); 
			}
		}
	}
	
	protected abstract Node getRootNode(); 
	
	protected synchronized final void handleElementBegin(XmlElement e) { 
		if (e == null) return; 
		
		final String name = e.getLocalName(); 
		
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
	
	protected synchronized final void handleElementEnd(XmlElement e, int endLength) { 
		if (e == null) return; 
		
		Node node = mCurrent; 
		if (node != null && e.getLocalName().equals(node.getName())) { 
			if (node.getParent() != null) 
				mCurrent = node.getParent(); 
			
			setNodeAttributes(node, e); 
			setNodeValue(node, e.getStartLength(), endLength); 
			node.onNodeEnded(); 
		}
	}
	
	protected void setNodeValue(Node node, int startLength, int endLength) { 
		if (node != null && node.getChildCount() == 0) 
			node.setValue(getString(startLength, endLength)); 
	}
	
	protected void setNodeAttributes(Node node, XmlElement e) { 
		if (node == null || e == null) 
			return; 
		
		String[] names = e.getAttributeNames(); 
		for (int i=0; names != null && i < names.length; i++) { 
			String name = names[i]; 
			String value = e.getAttribute(name); 
			
			if (name != null && value != null) 
				node.addAttribute(name, value); 
		}
	}
	
	@Override 
	public synchronized int length() { 
		return mBuffer.length(); 
	}
	
	@Override 
	public synchronized char charAt(int position) { 
		return mBuffer.charAt(position); 
	}
	
	@Override 
	public synchronized void append(CharSequence text) { 
		mBuffer.append(text); 
	}
	
	public synchronized String getString(int startLength, int endLength) { 
		if (startLength >= 0 && endLength > startLength && endLength <= mBuffer.length()) { 
			char[] dest = new char[endLength - startLength]; 
			mBuffer.getChars(startLength, endLength, dest, 0); 
			return new String(dest); 
		}
		return null; 
	}
	
}
