package org.javenstudio.android;

import org.javenstudio.android.reader.ReaderHelper;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.parser.util.SimpleHtmlParser;
import org.javenstudio.util.StringUtils;

public class SimpleHtmlImgParser implements SimpleHtmlParser.ElementParser {

	public final String parse(String html) { 
		return parse(html, false);
	}
	
	public final String parse(String html, boolean parseContent) { 
		SimpleHtmlParser parser = new SimpleHtmlParser(); 
		parser.setElementParser("img", this);
		return parser.parse(html, parseContent); 
	}
	
	@Override
	public void parse(String name, String html) {
		if (!"img".equalsIgnoreCase(name)) 
			return;
		
		String src = parseAttribute(html, "src");
		String alt = parseAttribute(html, "alt");
		String title = parseAttribute(html, "title");
		String original = parseAttribute(html, "original");
		String dataoriginal = parseAttribute(html, "data-original");
		int width = parseInt(html, "width");
		int height = parseInt(html, "height");
		
		handleImg(src, alt, title, original, dataoriginal, width, height);
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
	
	protected void handleImg(String src, String alt, String title, 
			String original, String dataoriginal, int width, int height) { 
		// do nothing
	}
	
    public static String normalizeHref(String href, String from) { 
    	return ReaderHelper.normalizeImageLocation(normalizeImageHref(href, from));
    }
    
    private static String normalizeImageHref(String href, String from) { 
        if (href == null || href.length() == 0) 
            return null; 
        
        //if (href.length() > 128) return null; 
        
        for (int i=0; i < href.length(); i++) { 
            char chr = href.charAt(i); 
            if (chr >= '0' && chr <= '9') continue; 
            if (chr >= 'a' && chr <= 'z') continue; 
            if (chr >= 'A' && chr <= 'Z') continue; 
            switch (chr) { 
                case ':': 
                case '/': 
                case '?': 
                case '.': 
                case '&': 
                case '=': 
                case '_':
                case '-':
                case '%': 
                case ' ': 
                case '!': 
                case '$': 
                    continue; 
                default: 
                    return null; 
            }
        }

        int firstPosHref = getFirstPosition(href);
        int firstPosFrom = getFirstPosition(from);
        
        if (firstPosHref <= 0 && firstPosFrom > 0) { 
        	if (href.startsWith("//")) {
        		String webrootUrl = "http:";
            	
            	int pos1 = from.indexOf("://");
            	if (pos1 > 0) 
            		webrootUrl = from.substring(0, pos1+1);
            	
                href = webrootUrl + href; 
        		
        	} else if (href.startsWith("/")) { 
            	String webrootUrl = from;
            	
            	int pos1 = from.indexOf('/', firstPosFrom);
            	if (pos1 > 0) 
            		webrootUrl = from.substring(0, pos1);
            	
                href = webrootUrl + href; 
                
            } else { 
                int pos2 = from.lastIndexOf('/'); 
                if (pos2 > 0) 
                    href = from.substring(0, pos2+1) + href; 
            }
        }
        
        StringBuilder sbuf = new StringBuilder();
        char pchr = 0; 
        int count1 = 0; 
        
        firstPosHref = getFirstPosition(href);
        if (firstPosHref < 0) return null;
        
        if (firstPosHref > 0)
        	sbuf.append(href.substring(0, firstPosHref));
        
        for (int i=firstPosHref; i < href.length(); i++) { 
            char chr = href.charAt(i); 
            char thepchr = pchr; 
            pchr = chr; 
            
            if ((chr >= '0' && chr <= '9') || (chr >= 'a' && chr <= 'z') || 
            	(chr >= 'A' && chr <= 'Z')) { 
            	sbuf.append(chr);
            	continue; 
            }
            
            switch (chr) { 
                case '/': 
                    if (thepchr == '/') 
                        continue; 
                    break; 
                case '?': 
                    count1 ++; 
                    if (count1 > 1) return null; 
                    break; 
                case ' ': 
                	sbuf.append("%20");
                	continue;
                case '.': 
                case '&': 
                case '=': 
                case '_':
                case '-':
                case '%': 
                case '!': 
                case '$': 
                    break; 
                default: 
                    return null; 
            }
            
            sbuf.append(chr);
        }
        
        return sbuf.toString(); 
    }
	
    private static int getFirstPosition(String url) { 
    	if (url != null) { 
    		int pos = url.indexOf("://");
    		if (pos > 0) return pos + 3;
    		return 0;
    	}
    	return -1;
    }
    
}
