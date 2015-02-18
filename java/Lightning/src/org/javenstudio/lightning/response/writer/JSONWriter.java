package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ReturnFields;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class JSONWriter extends BaseTextWriter {

	private static final String JSON_NL_STYLE = "json.nl";
	private static final String JSON_NL_MAP = "map";
	private static final String JSON_NL_FLAT = "flat";
	private static final String JSON_NL_ARROFARR = "arrarr";
	private static final String JSON_NL_ARROFMAP = "arrmap";
	private static final String JSON_WRAPPER_FUNCTION = "json.wrf";

	private String mNamedListStyle;
	private String mWrapperFunction;
	
	public JSONWriter(Writer writer, Request req, Response rsp)
			throws ErrorException {
		super(writer, req, rsp);
		mNamedListStyle = req.getParam(JSON_NL_STYLE, JSON_NL_FLAT).intern();
		mWrapperFunction = req.getParam(JSON_WRAPPER_FUNCTION);
	}

	public final String getNamedListStyle() { return mNamedListStyle; }
	public final String getWrapperFunction() { return mWrapperFunction; }
	
  	public void writeResponse() throws IOException, ErrorException {
  		if (mWrapperFunction != null) 
  			getWriter().write(mWrapperFunction + "(");
  		
  		if (getRequest().getParams().getBool(CommonParams.OMIT_HEADER, false)) 
  			getResponse().omitResponseHeader();
  		
  		writeNamedList(null, getResponse().getValues());
  		if (mWrapperFunction != null) 
  			getWriter().write(')');
  		
  		// ending with a newline looks much better from the command line
  		getWriter().write('\n');
  	}

  	protected void writeKey(String fname, boolean needsEscaping) throws IOException {
  		writeString(null, fname, needsEscaping);
  		getWriter().write(':');
  	}

  	/** 
  	 * Represents a NamedList directly as a JSON Object (essentially a Map)
  	 * Map null to "" and name mangle any repeated keys to avoid repeats in the
  	 * output.
  	 */
  	protected void writeNamedListAsMapMangled(String name, NamedList<?> val) 
  			throws IOException {
  		int sz = val.size();
  		
  		writeMapOpener(sz);
  		increaseLevel();

  		// In JSON objects (maps) we can't have null keys or duplicates...
  		// map null to "" and append a qualifier to duplicates.
  		//
  		// a=123,a=456 will be mapped to {a=1,a__1=456}
  		// Disad: this is ambiguous since a real key could be called a__1
  		//
  		// Another possible mapping could aggregate multiple keys to an array:
  		// a=123,a=456 maps to a=[123,456]
  		// Disad: this is ambiguous with a real single value that happens to be an array
  		//
  		// Both of these mappings have ambiguities.
  		HashMap<String,Integer> repeats = new HashMap<String,Integer>(4);

  		boolean first = true;
  		
  		for (int i=0; i < sz; i++) {
  			String key = val.getName(i);
  			if (key == null) 
  				key="";

  			if (first) {
  				first = false;
  				repeats.put(key, 0);
  				
  			} else {
  				writeMapSeparator();

  				Integer repeatCount = repeats.get(key);
  				if (repeatCount == null) {
  					repeats.put(key, 0);
  					
  				} else {
  					String newKey = key;
  					int newCount = repeatCount;
  					
  					do {  // avoid generated key clashing with a real key
  						newKey = key + ' ' + (++newCount);
  						repeatCount = repeats.get(newKey);
  					} while (repeatCount != null);

  					repeats.put(key,newCount);
  					key = newKey;
  				}
  			}

  			indent();
  			writeKey(key, true);
  			writeVal(key,val.getVal(i));
  		}

  		decreaseLevel();
  		writeMapCloser();
  	}

  	/** 
  	 * Represents a NamedList directly as a JSON Object (essentially a Map)
  	 * repeating any keys if they are repeated in the NamedList.  null is mapped
  	 * to "".
  	 */ 
  	protected void writeNamedListAsMapWithDups(String name, NamedList<?> val) 
  			throws IOException {
  		int sz = val.size();
  		
  		writeMapOpener(sz);
  		increaseLevel();

  		for (int i=0; i < sz; i++) {
  			if (i != 0) 
  				writeMapSeparator();

  			String key = val.getName(i);
  			if (key == null) 
  				key="";
  			
  			indent();
  			writeKey(key, true);
  			writeVal(key, val.getVal(i));
  		}

  		decreaseLevel();
  		writeMapCloser();
  	}

  	// Represents a NamedList directly as an array of JSON objects...
  	// NamedList("a"=1,"b"=2,null=3) => [{"a":1},{"b":2},3]
  	protected void writeNamedListAsArrMap(String name, NamedList<?> val) 
  			throws IOException {
  		int sz = val.size();
  		
  		indent();
  		writeArrayOpener(sz);
  		increaseLevel();

  		boolean first = true;
  		for (int i=0; i < sz; i++) {
  			String key = val.getName(i);

  			if (first) 
  				first = false;
  			else 
  				writeArraySeparator();

  			indent();

  			if (key == null) {
  				writeVal(null,val.getVal(i));
  				
  			} else {
  				writeMapOpener(1);
  				writeKey(key, true);
  				writeVal(key, val.getVal(i));
  				writeMapCloser();
  			}

  		}

  		decreaseLevel();
  		writeArrayCloser();
  	}

  	// Represents a NamedList directly as an array of JSON objects...
  	// NamedList("a"=1,"b"=2,null=3) => [["a",1],["b",2],[null,3]]
  	protected void writeNamedListAsArrArr(String name, NamedList<?> val) 
  			throws IOException {
  		int sz = val.size();
  		
  		indent();
  		writeArrayOpener(sz);
  		increaseLevel();

  		boolean first = true;
  		for (int i=0; i < sz; i++) {
  			String key = val.getName(i);

  			if (first) 
  				first = false;
  			else 
  				writeArraySeparator();

  			indent();

  			/*** if key is null, just write value???
      		if (key==null) {
        	writeVal(null,val.getVal(i));
      		} else {
  			 ***/

  			writeArrayOpener(1);
  			increaseLevel();
  			
  			if (key == null) 
  				writeNull(null);
  			else 
  				writeString(null, key, true);
  			
  			writeArraySeparator();
  			writeVal(key,val.getVal(i));
  			decreaseLevel();
  			writeArrayCloser();
  		}

  		decreaseLevel();
  		writeArrayCloser();
  	}

  	// Represents a NamedList directly as an array with keys/values
  	// interleaved.
  	// NamedList("a"=1,"b"=2,null=3) => ["a",1,"b",2,null,3]
  	protected void writeNamedListAsFlat(String name, NamedList<?> val) 
  			throws IOException {
  		int sz = val.size();
  		
  		writeArrayOpener(sz);
  		increaseLevel();

  		for (int i=0; i < sz; i++) {
  			if (i != 0) 
  				writeArraySeparator();
  			
  			String key = val.getName(i);
  			indent();
  			
  			if (key == null) 
  				writeNull(null);
  			else 
  				writeString(null, key, true);
  			
  			writeArraySeparator();
  			writeVal(key, val.getVal(i));
  		}

  		decreaseLevel();
  		writeArrayCloser();
  	}

  	@Override
  	public void writeNamedList(String name, NamedList<?> val) throws IOException {
  		if (val instanceof NamedMap) {
  			writeNamedListAsMapWithDups(name, val);
  		} else if (mNamedListStyle == JSON_NL_FLAT) {
  			writeNamedListAsFlat(name,val);
  		} else if (mNamedListStyle == JSON_NL_MAP){
  			writeNamedListAsMapWithDups(name,val);
  		} else if (mNamedListStyle == JSON_NL_ARROFARR) {
  			writeNamedListAsArrArr(name,val);
  		} else if (mNamedListStyle == JSON_NL_ARROFMAP) {
  			writeNamedListAsArrMap(name,val);
  		}
  	}

  	//
  	// Data structure tokens
  	// NOTE: a positive size paramater indicates the number of elements
  	//       contained in an array or map, a negative value indicates 
  	//       that the size could not be reliably determined.
  	// 
  
  	public void writeMapOpener(int size) throws IOException {
  		getWriter().write('{');
  	}
  
  	public void writeMapSeparator() throws IOException {
  		getWriter().write(',');
  	}

  	public void writeMapCloser() throws IOException {
  		getWriter().write('}');
  	}
  
  	public void writeArrayOpener(int size) throws IOException, IOException {
  		getWriter().write('[');
  	}
  
  	public void writeArraySeparator() throws IOException {
  		getWriter().write(',');
  	}

  	public void writeArrayCloser() throws IOException {
  		getWriter().write(']');
  	}

  	@Override
  	public void writeString(String name, String val, boolean needsEscaping) 
  			throws IOException {
  		// it might be more efficient to use a stringbuilder or write substrings
  		// if writing chars to the stream is slow.
  		if (needsEscaping) {

  			/* http://www.ietf.org/internet-drafts/draft-crockford-jsonorg-json-04.txt
      		All Unicode characters may be placed within
      		the quotation marks except for the characters which must be
      		escaped: quotation mark, reverse solidus, and the control
      		characters (U+0000 through U+001F).
  			 */
  			getWriter().write('"');

  			for (int i=0; i<val.length(); i++) {
  				char ch = val.charAt(i);
  				if ((ch > '#' && ch != '\\' && ch < '\u2028') || ch == ' ') { // fast path
  					getWriter().write(ch);
  					continue;
  				}
  				
  				switch (ch) {
  				case '"':
  				case '\\':
  					getWriter().write('\\');
  					getWriter().write(ch);
  					break;
  				case '\r': getWriter().write('\\'); getWriter().write('r'); break;
  				case '\n': getWriter().write('\\'); getWriter().write('n'); break;
  				case '\t': getWriter().write('\\'); getWriter().write('t'); break;
  				case '\b': getWriter().write('\\'); getWriter().write('b'); break;
  				case '\f': getWriter().write('\\'); getWriter().write('f'); break;
  				case '\u2028': // fallthrough
  				case '\u2029':
  					unicodeEscape(getWriter(), ch);
  					break;
  					// case '/':
  				default: 
  					if (ch <= 0x1F) 
  						unicodeEscape(getWriter(), ch);
  					else 
  						getWriter().write(ch);
  					break;
  				}
  			}

  			getWriter().write('"');
  		} else {
  			getWriter().write('"');
  			getWriter().write(val);
  			getWriter().write('"');
  		}
  	}

  	@Override
  	public void writeMap(String name, Map<?,?> val, boolean excludeOuter, 
  			boolean isFirstVal) throws IOException {
  		if (!excludeOuter) {
  			writeMapOpener(val.size());
  			increaseLevel();
  			isFirstVal = true;
  		}

  		boolean doIndent = excludeOuter || val.size() > 1;

  		for (Map.Entry<?,?> entry : val.entrySet()) {
  			Object e = entry.getKey();
  			String k = e==null ? "" : e.toString();
  			Object v = entry.getValue();

  			if (isFirstVal) 
  				isFirstVal = false;
  			else 
  				writeMapSeparator();

  			if (doIndent) indent();
  			
  			writeKey(k, true);
  			writeVal(k, v);
  		}

  		if (!excludeOuter) {
  			decreaseLevel();
  			writeMapCloser();
  		}
  	}

  	@Override
  	public void writeArray(String name, Iterator<?> val) throws IOException {
  		writeArrayOpener(-1); // no trivial way to determine array size
  		increaseLevel();
  		
  		boolean first = true;
  		while( val.hasNext() ) {
  			if (!first) indent();
  			
  			writeVal(null, val.next());
  			if (val.hasNext()) 
  				writeArraySeparator();
  			
  			first = false;
  		}
  		
  		decreaseLevel();
  		writeArrayCloser();
  	}

  	//
  	// Primitive types
  	//
  	@Override
  	public void writeNull(String name) throws IOException {
  		getWriter().write("null");
  	}

  	@Override
  	public void writeInt(String name, String val) throws IOException {
  		getWriter().write(val);
  	}

  	@Override
  	public void writeLong(String name, String val) throws IOException {
  		getWriter().write(val);
  	}

  	@Override
  	public void writeBool(String name, String val) throws IOException {
  		getWriter().write(val);
  	}

  	@Override
  	public void writeFloat(String name, String val) throws IOException {
  		getWriter().write(val);
  	}

  	@Override
  	public void writeDouble(String name, String val) throws IOException {
  		getWriter().write(val);
  	}

  	@Override
  	public void writeDate(String name, String val) throws IOException {
  		writeString(name, val, false);
  	}

  	private static char[] sHexdigits = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  	protected static void unicodeEscape(Appendable out, int ch) throws IOException {
  		out.append('\\');
  		out.append('u');
  		out.append(sHexdigits[(ch>>>12)     ]);
  		out.append(sHexdigits[(ch>>>8) & 0xf]);
  		out.append(sHexdigits[(ch>>>4) & 0xf]);
  		out.append(sHexdigits[(ch)     & 0xf]);
  	}

	@Override
	public void writeStartResultList(String name, long start, int size,
			long numFound, Float maxScore) throws IOException {
	    writeMapOpener((maxScore == null) ? 3 : 4);
	    increaseLevel();
	    
	    writeKey("numFound", false);
	    writeLong(null, numFound);
	    writeMapSeparator();
	    writeKey("start", false);
	    writeLong(null, start);

	    if (maxScore != null) {
	    	writeMapSeparator();
	    	writeKey("maxScore", false);
	    	writeFloat(null, maxScore);
	    }
	    
	    writeMapSeparator();
	    // indent();
	    writeKey("docs", false);
	    writeArrayOpener(size);

	    increaseLevel();
	}

	@Override
	public void writeEndResultList() throws IOException {
	    decreaseLevel();
	    writeArrayCloser();

	    decreaseLevel();
	    indent();
	    writeMapCloser();
	}

	@Override
	public void writeResultItem(String name, ResultItem doc, int idx)
			throws IOException {
		if (idx > 0) 
			writeArraySeparator();

		indent();
		writeMapOpener(doc.size()); 
		increaseLevel();

		boolean first = true;
		ReturnFields returnFields = getResponse().getReturnFields();
		
		for (String fname : doc.getFieldNames()) {
			if (!returnFields.wantsField(fname)) 
				continue;
        
			if (first) 
				first = false;
			else 
				writeMapSeparator();
        
			indent();
			writeKey(fname, true);
			Object val = doc.getFieldValue(fname);

			if (val instanceof Collection) {
				writeVal(fname, val);
				
			} else {
				// if multivalued field, write single value as an array
				if (isMultiValuedField(fname)) {
					writeArrayOpener(-1); // no trivial way to determine array size
					writeVal(fname, val);
					writeArrayCloser();
				} else {
					writeVal(fname, val);
				}
			}
		}
      
		decreaseLevel();
		writeMapCloser();
	}
	
	protected boolean isMultiValuedField(String fname) throws IOException { 
		return false;
	}
	
}
