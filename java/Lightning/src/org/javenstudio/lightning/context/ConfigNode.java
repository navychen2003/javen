package org.javenstudio.lightning.context;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.NamedList;

public abstract class ConfigNode implements ContextNode {

	public abstract String getNodeName();
	public abstract String getNodeValue();
	public abstract boolean isElementNode();
	
	public abstract ContextNode getChildNode(String expression) 
			throws ErrorException;
	
	public abstract ContextList getChildNodes() throws ErrorException;
	public abstract ContextList getChildNodes(String expression) 
			throws ErrorException;
	
	public abstract NamedList<?> getChildNodesAsNamedList() 
			throws ErrorException;
	
	public abstract Map<String,String> getAttributes() throws ErrorException;
	public abstract Map<String,String> getAttributes(String... exclusions) 
			throws ErrorException;
	
	public String getAttribute(String name) throws ErrorException { 
		return getAttribute(name, null);
	}
	
	public abstract String getAttribute(String name, String missingErr) 
			throws ErrorException;
	
}
