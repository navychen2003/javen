package org.javenstudio.common.parser.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Node {
	private static final AtomicLong sCounter = new AtomicLong();
	
	private final long mIdentity = sCounter.incrementAndGet();
	public final long getIdentity() { return mIdentity; }
	
	private static final int MIN_CAPACITY_INCREMENT = 8;
	private static final int MIN_CAPACITY = 4;
	
	private final Node mParent; 
	private final String mName; 
	private String mValue = null; 
	
	private String[] mAttrNames = null; 
	private String[] mAttrValues = null;
	private int mAttrSize = 0;
	
	private Node[] mChilds = null; 
	private int mChildSize = 0;
	
	public Node(Node parent, String name) { 
		mParent = parent; 
		mName = name; 
	}
	
	public Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		return new Node(this, name); 
	}
	
	public final Node getParent() { return mParent; } 
	public final String getName() { return mName; } 
	
	public final String getValue() { return mValue; } 
	public final void setValue(String value) { mValue = value; } 
	
	public final int getAttributeSize() { return mAttrSize; }
	
	public final String getAttributeName(int index) { 
		return index >= 0 && index < mAttrSize ? mAttrNames[index] : null;
	}
	
	public final String getAttributeValue(int index) { 
		return index >= 0 && index < mAttrSize ? mAttrValues[index] : null;
	}
	
	public final void addAttribute(String name, String value) { 
		if (name == null || value == null) 
			return;
		
		if (mAttrNames != null) { 
			if (mAttrSize == mAttrNames.length) {
				String[] attrNames = new String[mAttrSize + MIN_CAPACITY_INCREMENT];
				String[] attrValues = new String[mAttrSize + MIN_CAPACITY_INCREMENT];
			
				System.arraycopy(mAttrNames, 0, attrNames, 0, mAttrSize);
				System.arraycopy(mAttrValues, 0, attrValues, 0, mAttrSize);
			
				mAttrNames = attrNames;
				mAttrValues = attrValues;
			}
		} else { 
			mAttrNames = new String[MIN_CAPACITY];
			mAttrValues = new String[MIN_CAPACITY];
		}
		
		mAttrNames[mAttrSize] = name;
		mAttrValues[mAttrSize] = value;
		mAttrSize ++;
	}
	
	public final String getAttribute(String name) { 
		return getAttribute(name, true);
	}
	
	public final String getAttribute(String name, boolean ignoreCase) { 
		if (name == null || mAttrNames == null) 
			return null;
		
		for (int i=0; i < mAttrSize; i++) { 
			String attrName = mAttrNames[i];
			String attrValue = mAttrValues[i];
			
			if (ignoreCase ? name.equalsIgnoreCase(attrName) : name.equals(attrName)) 
				return attrValue;
		}
		
		return null;
	}
	
	public final String[] getAttributes(String name) { 
		return getAttributes(name, true);
	}
	
	public final String[] getAttributes(String name, boolean ignoreCase) { 
		if (name == null || mAttrNames == null) 
			return null;
		
		ArrayList<String> values = new ArrayList<String>();
		
		for (int i=0; i < mAttrSize; i++) { 
			String attrName = mAttrNames[i];
			String attrValue = mAttrValues[i];
			
			if (ignoreCase ? name.equalsIgnoreCase(attrName) : name.equals(attrName)) 
				values.add(attrValue);
		}
		
		return values.toArray(new String[values.size()]);
	}
	
	public final int getChildCount() { 
		return mChildSize; 
	}
	
	public final Node getChildAt(int index) { 
		return index >= 0 && index < mChildSize ? mChilds[index] : null; 
	}
	
	public final String[] getChildNames() { 
		if (mChilds == null) return null; 
		
		ArrayList<String> names = new ArrayList<String>();
		
		for (int i=0; i < mChildSize; i++) { 
			String name = mChilds[i].getName();
			names.add(name);
		}
		
		return names.toArray(new String[names.size()]); 
	}
	
	public final Node[] getChildList() { 
		if (mChilds == null) return null; 
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for (int i=0; i < mChildSize; i++) { 
			Node node = mChilds[i];
			nodes.add(node);
		}
		
		return nodes.toArray(new Node[nodes.size()]); 
	}
	
	public final Node[] getChildList(String name) { 
		return getChildList(name, true);
	}
	
	public final Node[] getChildList(String name, boolean ignoreCase) { 
		if (name == null || mChilds == null) 
			return null; 
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for (int i=0; i < mChildSize; i++) { 
			Node node = mChilds[i];
			
			String nodeName = node.getName();
			if (ignoreCase ? name.equalsIgnoreCase(nodeName) : name.equals(nodeName)) 
				nodes.add(node);
		}
		
		return nodes.toArray(new Node[nodes.size()]); 
	}
	
	public final Node getFirstChild(String name) { 
		return getFirstChild(name, true);
	}
	
	public final Node getFirstChild(String name, boolean ignoreCase) { 
		if (name == null || mChilds == null) 
			return null; 
		
		for (int i=0; i < mChildSize; i++) { 
			Node node = mChilds[i];
			
			String nodeName = node.getName();
			if (ignoreCase ? name.equalsIgnoreCase(nodeName) : name.equals(nodeName)) 
				return node;
		}
		
		return null; 
	}
	
	public final String getFirstChildValue(String name) { 
		return getFirstChildValue(name, true);
	}
	
	public final String getFirstChildValue(String name, boolean ignoreCase) { 
		Node node = getFirstChild(name, ignoreCase); 
		if (node != null) 
			return node.getValue(); 
		
		return null; 
	}
	
	public final void addChild(Node node) { 
		if (node == null || node.getParent() != this) 
			return; 
		
		if (mChilds != null) { 
			for (int i=0; i < mChildSize; i++) { 
				if (node == mChilds[i]) return;
			}
			
			if (mChildSize == mChilds.length) {
				Node[] childs = new Node[mChildSize + MIN_CAPACITY_INCREMENT];
				System.arraycopy(mChilds, 0, childs, 0, mChildSize);
				
				mChilds = childs;
			}
		} else { 
			mChilds = new Node[MIN_CAPACITY];
		}
		
		mChilds[mChildSize] = node;
		mChildSize ++;
		
		onChildAdded(node); 
	}
	
	protected void onChildAdded(Node node) {}
	protected void onNodeEnded() {}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + getIdentity() 
				+ ":" + getName();
	}
	
}
