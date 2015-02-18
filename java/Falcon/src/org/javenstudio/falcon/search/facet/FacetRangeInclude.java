package org.javenstudio.falcon.search.facet;

import java.util.EnumSet;
import java.util.Locale;

import org.javenstudio.falcon.ErrorException;

/**
 * An enumeration of the legal values for {@link #FACET_DATE_INCLUDE} 
 * and {@link #FACET_RANGE_INCLUDE}
 *
 * <ul>
 * <li>lower = all gap based ranges include their lower bound</li>
 * <li>upper = all gap based ranges include their upper bound</li>
 * <li>edge = the first and last gap ranges include their edge bounds (ie: lower 
 *     for the first one, upper for the last one) even if the corresponding 
 *     upper/lower option is not specified
 * </li>
 * <li>outer = the BEFORE and AFTER ranges 
 *     should be inclusive of their bounds, even if the first or last ranges 
 *     already include those boundaries.
 * </li>
 * <li>all = shorthand for lower, upper, edge, and outer</li>
 * </ul>
 * @see #FACET_DATE_INCLUDE
 * @see #FACET_RANGE_INCLUDE
 */
public enum FacetRangeInclude {
    ALL, LOWER, UPPER, EDGE, OUTER;
    
    @Override
    public String toString() { 
    	return super.toString().toLowerCase(Locale.ROOT); 
    }
    
    public static FacetRangeInclude get(String label) throws ErrorException {
    	try {
    		return valueOf(label.toUpperCase(Locale.ROOT));
    	} catch (IllegalArgumentException e) {
    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
    				label+" is not a valid type of for range 'include' information",e);
    	}
	}
    
    /**
     * Convinience method for parsing the param value according to the 
     * correct semantics and applying the default of "LOWER"
     */
    public static EnumSet<FacetRangeInclude> parseParam(final String[] param) 
    		throws ErrorException {
    	// short circut for default behavior
    	if (null == param || 0 == param.length ) 
    		return EnumSet.of(LOWER);

    	// build up set containing whatever is specified
    	final EnumSet<FacetRangeInclude> include = 
    			EnumSet.noneOf(FacetRangeInclude.class);
    	
    	for (final String o : param) {
    		include.add(FacetRangeInclude.get(o));
    	}

    	// if set contains all, then we're back to short circuting
    	if (include.contains(FacetRangeInclude.ALL)) 
    		return EnumSet.allOf(FacetRangeInclude.class);

    	// use whatever we've got.
    	return include;
    }
    
}
