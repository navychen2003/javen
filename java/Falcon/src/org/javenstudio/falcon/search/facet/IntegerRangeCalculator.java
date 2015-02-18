package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.search.schema.SchemaField;

final class IntegerRangeCalculator extends RangeEndpointCalculator<Integer> {

    public IntegerRangeCalculator(final SchemaField f) { 
    	super(f); 
    }
    
    @Override
    protected Integer parseVal(String rawval) {
    	return Integer.valueOf(rawval);
    }
    
    @Override
    public Integer parseAndAddGap(Integer value, String gap) {
    	return new Integer(value.intValue() + Integer.valueOf(gap).intValue());
    }
    
}
