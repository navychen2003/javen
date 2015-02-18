package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.search.schema.SchemaField;

final class LongRangeCalculator extends RangeEndpointCalculator<Long> {

    public LongRangeCalculator(final SchemaField f) { 
    	super(f); 
    }
    
    @Override
    protected Long parseVal(String rawval) {
    	return Long.valueOf(rawval);
    }
    
    @Override
    public Long parseAndAddGap(Long value, String gap) {
    	return new Long(value.longValue() + Long.valueOf(gap).longValue());
    }
    
}
