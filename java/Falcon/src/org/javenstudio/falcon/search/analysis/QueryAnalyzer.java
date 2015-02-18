package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

public class QueryAnalyzer extends IndexAnalyzer {

	public QueryAnalyzer(IndexSchema schema) {
		super(schema);
	}
	
    @Override
    protected Map<String, IAnalyzer> analyzerCache() {
    	Map<String, IAnalyzer> cache = new HashMap<String, IAnalyzer>();
    	for (SchemaField f : mSchema.getFields().values()) {
    		IAnalyzer analyzer = f.getType().getQueryAnalyzer();
    		cache.put(f.getName(), analyzer);
    	}
    	return cache;
    }

    @Override
    protected IAnalyzer getWrappedAnalyzer(String fieldName) throws IOException {
    	try {
	    	IAnalyzer analyzer = mAnalyzers.get(fieldName);
	    	return analyzer != null ? analyzer : 
	    		mSchema.getDynamicFieldType(fieldName).getQueryAnalyzer();
    	} catch (ErrorException ex) { 
    		throw new IOException(ex);
    	}
    }
	
}
