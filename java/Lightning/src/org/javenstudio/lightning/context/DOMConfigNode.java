package org.javenstudio.lightning.context;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.DOMUtils;
import org.javenstudio.falcon.util.NamedList;

final class DOMConfigNode extends ConfigNode {
	
	private final XPath mXPath = XPathFactory.newInstance().newXPath();
	private final DOMConfig mConfig;
	private final Node mNode;
	
	DOMConfigNode(DOMConfig conf, Node node) { 
		mConfig = conf;
		mNode = node;
	}
	
	public DOMConfig getConfig() { return mConfig; }
	
	@Override
	public Map<String,String> getAttributes() throws ErrorException { 
		return DOMUtils.toMap(mNode.getAttributes());
	}
	
	@Override
	public Map<String,String> getAttributes(String... exclusions) throws ErrorException { 
		return DOMUtils.toMapExcept(mNode.getAttributes(), exclusions);
	}
	
	@Override
	public String getAttribute(String name, String missing_err) throws ErrorException { 
		return DOMUtils.getAttr(mNode, name, missing_err);
	}
	
	@Override
	public ConfigNode getChildNode(String expression) throws ErrorException { 
		try {
			Node anode = (Node)mXPath.evaluate(expression, mNode, 
					XPathConstants.NODE);
			if (anode != null)
				return new DOMConfigNode(getConfig(), anode);
			else
				return null;
		} catch (Exception e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	public ContextList getChildNodes() throws ErrorException { 
		final NodeList nodes = mNode.getChildNodes();
		return getConfig().newNodeIterator(nodes);
	}
	
	@Override
	public ContextList getChildNodes(String expression) throws ErrorException { 
		try {
			NodeList nodes = (NodeList)mXPath.evaluate(expression, mNode, 
					XPathConstants.NODESET);
			if (nodes != null)
				return getConfig().newNodeIterator(nodes);
			else
				return null;
		} catch (Exception e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	public NamedList<?> getChildNodesAsNamedList() throws ErrorException { 
		return DOMUtils.childNodesToNamedList(mNode);
	}
	
	public Node getNode() { 
		return mNode;
	}
	
	@Override
	public String getNodeName() { 
		return mNode.getNodeName();
	}
	
	@Override
	public String getNodeValue() { 
		return mNode.getNodeValue();
	}
	
	@Override
	public boolean isElementNode() { 
		return mNode.getNodeType() == Node.ELEMENT_NODE;
	}
	
}
