package org.javenstudio.common.parser.html;

public interface ContentFactory {

	public boolean isDebug();
	public Content newDocumentContent(TagElement start); 
	public Content newSubContent(TagElement start); 
	
	public static final ContentFactory EMPTY = new ContentFactory() {
			public boolean isDebug() { return false; }
			public Content newDocumentContent(TagElement start) { return null; }
			public Content newSubContent(TagElement start) { return null; }
		};
	
}
