package org.javenstudio.falcon.search.update;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.falcon.util.TransformerProvider;
import org.javenstudio.falcon.util.XMLLogger;
import org.javenstudio.falcon.search.ISearchConfig;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.params.UpdateParams;

public class XMLContentLoader extends ContentLoader {
	private static Logger LOG = Logger.getLogger(XMLContentLoader.class);
	private static XMLLogger XMLLOG = new XMLLogger(LOG);
  
	public static final String CONTEXT_TRANSFORMER_KEY = "xsltupdater.transformer";
	public static final String XSLT_CACHE_PARAM = "xsltCacheLifetimeSeconds"; 

	public static final int XSLT_CACHE_DEFAULT = 60;
  
	private final ISearchCore mCore;
	private XMLInputFactory mInputFactory;
	private int mXsltCacheLifetimeSeconds;
	
	public XMLContentLoader(ISearchCore core) { 
		mCore = core;
	}
	
	@Override
	public XMLContentLoader init(Params args) throws ErrorException {
		mInputFactory = XMLInputFactory.newInstance();
		mInputFactory.setXMLReporter(XMLLOG);
		
		try {
			// The java 1.6 bundled stax parser (sjsxp) does not currently have a thread-safe
			// XMLInputFactory, as that implementation tries to cache and reuse the
			// XMLStreamReader.  Setting the parser-specific "reuse-instance" property to false
			// prevents this.
			// All other known open-source stax parsers (and the bea ref impl)
			// have thread-safe factories.
			mInputFactory.setProperty("reuse-instance", Boolean.FALSE);
			
		} catch (IllegalArgumentException ex) {
			// Other implementations will likely throw this exception since "reuse-instance"
			// isimplementation specific.
			if (LOG.isDebugEnabled())
				LOG.debug("Unable to set the 'reuse-instance' property for the input chain: " + mInputFactory);
		}
		
		mXsltCacheLifetimeSeconds = XSLT_CACHE_DEFAULT;
		if (args != null) {
			mXsltCacheLifetimeSeconds = args.getInt(XSLT_CACHE_PARAM, XSLT_CACHE_DEFAULT);
			
			if (LOG.isDebugEnabled())
				LOG.info("xsltCacheLifetimeSeconds=" + mXsltCacheLifetimeSeconds);
		}
		
		return this;
	}

	@Override
	public String getDefaultWT() {
		return "xml";
	}

