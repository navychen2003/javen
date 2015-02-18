package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Perhaps someday instead of having a giant "instanceof" case 
 * statement to pick an impl, we can add a "RangeFacetable" marker 
 * interface to FieldTypes and they can return instances of these 
 * directly from some method -- but until then, keep this locked down 
 * and private.
 */
public abstract class RangeEndpointCalculator<T extends Comparable<T>> {
	
    protected final SchemaField mField;
    
    public RangeEndpointCalculator(final SchemaField field) {
    	mField = field;
    }

    /**
     * Formats a Range endpoint for use as a range label name in the response.
     * Default Impl just uses toString()
     */
    public String formatValue(final T val) {
    	return val.toString();
    }
    
    /**
     * Parses a String param into an Range endpoint value throwing 
     * a useful exception if not possible
     */
    public final T getValue(final String rawval) throws ErrorException {
    	return parseVal(rawval);
    }
    
    /**
     * Parses a String param into an Range endpoint. 
     * Can throw a low level format exception as needed.
     */
    protected abstract T parseVal(final String rawval) 
    		throws ErrorException;

    /** 
     * Parses a String param into a value that represents the gap and 
     * can be included in the response, throwing 
     * a useful exception if not possible.
     *
     * Note: uses Object as the return type instead of T for things like 
     * Date where gap is just a DateMathParser string 
     */
    public final Object getGap(final String gap) throws ErrorException {
    	return parseGap(gap);
    }

    /**
     * Parses a String param into a value that represents the gap and 
     * can be included in the response. 
     * Can throw a low level format exception as needed.
     *
     * Default Impl calls parseVal
     */
    protected Object parseGap(final String rawval) throws ErrorException {
    	return parseVal(rawval);
    }

    /**
     * Adds the String gap param to a low Range endpoint value to determine 
     * the corrisponding high Range endpoint value, throwing 
     * a useful exception if not possible.
     */
    public final T addGap(T value, String gap) throws ErrorException {
    	return parseAndAddGap(value, gap);
    }
    
    /**
     * Adds the String gap param to a low Range endpoint value to determine 
     * the corrisponding high Range endpoint value.
     * Can throw a low level format exception as needed.
     */
    protected abstract T parseAndAddGap(T value, String gap) 
    		throws ErrorException;

}
