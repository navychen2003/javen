package org.javenstudio.common.parser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
	
	private final Node mParent; 
	private final String mName; 
	private String mValue = null; 
	private Map<String, String> mAttrs = null; 
	private List<Node> mChildList = null; 
	private Map<String, List<Node>> mChildMap = null; 
	
	public Node(Node parent, String name) { 
		mParent = parent; 
		mName = name; 
	}
	
	protected final synchronized List<Node> getChildList() { 
		if (mChildList == null) 
			mChildList = new ArrayList<Node>(); 
		return mChildList; 
	}
	
	private synchronized Map<String, List<Node>> getChildMap() { 
		if (mChildMap == null) 
			mChildMap = new HashMap<String, List<Node>>(); 
		return mChildMap; 
	}
	
	private synchronized Map<String, String> getAttrs() { 
		if (mAttrs == null) 
			mAttrs = new HashMap<String, String>(); 
		return mAttrs; 
	}
	
	public synchronized Node newChildNode(String name) { 
		return null; 
	}
	
	public final Node getParent() { return mParent; } 
	public final String getName() { return mName; } 
	
	public final String getValue() { return mValue; } 
	public final void setValue(String value) { mValue = value; } 
	
	public final synchronized void addAttribute(String name, String value) { 
		if (name != null && value != null) 
			getAttrs().put(name, value); 
	}
	
	public final synchronized String getAttribute(String name) { 
		return name != null ? getAttrs().get(name) : null; 
	}
	
	public final synchronized int getChildCount() { 
		return mChildList != null ? mChildList.size() : 0; 
	}
	
	public final synchronized Node getChildAt(int index) { 
		return index >= 0 && index < mChildList.size() ? mChildList.get(index) : null; 
	}
	
	public final synchronized String[] getChildNames() { 
		if (mChildMap == null) 
			return null; 
		
		return mChildMap.keySet().toArray(new String[0]); 
	}
	
	public final synchronized Node[] getChildList(String name) { 
		if (name == null || mChildMap == null) 
			return null; 
		
		List<Node> list = mChildMap.get(name); 
		if (list != null && list.size() > 0) 
			return list.toArray(new Node[list.size()]); 
		
		return null; 
	}
	
	public final synchronized Node getFirstChild(String name) { 
		if (name == null || mChildMap == null) 
			return null; 
		
		List<Node> list = mChildMap.get(name); 
		if (list != null && list.size() > 0) 
			return list.get(0); 
		
		return null; 
	}
	
	public final String getFirstChildValue(String name) { 
		Node node = getFirstChild(name); 
		if (node != null) 
			return node.getValue(); 
		
		return null; 
	}
	
	public final synchronized void addChild(Node node) { 
		if (node == null || node.getParent() != this) 
			return; 
		
		List<Node> childs = getChildList(); 
		for (int i=0; i < childs.size(); i++) { 
			if (node == childs.get(i)) 
				return; 
		}
		
		childs.add(node); 
		
		Map<String, List<Node>> map = getChildMap(); 
		List<Node> list = map.get(node.getName()); 
		if (list == null) { 
			list = new ArrayList<Node>(); 
			map.put(node.getName(), list); 
		}
		
		list.add(node); 
		
		onChildAdded(node); 
	}
	
	public void onChildAdded(Node node) { 
		// do nothing
	}
	
	public void onNodeEnded() { 
		// do nothing
	}
}
