package org.javenstudio.falcon.search.facet;

import java.util.Locale;

import org.javenstudio.falcon.ErrorException;

/**
 * @deprecated Use {@link FacetRangeOther}
 */
@Deprecated
public enum FacetDateOther {
    BEFORE, AFTER, BETWEEN, ALL, NONE;
    
    @Override
    public String toString() { 
    	return super.toString().toLowerCase(Locale.ROOT); 
    }
    
    public static FacetDateOther get(String label) throws ErrorException {
    	try {
    		return valueOf(label.toUpperCase(Locale.ROOT));
    	} catch (IllegalArgumentException e) {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
    				label+" is not a valid type of 'other' range facet information",e);
    	}
	}
    
}
