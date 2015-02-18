package org.javenstudio.lightning.context;

import java.io.InputStream;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.DOMUtils;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.SystemIdResolver;
import org.javenstudio.falcon.util.XMLLogger;

final class DOMConfig extends Config {
	private static final Logger LOG = Logger.getLogger(Config.class);
	private static final XMLLogger XLOG = new XMLLogger(LOG);
	
	private static final XPathFactory sXPathFactory = XPathFactory.newInstance();
	
	private final ContextLoader mLoader;
	private final String mPrefix;
	private final String mName;
	private final Document mDoc;

	/**
	 * Builds a config from a resource name with no xpath prefix.
	 */
	public DOMConfig(ContextLoader loader, String name) throws ErrorException {
		this(loader, name, (InputSource)null, null);
	}

	public DOMConfig(ContextLoader loader, String name, InputSource is, String prefix) 
			throws ErrorException {
		this(loader, name, is, prefix, true);
	}
	
	public DOMConfig(ContextLoader loader, String name, InputStream is, String prefix) 
			throws ErrorException {
		this(loader, name, is, prefix, true);
	}
	
	public DOMConfig(ContextLoader loader, String name, InputStream is, String prefix, 
			boolean subProps) throws ErrorException {
		this(loader, name, createSource(name, is), prefix, subProps);
	}
	
	public static InputSource createSource(String name, InputStream is) { 
		if (is != null) { 
			InputSource source = new InputSource(is);
			if (name != null) {
				source.setSystemId(
						SystemIdResolver.createSystemIdFromResourceName(name));
			}
			
			return source;
		}
		
		return null;
	}
	
	/**
	 * Builds a config:
	 * <p>
	 * Note that the 'name' parameter is used to obtain a valid input stream 
	 * if no valid one is provided through 'is'.
	 * If no valid stream is provided, a valid ContextLoader instance 
	 * should be provided through 'loader' so
	 * the resource can be opened (@see ContextLoader#openResource); 
	 * if no ContextLoader instance is provided, a default one
	 * will be created.
	 * </p>
	 * <p>
	 * Consider passing a non-null 'name' parameter in all use-cases since 
	 * it is used for logging & exception reporting.
	 * </p>
  	* @param loader the resource loader used to obtain an input stream if 'is' is null
  	* @param name the resource name used if the input stream 'is' is null
  	* @param is the resource as a SAX InputSource
  	* @param prefix an optional prefix that will be preprended to all 
  	* non-absolute xpath expressions
  	*/
	public DOMConfig(ContextLoader loader, String name, InputSource is, String prefix, 
			boolean subProps) throws ErrorException {
		mLoader = loader;
		mName = name;
		mPrefix = (prefix != null && !prefix.endsWith("/")) ? 
				prefix + '/' : prefix;
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			if (is == null) 
				is = createSource(name, loader.openResourceAsStream(name));

			// only enable xinclude, if a SystemId is available
			if (is.getSystemId() != null) {
				try {
					dbf.setXIncludeAware(true);
					dbf.setNamespaceAware(true);
				} catch(UnsupportedOperationException e) {
					LOG.warn(name + " XML parser doesn't support XInclude option");
				}
			}
      
			final DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new SystemIdResolver(loader));
			db.setErrorHandler(XLOG);
			
			try {
				mDoc = db.parse(is);
			} finally {
				// some XML parsers are broken and don't close the byte stream 
				// (but they should according to spec)
				IOUtils.closeQuietly(is.getByteStream());
			}
			if (subProps) 
				DOMUtils.substituteProperties(mDoc, loader.getProperties());
			
		} catch (Throwable e) { 
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	public void writeConfig(Writer writer) throws ErrorException {
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			
			DOMSource source = new DOMSource(mDoc);
			StreamResult result = new StreamResult(writer);
			
			transformer.transform(source, result);
		} catch (Throwable e) {
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public final ContextLoader getContextLoader() { return mLoader; }
	public final String getResourceName() { return mName; }
	public final String getName() { return mName; }

	protected String normalize(String path) {
		return (mPrefix == null || path.startsWith("/")) ? path : mPrefix+path;
	}
	
	//private XPath getXPath() {
	//	return sXPathFactory.newXPath();
	//}
	
	public void substituteProperties() throws ErrorException {
		DOMUtils.substituteProperties(mDoc, mLoader.getProperties());
	}

	private Object evaluate(String path, QName type) throws ErrorException {
		path = checkRootNode(path);
		
		XPath xpath = sXPathFactory.newXPath();
		try {
			String xstr = normalize(path);

			// TODO: instead of prepending /prefix/, we could do the search rooted at /prefix...
			Object o = xpath.evaluate(xstr, mDoc, type);
			return o;

		} catch (XPathExpressionException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error in xpath:" + path +" for " + mName, e);
		}
	}
	
	public ContextList getNodes(String path) throws ErrorException { 
		final NodeList nodes = (NodeList)evaluate(path, XPathConstants.NODESET);
		return newNodeIterator(nodes);
	}
	
	public ContextNode getNode(String path) throws ErrorException { 
		final Node node = (Node)evaluate(path, XPathConstants.NODE);
		if (node != null) 
			return new DOMConfigNode(this, node);
		
		return null;
	}
	
	ContextList newNodeIterator(final NodeList nodes) {
		if (nodes == null) 
			return null;
		
		return new ContextList() {
			private int mIndex = -1;
			
			@Override
			public int getLength() { 
				return nodes.getLength();
			}
			
			@Override
			public ContextNode getNodeAt(int index) { 
				Node node = nodes.item(index);
				if (node == null)
					return null;
				
				return new DOMConfigNode(DOMConfig.this, node);
			}
			
			@Override
			public boolean hasNext() {
				return mIndex+1 < nodes.getLength();
			}

			@Override
			public ContextNode next() {
				int index = (++mIndex);
				if (index >= nodes.getLength())
					return null;
				
				return getNodeAt(index);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	private Node getNode(String path, boolean errIfMissing) throws ErrorException {
		path = checkRootNode(path);
		
		XPath xpath = sXPathFactory.newXPath();
		Node nd = null;
		String xstr = normalize(path);

		try {
			nd = (Node)xpath.evaluate(xstr, mDoc, XPathConstants.NODE);

			if (nd == null) {
				if (errIfMissing) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							mName + " missing " + path);
				} else {
					if (LOG.isDebugEnabled())
						LOG.debug(mName + ": missing optional " + path);
					
					return null;
				}
			}

			if (LOG.isDebugEnabled())
				LOG.debug(mName + ": " + path + " = Node{" + nd + "}");
			
			return nd;

		} catch (Exception e) {
			if (e instanceof ErrorException) {
				throw (ErrorException)e;
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error in xpath:" + xstr+ " for " + mName, e);
			}
		}
	}

	@Override
	public String getVal(String path, boolean errIfMissing) throws ErrorException {
		Node nd = getNode(path, errIfMissing);
		if (nd == null) return null;

		return DOMUtils.getText(nd);

		/******
    	short typ = nd.getNodeType();
    	if (typ==Node.ATTRIBUTE_NODE || typ==Node.TEXT_NODE) {
      	return nd.getNodeValue();
    	}
    	return nd.getTextContent();
		 ******/
	}
	
	@Override
	public void setVal(String path, String val) throws ErrorException {
		Node nd = getNode(path, false);
		if (nd == null) return;
		
		if (val != null && val.length() > 0)
			DOMUtils.setText(nd, val);
	}
	
}
