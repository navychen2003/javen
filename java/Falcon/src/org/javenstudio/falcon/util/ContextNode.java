package org.javenstudio.falcon.util;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public interface ContextNode {

	public String getNodeName();
	public String getNodeValue();
	
	public ContextNode getChildNode(String expression) 
			throws ErrorException;
	
	public ContextList getChildNodes() throws ErrorException;
	public ContextList getChildNodes(String expression) 
			throws ErrorException;
	
	public NamedList<?> getChildNodesAsNamedList() 
			throws ErrorException;
	
	public Map<String,String> getAttributes() throws ErrorException;
	public Map<String,String> getAttributes(String... exclusions) 
			throws ErrorException;
	
	public String getAttribute(String name) throws ErrorException;
	public String getAttribute(String name, String missingErr) 
			throws ErrorException;
	
}
