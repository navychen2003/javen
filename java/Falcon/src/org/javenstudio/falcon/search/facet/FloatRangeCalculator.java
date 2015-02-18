package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.search.schema.SchemaField;

final class FloatRangeCalculator extends RangeEndpointCalculator<Float> {

    public FloatRangeCalculator(final SchemaField f) { 
    	super(f); 
    }
    
    @Override
    protected Float parseVal(String rawval) {
    	return Float.valueOf(rawval);
    }
    
    @Override
    public Float parseAndAddGap(Float value, String gap) {
    	return new Float(value.floatValue() + Float.valueOf(gap).floatValue());
    }
    
}
