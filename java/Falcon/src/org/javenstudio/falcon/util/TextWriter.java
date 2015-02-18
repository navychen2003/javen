package org.javenstudio.falcon.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

/** 
 * Base class for text-oriented response writers.
 *
 */
public abstract class TextWriter {
	static final Logger LOG = Logger.getLogger(TextWriter.class);

	// indent up to 40 spaces
	static final char[] sIndentChars = new char[81];
	static {
		Arrays.fill(sIndentChars,' ');
		sIndentChars[0] = '\n';  // start with a newline
	}
	
	private final FastWriter mWriter;
	
	//private Calendar mCalendar;  // reusable calendar instance
	private int mLevel = 0;

	public TextWriter(Writer writer) throws ErrorException { 
		mWriter = FastWriter.wrap(writer);
	}
	
	/** done with this ResponseWriter... make sure any buffers are flushed to writer */
	public void close() throws IOException {
		mWriter.flushBuffer();
	}

	/** returns the Writer that the response is being written to */
	public final Writer getWriter() { return mWriter; }

	public abstract boolean isIndent();
	
	public void indent() throws IOException {
		if (isIndent()) indent(mLevel);
	}

	public void indent(int level) throws IOException {
		mWriter.write(sIndentChars, 0, Math.min((level<<1)+1, sIndentChars.length));
	}

	//
	// Functions to manipulate the current logical nesting level.
	// Any indentation will be partially based on level.
	//
	public void setLevel(int level) { mLevel = level; }
	public int getLevel() { return mLevel; }
	
	public int increaseLevel() { return ++mLevel; }
	public int decreaseLevel() { return --mLevel; }
	
	public void writeVal(String name, Object val) throws IOException {
		// if there get to be enough types, perhaps hashing on the type
		// to get a handler might be faster (but types must be exact to do that...)

		// go in order of most common to least common
		if (val == null) {
			writeNull(name);
			return;
		} else if (writeValKnown(name, val)) { 
			return;
		}
		
		if (val instanceof ResultItem) { 
			writeResultItem(name, (ResultItem)val, 0);
		} else if (val instanceof ResultList) {
			writeResultList(name, (ResultList)val);
		} else if (val instanceof String) {
			writeString(name, val.toString(), true);
			// micro-optimization... using toString() avoids a cast first
		} else if (val instanceof Number) {
			if (val instanceof Integer) {
				writeInt(name, val.toString());
			} else if (val instanceof Long) {
				writeLong(name, val.toString());
			} else if (val instanceof Float) {
				// we pass the float instead of using toString() because
				// it may need special formatting. same for double.
				writeFloat(name, ((Float)val).floatValue());
			} else if (val instanceof Double) {
				writeDouble(name, ((Double)val).doubleValue());        
			} else if (val instanceof Short) {
				writeInt(name, val.toString());
			} else if (val instanceof Byte) {
				writeInt(name, val.toString());
			} else {
				writeValUnknown(name, val);
			}
		} else if (val instanceof Boolean) {
			writeBool(name, val.toString());
		} else if (val instanceof Date) {
			writeDate(name,(Date)val);
		} else if (val instanceof Map) {
			writeMap(name, (Map<?,?>)val, false, true);
		} else if (val instanceof NamedList) {
			writeNamedList(name, (NamedList<?>)val);
		} else if (val instanceof Iterable) {
			writeArray(name,((Iterable<?>)val).iterator());
		} else if (val instanceof Object[]) {
			writeArray(name,(Object[])val);
		} else if (val instanceof Iterator) {
			writeArray(name,(Iterator<?>)val);
		} else if (val instanceof byte[]) {
			byte[] arr = (byte[])val;
			writeByteArr(name, arr, 0, arr.length);
		} else if (val instanceof BytesRef) {
			BytesRef arr = (BytesRef)val;
			writeByteArr(name, arr.getBytes(), arr.getOffset(), arr.getLength());
		} else {
			writeValUnknown(name, val);
		}
	}
	
