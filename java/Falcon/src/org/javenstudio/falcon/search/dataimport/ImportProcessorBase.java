package org.javenstudio.falcon.search.dataimport;

import java.util.Iterator;

import org.javenstudio.falcon.ErrorException;

/**
 * <p> Base class for all implementations of {@link ImportProcessor} </p> 
 * <p> Most implementations of {@link ImportProcessor}
 * extend this base class which provides common functionality. </p>
 * <p/>
 * <b>This API is experimental and subject to change</b>
 *
 * @since 1.3
 */
public class ImportProcessorBase extends ImportProcessor {
	//private static final Logger LOG = Logger.getLogger(EntityProcessorBase.class);

	protected final ImportContext mContext;
	protected final String mEntityName;
	
	protected Iterator<ImportRow> mRowIterator = null;
	protected ImportCacheSupport mCacheSupport = null;

	protected String mQuery = null;
	protected String mOnError = ImportContext.ABORT;
	protected boolean mIsFirstInit = true;
  
	public ImportProcessorBase(ImportContext context, String entityName) 
			throws ErrorException {
		mContext = context;
		mEntityName = entityName;
		
		if (mIsFirstInit) 
			firstInit(context, entityName);
		
		if (mCacheSupport != null) 
			mCacheSupport.initNewParent(context);
		
	}

	public ImportContext getContext() { 
		return mContext;
	}
	
	@Override
	public String getEntityName() { 
		return mEntityName;
	}
	
	/** first time init call. do one-time operations here */
	protected void firstInit(ImportContext context, String entityName) 
			throws ErrorException {
		//String s = context.getAttribute(ImportContext.ON_ERROR);
		//if (s != null) mOnError = s;
		
		initCache(context, entityName);
		mIsFirstInit = false;
	}

    protected void initCache(ImportContext context, String entityName) 
    		throws ErrorException {
    	String cacheImplName = context.getAttributes().getCacheImplName();

        if (cacheImplName != null ) {
        	mCacheSupport = new ImportCacheSupport(context, 
        			entityName, cacheImplName);
        }
    }

    @Override
    public void init(ImportRequest req) throws ErrorException { 
    	// do nothing
    }
    
    @Override
    public ImportRow nextModifiedRow() throws ErrorException {
    	return null;
    }

    @Override
    public ImportRow nextDeletedRow() throws ErrorException {
    	return null;
    }

    //@Override
    //public ImportRow nextModifiedParentRow() throws ErrorException {
    //	return null;
    //}

    /**
     * For a simple implementation, this is the only method that 
     * the sub-class should implement. 
     * This is intended to stream rows one-by-one. Return null to 
     * signal end of rows
     *
     * @return a row where the key is the name of the field and value 
     * 	can be any Object or a Collection of objects. Return
     * 	null to signal end of rows
     */
    @Override
    public ImportRow nextRow() throws ErrorException {
    	return null;// do not do anything
    }
  
    protected ImportRow getNext() throws ErrorException {
    	if (mCacheSupport == null) {
    		try {
    			if (mRowIterator == null)
    				return null;
    			
    			if (mRowIterator.hasNext())
    				return mRowIterator.next();
    			
    			mQuery = null;
    			mRowIterator = null;
    			return null;
    			
    		} catch (Throwable e) {
    			mQuery = null;
    			mRowIterator = null;
    			
    			if (e instanceof ErrorException) 
    				throw (ErrorException)e; 
    			else 
    				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
    		}
    	} else  {
    		return mCacheSupport.getCacheData(mContext, mQuery, mRowIterator);
    	}      
    }

    @Override
    public void destroy() throws ErrorException {
    	mQuery = null;
    	
    	if (mCacheSupport != null) 
    		mCacheSupport.destroyAll();
    	
    	mCacheSupport = null;
    }

}
