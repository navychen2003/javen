package org.javenstudio.common.parser.html;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.TagHandler;
import org.javenstudio.common.util.Logger;

public class HTMLHandler implements TagHandler {
	static final Logger LOG = Logger.getLogger(HTMLHandler.class);

	private final TagTraveller mTraveller; 
	private final CharacterBuilder mBuilder; 
	private final Stack<TagElement> mElements; 
	private final ContentFactory mFactory; 
	
	private Content mDocument = null; 
	private Content mSubCurrent = null; 
	
	public HTMLHandler(ContentFactory factory) {
		this(factory, new HTMLCharacterBuilder()); 
	}
	
	public HTMLHandler(ContentFactory factory, CharacterBuilder builder) {
		mTraveller = new TagTraveller(this); 
		mElements = new Stack<TagElement>(); 
		mBuilder = builder; 
		mFactory = factory; 
	}
	
	public final TagTraveller getTraveller() { 
		return mTraveller; 
	}
	
	public final CharacterBuilder getCharacterBuilder() { 
		return mBuilder; 
	}
	
	public final Tag newSubContentBeginTag(String name) { 
		return newSubContentBeginTag(name, null, null); 
	}
	
	public final Tag newSubContentBeginTag(String name, String[] attrNames, String[] attrValues) { 
		return mTraveller.newBeginTree(name, attrNames, attrValues, null).getRootTag(); 
	}
	
	public final Tag newSubContentBeginTag(String name, Tag.AttributesMatcher matcher) { 
		return mTraveller.newBeginTree(name, null, null, matcher).getRootTag(); 
	}
	
	public final Tag newSubContentEndTag(String name) { 
		return newSubContentEndTag(name, null, null); 
	}
	
	public final Tag newSubContentEndTag(String name, String[] attrNames, String[] attrValues) { 
		return mTraveller.newEndTree(name, attrNames, attrValues, null).getRootTag(); 
	}
	
	public final Tag newSubContentEndTag(String name, Tag.AttributesMatcher matcher) { 
		return mTraveller.newEndTree(name, null, null, matcher).getRootTag(); 
	}
	
	public final Tag newSubContentTag(String name) { 
		return newSubContentTag(name, null, null); 
	}
	
	public final Tag newSubContentTag(String name, String[] attrNames, String[] attrValues) { 
		return mTraveller.newContentTree(name, attrNames, attrValues, null).getRootTag(); 
	}
	
	public final Tag newSubContentTag(String name, Tag.AttributesMatcher matcher) { 
		return mTraveller.newContentTree(name, null, null, matcher).getRootTag(); 
	}
	
	@Override 
	public void handleStartDocument() throws SAXException { 
		try { 
			mTraveller.init(); 
		} catch (ParseException e) { 
			throw new SAXException(e); 
		}
	}
	
	@Override 
	public void handleEndDocument() { 
		Content content = mDocument; 
		if (content != null) 
			content.handleEnd(); 
	}
	
	private class XmlAttributes implements Tag.IAttributes { 
		private final Attributes mAttr; 
		private XmlAttributes(Attributes attr) { mAttr = attr; }
		
		@Override
		public int getLength() { 
			return mAttr.getLength(); 
		}
		
		@Override
		public String getLocalName(int index) { 
			return mAttr.getLocalName(index); 
		}
		
		@Override
		public String getValue(int index) { 
			return mAttr.getValue(index); 
		}
		
		@Override 
		public String toString() { 
			StringBuilder sbuf = new StringBuilder(); 
			sbuf.append("XmlAttrs{"); 
			for (int i=0; i < getLength(); i++) { 
				String name = getLocalName(i); 
				String value = getValue(i); 
				if (i != 0) sbuf.append(" "); 
				sbuf.append(name); 
				sbuf.append("=\""); 
				sbuf.append(value); 
				sbuf.append("\""); 
			}
			sbuf.append("}"); 
			return sbuf.toString(); 
		}
	}
	
