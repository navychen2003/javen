package org.javenstudio.falcon.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public class SimpleXMLWriter extends TextWriter {

	private static final char[] XML_START1 = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".toCharArray();
	
	private final String mRootTag;
	private boolean mDoIndent = false;
	
	public SimpleXMLWriter(Writer writer, String rootTag, 
			boolean indent) throws ErrorException {
		super(writer);
		if (rootTag == null || rootTag.length() == 0)
			throw new IllegalArgumentException("rootTag is empty");
		
		mRootTag = rootTag;
		mDoIndent = indent;
	}

	public void write(NamedList<?> data) throws IOException { 
	    getWriter().write(XML_START1);
	    getWriter().write("<" + mRootTag + ">");
	    
	    if (data != null) { 
	    	//if (data instanceof NamedList)
	    		writeNamedList((NamedList<?>)data);
	    	//else
	    	//	writeVal(null, data);
	    }
	    
	    getWriter().write("\n</" + mRootTag + ">\n");
	}
	
	private void writeNamedList(NamedList<?> lst) throws IOException { 
		if (lst != null) { 
		    int sz = lst.size();
		    int start = 0;
		
		    for (int i=start; i < sz; i++) {
		    	writeVal(lst.getName(i), lst.getVal(i));
		    }
	    }
	}
	
  	/** 
  	 * Writes the XML attribute name/val. 
  	 * A null val means that the attribute is missing. 
  	 */
  	private void writeAttr(String name, String val) throws IOException {
  		writeAttr(name, val, true);
  	}

  	public void writeAttr(String name, String val, boolean escape) 
  			throws IOException{
	    if (val != null) {
	    	getWriter().write(' ');
	    	getWriter().write(name);
	    	getWriter().write("=\"");
	    	
	    	if (escape)
	    		XMLUtils.escapeAttributeValue(val, getWriter());
	    	else 
	    		getWriter().write(val);
	    	
	    	getWriter().write('"');
	    }
  	}

  	private void startTag(String tag, String name, boolean closeTag) 
  			throws IOException {
	    if (isIndent()) indent();
	
	    getWriter().write('<');
	    getWriter().write(tag);
	    
	    if (name!=null) {
	    	writeAttr("name", name);
	    	if (closeTag) 
	    		getWriter().write("/>");
	    	else 
	    		getWriter().write(">");
	    	
	    } else {
	    	if (closeTag) 
	    		getWriter().write("/>");
	    	else 
	    		getWriter().write('>');
	    }
  	}

  	@Override
  	public void writeNamedList(String name, NamedList<?> val) 
  			throws IOException {
	    int sz = val.size();
	    startTag("lst", name, sz<=0);
	
	    increaseLevel();
	    for (int i=0; i < sz; i++) {
	    	writeVal(val.getName(i), val. getVal(i));
	    }
	    decreaseLevel();
	
	    if (sz > 0) {
	    	if (isIndent()) indent();
	    	getWriter().write("</lst>");
	    }
  	}

  	@Override
  	public void writeMap(String name, Map<?,?> map, boolean excludeOuter, 
  			boolean isFirstVal) throws IOException {
	    int sz = map.size();
	
	    if (!excludeOuter) {
	    	startTag("lst", name, sz<=0);
	    	increaseLevel();
	    }
	
	    for (Map.Entry<?,?> entry : map.entrySet()) {
	    	Object k = entry.getKey();
	    	Object v = entry.getValue();
	    	// if (sz < indentThreshold) indent();
	    	writeVal(null == k ? null : k.toString(), v);
	    }
	
	    if (!excludeOuter) {
	    	decreaseLevel();
	    	if (sz > 0) {
	    		if (isIndent()) indent();
	    		getWriter().write("</lst>");
	    	}
	    }
  	}

  	@Override
  	public void writeArray(String name, Object[] val) throws IOException {
  		writeArray(name, Arrays.asList(val).iterator());
  	}

  	@Override
  	public void writeArray(String name, Iterator<?> iter) throws IOException {
	    if (iter.hasNext() ) {
	    	startTag("arr", name, false);
	    	
	    	increaseLevel();
	    	while (iter.hasNext()) {
	    		writeVal(null, iter.next());
	    	}
	    	decreaseLevel();
	    	
	    	if (isIndent()) indent();
	    	getWriter().write("</arr>");
	    	
	    } else {
	    	startTag("arr", name, true );
	    }
  	}

  	@Override
  	public void writeNull(String name) throws IOException {
  		writePrim("null", name, "", false);
  	}

  	@Override
  	public void writeString(String name, String val, boolean escape) 
  			throws IOException {
  		writePrim("str",name,val,escape);
  	}

  	@Override
  	public void writeInt(String name, String val) throws IOException {
  		writePrim("int", name, val, false);
  	}

  	@Override
  	public void writeLong(String name, String val) throws IOException {
  		writePrim("long", name, val, false);
  	}

  	@Override
  	public void writeBool(String name, String val) throws IOException {
  		writePrim("bool", name, val, false);
  	}

  	@Override
  	public void writeFloat(String name, String val) throws IOException {
  		writePrim("float", name, val, false);
  	}

  	@Override
  	public void writeFloat(String name, float val) throws IOException {
  		writeFloat(name, Float.toString(val));
  	}

  	@Override
  	public void writeDouble(String name, String val) throws IOException {
  		writePrim("double", name, val, false);
  	}

  	@Override
  	public void writeDouble(String name, double val) throws IOException {
  		writeDouble(name, Double.toString(val));
  	}

  	@Override
  	public void writeDate(String name, String val) throws IOException {
  		writePrim("date", name, val, false);
  	}

	//
  	// OPT - specific writeInt, writeFloat, methods might be faster since
  	// there would be less write calls (write("<int name=\"" + name + ... + </int>)
  	//
  	private void writePrim(String tag, String name, String val, 
  			boolean escape) throws IOException {
  		int contentLen = val == null ? 0 : val.length();
	
	    startTag(tag, name, contentLen == 0);
	    if (contentLen == 0) 
	    	return;
	
	    if (escape) 
	    	XMLUtils.escapeCharData(val, getWriter());
	    else 
	    	getWriter().write(val, 0, contentLen);
	
	    getWriter().write('<');
	    getWriter().write('/');
	    getWriter().write(tag);
	    getWriter().write('>');
  	}

	@Override
	public boolean isIndent() {
		return mDoIndent;
	}

	@Override
	public void writeStartResultList(String name, long start, int size,
			long numFound, Float maxScore) throws IOException {
	}

	@Override
	public void writeEndResultList() throws IOException {
	}

	@Override
	public void writeResultItem(String name, ResultItem item, int idx)
			throws IOException {
	}

}
