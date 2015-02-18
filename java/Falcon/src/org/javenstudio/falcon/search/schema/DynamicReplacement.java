package org.javenstudio.falcon.search.schema;

import org.javenstudio.falcon.ErrorException;

public abstract class DynamicReplacement implements Comparable<DynamicReplacement> {
	
	public static final int STARTS_WITH = 1;
    public static final int ENDS_WITH = 2;

    private final String mRegex;
    private final int mType;
    private final String mText;

    protected DynamicReplacement(String regex) throws ErrorException {
    	mRegex = regex;
    	
    	if (mRegex.startsWith("*")) {
    		mType = ENDS_WITH;
    		mText = regex.substring(1);
    		
    	} else if (mRegex.endsWith("*")) {
    		mType = STARTS_WITH;
    		mText = regex.substring(0, regex.length()-1);
    		
    	} else {
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
    				"dynamic field name must start or end with *");
    	}
    }

    public final String getRegex() { return mRegex; }
    public final int getType() { return mType; }
    public final String getText() { return mText; }
    
    public boolean matches(String name) {
    	if (mType == STARTS_WITH && name.startsWith(mText)) 
    		return true;
    	else if (mType == ENDS_WITH && name.endsWith(mText)) 
    		return true;
    	else 
    		return false;
    }

    /**
     * Sort order is based on length of regex.  Longest comes first.
     * @param other The object to compare to.
     * @return a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than
     * the specified object.
     */
    @Override
    public int compareTo(DynamicReplacement other) {
    	return other.mRegex.length() - mRegex.length();
    }
    
}
