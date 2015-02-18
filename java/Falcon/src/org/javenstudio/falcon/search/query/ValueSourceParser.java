package org.javenstudio.falcon.search.query;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.falcon.search.ISearchCore;

/**
 * A factory that parses user queries to generate ValueSource instances.
 * Intended usage is to create pluggable, named functions for use in function queries.
 */
public abstract class ValueSourceParser implements NamedListPlugin {
	
	protected ISearchCore mCore;
	
	public final ISearchCore getSearchCore() { 
		return mCore;
	}
	
	/**
	 * Initialize the plugin.
	 */
	@Override
	public void init(NamedList<?> args) { 
		// do nothing
	}

	/**
	 * Parse the user input into a ValueSource.
	 */
	public abstract ValueSource parse(FunctionQueryBuilder fp) 
			throws ErrorException;

}
