package org.javenstudio.falcon.search.schema;

import org.javenstudio.falcon.ErrorException;

public class DynamicCopy extends DynamicReplacement {
	
	private final SchemaField mTargetField;
    private final int mMaxChars;

    public DynamicCopy(String regex, SchemaField targetField) 
    		throws ErrorException {
    	this(regex, targetField, CopyField.UNLIMITED);
    }

    public DynamicCopy(String regex, SchemaField targetField, int maxChars) 
    		throws ErrorException {
    	super(regex);
    	mTargetField = targetField;
    	mMaxChars = maxChars;
    }
    
    public final SchemaField getTargetField() { return mTargetField; }
    public final int getMaxChars() { return mMaxChars; }
    
    public SchemaField getTargetField(String sourceField) throws ErrorException {
    	return mTargetField;
    }

    @Override
    public String toString() {
    	return mTargetField.toString();
    }
    
}
