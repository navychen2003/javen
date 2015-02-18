package org.javenstudio.common.parser.html;

import java.util.HashSet;
import java.util.Set;

import org.javenstudio.util.StringUtils;

public class HTMLCharacterBuilder implements CharacterBuilder {

	private static final String[] DEFAULT_TAGS = new String[] { 
			"br", "p", /*"div",*/ "em", "b", "strong", "cite", "dfn", "i", "big", 
			"small", "font", "blockquote", "tt", "a", "u", "sup", "sub", 
			"img", "h1", "h2", "h3", "h4", "h5", "h6" 
		};
	
	private final Set<String> mIncludeTags;
	private final StringBuilder mBuilder; 
	private final boolean mIncludeHtmlTag; 
	
	protected boolean mIgnoreText = false;
	
	public HTMLCharacterBuilder() { 
		this(false); 
	}
	
	public HTMLCharacterBuilder(boolean includeHtmlTag) { 
		mBuilder = new StringBuilder(); 
		mIncludeTags = initIncludeTags(DEFAULT_TAGS);
		mIncludeHtmlTag = includeHtmlTag; 
	}
	
	private Set<String> initIncludeTags(String[] tags) { 
		Set<String> set = new HashSet<String>();
		for (String tag : tags) { 
			set.add(tag.toLowerCase());
		}
		
		return set;
	}
	
	public final void includeTag(String tag) { 
		if (tag != null) 
			mIncludeTags.add(tag.toLowerCase());
	}
	
	public final void removeIncludeTag(String tag) { 
		if (tag != null) 
			mIncludeTags.remove(tag.toLowerCase());
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
	
	protected void handleStartHtmlTag(String localName, String qName, TagElement tag) { 
		final String name = localName; 
		if (name == null) return; 
		
		if (name.equalsIgnoreCase("br")) {
			start(name, null, true); 
        } else if (name.equalsIgnoreCase("script")) { 
        	handleScriptStart();
        } else if (name.equalsIgnoreCase("style")) { 
        	handleStyleStart();
        } else if (mIncludeTags.contains(name.toLowerCase())) { 
        	start(name, tag, false);
        }
	}
	
	protected void handleEndHtmlTag(String localName, String qName, TagElement tag) { 
		final String name = localName; 
		if (name == null) return; 
		
		if (name.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emite the linebreaks when we handle the close tag.
        } else if (name.equalsIgnoreCase("script")) { 
        	handleScriptEnd();
        } else if (name.equalsIgnoreCase("style")) { 
        	handleStyleEnd();
        } else if (mIncludeTags.contains(name)) { 
        	end(name);
        }
	}
	
	protected void handleScriptStart() { mIgnoreText = true; }
	protected void handleScriptEnd() { mIgnoreText = false; }
	
	protected void handleStyleStart() { mIgnoreText = true; }
	protected void handleStyleEnd() { mIgnoreText = false; }
	
	protected void end(String name) { 
		if (name != null && name.length() > 0) { 
			append("</"); 
			append(name); 
			append(">"); 
		}
	}
	
	protected void start(String name, TagElement tag, boolean endme) { 
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
