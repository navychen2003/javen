package org.javenstudio.common.parser.util;

import java.util.ArrayList;
import java.util.HashMap;

public class ParseUtils {

	public static String extractContentFromHtml(String html) {
		return extractContentFromHtml(html, null); 
	}

	public static String extractContentFromHtml(String html, ArrayList<String> links) {
		StringBuilder content = new StringBuilder(); 

		if( html == null ) 
			return ""; 

		int pos = 0; 
		char pchr = 0; 
		
		while (pos < html.length()) {
			char ch = html.charAt(pos++); 
			if (ch == '<') {
		        boolean gotname = false; 
		        String itemname = ""; 
		        StringBuilder itembuf = new StringBuilder(); 
		        
		        while (pos < html.length()) {
		        	char chr = html.charAt(pos++); 
		        	if (chr == '>') 
		        		break; 
		        	if (isWhiteSpace(chr)) {
		        		if (itemname.length() > 0) 
		        			gotname = true; 
		        	} else if (gotname == false) {
		        		if (chr != '/') 
		        			itemname += chr; 
		        	}
		        	if (gotname) itembuf.append(chr); 
		        }
		        
		        if (itemname.equalsIgnoreCase("script") || itemname.equalsIgnoreCase("style")) {
		        	String endname = "</"+itemname+">"; 
		        	int pos2 = indexOfIgnoreCase(html, endname, pos); 
		        	if (pos2 > pos) 
		        		pos = pos2 + endname.length(); 
		        }
		        
		        itemname = itemname.toLowerCase(); 
		        if (itemname.equals("p") || itemname.equals("div") || itemname.equals("td") ||
		            itemname.equals("li") || itemname.equals("br")) {
		        	pchr = '\n'; 
		        	content.append(pchr); 
		        }
		        else if (itemname.equals("a")) {
		        	if (links != null) {
		        		String asrc = extractParameterFromElement(itembuf.toString(), "href"); 
		        		if (asrc != null && asrc.length() > 0) 
		        			links.add(asrc); 
		        	}
		        }
		        else if (itemname.equals("font")) {
		        	
		        }
		        else {
		          //pchr = ' '; 
		          //content.append(pchr); 
		        }
			}
		 	else if (ch == '&') {
		        boolean gotname = false; 
		        String itemname = "&", temp = "&"; 
		        while (pos < html.length()) {
		        	char chr = html.charAt(pos++); 
		        	if (!isIllegalChar(ch)) {
		        		if (!(isWhiteSpace(pchr) && isWhiteSpace(ch))) 
		        			temp += chr; 
		        		pchr = chr; 
		        	}
		        	if (chr == ';') {
			            if (itemname.length() > 1) {
			            	itemname += chr; 
			            	gotname = true; 
			            }
			            break; 
			     	}
			        if (isWhiteSpace(chr)) 
			            break; 
			        if (isEnglishOrNumberChar(chr) || chr == '#') 
			            itemname += chr; 
			       	else
			            break; 
				}
				if (gotname == false) 
					content.append(temp); 
				else
					content.append(getMappedValue(itemname)); 
			}
		 	else if (!isIllegalChar(ch)) {
		        pchr = appendContentChar(content,ch,pchr); 
			}
		}

		return content.toString(); 
	}

	public static String removeWhiteSpaces(String text) { 
		if (text != null && text.length() > 1) { 
			StringBuilder sbuf = new StringBuilder(); 
			boolean lastSpace = false; 
			for (int i=0; i < text.length(); i++) { 
				char chr = text.charAt(i); 
				boolean space = isWhiteSpace(chr); 
				if (space) { 
					if (!lastSpace) sbuf.append(' '); 
				} else 
					sbuf.append(chr); 
				lastSpace = space; 
			}
			text = sbuf.toString(); 
		}
		return text; 
	}
	
	public static boolean isWhiteSpace(char chr) {
	    if (chr == ' ' || chr == '\t' || chr == '\r' || chr == '\n' || chr == '\u3000' || chr == '\u00A0')
	    	return true; 
	    else
	    	return false; 
	}

	public static boolean isIllegalChar(char chr) {
		if (chr == '\r')
			return true; 
		else
			return false; 
	}

