package org.javenstudio.common.parser.util;

import java.util.ArrayList;

import org.javenstudio.util.StringUtils;

public class ApacheDirectoryParser extends DirectoryParser {

	public ApacheDirectoryParser() {} 
	
	public WebFile[] parse(String html) {
		if (html == null || html.length() == 0) 
			return null; 
		
		final ArrayList<WebFile> files = new ArrayList<WebFile>(); 
		
		SimpleHtmlParser.ElementParser ep = new SimpleHtmlParser.ElementParser() {
				private boolean mFoundStart = false; 
				
				@Override
				public void parse(String name, String html) {
					if (mFoundStart == false) {
						if ("td".equalsIgnoreCase(name)) 
							mFoundStart = true; 
						return;
					}
					if ("a".equalsIgnoreCase(name)) {
						String href = StringUtils.trimChars(
								ParseUtils.extractParameterFromElement(html, "href"), 
								" \t\r\n'\"");
						
						if (href != null && href.length() > 0) {
							char firstChar = href.charAt(0);
							char lastChar = href.charAt(href.length()-1);
							if (firstChar != '/') {
								if (lastChar == '/')
									files.add(new WebDirectory(href.substring(0, href.length()-1))); 
								else
									files.add(new WebFile(href)); 
							}
						}
					}
				}
			}; 
		
		SimpleHtmlParser parser = new SimpleHtmlParser(); 
		parser.setElementParser("td", ep);
		parser.setElementParser("a", ep);
		parser.parse(html, false); 
		
		return files.toArray(new WebFile[files.size()]); 
	}
	
}
