package org.javenstudio.falcon.search.facet;

import java.util.Locale;

import org.javenstudio.falcon.ErrorException;

/**
 * An enumeration of the legal values for {@link #FACET_RANGE_OTHER} 
 * and {@link #FACET_DATE_OTHER} ...
 * <ul>
 * <li>before = the count of matches before the start</li>
 * <li>after = the count of matches after the end</li>
 * <li>between = the count of all matches between start and end</li>
 * <li>all = all of the above (default value)</li>
 * <li>none = no additional info requested</li>
 * </ul>
 * @see #FACET_RANGE_OTHER
 * @see #FACET_DATE_OTHER
 */
public enum FacetRangeOther {
    BEFORE, AFTER, BETWEEN, ALL, NONE;
    
    @Override
    public String toString() { 
    	return super.toString().toLowerCase(Locale.ROOT); 
    }
    
    public static FacetRangeOther get(String label) throws ErrorException {
    	try {
    		return valueOf(label.toUpperCase(Locale.ROOT));
    	} catch (IllegalArgumentException e) {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
    				label+" is not a valid type of 'other' range facet information",e);
    	}
	}
    
}
