package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ReturnFields;
import org.javenstudio.falcon.util.XMLUtils;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class XMLWriter extends BaseTextWriter {

	public static float CURRENT_VERSION = 2.2f;

	private static final char[] XML_START1 = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".toCharArray();
	
	private static final char[] XML_STYLESHEET = 
			"<?xml-stylesheet type=\"text/xsl\" href=\"".toCharArray();
	
	private static final char[] XML_STYLESHEET_END = 
			"\"?>\n".toCharArray();

	/*
  	private static final char[] XML_START2_SCHEMA=(
  	"<response xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
  	+" xsi:noNamespaceSchemaLocation=\"http://pi.cnet.com/cnet-search/response.xsd\">\n"
          ).toCharArray();
	 ***/
  
  	private static final char[] XML_START2_NOSCHEMA=("<response>\n").toCharArray();

  	private final int mVersion;

  	public XMLWriter(Writer writer, Request req, Response rsp) 
  			throws ErrorException {
  		super(writer, req, rsp);

  		String version = req.getParam(CommonParams.VERSION);
  		float ver = version == null? CURRENT_VERSION : Float.parseFloat(version);
  		mVersion = (int)(ver*1000);
  		if (mVersion < 2200) {
  			throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST,
  					"XMLWriter does not support version: "+version );
  		}
  	}

  	public void writeResponse() throws IOException, ErrorException {
	    getWriter().write(XML_START1);
	
	    String stylesheet = getRequest().getParam("stylesheet");
	    if (stylesheet != null && stylesheet.length() > 0) {
	    	getWriter().write(XML_STYLESHEET);
	    	XMLUtils.escapeAttributeValue(stylesheet, getWriter());
	    	getWriter().write(XML_STYLESHEET_END);
	    }
	
	    /*
	    String noSchema = req.getParams().get("noSchema");
	    // todo - change when schema becomes available?
	    if (false && noSchema == null)
	      	writer.write(XML_START2_SCHEMA);
	    else
	      	writer.write(XML_START2_NOSCHEMA);
	     ***/
	    getWriter().write(XML_START2_NOSCHEMA);
	
	    // dump response values
	    NamedList<?> lst = getResponse().getValues();
	    
	    if (getRequest().getParams().getBool(CommonParams.OMIT_HEADER, false)) 
	    	getResponse().omitResponseHeader();
	    
	    int sz = lst.size();
	    int start = 0;
	
	    for (int i=start; i < sz; i++) {
	    	writeVal(lst.getName(i), lst.getVal(i));
	    }
	
	    getWriter().write("\n</response>\n");
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
	public void writeStartResultList(String name, long start, int size,
			long numFound, Float maxScore) throws IOException {
	    if (isIndent()) indent();

	    getWriter().write("<result");
	    writeAttr("name", name);
	    writeAttr("numFound", Long.toString(numFound));
	    writeAttr("start", Long.toString(start));
	    
	    if (maxScore != null) 
	    	writeAttr("maxScore",Float.toString(maxScore));
	    
	    getWriter().write(">");
	    increaseLevel();
	}

	@Override
	public void writeEndResultList() throws IOException {
	    decreaseLevel();
	    if (isIndent()) indent();
	    getWriter().write("</result>");
	}

	/**
	 * The ResultItem should already have multivalued fields implemented as
	 * Collections -- this will not rewrite to &lt;arr&gt;
	 */ 
	@Override
	public void writeResultItem(String name, ResultItem doc, int idx)
			throws IOException {
	    startTag("doc", name, false);
	    increaseLevel();

	    ReturnFields returnFields = getResponse().getReturnFields();
	    
	    for (String fname : doc.getFieldNames()) {
	    	if (!returnFields.wantsField(fname)) 
	    		continue;
	      
	    	Object val = doc.getFieldValue(fname);
	    	writeVal(fname, val);
	    }
	    
	    decreaseLevel();
	    getWriter().write("</doc>");
	}

}
