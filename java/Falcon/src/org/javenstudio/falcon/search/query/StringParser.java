package org.javenstudio.falcon.search.query;

import org.javenstudio.falcon.ErrorException;

/**
 * Simple class to help with parsing a string
 * <b>Note: This API is experimental and may change in 
 * non backward-compatible ways in the future</b>
 */
public class StringParser {
	
    private String mValue;
    private int mPos;
    private int mEnd;

    public StringParser(String val) {
    	this(val, 0, val.length());
    }

    public StringParser(String val, int start, int end) {
    	mValue = val;
    	mPos = start;
    	mEnd = end;
    }

    public String getValue() { return mValue; }
    public int getPos() { return mPos; }
    public int getEnd() { return mEnd; }
    
    public void increasePos(int val) { mPos += val; }
    public void setPos(int val) { mPos = val; }
    
    public void eatws() {
    	while (mPos < mEnd && Character.isWhitespace(mValue.charAt(mPos))) {
    		mPos++;
    	}
    }

    public char ch() {
    	return mPos < mEnd ? mValue.charAt(mPos) : 0;
    }

    public void skip(int nChars) {
    	mPos = Math.max(mPos + nChars, mEnd);
    }

    public boolean opt(String s) {
    	eatws();
    	
    	int slen = s.length();
    	if (mValue.regionMatches(mPos, s, 0, slen)) {
    		mPos += slen;
    		return true;
    	}
    	
    	return false;
    }

    public boolean opt(char ch) {
    	eatws();
    	
    	if (mPos < mEnd && mValue.charAt(mPos) == ch) {
    		mPos ++;
    		return true;
    	}
    	
    	return false;
    }

