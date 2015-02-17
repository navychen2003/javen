package org.javenstudio.common.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface TagHandler {

	public void handleStartDocument() throws SAXException; 
	public void handleEndDocument(); 
	
	public void handleStartTag(String localName, String qName, Attributes attributes); 
	public void handleEndTag(String localName, String qName); 
	
	public int length(); 
	public char charAt(int position); 
	public void append(CharSequence text); 
	
}
