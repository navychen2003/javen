package org.javenstudio.falcon.search.dataimport;

public class ImportField {

	private final String mFieldName;
	private final Object mFieldValue;
	private float mFieldBoost = 1.0f;
	
	public ImportField(String fieldName, Object fieldValue) { 
		mFieldName = fieldName;
		mFieldValue = fieldValue;
	}
	
	public final String getName() { return mFieldName; }
	public final Object getValue() { return mFieldValue; }
	
	public final float getBoost() { return mFieldBoost; }
	public final void setBoost(float boost) { mFieldBoost = boost; }
	
}
