package org.javenstudio.falcon.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.ext.EntityResolver2;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.Constants;

/**
 * This is a helper class to support resolving of XIncludes or other hrefs
 * inside XML files on top of a {@link ContextLoader}. Just plug this class
 * on top of a {@link ContextLoader} and pass it as {@link EntityResolver} to SAX parsers
 * or via wrapper methods as {@link URIResolver} to XSL transformers 
 * or {@link XMLResolver} to STAX parsers.
 * The resolver handles special SystemIds with an URI scheme of {@code falres:} that point
 * to resources. To produce such systemIds when you initially call the parser, use
 * {@link #createSystemIdFromResourceName} which produces a SystemId that can
 * be included along the InputStream coming from {@link ContextLoader#openResource}.
 * <p>In general create the {@link InputSource} to be passed to the parser like:</p>
 * <pre class="prettyprint">
 *  InputSource is = new InputSource(loader.openSchema(name));
 *  is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(name));
 *  final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
 *  db.setEntityResolver(new SystemIdResolver(loader));
 *  Document doc = db.parse(is);
 * </pre>
 */
public final class SystemIdResolver implements EntityResolver, EntityResolver2 {
	private static final Logger LOG = Logger.getLogger(SystemIdResolver.class);

	public static final String RESOURCE_LOADER_URI_SCHEME = 
			Constants.RESOURCE_LOADER_URI_SCHEME;
	public static final String RESOURCE_LOADER_AUTHORITY_ABSOLUTE = 
			Constants.RESOURCE_LOADER_AUTHORITY_ABSOLUTE;

	private final ContextLoader mLoader;

	public SystemIdResolver(ContextLoader loader) {
		mLoader = loader;
	}
  
	public EntityResolver asEntityResolver() {
		return this;
	}
  
	public URIResolver asURIResolver() {
		return new URIResolver() {
				@Override
				public Source resolve(String href, String base) throws TransformerException {
					try {
						final InputSource src = 
								SystemIdResolver.this.resolveEntity(null, null, base, href);
						return (src == null) ? null : new SAXSource(src);
					} catch (IOException ioe) {
						throw new TransformerException("Cannot resolve entity", ioe);
					}
				}
			};
	}
  
	public XMLResolver asXMLResolver() {
		return new XMLResolver() {
				@Override
				public Object resolveEntity(String publicId, String systemId, 
						String baseURI, String namespace) throws XMLStreamException {
					try {
						final InputSource src = SystemIdResolver.this.resolveEntity(
								null, publicId, baseURI, systemId);
						return (src == null) ? null : src.getByteStream();
					} catch (IOException ioe) {
						throw new XMLStreamException("Cannot resolve entity", ioe);
					}
				}
			};
	}
  
	private URI resolveRelativeURI(String baseURI, String systemId) 
			throws URISyntaxException {
		URI uri;
    
		// special case for backwards compatibility: if relative systemId starts with "/" 
		// (we convert that to an absolute falres:-URI)
		if (systemId.startsWith("/")) {
			uri = new URI(RESOURCE_LOADER_URI_SCHEME, RESOURCE_LOADER_AUTHORITY_ABSOLUTE, 
					"/", null, null).resolve(systemId);
		} else {
			// simply parse as URI
			uri = new URI(systemId);
		}
    
		// do relative resolving
		if (baseURI != null ) 
			uri = new URI(baseURI).resolve(uri);
    
		return uri;
	}
  
	// *** EntityResolver(2) methods:
  
	public InputSource getExternalSubset(String name, String baseURI) {
		return null;
	}
  
	public InputSource resolveEntity(String name, String publicId, String baseURI, 
			String systemId) throws IOException {
		if (systemId == null)
			return null;
		
		try {
			final URI uri = resolveRelativeURI(baseURI, systemId);
      
			if (LOG.isDebugEnabled()) { 
				LOG.debug("resolveEntity: publicId=" + publicId 
						+ " systemId=" + systemId + " uri=" + uri);
			}
			
			// check schema and resolve with ResourceLoader
			if (RESOURCE_LOADER_URI_SCHEME.equals(uri.getScheme())) {
				String path = uri.getPath(), authority = uri.getAuthority();
				if (!RESOURCE_LOADER_AUTHORITY_ABSOLUTE.equals(authority)) 
					path = path.substring(1);
				
				try {
					final InputSource is = new InputSource(
							mLoader.openResourceAsStream(path));
					
					is.setSystemId(uri.toASCIIString());
					is.setPublicId(publicId);
					
					return is;
				} catch (RuntimeException re) {
					// unfortunately XInclude fallback only works with IOException, 
					// but openResource() never throws that one
					throw (IOException) (new IOException(re.getMessage()).initCause(re));
				}
			}
		} catch (URISyntaxException use) {
			LOG.warn("An URI systax problem occurred during resolving SystemId, " 
					+ "falling back to default resolver", use);
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("resolveEntity: publicId=" + publicId 
					+ " systemId=" + systemId + " return null");
		}
		
		// resolve all other URIs using the standard resolver
		return null;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		return resolveEntity(null, publicId, null, systemId);
	}
  
	public static String createSystemIdFromResourceName(String name) {
		name = name.replace(File.separatorChar, '/');
		
		final String authority;
		if (name.startsWith("/")) {
			// a hack to preserve absolute filenames and keep them absolute after resolving, 
			// we set the URI's authority to "@" on absolute filenames:
			authority = RESOURCE_LOADER_AUTHORITY_ABSOLUTE;
		} else {
			authority = null;
			name = "/" + name;
		}
		
		try {
			return new URI(RESOURCE_LOADER_URI_SCHEME, 
					authority, name, null, null).toASCIIString();
		} catch (URISyntaxException use) {
			throw new IllegalArgumentException("Invalid syntax of Resource URI", use);
		}
	}

}
