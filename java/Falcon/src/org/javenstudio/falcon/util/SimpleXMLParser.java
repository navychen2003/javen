package org.javenstudio.falcon.util;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public class SimpleXMLParser {
	static final Logger LOG = Logger.getLogger(SimpleXMLParser.class);
	static final XMLLogger XLOG = new XMLLogger(LOG);

	// reuse the factory among all parser instances so things like string caches
	// won't be duplicated
	static final XMLInputFactory sFactory;
	static {
		sFactory = XMLInputFactory.newInstance();
		try {
			// The java 1.6 bundled stax parser (sjsxp) does not currently have a thread-safe
			// XMLInputFactory, as that implementation tries to cache and reuse the
			// XMLStreamReader.  Setting the parser-specific "reuse-instance" property to false
			// prevents this.
			// All other known open-source stax parsers (and the bea ref impl)
			// have thread-safe factories.
			sFactory.setProperty("reuse-instance", Boolean.FALSE);
		} catch (IllegalArgumentException ex) {
			// Other implementations will likely throw this exception since "reuse-instance"
			// isimplementation specific.
			if (LOG.isWarnEnabled()) {
				LOG.warn("Unable to set the 'reuse-instance' property " 
						+ "for the input factory: " + sFactory);
			}
		}
		sFactory.setXMLReporter(XLOG);
	}

	private final String mRootTag;
	
	public SimpleXMLParser(String rootTag) {
		if (rootTag == null || rootTag.length() == 0)
			throw new IllegalArgumentException("rootTag is empty");
		
		mRootTag = rootTag;
	}
  
	public String getWriterType() {
		return "xml";
	}

	public String getRootTag() { 
		return mRootTag;
	}
	
	public NamedList<Object> parse(Reader in) throws ErrorException {
		XMLStreamReader parser = null;
		try {
			parser = sFactory.createXMLStreamReader(in);
		} catch (XMLStreamException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"parsing error", e);
		}

		return process(parser);    
	}

	public NamedList<Object> parse(InputStream in, String encoding) 
			throws ErrorException {
		XMLStreamReader parser = null;
		try {
			parser = sFactory.createXMLStreamReader(in, encoding);
		} catch (XMLStreamException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"parsing error", e);
		}

		return process(parser);
	}

	/**
	 * parse the text into a named list...
	 */
	private NamedList<Object> process(XMLStreamReader parser) 
			throws ErrorException {
		try {
			NamedList<Object> result = null;
			String rootTag = getRootTag();
			
			for (int event = parser.next();  
					event != XMLStreamConstants.END_DOCUMENT;
					event = parser.next()) {
				
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					if (result != null) {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"already read the response!");
					}
            
					// only top-level element is "response
					String name = parser.getLocalName();
					if (name.equals(rootTag)) {
						result = readNamedList(parser);
						
					} else {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"really needs to be " + rootTag + ". " 
								+ "not: " + name);
					}
					break;
				} 
			}
			
			return result;
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"parsing error", ex);
			
		} finally {
			try {
				parser.close();
			} catch( Exception ex ){}
		}
	}

	protected enum KnownType {
		STR    (true)  { @Override public String  read( String txt ) { return txt;                  } },
		INT    (true)  { @Override public Integer read( String txt ) { return Integer.valueOf(txt); } },
		FLOAT  (true)  { @Override public Float   read( String txt ) { return Float.valueOf(txt);   } },
		DOUBLE (true)  { @Override public Double  read( String txt ) { return Double.valueOf(txt);  } },
		LONG   (true)  { @Override public Long    read( String txt ) { return Long.valueOf(txt);    } },
		BOOL   (true)  { @Override public Boolean read( String txt ) { return Boolean.valueOf(txt); } },
		NULL   (true)  { @Override public Object  read( String txt ) { return null;                 } },
		DATE   (true)  { 
			@Override 
			public Date read( String txt ) { 
				try {
					return DateUtils.parseDate(txt);      
				} catch (Throwable ex) {
					if (LOG.isErrorEnabled())
						LOG.error(ex.toString());
				}
				return null;
			} 
		},
    
		ARR    (false) { @Override public Object read( String txt ) { return null; } },
		LST    (false) { @Override public Object read( String txt ) { return null; } };
		//RESULT (false) { @Override public Object read( String txt ) { return null; } },
		//DOC    (false) { @Override public Object read( String txt ) { return null; } };
    
		final boolean mIsLeaf;
    
		KnownType (boolean isLeaf) {
			mIsLeaf = isLeaf;
		}
    
		public abstract Object read(String txt);
    
		public static KnownType get(String v) {
			if (v != null) {
				try {
					return KnownType.valueOf(v.toUpperCase(Locale.ROOT));
				} catch (Exception ex) {}
			}
			return null;
		}
	};
  
	protected NamedList<Object> readNamedList(XMLStreamReader parser) 
			throws XMLStreamException {
		if (XMLStreamConstants.START_ELEMENT != parser.getEventType()) {
			throw new XMLStreamException("must be start element, not: " 
					+ parser.getEventType());
		}

		StringBuilder builder = new StringBuilder();
		NamedList<Object> nl = new NamedMap<Object>();
		KnownType type = null;
		String name = null;
    
		// just eat up the events...
		int depth = 0;
		while (true) {
			switch (parser.next()) {
			case XMLStreamConstants.START_ELEMENT:
				depth ++;
				
				builder.setLength(0); // reset the text
				type = KnownType.get(parser.getLocalName());
				if (type == null) {
					throw new XMLStreamException("this must be known type! not: " 
							+ parser.getLocalName());
				}
        
				name = null;
				int cnt = parser.getAttributeCount();
				
				for (int i=0; i < cnt; i++) {
					if ("name".equals(parser.getAttributeLocalName(i))) {
						name = parser.getAttributeValue(i);
						break;
					}
				}

				/** The name in a NamedList can actually be null
        		if( name == null ) {
          			throw new XMLStreamException("requires 'name' attribute: " 
          				+ parser.getLocalName(), parser.getLocation() );
        		}
				 **/
        
				if (!type.mIsLeaf) {
					switch (type) {
					case LST:    nl.add( name, readNamedList( parser ) ); depth--; continue;
					case ARR:    nl.add( name, readArray(     parser ) ); depth--; continue;
					//case RESULT: nl.add( name, readDocuments( parser ) ); depth--; continue;
					//case DOC:    nl.add( name, readDocument(  parser ) ); depth--; continue;
					default: break;
					}
					throw new XMLStreamException("branch element not handled!", parser.getLocation());
				}
				break;
        
			case XMLStreamConstants.END_ELEMENT:
				if (--depth < 0) 
					return nl;
        
				nl.add(name, type.read( builder.toString().trim()));
				break;

			// TODO?  should this be trimmed? make sure it only gets one/two space?
			case XMLStreamConstants.SPACE: 
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS:
				builder.append( parser.getText() );
				break;
			}
		}
	}

	protected List<Object> readArray(XMLStreamReader parser) throws XMLStreamException {
		if (XMLStreamConstants.START_ELEMENT != parser.getEventType()) 
			throw new XMLStreamException("must be start element, not: " + parser.getEventType());
    
		if (!"arr".equals(parser.getLocalName().toLowerCase(Locale.ROOT))) 
			throw new XMLStreamException("must be 'arr', not: " + parser.getLocalName());
    
		StringBuilder builder = new StringBuilder();
		KnownType type = null;

		List<Object> vals = new ArrayList<Object>();
		int depth = 0;
		
		while (true) {
			switch (parser.next()) {
			case XMLStreamConstants.START_ELEMENT:
				depth ++;
				
				KnownType t = KnownType.get( parser.getLocalName() );
				if (t == null) 
					throw new RuntimeException("this must be known type! not: " + parser.getLocalName());
        
				if (type == null) 
					type = t;
        
				/*** actually, there is no rule that arrays need the same type
        		else if( type != t && !(t == KnownType.NULL || type == KnownType.NULL)) {
          			throw new XMLStreamException( "arrays must have the same type! ("
          				+type+"!="+t+") "+parser.getLocalName() );
        		}
				 ***/
				
				type = t;
				builder.setLength(0); // reset the text
        
				if (!type.mIsLeaf) {
					switch (type) {
					case LST:    vals.add( readNamedList( parser ) ); depth--; continue;
					case ARR:    vals.add( readArray( parser ) ); depth--; continue;
					//case RESULT: vals.add( readDocuments( parser ) ); depth--; continue;
					//case DOC:    vals.add( readDocument( parser ) ); depth--; continue;
					default: break;
					}
					throw new XMLStreamException("branch element not handled!", parser.getLocation());
				}
				break;
        
			case XMLStreamConstants.END_ELEMENT:
				if (--depth < 0) 
					return vals; // the last element is itself
        
				Object val = type.read(builder.toString().trim());
				
				if (val == null && type != KnownType.NULL) 
					throw new XMLStreamException("error reading value:" + type, parser.getLocation());
        
				vals.add(val);
				break;

			// TODO?  should this be trimmed? make sure it only gets one/two space?
			case XMLStreamConstants.SPACE: 
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS:
				builder.append(parser.getText());
				break;
			}
		}
	}

}
