package org.javenstudio.common.indexdb.index.field;

import java.util.HashMap;
import java.util.Map;

public final class FieldNumbers {
    
    private final Map<Integer,String> mNumberToName;
    private final Map<String,Integer> mNameToNumber;
    private int mLowestUnassignedFieldNumber = -1;
    
	public FieldNumbers() {
    	mNameToNumber = new HashMap<String, Integer>();
    	mNumberToName = new HashMap<Integer, String>();
    }
    
    /**
     * Returns the global field number for the given field name. If the name
     * does not exist yet it tries to add it with the given preferred field
     * number assigned if possible otherwise the first unassigned field number
     * is used as the field number.
     */
    public synchronized int addOrGet(String fieldName, int preferredFieldNumber) {
    	Integer fieldNumber = mNameToNumber.get(fieldName);
    	if (fieldNumber == null) {
    		final Integer preferredBoxed = Integer.valueOf(preferredFieldNumber);

    		if (preferredFieldNumber != -1 && !mNumberToName.containsKey(preferredBoxed)) {
    			// cool - we can use this number globally
    			fieldNumber = preferredBoxed;
    		} else {
    			// find a new FieldNumber
    			while (mNumberToName.containsKey(++mLowestUnassignedFieldNumber)) {
    				// might not be up to date - lets do the work once needed
    			}
    			fieldNumber = mLowestUnassignedFieldNumber;
    		}
        
    		mNumberToName.put(fieldNumber, fieldName);
    		mNameToNumber.put(fieldName, fieldNumber);
    	}

    	return fieldNumber.intValue();
    }

    /**
     * Sets the given field number and name if not yet set. 
     */
    synchronized void setIfNotSet(int fieldNumber, String fieldName) {
    	final Integer boxedFieldNumber = Integer.valueOf(fieldNumber);
    	if (!mNumberToName.containsKey(boxedFieldNumber) && !mNameToNumber.containsKey(fieldName)) {
    		mNumberToName.put(boxedFieldNumber, fieldName);
    		mNameToNumber.put(fieldName, boxedFieldNumber);
    	} else {
    		assert containsConsistent(boxedFieldNumber, fieldName);
    	}
    }
    
    // used by assert
    synchronized boolean containsConsistent(Integer number, String name) {
    	return name.equals(mNumberToName.get(number)) && number.equals(mNameToNumber.get(name));
    }
    
}