	protected boolean writeValKnown(String name, Object val) 
			throws IOException { 
		return false;
	}
	
	protected void writeValUnknown(String name, Object val) 
			throws IOException { 
		// default... for debugging only
		if (val == null) {
			writeNull(name);
			
		} else {
			if (LOG.isDebugEnabled())
				LOG.debug("writeValUnknown: class=" + val.getClass().getName());
			
			writeString(name, val.getClass().getName() 
					+ ": " + val.toString(), true);
		}
	}
	
	public final void writeResultList(String name, ResultList docs) throws IOException { 
	    writeStartResultList(name, docs.getStart(), docs.size(), 
	    		docs.getNumFound(), docs.getMaxScore());
	    
	    for (int i=0; i < docs.size(); i++) {
	    	writeResultItem(null, docs.get(i), i);
	    }
	    
	    writeEndResultList();
	}
	
	public abstract void writeStartResultList(String name, long start, int size, 
			long numFound, Float maxScore) throws IOException; 
	
	public abstract void writeEndResultList() throws IOException;
	
	public abstract void writeResultItem(String name, ResultItem item, int idx) 
			throws IOException;
	
  	public abstract void writeNamedList(String name, NamedList<?> val) 
  			throws IOException;
  
	public abstract void writeString(String name, String val, 
			boolean needsEscaping) throws IOException;

	public abstract void writeMap(String name, Map<?,?> val, 
			boolean excludeOuter, boolean isFirstVal) throws IOException;

	public void writeArray(String name, Object[] val) throws IOException {
		writeArray(name, Arrays.asList(val).iterator());
	}
  
	public abstract void writeArray(String name, Iterator<?> val) 
			throws IOException;

	public abstract void writeNull(String name) throws IOException;

	/** if this form of the method is called, val is the Java string form of an int */
	public abstract void writeInt(String name, String val) throws IOException;

	public void writeInt(String name, int val) throws IOException {
		writeInt(name,Integer.toString(val));
	}

	/** if this form of the method is called, val is the Java string form of a long */
	public abstract void writeLong(String name, String val) 
			throws IOException;

	public  void writeLong(String name, long val) throws IOException {
		writeLong(name,Long.toString(val));
	}

	/** if this form of the method is called, val is the Java string form of a boolean */
	public abstract void writeBool(String name, String val) 
			throws IOException;

	public void writeBool(String name, boolean val) throws IOException {
		writeBool(name,Boolean.toString(val));
	}

	/** if this form of the method is called, val is the Java string form of a float */
	public abstract void writeFloat(String name, String val) 
			throws IOException;

	public void writeFloat(String name, float val) throws IOException {
		String s = Float.toString(val);
		// If it's not a normal number, write the value as a string instead.
		// The following test also handles NaN since comparisons are always false.
		if (val > Float.NEGATIVE_INFINITY && val < Float.POSITIVE_INFINITY) {
			writeFloat(name, s);
		} else {
			writeString(name, s, false);
		}
	}

	/** if this form of the method is called, val is the Java string form of a double */
	public abstract void writeDouble(String name, String val) 
			throws IOException;

	public void writeDouble(String name, double val) throws IOException {
		String s = Double.toString(val);
		// If it's not a normal number, write the value as a string instead.
		// The following test also handles NaN since comparisons are always false.
		if (val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
			writeDouble(name, s);
		} else {
			writeString(name, s, false);
		}
	}

	public void writeDate(String name, Date val) throws IOException {
		writeDate(name, DateUtils.formatExternal(val));
	}

	/** if this form of the method is called, val is the ISO8601 based date format */
	public abstract void writeDate(String name, String val) 
			throws IOException;

	public void writeByteArr(String name, byte[] buf, int offset, int len) throws IOException {
		writeString(name, Base64Utils.byteArrayToBase64(buf, offset, len), false);
	}
	
}