    public void expect(String s) throws ErrorException {
    	eatws();
    	
    	int slen = s.length();
    	if (mValue.regionMatches(mPos, s, 0, slen)) {
    		mPos += slen;
    		
    	} else {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
    				"Expected '" + s + "' at position " + mPos + " in '" + mValue + "'");
    	}
    }

    public float getFloat() {
    	eatws();
    	
    	char[] arr = new char[mEnd - mPos];
    	int i;
    	
    	for (i = 0; i < arr.length; i++) {
    		char ch = mValue.charAt(mPos);
    		if ((ch >= '0' && ch <= '9')
    			|| ch == '+' || ch == '-'
    			|| ch == '.' || ch == 'e' || ch == 'E') {
    			mPos ++;
    			arr[i] = ch;
    			
    		} else 
    			break;
    	}

    	return Float.parseFloat(new String(arr, 0, i));
    }

    public Number getNumber() {
    	eatws();
    	
    	int start = mPos;
    	boolean flt = false;

    	while (mPos < mEnd) {
    		char ch = mValue.charAt(mPos);
    		if ((ch >= '0' && ch <= '9') || ch == '+' || ch == '-') {
    			mPos ++;
    			
    		} else if (ch == '.' || ch =='e' || ch == 'E') {
    			flt = true;
    			mPos ++;
    			
    		} else 
    			break;
    	}

    	String v = mValue.substring(start, mPos);
    	if (flt) 
    		return Double.parseDouble(v);
    	else 
    		return Long.parseLong(v);
    }

    public double getDouble() {
    	eatws();
    	
    	char[] arr = new char[mEnd - mPos];
    	int i;
    	
    	for (i = 0; i < arr.length; i++) {
    		char ch = mValue.charAt(mPos);
    		if ((ch >= '0' && ch <= '9')
    			|| ch == '+' || ch == '-'
    			|| ch == '.' || ch == 'e' || ch == 'E') {
    			mPos ++;
    			arr[i] = ch;
    			
    		} else 
    			break;
    	}

    	return Double.parseDouble(new String(arr, 0, i));
    }

    public int getInt() {
    	eatws();
    	
    	char[] arr = new char[mEnd - mPos];
    	int i;
    	
    	for (i = 0; i < arr.length; i++) {
    		char ch = mValue.charAt(mPos);
    		if ((ch >= '0' && ch <= '9')
    			|| ch == '+' || ch == '-') {
    			mPos ++;
    			arr[i] = ch;
    			
    		} else 
    			break;
    	}

    	return Integer.parseInt(new String(arr, 0, i));
    }

    public String getId() throws ErrorException {
    	return getId("Expected identifier");
    }

    public String getId(String errMessage) throws ErrorException {
    	eatws();
    	
    	int id_start = mPos;
    	char ch;
    	
    	if (mPos < mEnd && (ch = mValue.charAt(mPos)) != '$' && 
    		Character.isJavaIdentifierStart(ch)) {
    		mPos ++;
    		
    		while (mPos < mEnd) {
    			ch = mValue.charAt(mPos);
//          if (!Character.isJavaIdentifierPart(ch) && ch != '.' && ch != ':') {
    			if (!Character.isJavaIdentifierPart(ch) && ch != '.') 
    				break;
    			
    			mPos ++;
    		}
    		
    		return mValue.substring(id_start, mPos);
    	}

    	if (errMessage != null) {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
    				errMessage + " at pos " + mPos + " str='" + mValue + "'");
    	}
      
    	return null;
    }

    public String getGlobbedId(String errMessage) throws ErrorException {
    	eatws();
    	
    	int id_start = mPos;
    	char ch;
    	
    	if (mPos < mEnd && (ch = mValue.charAt(mPos)) != '$' && 
    		(Character.isJavaIdentifierStart(ch) || ch=='?' || ch=='*')) {
    		mPos ++;
    		
    		while (mPos < mEnd) {
    			ch = mValue.charAt(mPos);
    			if (!(Character.isJavaIdentifierPart(ch) || ch=='?' || ch=='*') && ch != '.') 
    				break;
    			
    			mPos ++;
    		}
    		
    		return mValue.substring(id_start, mPos);
    	}

    	if (errMessage != null) {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
    				errMessage + " at pos " + mPos + " str='" + mValue + "'");
    	}
      
    	return null;
    }

    /**
     * Skips leading whitespace and returns whatever sequence of non 
     * whitespace it can find (or hte empty string)
     */
    public String getSimpleString() {
    	eatws();
    	
    	int startPos = mPos;
    	char ch;
    	
    	while (mPos < mEnd) {
    		ch = mValue.charAt(mPos);
    		if (Character.isWhitespace(ch)) 
    			break;
    		
    		mPos ++;
    	}
    	
    	return mValue.substring(startPos, mPos);
    }

    /**
     * Sort direction or null if current position does not inidcate a 
     * sort direction. (True is desc, False is asc).  
     * Position is advanced to after the comma (or end) when result is non null 
     */
    public Boolean getSortDirection() throws ErrorException {
    	final int startPos = mPos;
    	final String order = getId(null);

    	Boolean top = null;

    	if (null != order) {
    		if ("desc".equals(order) || "top".equals(order)) {
    			top = true;
    		} else if ("asc".equals(order) || "bottom".equals(order)) {
    			top = false;
    		}

    		// it's not a legal direction if more stuff comes after it
    		eatws();
    		
    		final char c = ch();
    		if (0 == c) {
    			// :NOOP
    		} else if (',' == c) {
    			mPos ++;
    		} else {
    			top = null;
    		}
    	}

    	if (null == top) 
    		mPos = startPos; // no direction, reset
    	
    	return top;
    }

    // return null if not a string
    public String getQuotedString() throws ErrorException {
    	eatws();
    	
    	char delim = peekChar();
    	if (!(delim == '\"' || delim == '\'')) 
    		return null;
    	
    	int val_start = ++mPos;
    	
    	StringBuilder sb = new StringBuilder(); // needed for escaping
    	for (; ;) {
    		if (mPos >= mEnd) {
    			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
    					"Missing end quote for string at pos " + (val_start - 1) + " str='" + mValue + "'");
    		}
    		
    		char ch = mValue.charAt(mPos);
    		if (ch == '\\') {
    			mPos ++;
    			if (mPos >= mEnd) 
    				break;
    			
    			ch = mValue.charAt(mPos);
    			switch (ch) {
    			case 'n':
    				ch = '\n';
    				break;
    			case 't':
    				ch = '\t';
    				break;
    			case 'r':
    				ch = '\r';
    				break;
    			case 'b':
    				ch = '\b';
    				break;
    			case 'f':
    				ch = '\f';
    				break;
    			case 'u':
    				if (mPos + 4 >= mEnd) {
    					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
    							"bad unicode escape \\uxxxx at pos" + (val_start - 1) + " str='" + mValue + "'");
    				}
    				
    				ch = (char) Integer.parseInt(mValue.substring(mPos + 1, mPos + 5), 16);
    				mPos += 4;
    				break;
    			}
    			
    		} else if (ch == delim) {
    			mPos ++;  // skip over the quote
    			break;
    		}
    		
    		sb.append(ch);
    		mPos ++;
    	}

    	return sb.toString();
    }

    // next non-whitespace char
    public char peek() {
    	eatws();
    	return mPos < mEnd ? mValue.charAt(mPos) : 0;
    }

    // next char
    public char peekChar() {
    	return mPos < mEnd ? mValue.charAt(mPos) : 0;
    }

    @Override
    public String toString() {
    	return "'" + mValue + "'" + ", pos=" + mPos;
    }

}
