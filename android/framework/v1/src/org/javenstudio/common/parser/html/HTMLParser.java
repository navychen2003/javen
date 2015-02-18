package org.javenstudio.common.parser.html;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.Parser;
import org.javenstudio.common.parser.TagHandler;
import org.javenstudio.common.parser.util.XmlUtils;

public final class HTMLParser {

	public static HTMLParser newParser() { 
		return newParser(null, null); 
	}
	
	public static HTMLParser newParser(XMLReader parser) { 
		return newParser(parser, null); 
	}
	
	public static HTMLParser newParser(TagHandler handler) { 
		return newParser(null, handler); 
	}
	
	public static HTMLParser newParser(XMLReader parser, TagHandler handler) { 
		return new HTMLParser(parser, handler); 
	}
	
	/**
     * Lazy initialization holder for HTML parser. This class will
     * a) be preloaded by the zygote, or b) not loaded until absolutely
     * necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }
	
    private XMLReader mParser; 
    private final TagHandler mHandler; 
    
    private HTMLParser(XMLReader parser, TagHandler handler) {
    	mParser = parser; 
    	mHandler = handler; 
    }
    
    public void parse(String source) throws ParseException { 
    	parse(source, null); 
    }
    
    public synchronized void parse(String source, TagHandler handler) throws ParseException { 
    	if (handler == null) handler = mHandler; 
    	if (handler == null) 
    		throw new ParseException("no tag handler defined"); 
    	
    	XMLReader parser = mParser; 
    	if (parser == null) { 
	        try {
	        	Parser p = new Parser();
	            p.setProperty(Parser.schemaProperty, HtmlParser.schema);
	            mParser = parser = p; 
	        } catch (org.xml.sax.SAXNotRecognizedException e) {
	            // Should not happen.
	            throw new ParseException(e);
	        } catch (org.xml.sax.SAXNotSupportedException e) {
	            // Should not happen.
	            throw new ParseException(e);
	        }
    	}
        
        new HtmlHandler(source, parser, handler).parse(); 
    }
    
    private static class HtmlHandler implements ContentHandler { 

        private final String mSource;
        private final XMLReader mReader;
        private final TagHandler mHandler; 
        
        public HtmlHandler(String source, XMLReader parser, TagHandler handler) { 
        	mSource = source; 
        	mReader = parser; 
        	mHandler = handler; 
        }
    	
        public void parse() throws ParseException { 
        	mReader.setContentHandler(this);
            try {
                mReader.parse(new InputSource(new StringReader(mSource)));
            } catch (IOException e) {
                // We are reading from a string. There should not be IO problems.
                throw new ParseException(e);
            } catch (SAXException e) {
                // TagSoup doesn't throw parse exceptions.
                throw new ParseException(e);
            }
        }
        
        @Override 
        public void setDocumentLocator(Locator locator) {
        }

        @Override 
        public void startDocument() throws SAXException {
        	mHandler.handleStartDocument(); 
        }

        @Override 
        public void endDocument() throws SAXException {
        	mHandler.handleEndDocument(); 
        }

        @Override 
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        @Override 
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override 
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            mHandler.handleStartTag(localName, qName, attributes);
        }

        @Override 
        public void endElement(String uri, String localName, String qName) throws SAXException {
            mHandler.handleEndTag(localName, qName);
        }

        @Override 
        public void characters(char ch[], int start, int length) throws SAXException {
        	XmlUtils.onCharacters(mHandler, ch, start, length); 
        }

        @Override 
        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        }

        @Override 
        public void processingInstruction(String target, String data) throws SAXException {
        }

        @Override 
        public void skippedEntity(String name) throws SAXException {
        }
    }
    
}
