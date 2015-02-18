package org.javenstudio.falcon.search.query.parser;

import org.javenstudio.falcon.ErrorException;

/** 
 * Represents a dynamic field, for easier matching, 
 * inspired by same class in IndexSchema 
 */
public class ExtendedDynamicField implements Comparable<ExtendedDynamicField> {
	
	static final int STARTS_WITH = 1;
	static final int ENDS_WITH = 2;
	static final int CATCHALL = 3;
    
    private final String mWildcard;
    private final int mType;
    private final String mText;
    
	protected ExtendedDynamicField(String wildcard) throws ErrorException {
		mWildcard = wildcard;
		if (wildcard.equals("*")) {
			mType = CATCHALL;
			mText = null;
			
		} else if (wildcard.startsWith("*")) {
			mType = ENDS_WITH;
			mText = wildcard.substring(1);
			
		} else if (wildcard.endsWith("*")) {
			mType = STARTS_WITH;
			mText = wildcard.substring(0,wildcard.length()-1);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"dynamic field name must start or end with *");
		}
    }
    
	public final String getWildcard() { return mWildcard; }
	
    /**
     * Returns true if the regex wildcard for this DynamicField 
     * would match the input field name
     */
    public boolean matches(String name) {
    	if (mType == CATCHALL) return true;
    	else if (mType == STARTS_WITH && name.startsWith(mText)) return true;
    	else if (mType == ENDS_WITH && name.endsWith(mText)) return true;
    	else return false;
    }
    
    /**
     * Sort order is based on length of regex.  Longest comes first.
     * @param other The object to compare to.
     * @return a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than
     * the specified object.
     */
    @Override
    public int compareTo(ExtendedDynamicField other) {
    	return other.mWildcard.length() - this.mWildcard.length();
    }
    
    @Override
    public String toString() {
    	return mWildcard;
    }
    
}
