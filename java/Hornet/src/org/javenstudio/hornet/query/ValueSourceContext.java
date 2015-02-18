package org.javenstudio.hornet.query;

import java.util.IdentityHashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.ISearcher;

public abstract class ValueSourceContext {

	public abstract Object get(Object name);
	public abstract Object put(Object name, Object value);
	
	/**
	 * Returns a new non-threadsafe context map.
	 */
	public static ValueSourceContext create(ISearcher searcher) {
		DefaultValueSourceContext context = new DefaultValueSourceContext();
		context.put("searcher", searcher);
		return context;
	}
	
	static class DefaultValueSourceContext extends ValueSourceContext { 
		private final Map<Object,Object> mContext = new IdentityHashMap<Object,Object>();
		
		public DefaultValueSourceContext() {}
		
		@Override
		public Object put(Object name, Object value) { 
			return mContext.put(name, value);
		}
		
		@Override
		public Object get(Object name) { 
			return mContext.get(name);
		}
	}
	
}