	@Override 
	public void handleStartTag(String localName, String qName, Attributes attributes) { 
		final XmlAttributes attrs = new XmlAttributes(attributes); 
		final int startLength = mBuilder.length(); 
		final TagElement e = new TagElement(this, localName, startLength); 
		
		for (int i=0; attrs != null && i < attrs.getLength(); i++) { 
			String name = attrs.getLocalName(i); 
			String value = attrs.getValue(i); 
			
			if (name != null && value != null) 
				e.addAttribute(name.toLowerCase(), value); 
		}
		
		if (LOG.isDebugEnabled() && mFactory.isDebug())
			LOG.debug("Analyzer.handleStartTag: " + localName + " Tag: " + e);
		
		mElements.push(e); 
		mBuilder.handleStartTag(localName, qName, e); 
		
		Content content = mDocument; 
		if (content == null) { 
			content = mFactory.newDocumentContent(e); 
			mDocument = content; 
		}
		
		if (content != null) 
			content.handleElementStart(e); 
		
		boolean handled = false; 
		if (mTraveller.isBeginFound() && !mTraveller.isEndFound()) 
			handled = onSubContentStartTag(e); 
		
		mTraveller.startTag(localName, attrs); 
		
		if (!handled && mTraveller.isBeginFound() && !mTraveller.isEndFound()) 
			handled = onSubContentStartTag(e); 
	}
	
	@Override 
	public void handleEndTag(String localName, String qName) { 
		final TagElement e = !mElements.empty() ? mElements.pop() : null;
		
		if (LOG.isDebugEnabled() && mFactory.isDebug())
			LOG.debug("Analyzer.handleEndTag: " + localName + " Tag: " + e);
		
		mBuilder.handleEndTag(localName, qName, e); 
		final int endLength = mBuilder.length(); 
		
		if (e != null) { 
			Content content = mDocument; 
			if (content != null) 
				content.handleElementEnd(e, endLength); 
		}
		
		boolean handled = false; 
		if (mTraveller.isBeginFound() && !mTraveller.isEndFound()) 
			handled = onSubContentEndTag(e, endLength); 
		
		mTraveller.endTag(localName); 
		
		if (!handled && mTraveller.isBeginFound() && !mTraveller.isEndFound()) 
			handled = onSubContentEndTag(e, endLength); 
	}
	
	private boolean onSubContentStartTag(TagElement e) { 
		Content content = mSubCurrent; 
		if (content != null) {
			content.handleElementStart(e); 
			return true;
		}
		
		return false;
	}
	
	private boolean onSubContentEndTag(TagElement e, int endLength) { 
		Content content = mSubCurrent; 
		if (content != null) { 
			content.handleElementEnd(e, endLength); 
			
			if (e == null || e == content.getStart()) { 
				// found sub content end
				mSubCurrent = null; 
				content.handleEnd(); 
				
				if (LOG.isDebugEnabled() && mFactory.isDebug())
					LOG.debug("Analyzer: sub content end");
			}
			
			return true;
		}
		
		return false;
	}
	
	protected void onContentTreeFound() { 
		if (mSubCurrent != null) return;
		
		TagElement e = !mElements.isEmpty() ? mElements.lastElement() : null; 
		if (e != null) { 
			mSubCurrent = mFactory.newSubContent(e); 
			
			if (LOG.isDebugEnabled() && mFactory.isDebug())
				LOG.debug("Analyzer: sub content start");
			
		}
	}
	
	@Override 
	public int length() { 
		return mBuilder.length(); 
	}
	
	@Override 
	public char charAt(int position) { 
		return mBuilder.charAt(position); 
	}
	
	@Override 
	public void append(CharSequence text) { 
		mBuilder.append(text); 
	}
	
	public String getString(int startLength, int endLength) { 
		if (startLength >= 0 && endLength > startLength && endLength <= mBuilder.length()) { 
			char[] dest = new char[endLength - startLength]; 
			mBuilder.getChars(startLength, endLength, dest, 0); 
			
			return new String(dest); 
		} else
			return null; 
	}
	
}
