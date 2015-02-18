package org.javenstudio.falcon.search.dataimport;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.search.update.InputDocument;

public class ImportDoc extends InputDocument {
	private static final long serialVersionUID = 1L;
	
    private Map<String ,Object> mSession = null;

    public ImportDoc() {}
    
    public void setSessionAttribute(String key, Object val){
    	if (mSession == null) 
    		mSession = new HashMap<String, Object>();
    	
    	mSession.put(key, val);
    }

    public Object getSessionAttribute(String key) {
    	return mSession == null ? null : mSession.get(key);
    }
    
}
