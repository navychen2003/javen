package org.javenstudio.lightning.context;

import java.io.Writer;
import java.util.Properties;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.ContextResource;

public abstract class Config extends ContextResource {
	
	public abstract String getResourceName();
	public abstract String getName();
	
	protected String checkRootNode(String path) { 
		if (path != null) { 
			String rootNode = getContextLoader().getConfigRootName();
			if (rootNode != null && path.startsWith("*/")) 
				path = rootNode + path.substring(1);
		}
		return path;
	}

	public Properties readProperties(ContextNode node) throws ErrorException {
		Properties properties = new Properties();
		
		if (node != null) { 
		    ContextList props = node.getChildNodes("property");
		    
		    for (int i=0; i < props.getLength(); i++) {
		    	ContextNode prop = props.getNodeAt(i);
		    	properties.setProperty(prop.getAttribute("name"), 
		    			prop.getAttribute("value"));
		    }
		}
		
	    return properties;
	}
	
	public void writeConfig(Writer writer) throws ErrorException {
		throw new java.lang.UnsupportedOperationException();
	}
	
}
