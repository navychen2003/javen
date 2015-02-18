package org.javenstudio.falcon.search.dataimport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public class ImportCacheSupport {

	private Map<String,ImportCache> mQueryVsCache = new HashMap<String,ImportCache>();
	private Map<String,Iterator<ImportRow>> mQueryVsCacheIterator;
	private Iterator<ImportRow> mDataSourceRowCache;
	
	private final String mEntityName;
	private final String mCacheImplName;
	private String mCacheForeignKey;
	private boolean mCacheDoKeyLookup;
	
	public ImportCacheSupport(ImportContext context, String entityName, 
			String cacheImplName) throws ErrorException {
		mEntityName = entityName;
		mCacheImplName = cacheImplName;
    
		String where = context.getAttributes().getCacheWhere();
		String cacheKey = context.getAttributes().getCachePrimaryKey();
		String lookupKey = context.getAttributes().getCacheForeignKey();
		
		if (cacheKey != null && lookupKey == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"'cacheKey' is specified for the entity " + entityName
					+ " but 'cacheLookup' is missing");
		}
		
		if (where == null && cacheKey == null) {
			mCacheDoKeyLookup = false;
			
		} else {
			mCacheForeignKey = lookupKey;
			mCacheDoKeyLookup = true;
			
			if (where != null) {
				String[] splits = where.split("=");
				cacheKey = splits[0];
				
				mCacheForeignKey = splits[1].trim();
			}
		}
		
		context.setSessionAttribute(ImportContext.CACHE_PRIMARY_KEY, cacheKey,
				ImportContext.SCOPE_ENTITY);
		context.setSessionAttribute(ImportContext.CACHE_FOREIGN_KEY, mCacheForeignKey,
				ImportContext.SCOPE_ENTITY);
		context.setSessionAttribute(ImportContext.CACHE_DELETE_PRIOR_DATA, "true", 
				ImportContext.SCOPE_ENTITY);
		context.setSessionAttribute(ImportContext.CACHE_READ_ONLY, "false",
				ImportContext.SCOPE_ENTITY);
	}
  
	public String getEntityName() { return mEntityName; }
	
	private ImportCache instantiateCache(ImportContext context) 
			throws ErrorException {
		ImportCache cache = context.getSearchCore().newInstance(
				mCacheImplName, ImportCache.class); 
		
		cache.open(context);
		return cache;
	}
  
	public void initNewParent(ImportContext context) {
		mDataSourceRowCache = null;
		mQueryVsCacheIterator = new HashMap<String,Iterator<ImportRow>>();
		
		for (Map.Entry<String,ImportCache> entry : mQueryVsCache.entrySet()) {
			mQueryVsCacheIterator.put(entry.getKey(), entry.getValue().iterator());
		}
	}
  
	public void destroyAll() {
		if (mQueryVsCache != null) {
			for (ImportCache cache : mQueryVsCache.values()) {
				cache.destroy();
			}
		}
		
		mQueryVsCache = null;
		mDataSourceRowCache = null;
		mCacheForeignKey = null;
	}
  
	/**
	 * Get all the rows from the datasource for the given query and cache them
	 */
	public void populateCache(String query, 
			Iterator<ImportRow> rowIterator) throws ErrorException {
		ImportRow aRow = null;
		ImportCache cache = mQueryVsCache.get(query);
		
		while ((aRow = getNextFromCache(query, rowIterator)) != null) {
			cache.add(aRow);
		}
	}
  
	private ImportRow getNextFromCache(String query, 
			Iterator<ImportRow> rowIterator) throws ErrorException {
		try {
			if (rowIterator == null) return null;
			if (rowIterator.hasNext()) return rowIterator.next();
			return null;
		} catch (Throwable e) {
			if (e instanceof ErrorException) 
				throw (ErrorException)e; 
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
  
	public ImportRow getCacheData(ImportContext context, String query,
			Iterator<ImportRow> rowIterator) throws ErrorException {
		if (mCacheDoKeyLookup) 
			return getIdCacheData(context, query, rowIterator);
		else 
			return getSimpleCacheData(context, query, rowIterator);
	}
  
	/**
	 * If the where clause is present the cache is sql Vs Map of 
	 * key Vs List of Rows.
	 * 
	 * @param query the query string for which cached data is to be returned
	 * @return the cached row corresponding to the given query after all variables
	 *         have been resolved
	 */
	protected ImportRow getIdCacheData(ImportContext context, String query,
			Iterator<ImportRow> rowIterator) throws ErrorException {
		Object key = context.resolve(mCacheForeignKey);
		if (key == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"The cache lookup value: " + mCacheForeignKey + " is resolved to be null in the entity:"
					+ getEntityName());
		}
		
		if (mDataSourceRowCache == null) {
			ImportCache cache = mQueryVsCache.get(query);
      
			if (cache == null) {        
				cache = instantiateCache(context);
				mQueryVsCache.put(query, cache);
				populateCache(query, rowIterator);
			}
			
			mDataSourceRowCache = cache.iterator(key);
		}
		
		return getFromRowCacheTransformed();
	}
  
	/**
	 * If where clause is not present the cache is a Map of query vs List of Rows.
	 * 
	 * @param query
	 *          string for which cached row is to be returned
	 * 
	 * @return the cached row corresponding to the given query
	 */
	protected ImportRow getSimpleCacheData(ImportContext context,
			String query, Iterator<ImportRow> rowIterator) throws ErrorException {
		if (mDataSourceRowCache == null) {
			ImportCache cache = mQueryVsCache.get(query);
			if (cache == null) {
				cache = instantiateCache(context);
				mQueryVsCache.put(query, cache);
				
				populateCache(query, rowIterator);
				mQueryVsCacheIterator.put(query, cache.iterator());
			}
			
			Iterator<ImportRow> cacheIter = mQueryVsCacheIterator.get(query);
			mDataSourceRowCache = cacheIter;
		}
    
		return getFromRowCacheTransformed();
	}
  
	protected ImportRow getFromRowCacheTransformed() {
		if (mDataSourceRowCache == null || !mDataSourceRowCache.hasNext()) {
			mDataSourceRowCache = null;
			return null;
		}
		
		ImportRow r = mDataSourceRowCache.next();
		return r;
	}
  
}
