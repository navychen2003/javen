package org.javenstudio.common.parser.html;

import org.javenstudio.util.StringUtils;

public class HTMLCharacterBuilder implements CharacterBuilder {

	private final StringBuilder mBuilder; 
	private final boolean mIncludeHtmlTag; 
	
	protected boolean mIgnoreText = false;
	
	public HTMLCharacterBuilder() { 
		this(false); 
	}
	
	public HTMLCharacterBuilder(boolean includeHtmlTag) { 
		mBuilder = new StringBuilder(); 
		mIncludeHtmlTag = includeHtmlTag; 
	}
	
	@Override 
	public int length() { 
		return mBuilder.length(); 
	}
	
	@Override 
	public char charAt(int index) { 
		return mBuilder.charAt(index); 
	}
	
	@Override 
	public void append(CharSequence text) { 
		if (!mIgnoreText)
			mBuilder.append(text); 
	}
	
	@Override 
	public void getChars(int start, int end, char[] dest, int destStart) { 
		mBuilder.getChars(start, end, dest, destStart); 
	}
	
	@Override 
	public void handleStartTag(String localName, String qName, TagElement tag) { 
		if (mIncludeHtmlTag) 
			handleStartHtmlTag(localName, qName, tag); 
	}
	
	@Override 
	public void handleEndTag(String localName, String qName, TagElement tag) { 
		if (mIncludeHtmlTag) 
			handleEndHtmlTag(localName, qName, tag); 
	}
	
	private void handleStartHtmlTag(String localName, String qName, TagElement tag) { 
		final String name = localName; 
		if (name == null) return; 
		
		if (name.equalsIgnoreCase("br")) {
			start(name, null, true); 
        } else if (name.equalsIgnoreCase("p")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("div")) {
        	//start(name, null, false); 
        } else if (name.equalsIgnoreCase("em")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("b")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("strong")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("cite")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("dfn")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("i")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("big")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("small")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("font")) {
        	start(name, tag, false); 
        } else if (name.equalsIgnoreCase("blockquote")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("tt")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("a")) {
        	start(name, tag, false); 
        } else if (name.equalsIgnoreCase("u")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("sup")) {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("sub")) {
        	start(name, null, false); 
        } else if (name.length() == 2 && Character.toLowerCase(name.charAt(0)) == 'h' &&
        		name.charAt(1) >= '1' && name.charAt(1) <= '6') {
        	start(name, null, false); 
        } else if (name.equalsIgnoreCase("script")) { 
        	handleScriptStart();
        }
	}
	
	private void handleEndHtmlTag(String localName, String qName, TagElement tag) { 
		final String name = localName; 
		if (name == null) return; 
		
		if (name.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emite the linebreaks when we handle the close tag.
        } else if (name.equalsIgnoreCase("p")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("div")) {
        	//end(name); 
        } else if (name.equalsIgnoreCase("em")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("b")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("strong")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("cite")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("dfn")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("i")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("big")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("small")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("font")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("blockquote")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("tt")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("a")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("u")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("sup")) {
        	end(name); 
        } else if (name.equalsIgnoreCase("sub")) {
        	end(name); 
        } else if (name.length() == 2 && Character.toLowerCase(name.charAt(0)) == 'h' &&
        		name.charAt(1) >= '1' && name.charAt(1) <= '6') {
            end(name); 
        } else if (name.equalsIgnoreCase("script")) { 
        	handleScriptEnd();
        }
	}
	
	protected void handleScriptStart() { 
		mIgnoreText = true;
	}
	
	protected void handleScriptEnd() { 
		mIgnoreText = false;
	}
	
	private void end(String name) { 
		if (name != null && name.length() > 0) { 
			append("</"); 
			append(name); 
			append(">"); 
		}
	}
	
	private void start(String name, TagElement tag, boolean endme) { 
		if (name == null || name.length() == 0) 
			return; 
		
		append("<" + name); 
		
		if (tag != null && name.equals(tag.getLocalName())) { 
			for (int i=0; i < tag.getLength(); i++) { 
				String attrName = tag.getLocalName(i); 
				String attrValue = tag.getValue(i); 
				
				if (attrName != null && attrName.length() > 0) { 
					append(" "); 
					append(attrName); 
					
					if (attrValue != null) { 
						append("=\""); 
						append(StringUtils.HTMLEncode(attrValue)); 
						append("\""); 
					}
				}
			}
		}
		
		if (endme) append(" /"); 
		
		append(">"); 
	}
	
	@Override 
	public String toString() { 
		return mBuilder.toString(); 
	}
	
}
