package org.javenstudio.falcon.search.facet;

import java.util.Date;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DateParser;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.type.DateFieldType;

final class DateRangeCalculator extends RangeEndpointCalculator<Date> {
	
    private final Date mNow;
    
    public DateRangeCalculator(final SchemaField f, final Date now) { 
    	super(f); 
    	
    	mNow = now;
    	
    	if (!(mField.getType() instanceof DateFieldType)) {
    		throw new IllegalArgumentException(
    				"SchemaField must use filed type extending DateField");
    	}
    }
    
    @Override
    public String formatValue(Date val) {
    	return ((DateFieldType)mField.getType()).toExternal(val);
    }
    
    @Override
    protected Date parseVal(String rawval) throws ErrorException {
    	return ((DateFieldType)mField.getType()).parseMath(mNow, rawval);
    }
    
    @Override
    protected Object parseGap(final String rawval) {
    	return rawval;
    }
    
    @Override
    public Date parseAndAddGap(Date value, String gap) throws ErrorException {
    	try {
    		final DateParser dmp = new DateParser();
    		dmp.setNow(value);
    		
    		return dmp.parseMath(gap);
    	} catch (Exception ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
	}
    
}
