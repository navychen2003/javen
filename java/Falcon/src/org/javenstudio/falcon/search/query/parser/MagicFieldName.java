package org.javenstudio.falcon.search.query.parser;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/** 
 * Identifies the list of all known "magic fields" that trigger 
 * special parsing behavior
 */
public enum MagicFieldName {
	
    VAL("_val_", "func"), QUERY("_query_", null);
    
    private final String mField;
    private final String mSubParser;
    
    MagicFieldName(final String field, final String subParser) {
    	mField = field;
    	mSubParser = subParser;
    }
    
    public final String getFieldName() { return mField; }
    public final String getSubParser() { return mSubParser; }
    
    @Override
    public String toString() {
    	return mField;
    }
    
    private final static Map<String,MagicFieldName> sLookup 
      	= new HashMap<String,MagicFieldName>();
    
    static {
    	for (MagicFieldName s : EnumSet.allOf(MagicFieldName.class)) {
    		sLookup.put(s.toString(), s);
    	}
    }
    
    public static MagicFieldName get(final String field) {
    	return sLookup.get(field);
    }
    
}
