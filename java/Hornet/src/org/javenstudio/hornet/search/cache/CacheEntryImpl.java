package org.javenstudio.hornet.search.cache;

final class CacheEntryImpl extends CacheEntry {
    
	private final Object mReaderKey;
    private final String mFieldName;
    private final Class<?> mCacheType;
    private final Object mCustom;
    private final Object mValue;
    
	CacheEntryImpl(Object readerKey, String fieldName,
			Class<?> cacheType, Object custom, Object value) {
		mReaderKey = readerKey;
        mFieldName = fieldName;
        mCacheType = cacheType;
        mCustom = custom;
        mValue = value;

        // :HACK: for testing.
//         if (null != locale || SortField.CUSTOM != sortFieldType) {
//           throw new RuntimeException("Locale/sortFieldType: " + this);
//         }
	}
	
    @Override
    public Object getReaderKey() { 
    	return mReaderKey; 
    }
    
    @Override
    public String getFieldName() { 
    	return mFieldName; 
    }
    
    @Override
    public Class<?> getCacheType() { 
    	return mCacheType; 
    }
    
    @Override
    public Object getCustom() { 
    	return mCustom; 
    }
    
    @Override
    public Object getValue() { 
    	return mValue; 
    }
    
 }
