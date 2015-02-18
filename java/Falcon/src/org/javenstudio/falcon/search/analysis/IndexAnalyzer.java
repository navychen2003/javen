package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.panda.analysis.AnalyzerWrapper;

public class IndexAnalyzer extends AnalyzerWrapper {
	
	protected final IndexSchema mSchema;
    protected final Map<String, IAnalyzer> mAnalyzers;

    public IndexAnalyzer(IndexSchema schema) {
    	mSchema = schema;
    	mAnalyzers = analyzerCache();
    }

    protected Map<String, IAnalyzer> analyzerCache() {
    	Map<String, IAnalyzer> cache = new HashMap<String, IAnalyzer>();
    	for (SchemaField f : mSchema.getFields().values()) {
    		IAnalyzer analyzer = f.getType().getAnalyzer();
    		cache.put(f.getName(), analyzer);
    	}
    	return cache;
    }

    @Override
    protected IAnalyzer getWrappedAnalyzer(String fieldName) throws IOException {
    	try { 
    		IAnalyzer analyzer = mAnalyzers.get(fieldName);
    		return analyzer != null ? analyzer : 
    			mSchema.getDynamicFieldType(fieldName).getAnalyzer();
    	} catch (ErrorException ex) { 
    		throw new IOException(ex);
    	}
    }

    @Override
    protected TokenComponents wrapComponents(String fieldName, 
    		TokenComponents components) {
    	return components;
    }
    
}
