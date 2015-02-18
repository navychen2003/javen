package org.javenstudio.common.parser.html;

public interface Content {

	public TagElement getStart(); 
	public void handleElementStart(TagElement e); 
	public void handleElementEnd(TagElement e, int endLength); 
	public void handleEnd(); 
	
}
