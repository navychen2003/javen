package org.javenstudio.common.parser.util;

import java.util.HashMap;
import java.util.Map;

public class SimpleHtmlParser {

	public static interface ElementParser {
		public void parse(String name, String html); 
	}
	
	private static class TempValues {
		public String html = null; 
		public StringBuilder content = null; 
		public int pos = 0; 
		public char pchr = 0; 
		public char chr = 0; 
		
		public TempValues(boolean parseContent) {
			if (parseContent) 
				content = new StringBuilder(); 
		}
		
		public void append(String str) {
			if (content != null && str != null) 
				content.append(str); 
		}
		
		public void append(char chr) {
			if (content != null) 
				content.append(chr); 
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

		TempValues values = new TempValues(parseContent); 
		values.html = html; 
		values.pos = 0; 
		values.pchr = 0; 
		
		StringBuilder itembuf = new StringBuilder(); 
		
		while (values.pos < values.html.length()) {
			values.chr = values.html.charAt(values.pos++); 
			if (values.chr == '<') {
		        boolean gotname = false, gotend = false; 
		        String itemname = ""; 
		        itembuf.setLength(0); 
		        
		        while (values.pos < values.html.length()) {
		        	char chr = values.html.charAt(values.pos++); 
		        	if (chr == '>') 
		        		break; 
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
		        	values.pchr = '\n'; 
		        	values.append(values.pchr); 
		        }
		        else {
		          //pchr = ' '; 
		          //values.append(pchr); 
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
		
		return parseContent ? values.content.toString() : null; 
	}
	
	private void parseElement(String name, String subhtml) {
		if (name == null) return; 
		
		ElementParser parser = mParsers.get(name); 
		if (parser != null) 
			parser.parse(name, subhtml); 
	}
	
	private void parseIllegalChar(TempValues values) {
		if (!ParseUtils.isIllegalChar(values.chr)) {
	        values.pchr = ParseUtils.appendContentChar(values.content, values.chr, values.pchr); 
		}
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
