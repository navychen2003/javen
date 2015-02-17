package org.javenstudio.common.parser.util;

import java.util.HashMap;
import java.util.Map;

public class SimpleHtmlParser {

	public static interface ElementParser {
		public void parse(String name, String html); 
	}
	
	private static class TempValues {
		private final StringBuilder content; 
		public String html = null; 
		public int pos = 0; 
		public char pchr = 0; 
		public char chr = 0; 
		
		public TempValues(boolean parseContent) {
			if (parseContent) 
				content = new StringBuilder(); 
			else
				content = null;
		}
		
		public void append(String str) {
			if (content != null && str != null) 
				content.append(str); 
		}
		
		public void append(char chr) {
			if (content != null && chr != 0) 
				content.append(chr); 
		}
		
		public String getContent() { 
			return content != null ? content.toString() : null;
		}
	}
	
	private Map<String, ElementParser> mParsers = new HashMap<String, ElementParser>(); 
	
	public SimpleHtmlParser() {} 
	
	public void setElementParser(String name, ElementParser parser) {
		if (name != null && name.length() > 0 && parser != null) 
			mParsers.put(name, parser); 
	}
	
	public String parse(String html) {
		return parse(html, true); 
	}
	
	public String parse(String html, boolean parseContent) {
		if (html == null || html.length() == 0) 
			return null; 

		StringBuilder itembuf = new StringBuilder(); 
		TempValues values = new TempValues(parseContent); 
		
		values.html = html; 
		values.pos = 0; 
		values.pchr = ' '; 
		
		while (values.pos < values.html.length()) {
			values.chr = values.html.charAt(values.pos++); 
			if (values.chr == '<') {
		        boolean gotname = false, gotend = false; 
		        String itemname = ""; 
		        itembuf.setLength(0); 
		        
		        char preChr = 0;
		        while (values.pos < values.html.length()) {
		        	char chr = values.html.charAt(values.pos++); 
		        	if (chr == '>') {
		        		if (preChr == '/') { 
		        			//gotend = true;
		        			if (itembuf.length() > 0 && itembuf.charAt(itembuf.length()-1) == '/')
		        				itembuf.deleteCharAt(itembuf.length()-1);
		        		}
		        		break; 
		        	}
		        	if (ParseUtils.isWhiteSpace(chr)) {
		        		if (itemname.length() > 0) 
		        			gotname = true; 
		        	} else if (gotname == false) {
		        		if (chr != '/') 
		        			itemname += chr; 
		        		else
		        			gotend = true; 
		        	}
		        	if (gotname) itembuf.append(chr); 
		        	preChr = chr;
		        }
		        
		        if (!gotend && (itemname.equalsIgnoreCase("script") || itemname.equalsIgnoreCase("style"))) {
		        	String endname = "</"+itemname+">"; 
		        	int pos2 = ParseUtils.indexOfIgnoreCase(html, endname, values.pos); 
		        	if (pos2 > values.pos) 
		        		values.pos = pos2 + endname.length(); 
		        }
		        
		        itemname = itemname.toLowerCase(); 
		        if (itemname.equals("p") || itemname.equals("div") || itemname.equals("td") ||
		            itemname.equals("li") || itemname.equals("br")) {
		        	values.chr = '\n'; 
		        	parseIllegalChar(values); 
		        } else {
		        	values.chr = ' ';
		        	parseIllegalChar(values); 
		        }
		        
		        if (gotend == false)
		        	parseElement(itemname, itembuf.toString()); 
			}
		 	else if (values.chr == '&') {
		 		parseEncodedChar(values); 
			}
		 	else {
		 		parseIllegalChar(values); 
		 	}
		}
		
		return parseContent ? values.getContent() : null; 
	}
	
	private void parseElement(String name, String subhtml) {
		if (name == null) return; 
		
		ElementParser parser = mParsers.get(name); 
		if (parser != null) 
			parser.parse(name, subhtml); 
	}
	
	private void parseIllegalChar(TempValues values) {
		if (!ParseUtils.isIllegalChar(values.chr)) {
	        values.pchr = appendContentChar(values); 
		}
	}
	
	private static char appendContentChar(TempValues values) {
		char ppchr = values.pchr; 
		if (!(ParseUtils.isWhiteSpace(values.pchr) && ParseUtils.isWhiteSpace(values.chr))) {
			values.append(values.chr); 
			ppchr = values.chr; 
		}
		return ppchr; 
	}
	
	private void parseEncodedChar(TempValues values) {
		boolean gotname = false; 
        String itemname = "&", temp = "&"; 
        
        while (values.pos < values.html.length()) {
        	char chr = values.html.charAt(values.pos++); 
        	if (!ParseUtils.isIllegalChar(values.chr)) {
        		if (!(ParseUtils.isWhiteSpace(values.pchr) && ParseUtils.isWhiteSpace(values.chr))) 
        			temp += chr; 
        		values.pchr = chr; 
        	}
        	if (chr == ';') {
	            if (itemname.length() > 1) {
	            	itemname += chr; 
	            	gotname = true; 
	            }
	            break; 
	     	}
	        if (ParseUtils.isWhiteSpace(chr)) 
	            break; 
	        if (ParseUtils.isEnglishOrNumberChar(chr) || chr == '#') 
	            itemname += chr; 
	       	else
	            break; 
		}
		if (gotname == false) 
			values.append(temp); 
		else
			values.append(ParseUtils.getMappedValue(itemname)); 
	}
	
}
