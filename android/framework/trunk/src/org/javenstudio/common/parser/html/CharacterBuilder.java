package org.javenstudio.common.parser.html;

public interface CharacterBuilder {

	public int length(); 
	public char charAt(int index); 
	public void append(CharSequence text); 
	public void getChars(int start, int end, char[] dest, int destStart); 
	public String toString(); 
	
	public void handleStartTag(String localName, String qName, TagElement tag); 
	public void handleEndTag(String localName, String qName, TagElement tag); 
	
}