	@Override
	public void load(ISearchRequest req, ISearchResponse rsp, ContentStream stream, 
			UpdateProcessor processor) throws ErrorException {
		try { 
			doLoad(req, rsp, stream, processor); 
			
		} catch (Exception ex) { 
			if (ex instanceof ErrorException) 
				throw (ErrorException)ex;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	protected void doLoad(ISearchRequest req, ISearchResponse rsp, ContentStream stream, 
			UpdateProcessor processor) throws Exception {
		final String charset = ContentStreamBase.getCharsetFromContentType(
				stream.getContentType());
    
		InputStream is = null;
		XMLStreamReader parser = null;

		String tr = req.getParams().get(CommonParams.TR, null);
		if (tr != null) {
			Transformer t = getTransformer(tr, req);
			final DOMResult result = new DOMResult();
      
			// first step: read XML and build DOM using Transformer 
			// (this is no overhead, as XSL always produces
			// an internal result DOM tree, we just access it directly as input for StAX):
			try {
				is = stream.getStream();
				
				final InputSource isrc = new InputSource(is);
				isrc.setEncoding(charset);
				
				final SAXSource source = new SAXSource(isrc);
				t.transform(source, result);
				
			} catch(TransformerException te) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						te.getMessage(), te);
				
			} finally {
				IOUtils.closeQuietly(is);
			}
			
			// second step feed the intermediate DOM tree into StAX parser:
			try {
				parser = mInputFactory.createXMLStreamReader(new DOMSource(result.getNode()));
				processUpdate(req, processor, parser);
				
			} catch (XMLStreamException e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						e.getMessage(), e);
				
			} finally {
				if (parser != null) parser.close();
			}
			
		} else { 
			// Normal XML Loader
			try {
				is = stream.getStream();
				if (LOG.isDebugEnabled()) {
					final byte[] body = IOUtils.toByteArray(is);
					// TODO: The charset may be wrong, as the real charset is later
					// determined by the XML parser, the content-type is only used as a hint!
					LOG.debug("content body: " + new String(body, (charset == null) ? 
							ContentStreamBase.DEFAULT_CHARSET : charset));
					
					IOUtils.closeQuietly(is);
					is = new ByteArrayInputStream(body);
				}
				
				parser = (charset == null) ? mInputFactory.createXMLStreamReader(is) : 
						mInputFactory.createXMLStreamReader(is, charset);
				processUpdate(req, processor, parser);
				
			} catch (XMLStreamException e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						e.getMessage(), e);
				
			} finally {
				if (parser != null) parser.close();
				IOUtils.closeQuietly(is);
			}
		}
	}
  
	/** 
	 * Get Transformer from request context, or from TransformerProvider.
	 *  This allows either getContentType(...) or write(...) to instantiate the Transformer,
	 *  depending on which one is called first, then the other one reuses the same Transformer
	 */
	protected Transformer getTransformer(String xslt, ISearchRequest request) 
			throws IOException {
		// not the cleanest way to achieve this
		// no need to synchronize access to context, right? 
		// Nothing else happens with it at the same time
		final Map<Object,Object> ctx = request.getContextMap();
		
		Transformer result = (Transformer)ctx.get(CONTEXT_TRANSFORMER_KEY);
		if (result == null) {
			ISearchConfig config = mCore.getSearchConfig();
			
			result = TransformerProvider.getInstance().getTransformer(
					config.getContextLoader(), xslt, mXsltCacheLifetimeSeconds);
			result.setErrorListener(XMLLOG);
			
			ctx.put(CONTEXT_TRANSFORMER_KEY,result);
		}
		
		return result;
	}

	protected void processUpdate(ISearchRequest req, UpdateProcessor processor, 
			XMLStreamReader parser) throws Exception {
		AddCommand addCmd = null;
		
		Params params = req.getParams();
		while (true) {
			int event = parser.next();
			
			switch (event) {
			case XMLStreamConstants.END_DOCUMENT:
				parser.close();
				return;

			case XMLStreamConstants.START_ELEMENT:
				String currTag = parser.getLocalName();
				
				if (currTag.equals(UpdateCommand.ADD)) {
					if (LOG.isDebugEnabled())
						LOG.debug("processUpdate(add)");

					addCmd = new AddCommand(req, mCore.getSchema());

					// First look for commitWithin parameter on the request, 
					// will be overwritten for individual <add>'s
					addCmd.setCommitWithin(params.getInt(UpdateParams.COMMIT_WITHIN, -1));
					addCmd.setOverwrite(params.getBool(UpdateParams.OVERWRITE, true));
            
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						String attrName = parser.getAttributeLocalName(i);
						String attrVal = parser.getAttributeValue(i);
						
						if (UpdateCommand.OVERWRITE.equals(attrName)) {
							addCmd.setOverwrite(StrHelper.parseBoolean(attrVal));
							
						} else if (UpdateCommand.COMMIT_WITHIN.equals(attrName)) {
							addCmd.setCommitWithin(Integer.parseInt(attrVal));
							
						} else {
							LOG.warn("Unknown attribute id in add:" + attrName);
						}
					}

				} else if ("doc".equals(currTag)) {
					if (addCmd != null) {
						if (LOG.isDebugEnabled())
							LOG.debug("adding doc ...");
						
						addCmd.clear();
						addCmd.setInputDocument(readDoc(parser));
						
						processor.processAdd(addCmd);
						
					} else {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Unexpected <doc> tag without an <add> tag surrounding it.");
					}
					
				} else if (UpdateCommand.COMMIT.equals(currTag) || 
						UpdateCommand.OPTIMIZE.equals(currTag)) {
					if (LOG.isDebugEnabled())
						LOG.debug("parsing " + currTag);

					CommitCommand cmd = new CommitCommand(req, 
							UpdateCommand.OPTIMIZE.equals(currTag));
					
					ModifiableParams mp = new ModifiableParams();
            
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						String attrName = parser.getAttributeLocalName(i);
						String attrVal = parser.getAttributeValue(i);
						
						mp.set(attrName, attrVal);
					}

					UpdateHelper.validateCommitParams(mp);
					// default to the normal request params for commit options
					Params p = Params.wrapDefaults(mp, req.getParams()); 
					UpdateHelper.updateCommit(cmd, p);

					processor.processCommit(cmd);
					// end commit
					
				} else if (UpdateCommand.ROLLBACK.equals(currTag)) {
					if (LOG.isDebugEnabled())
						LOG.trace("parsing " + currTag);

					RollbackCommand cmd = new RollbackCommand(req);
					processor.processRollback(cmd);
					// end rollback
					
				} else if (UpdateCommand.DELETE.equals(currTag)) {
					if (LOG.isDebugEnabled())
						LOG.debug("parsing delete");
					
					processDelete(req, processor, parser);
					// end delete
				}
				
				break;
			}
		}
	}

	protected void processDelete(ISearchRequest req, UpdateProcessor processor, 
			XMLStreamReader parser) throws Exception {
		// Parse the command
		DeleteCommand deleteCmd = new DeleteCommand(req, mCore.getSchema());

		// First look for commitWithin parameter on the request, 
		// will be overwritten for individual <delete>'s
		Params params = req.getParams();
		deleteCmd.setCommitWithin(params.getInt(UpdateParams.COMMIT_WITHIN, -1));

		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attrName = parser.getAttributeLocalName(i);
			String attrVal = parser.getAttributeValue(i);
			
			if ("fromPending".equals(attrName)) {
				// deprecated
				if (LOG.isDebugEnabled())
					LOG.debug("attribute: " + attrName + " is deprecated");
				
			} else if ("fromCommitted".equals(attrName)) {
				// deprecated
				if (LOG.isDebugEnabled())
					LOG.debug("attribute: " + attrName + " is deprecated");
				
			} else if (UpdateCommand.COMMIT_WITHIN.equals(attrName)) {
				deleteCmd.setCommitWithin(Integer.parseInt(attrVal));
				
			} else {
				LOG.warn("unexpected attribute delete/@" + attrName);
			}
		}

		StringBuilder text = new StringBuilder();
		while (true) {
			int event = parser.next();
			
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				String mode = parser.getLocalName();
				
				if (!("id".equals(mode) || "query".equals(mode))) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"unexpected XML tag /delete/" + mode);
				}
				
				text.setLength(0);
          
				if ("id".equals(mode)) {
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						String attrName = parser.getAttributeLocalName(i);
						String attrVal = parser.getAttributeValue(i);
						
						if (UpdateCommand.VERSION.equals(attrName)) {
							deleteCmd.setVersion(Long.parseLong(attrVal));
						}
					}
				}
				break;

			case XMLStreamConstants.END_ELEMENT:
				String currTag = parser.getLocalName();
				if ("id".equals(currTag)) {
					deleteCmd.setId(text.toString());
					
				} else if ("query".equals(currTag)) {
					deleteCmd.setQueryString(text.toString());
					
				} else if ("delete".equals(currTag)) {
					return;
					
				} else {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"unexpected XML tag /delete/" + currTag);
				}
				
				processor.processDelete(deleteCmd);
				deleteCmd.clear();
				
				break;

			// Add everything to the text
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS:
				text.append(parser.getText());
				break;
			}
		}
	}

	/**
	 * Given the input stream, read a document
	 *
	 */
	protected InputDocument readDoc(XMLStreamReader parser) throws Exception {
		InputDocument doc = new InputDocument();

		String attrName = "";
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			attrName = parser.getAttributeLocalName(i);
			if ("boost".equals(attrName)) {
				doc.setDocumentBoost(Float.parseFloat(parser.getAttributeValue(i)));
				
			} else {
				LOG.warn("Unknown attribute doc/@" + attrName);
			}
		}

		StringBuilder text = new StringBuilder();
		
		String name = null;
		String update = null;
		float boost = 1.0f;
		boolean isNull = false;
		
		while (true) {
			int event = parser.next();
			switch (event) {
			// Add everything to the text
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS:
				text.append(parser.getText());
				break;

			case XMLStreamConstants.END_ELEMENT:
				if ("doc".equals(parser.getLocalName())) {
					return doc;
					
				} else if ("field".equals(parser.getLocalName())) {
					Object v = isNull ? null : text.toString();
					if (update != null) {
						Map<String,Object> extendedValue = new HashMap<String,Object>(1);
						extendedValue.put(update, v);
						
						v = extendedValue;
					}
					
					doc.addField(name, v, boost);
					boost = 1.0f;
				}
				
				break;

			case XMLStreamConstants.START_ELEMENT:
				text.setLength(0);
				
				String localName = parser.getLocalName();
				if (!"field".equals(localName)) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"unexpected XML tag doc/" + localName);
				}
				
				boost = 1.0f;
				update = null;
				String attrVal = "";
				
				for (int i = 0; i < parser.getAttributeCount(); i++) {
					attrName = parser.getAttributeLocalName(i);
					attrVal = parser.getAttributeValue(i);
					
					if ("name".equals(attrName)) {
						name = attrVal;
						
					} else if ("boost".equals(attrName)) {
						boost = Float.parseFloat(attrVal);
						
					} else if ("null".equals(attrName)) {
						isNull = StrHelper.parseBoolean(attrVal);
						
					} else if ("update".equals(attrName)) {
						update = attrVal;
						
					} else {
						LOG.warn("Unknown attribute doc/field/@" + attrName);
					}
				}
				
				break;
			}
		}
	}
	
}
