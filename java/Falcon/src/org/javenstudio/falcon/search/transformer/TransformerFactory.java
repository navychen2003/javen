package org.javenstudio.falcon.search.transformer;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;

/**
 * New instance for each request
 *
 */
public abstract class TransformerFactory implements NamedListPlugin {
	
	protected String mDefaultUserArgs = null;

	@Override
	public void init(NamedList<?> args) {
		mDefaultUserArgs = (String)args.get("args");
	}

	public abstract DocTransformer create(String field, 
			Params params, ISearchRequest req) throws ErrorException;

	static final Map<String,TransformerFactory> sDefaultFactories = 
			new HashMap<String,TransformerFactory>();
	
	static {
		//defaultFactories.put( "explain", new ExplainAugmenterFactory() );
		//defaultFactories.put( "value", new ValueAugmenterFactory() );
		//defaultFactories.put( "docid", new DocIdAugmenterFactory() );
		//defaultFactories.put( "shard", new ShardAugmenterFactory() );
	}
	
	public static void loadDefaultFactories(ISearchCore core, 
			Map<String, TransformerFactory> factories) throws ErrorException { 
		for (Map.Entry<String, TransformerFactory> entry : sDefaultFactories.entrySet()) { 
			String name = entry.getKey();
			
			if (!factories.containsKey(name)) { 
				TransformerFactory factory = entry.getValue();
				factories.put(name, factory);
				
				//factory.init(NamedMap.EMPTY); // default ones don't need init
			}
		}
	}
	
}
