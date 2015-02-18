package org.javenstudio.hornet.search.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.javenstudio.common.indexdb.IAtomicReader;

/** Expert: Internal cache. */
abstract class Cache {

	final FieldCacheImpl mWrapper;
	final Map<Object,Map<Entry,Object>> mReaderCache = new WeakHashMap<Object,Map<Entry,Object>>();
  
	Cache(FieldCacheImpl wrapper) {
		mWrapper = wrapper;
	}
	
	protected abstract Object createValue(IAtomicReader reader, Entry key, boolean setDocsWithField)
			throws IOException;

	/** Remove this reader from the cache, if present. */
	public void purge(IAtomicReader r) {
		Object readerKey = r.getCacheKey();
		synchronized (mReaderCache) {
			mReaderCache.remove(readerKey);
		}
	}

	/** 
	 * Sets the key to the value for the provided reader;
	 *  if the key is already set then this doesn't change it. 
	 */
	public void put(IAtomicReader reader, Entry key, Object value) {
		final Object readerKey = reader.getCacheKey();
		
		synchronized (mReaderCache) {
			Map<Entry,Object> innerCache = mReaderCache.get(readerKey);
			if (innerCache == null) {
				// First time this reader is using FieldCache
				innerCache = new HashMap<Entry,Object>();
				mReaderCache.put(readerKey, innerCache);
				mWrapper.initReader(reader);
			}
			
			if (innerCache.get(key) == null) {
				innerCache.put(key, value);
			} else {
				// Another thread beat us to it; leave the current
				// value
			}
		}
	}

	public Object get(IAtomicReader reader, Entry key, boolean setDocsWithField) throws IOException {
		Map<Entry,Object> innerCache;
		Object value;
		
		final Object readerKey = reader.getCacheKey();
		synchronized (mReaderCache) {
			innerCache = mReaderCache.get(readerKey);
			if (innerCache == null) {
				// First time this reader is using FieldCache
				innerCache = new HashMap<Entry,Object>();
				mReaderCache.put(readerKey, innerCache);
				mWrapper.initReader(reader);
				value = null;
			} else {
				value = innerCache.get(key);
			}
			
			if (value == null) {
				value = new FieldCache.CreationPlaceholder();
				innerCache.put(key, value);
			}
		}
		
		if (value instanceof FieldCache.CreationPlaceholder) {
			synchronized (value) {
				FieldCache.CreationPlaceholder progress = (FieldCache.CreationPlaceholder) value;
				if (progress.mValue == null) {
					progress.mValue = createValue(reader, key, setDocsWithField);
					synchronized (mReaderCache) {
						innerCache.put(key, progress.mValue);
					}

					// Only check if key.custom (the parser) is
					// non-null; else, we check twice for a single
					// call to FieldCache.getXXX
					if (key.mCustom != null && mWrapper != null) {
						//final PrintStream infoStream = wrapper.getInfoStream();
						//if (infoStream != null) {
						//  printNewInsanity(infoStream, progress.value);
						//}
					}
				}
				return progress.mValue;
			}
		}
		
		return value;
	}

}
