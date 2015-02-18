package org.javenstudio.common.util;

public class SimpleHtmlEncoder implements InputEncoder {

	public SimpleHtmlEncoder() {} 
	
	public String encode(String text) {
		if (text == null || text.length() == 0) 
			return text; 
		
		StringBuilder sbuf = new StringBuilder(); 
		
		for (int i=0; i < text.length(); i++) {
			char chr = text.charAt(i); 
			switch (chr) {
			case '\r': 
				break; 
			case '\n': 
				sbuf.append("<br />");
				break; 
			default: 
				sbuf.append(chr); 
				break; 
			}
		}
		
		return sbuf.toString(); 
	}
	
}
