package org.javenstudio.falcon.util;

import java.io.IOException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.SystemIdResolver;
import org.javenstudio.falcon.util.XMLLogger;

/** 
 * Singleton that creates a Transformer for the XSLTServletFilter.
 *  For now, only caches the last created Transformer, but
 *  could evolve to use an LRU cache of Transformers.
 *  
 *  See http://www.javaworld.com/javaworld/jw-05-2003/jw-0502-xsl_p.html for
 *  one possible way of improving caching. 
 */
public class TransformerProvider {
	private static final Logger LOG = Logger.getLogger(TransformerProvider.class);
	private static final XMLLogger XMLLOG = new XMLLogger(LOG);
	
	public static TransformerProvider sInstance = new TransformerProvider();
	public static TransformerProvider getInstance() { 
		return sInstance;
	}
	
	private String mLastFilename;
	private Templates mLastTemplates = null;
	private long mCacheExpires = 0;
  
	/** singleton */
	private TransformerProvider() {
		// tell'em: currently, we only cache the last used XSLT transform, and blindly recompile it
		// once cacheLifetimeSeconds expires
		LOG.warn("The TransformerProvider's simplistic XSLT caching mechanism is not appropriate "
				+ "for high load scenarios, unless a single XSLT transform is used"
				+ " and xsltCacheLifetimeSeconds is set to a sufficiently high value."
			);
	}
  
	/** 
	 * Return a new Transformer, possibly created from our cached Templates object  
	 * @throws IOException If there is a low-level I/O error.
	 */ 
	public synchronized Transformer getTransformer(ContextLoader loader, String filename, 
			int cacheLifetimeSeconds) throws IOException {
		// For now, the Templates are blindly reloaded once cacheExpires is over.
		// It'd be better to check the file modification time to reload only if needed.
		if (mLastTemplates != null && filename.equals(mLastFilename) && 
				System.currentTimeMillis() < mCacheExpires) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("Using cached Templates:" + filename);
			
		} else {
			mLastTemplates = getTemplates(loader, filename, cacheLifetimeSeconds);
		}
    
		Transformer result = null;
		try {
			result = mLastTemplates.newTransformer();
			
		} catch(TransformerConfigurationException tce) {
			LOG.error(getClass().getName() + ".getTransformer error", tce);
			
			final IOException ioe = new IOException("newTransformer fails ( " + mLastFilename + ")");
			ioe.initCause(tce);
			throw ioe;
		}
    
		return result;
	}
  
	/** Return a Templates object for the given filename */
	private Templates getTemplates(ContextLoader loader, String filename, 
			int cacheLifetimeSeconds) throws IOException {
		Templates result = null;
		
		mLastFilename = null;
		try {
			if (LOG.isDebugEnabled()) 
				LOG.debug("compiling XSLT templates:" + filename);
			
			final String fn = "xslt/" + filename;
			final TransformerFactory tFactory = TransformerFactory.newInstance();
			
			tFactory.setURIResolver(new SystemIdResolver(loader).asURIResolver());
			tFactory.setErrorListener(XMLLOG);
			
			final StreamSource src = new StreamSource(loader.openResourceAsStream(fn),
					SystemIdResolver.createSystemIdFromResourceName(fn));
			
			try {
				result = tFactory.newTemplates(src);
			} finally {
				// some XML parsers are broken and don't close the byte stream 
				// (but they should according to spec)
				IOUtils.closeQuietly(src.getInputStream());
			}
			
		} catch (Exception e) {
			LOG.error(getClass().getName() + ".newTemplates error", e);
			
			final IOException ioe = new IOException("Unable to initialize Templates '" + filename + "'");
			ioe.initCause(e);
			throw ioe;
		}
    
		mLastFilename = filename;
		mLastTemplates = result;
		mCacheExpires = System.currentTimeMillis() + (cacheLifetimeSeconds * 1000);
    
		return result;
	}
	
}
