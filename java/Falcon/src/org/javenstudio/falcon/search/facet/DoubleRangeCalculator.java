package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.search.schema.SchemaField;

final class DoubleRangeCalculator extends RangeEndpointCalculator<Double> {

    public DoubleRangeCalculator(final SchemaField f) { 
    	super(f); 
    }
    
    @Override
    protected Double parseVal(String rawval) {
    	return Double.valueOf(rawval);
    }
    
    @Override
    public Double parseAndAddGap(Double value, String gap) {
    	return new Double(value.doubleValue() + Double.valueOf(gap).doubleValue());
    }
    
}
