package org.javenstudio.android;

import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.parser.util.SimpleHtmlParser;
import org.javenstudio.util.StringUtils;

public class SimpleHtmlHrefParser implements SimpleHtmlParser.ElementParser {

	public final String parse(String html) { 
		return parse(html, false);
	}
	
	public final String parse(String html, boolean parseContent) { 
		SimpleHtmlParser parser = new SimpleHtmlParser(); 
		parser.setElementParser("a", this);
		return parser.parse(html, parseContent); 
	}
	
	@Override
	public void parse(String name, String html) {
		if (!"a".equalsIgnoreCase(name)) 
			return;
		
		String href = parseAttribute(html, "href");
		
		handleHref(href);
	}
	
	protected String parseAttribute(String html, String name) { 
		return StringUtils.trimChars(
				ParseUtils.extractContentFromHtml(
						ParseUtils.extractParameterFromElement(html, name)), 
				" \t\r\n'\""); 
	}
	
	protected int parseInt(String html, String name) { 
		try { 
			return Integer.parseInt(parseAttribute(html, name));
		} catch (Throwable e) { 
			return 0;
		}
	}
	
	protected void handleHref(String href) { 
		// do nothing
	}
	
    public static String normalizeHref(String href, String from) { 
    	return SimpleHtmlImgParser.normalizeHref(href, from);
    }

}
