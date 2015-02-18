package org.javenstudio.common.parser.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.javenstudio.common.parser.TagHandler;

public class XmlParser extends DefaultHandler {

	private final TagHandler mHandler; 
	private final SAXParser mParser; 
	
	public XmlParser(TagHandler handler) throws SAXException, ParserConfigurationException { 
		mHandler = handler; 
		mParser = SAXParserFactory.newInstance().newSAXParser(); 
	}
	
	public void parse(String source) throws IOException, SAXException { 
		parse(new InputSource(new StringReader(source))); 
	}
	
	public void parse(InputSource source) throws IOException, SAXException { 
		mParser.parse(source, this); 
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
	
}
