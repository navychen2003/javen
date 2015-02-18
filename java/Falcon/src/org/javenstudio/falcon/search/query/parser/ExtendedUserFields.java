package org.javenstudio.falcon.search.query.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;

/**
 * Class that encapsulates the input from userFields parameter and can answer whether
 * a field allowed or disallowed as fielded query in the query string
 */
public class ExtendedUserFields {
	
    private Map<String,Float> mUserFieldsMap;
    private ExtendedDynamicField[] mDynamicUserFields;
    private ExtendedDynamicField[] mNegativeDynamicUserFields;
    
    public ExtendedUserFields(Map<String,Float> ufm) throws ErrorException {
    	mUserFieldsMap = ufm;
    	if (mUserFieldsMap.size() == 0) 
    		mUserFieldsMap.put("*", null);
      
    	// Process dynamic patterns in userFields
    	ArrayList<ExtendedDynamicField> dynUserFields = new ArrayList<ExtendedDynamicField>();
    	ArrayList<ExtendedDynamicField> negDynUserFields = new ArrayList<ExtendedDynamicField>();
    	
    	for (String f : mUserFieldsMap.keySet()) {
    		if (f.contains("*")) {
    			if (f.startsWith("-"))
    				negDynUserFields.add(new ExtendedDynamicField(f.substring(1)));
    			else
    				dynUserFields.add(new ExtendedDynamicField(f));
    		}
    	}
    	
    	Collections.sort(dynUserFields);
    	mDynamicUserFields = dynUserFields.toArray(
    			new ExtendedDynamicField[dynUserFields.size()]);
    	
    	Collections.sort(negDynUserFields);
    	mNegativeDynamicUserFields = negDynUserFields.toArray(
    			new ExtendedDynamicField[negDynUserFields.size()]);
    }
    
    /**
     * Is the given field name allowed according to UserFields spec given in the uf parameter?
     * @param fname the field name to examine
     * @return true if the fielded queries are allowed on this field
     */
    public boolean isAllowed(String fname) {
    	boolean res = ((mUserFieldsMap.containsKey(fname) || isDynField(fname, false)) && 
    			!mUserFieldsMap.containsKey("-" + fname) &&
    			!isDynField(fname, true));
    	
    	return res;
    }
    
    private boolean isDynField(String field, boolean neg) {
    	return getDynFieldForName(field, neg) == null ? false : true;
    }
    
    private String getDynFieldForName(String f, boolean neg) {
    	for (ExtendedDynamicField df : (neg ? mNegativeDynamicUserFields : mDynamicUserFields)) {
    		if (df.matches(f)) return df.getWildcard();
    	}
    	return null;
    }
    
    /**
     * Finds the default user field boost associated with the given field.
     * This is parsed from the uf parameter, and may be specified as wildcards, 
     * 	e.g. *name^2.0 or *^3.0
     * @param field the field to find boost for
     * @return the float boost value associated with the given field or 
     * 	a wildcard matching the field
     */
    public Float getBoost(String field) {
    	return (mUserFieldsMap.containsKey(field)) ?
    			mUserFieldsMap.get(field) : // Exact field
    			mUserFieldsMap.get(getDynFieldForName(field, false)); // Dynamic field
    }
    
}
