package org.javenstudio.falcon.search.dataimport;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ImportWriterBase implements ImportWriter {

	protected String mKeyFieldName;
	protected Set<Object> mDeltaKeys = null;
  
	@Override
	public void setDeltaKeys(Set<Map<String,Object>> passedInDeltaKeys) {
		mDeltaKeys = new HashSet<Object>();
		
		for (Map<String,Object> aMap : passedInDeltaKeys) {
			if (aMap.size() > 0) {
				Object key = null;
				if (mKeyFieldName != null) 
					key = aMap.get(mKeyFieldName);
				else 
					key = aMap.entrySet().iterator().next();
				
				if (key != null) 
					mDeltaKeys.add(key);
			}
		}
	}
	
}