	public static boolean isLeftBrackets(char chr) {
	    switch (chr) {
	    	case '(': 
	    	case '<': 
	    	case '[': 
	    	case '{': 
	    	case '"': 
	    		return true; 
	    }
	    return false; 
	}

	public static boolean isEnglishOrNumberChar(char chr) {
		if ((chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z') ||
			(chr >= '0' && chr <= '9') )
			return true; 
		else
			return false; 
	}

	static HashMap<String,String> _map = new HashMap<String,String>(); 
	static {
		_map.put("&nbsp;", " "); 
		_map.put("&gt;", ">"); 
		_map.put("&lt;", "<"); 
		_map.put("&quot;", "\""); 
		_map.put("&amp;", "&"); 
		_map.put("&iexcl;", "\u00a1");
		_map.put("&cent;", "\u00a2");
		_map.put("&pound;", "\u00a3");
		_map.put("&curren;", "\u00a4");
		_map.put("&yen;", "\u00a5");
		_map.put("&brvbar;", "\u00a6");
		_map.put("&sect;", "\u00a7");
		_map.put("&uml;", "\u00a8");
		_map.put("&copy;", "\u00a9");
		_map.put("&ordf;", "\u00aa");
		_map.put("&laquo;", "\u00ab");
		_map.put("&not", "\u00ac");
		_map.put("&shy;", "\u00ad");
		_map.put("&reg;", "\u00ae");
		_map.put("&macr;", "\u00af");
		_map.put("&deg;", "\u00b0");
		_map.put("&plusmn;", "\u00b1");
		_map.put("&sup2;", "\u00b2");
		_map.put("&sup3;", "\u00b3");
		_map.put("&acute;", "\u00b4");
		_map.put("&micro;", "\u00b5");
		_map.put("&para;", "\u00b6");
		_map.put("&middot;", "\u00b7");
		_map.put("&cedil;", "\u00b8");
		_map.put("&sup1;", "\u00b9");
		_map.put("&ordm;", "\u00ba");
		_map.put("&raquo;", "\u00bb");
		_map.put("&frac14;", "\u00bc");
		_map.put("&frac12;", "\u00bd");
		_map.put("&frac34;", "\u00be");
		_map.put("&iquest;", "\u00bf");
		_map.put("&agrave;", "\u00c0");
		_map.put("&aacute;", "\u00c1");
		_map.put("&acirc;", "\u00c2");
		_map.put("&atilde;", "\u00c3");
		_map.put("&auml;", "\u00c4");
		_map.put("&aring;", "\u00c5");
		_map.put("&aelig;", "\u00c6");
		_map.put("&ccedil;", "\u00c7");
		_map.put("&egrave;", "\u00c8");
		_map.put("&eacute;", "\u00c9");
		_map.put("&ecirc;", "\u00ca");
		_map.put("&euml;", "\u00cb");
		_map.put("&igrave;", "\u00cc");
		_map.put("&iacute;", "\u00cd");
		_map.put("&icirc;", "\u00ce");
		_map.put("&iuml;", "\u00cf");
		_map.put("&eth;", "\u00d0");
		_map.put("&ntilde;", "\u00d1");
		_map.put("&ograve;", "\u00d2");
		_map.put("&oacute;", "\u00d3");
		_map.put("&ocirc;", "\u00d4");
		_map.put("&otilde;", "\u00d5");
		_map.put("&ouml;", "\u00d6");
		_map.put("&times;", "\u00d7");
		_map.put("&oslash;", "\u00d8");
		_map.put("&ugrave;", "\u00d9");
		_map.put("&uacute;", "\u00da");
		_map.put("&ucirc;", "\u00db");
		_map.put("&uuml;", "\u00dc");
		_map.put("&yacute;", "\u00dd");
		_map.put("&thorn;", "\u00de");
		_map.put("&szlig;", "\u00df");
		_map.put("&agrave;", "\u00e0");
		_map.put("&aacute;", "\u00e1");
		_map.put("&acirc;", "\u00e2");
		_map.put("&atilde;", "\u00e3");
		_map.put("&auml;", "\u00e4");
		_map.put("&aring;", "\u00e5");
		_map.put("&aelig;", "\u00e6");
		_map.put("&ccedil;", "\u00e7");
		_map.put("&egrave;", "\u00e8");
		_map.put("&eacute;", "\u00e9");
		_map.put("&ecirc;", "\u00ea");
		_map.put("&euml;", "\u00eb");
		_map.put("&igrave;", "\u00ec");
		_map.put("&iacute;", "\u00ed");
		_map.put("&icirc;", "\u00ee");
		_map.put("&iuml;", "\u00ef");
		_map.put("&eth;", "\u00f0");
		_map.put("&ntilde;", "\u00f1");
		_map.put("&ograve;", "\u00f2");
		_map.put("&oacute;", "\u00f3");
		_map.put("&ocirc;", "\u00f4");
		_map.put("&otilde;", "\u00f5");
		_map.put("&ouml;", "\u00f6");
		_map.put("&divide;", "\u00f7");
		_map.put("&oslash;", "\u00f8");
		_map.put("&ugrave;", "\u00f9");
		_map.put("&uacute;", "\u00fa");
		_map.put("&ucirc;", "\u00fb");
		_map.put("&uuml;", "\u00fc");
		_map.put("&yacute;", "\u00fd");
		_map.put("&thorn;", "\u00fe");
		_map.put("&yuml;", "\u00ff");
	}

	public static String getMappedValue(String key) {
		if (key.length() >= 4 && key.length() <= 6) {
			if (key.charAt(0) == '&' && key.charAt(1) == '#' && 
				key.charAt(key.length()-1) == ';') {
		        String str = key.substring(2,key.length()-1); 
		        try {
		        	int num = Integer.valueOf(str); 
		        	char chr = (char)num; 
		        	str = "" + chr; 
		        	if (num >=32 && num <=255 && str.length() > 0)
		        		return str; 
		        } catch(Exception e) {
		        }
			}
		}

		String skey = key.toLowerCase(); 
		if (_map.containsKey(skey)) 
			return _map.get(skey); 
		else
			return key; 
	}

	public static char appendContentChar(StringBuilder content, char chr, char pchr) {
		char ppchr = pchr; 
		if (!(isWhiteSpace(pchr) && isWhiteSpace(chr))) {
			if (content != null) 
				content.append(chr); 
			ppchr = chr; 
		}
		return ppchr; 
	}
	
	public static String extractParameterFromElement(String itembuf, String name) {
		if (itembuf == null || itembuf.length() == 0 || name == null || name.length() == 0) 
			return null; 

		name = name.toLowerCase(); 

		int pos = 0; 
		while (pos < itembuf.length()) {
			StringBuilder sbuf = new StringBuilder();
			pos = skipChars(itembuf, pos, new char[]{' ','\t','\r','\n'});
			pos = readToChars(itembuf, pos, sbuf, new char[]{' ','\t','\r','\n','='});
			String paramname = trim(sbuf.toString()); 

			sbuf.setLength(0);
			pos = readToChars(itembuf, pos, sbuf, new char[]{' ','\t','\r','\n'});
			String paramvalue = trim(sbuf.toString()); 

			if (paramname != null && paramname.length() > 0) {
				String lowername = paramname.toLowerCase(); 
		        if (lowername.equals(name)) 
		          return paramvalue; 
			}
		}

		return null; 
	}

	private static int skipChars(String str, int pos, char[] chars) {
		while (pos < str.length()) {
			char chr = str.charAt(pos++);
			int i = 0;
			for (i=0; i<chars.length; i++)
		        if (chr == chars[i]) break;
			if (i >= chars.length) {
				pos --;
		        break;
			}
		}
		return pos;
	}

	private static int readToChars(String str, int pos, StringBuilder sbuf, char[] chars) {
		while (pos < str.length()) {
			char chr = str.charAt(pos++);
			int i = 0;
			for (i=0; i<chars.length; i++)
		        if (chr == chars[i]) break;
			if (i < chars.length) break;
			if (chr == '"') {
		        sbuf.append(chr);
		        while (pos < str.length()) {
		        	chr = str.charAt(pos++);
		        	sbuf.append(chr);
		        	if (chr == '"') break;
		        }
			}
			else
		        sbuf.append(chr);
		}
		return pos;
	}

	public static String strim(String str) {
		str = trim(str); 
		return str == null ? "" : str; 
	}
		  
	public static String trim(String str) {
		if (str != null ) {
			int pos1 = 0;
			int pos2 = str.length();

			while (pos1 < str.length()) {
		        char chr = str.charAt(pos1++);
		        //if (chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n')
		        if (isWhiteSpace(chr)) 
		        	continue;
		        pos1 --;
		        break;
			}

			while (pos2 > 0) {
		        char chr = str.charAt(--pos2);
		        //if( chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n' )
		        if (isWhiteSpace(chr)) 
		        	continue;
		        pos2 ++;
		        break;
			}

			if (pos1 < 0 || pos2 < 0)
		        str = "";
		  	else if( pos1 > pos2 )
		        str = "";
			else if( (pos1 != 0 || pos2 != str.length()) )
		        str = str.substring(pos1, pos2);
		}

		return str;
	}
	
   /**
    * Returns the index within this string of the first occurrence of the
    * specified substring. The integer returned is the smallest value
    * <i>k</i> such that:
    * <blockquote><pre>
    * this.startsWith(str, <i>k</i>)
    * </pre></blockquote>
    * is <code>true</code>.
    *
    * @param   str   any string.
    * @return  if the string argument occurs as a substring within this
    *          object, then the index of the first character of the first
    *          such substring is returned; if it does not occur as a
    *          substring, <code>-1</code> is returned.
    */
	public static int indexOfIgnoreCase(String str, String substr) {
		return indexOfIgnoreCase(str, substr, 0);
	}

   /**
    * Returns the index within this string of the first occurrence of the
    * specified substring, starting at the specified index.  The integer
    * returned is the smallest value <tt>k</tt> for which:
    * <blockquote><pre>
    *     k &gt;= Math.min(fromIndex, this.length()) && this.startsWith(str, k)
    * </pre></blockquote>
    * If no such value of <i>k</i> exists, then -1 is returned.
    *
    * @param   str         the substring for which to search.
    * @param   fromIndex   the index from which to start the search.
    * @return  the index within this string of the first occurrence of the
    *          specified substring, starting at the specified index.
    */
	public static int indexOfIgnoreCase(String str, String substr, int fromIndex) {
	    return indexOfIgnoreCase(
			                  str, 0, str.length(), 
	                     	substr, 0, substr.length(), fromIndex);
	}

   /**
    * Code shared by String and StringBuffer to do searches. The
    * source is the character array being searched, and the target
    * is the string being searched for.
    *
    * @param   source       the characters being searched.
    * @param   sourceOffset offset of the source string.
    * @param   sourceCount  count of the source string.
    * @param   target       the characters being searched for.
    * @param   targetOffset offset of the target string.
    * @param   targetCount  count of the target string.
    * @param   fromIndex    the index to begin searching from.
    */
	public static int indexOfIgnoreCase(
	                     String source, int sourceOffset, int sourceCount,
	                     String target, int targetOffset, int targetCount,
	                     int fromIndex) {
	  	if (fromIndex >= sourceCount) {
	      return (targetCount == 0 ? sourceCount : -1);
	  	}
	  	if (fromIndex < 0) {
	  	  fromIndex = 0;
	  	}
	  	if (targetCount == 0) {
	      return fromIndex;
	  	}

	    char first  = target.charAt(targetOffset);
	    int max = sourceOffset + (sourceCount - targetCount);

	    for (int i = sourceOffset + fromIndex; i <= max; i++) {
	        /* Look for first character. */
	        if (lower(source.charAt(i)) != lower(first)) {
	            while (++i <= max && lower(source.charAt(i)) != lower(first));
	        }

	        /* Found first character, now look at the rest of v2 */
	        if (i <= max) {
	            int j = i + 1;
	            int end = j + targetCount - 1;
	            for (int k = targetOffset + 1; j < end && lower(source.charAt(j)) ==
	                     lower(target.charAt(k)); j++, k++);

	            if (j == end) {
	                /* Found whole string. */
	                return i - sourceOffset;
	            }
	        }
	    }
	    return -1;
	}

	static char lower(char ch) {
		return ch>='A'&&ch<='Z' ? (char)(ch+32) : ch; 
	}
	
}
