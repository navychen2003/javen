package org.javenstudio.hornet.search.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocTermOrds;
import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderClosedListener;
import org.javenstudio.common.indexdb.ISegmentClosedListener;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Expert: The default cache implementation, storing all values in memory.
 * A WeakHashMap is used for storage.
 */
final class FieldCacheImpl extends FieldCache {
	
	private Map<Class<?>,Cache> mCaches;
	
	public FieldCacheImpl() {
		init();
	}
	
	private synchronized void init() {
		mCaches = new HashMap<Class<?>,Cache>(9);
		mCaches.put(Byte.TYPE, new ByteCache(this));
		mCaches.put(Short.TYPE, new ShortCache(this));
		mCaches.put(Integer.TYPE, new IntCache(this));
		mCaches.put(Float.TYPE, new FloatCache(this));
		mCaches.put(Long.TYPE, new LongCache(this));
		mCaches.put(Double.TYPE, new DoubleCache(this));
		mCaches.put(IDocTerms.class, new DocTermsCache(this));
		mCaches.put(IDocTermsIndex.class, new DocTermsIndexCache(this));
		mCaches.put(IDocTermOrds.class, new DocTermOrdsCache(this));
		mCaches.put(DocsWithFieldCache.class, new DocsWithFieldCache(this));
	}

	@Override
	public synchronized void purgeAllCaches() {
		init();
	}

	@Override
	public synchronized void purge(IAtomicReader r) {
		for (Cache c : mCaches.values()) {
			c.purge(r);
		}
	}
  
	@Override
	public synchronized CacheEntry[] getCacheEntries() {
		List<CacheEntry> result = new ArrayList<CacheEntry>(17);
		
		for (final Map.Entry<Class<?>,Cache> cacheEntry: mCaches.entrySet()) {
			final Cache cache = cacheEntry.getValue();
			final Class<?> cacheType = cacheEntry.getKey();
			
			synchronized (cache.mReaderCache) {
				for (final Map.Entry<Object,Map<Entry, Object>> readerCacheEntry : cache.mReaderCache.entrySet()) {
					final Object readerKey = readerCacheEntry.getKey();
					if (readerKey == null) continue;
					
					final Map<Entry, Object> innerCache = readerCacheEntry.getValue();
					for (final Map.Entry<Entry, Object> mapEntry : innerCache.entrySet()) {
						Entry entry = mapEntry.getKey();
						result.add(new CacheEntryImpl(readerKey, entry.mField,
								cacheType, entry.mCustom, mapEntry.getValue()));
					}
				}
			}
		}
		return result.toArray(new CacheEntry[result.size()]);
	}
  
	// per-segment fieldcaches don't purge until the shared core closes.
	final ISegmentClosedListener mPurgeCore = new ISegmentClosedListener() {
			@Override
			public void onClose(ISegmentReader owner) {
				FieldCacheImpl.this.purge(owner);
			}
		};

	// composite/SlowMultiReaderWrapper fieldcaches don't purge until composite reader is closed.
	final IIndexReaderClosedListener mPurgeReader = new IIndexReaderClosedListener() {
			@Override
			public void onClose(IIndexReader owner) {
				assert owner instanceof IAtomicReader;
				FieldCacheImpl.this.purge((IAtomicReader) owner);
			}
		};
  
	final void initReader(IAtomicReader reader) {
		if (reader instanceof ISegmentReader) {
			((ISegmentReader) reader).addSegmentClosedListener(mPurgeCore);
			
		} else {
			// we have a slow reader of some sort, try to register a purge event
			// rather than relying on gc:
			Object key = reader.getCacheKey();
			if (key instanceof IAtomicReader) {
				((IAtomicReader)key).addClosedListener(mPurgeReader); 
			} else {
				// last chance
				reader.addClosedListener(mPurgeReader); 				
			}
		}
	}

	@Override
	public byte[] getBytes(IAtomicReader reader, String field, boolean setDocsWithField) 
			throws IOException {
		return getBytes(reader, field, null, setDocsWithField);
	}

	@Override
	public byte[] getBytes(IAtomicReader reader, String field, 
			ISortField.ByteParser parser, boolean setDocsWithField) throws IOException {
		return (byte[]) mCaches.get(Byte.TYPE).get(reader, new Entry(field, parser), setDocsWithField);
	}
  
	@Override
	public short[] getShorts (IAtomicReader reader, String field, boolean setDocsWithField) 
			throws IOException {
		return getShorts(reader, field, null, setDocsWithField);
	}

	@Override
	public short[] getShorts(IAtomicReader reader, String field, 
			ISortField.ShortParser parser, boolean setDocsWithField) throws IOException {
		return (short[]) mCaches.get(Short.TYPE).get(reader, new Entry(field, parser), setDocsWithField);
	}

	// null Bits means no docs matched
	void setDocsWithField(IAtomicReader reader, String field, Bits docsWithField) {
		final int maxDoc = reader.getMaxDoc();
		final Bits bits;
		
		if (docsWithField == null) {
			bits = new Bits.MatchNoBits(maxDoc);
			
		} else if (docsWithField instanceof FixedBitSet) {
			final int numSet = ((FixedBitSet) docsWithField).cardinality();
			if (numSet >= maxDoc) {
				// The cardinality of the BitSet is maxDoc if all documents have a value.
				assert numSet == maxDoc;
				bits = new Bits.MatchAllBits(maxDoc);
			} else {
				bits = docsWithField;
			}
		} else {
			bits = docsWithField;
		}
		
		mCaches.get(DocsWithFieldCache.class).put(reader, new Entry(field, null), bits);
	}
  
	@Override
	public int[] getInts(IAtomicReader reader, String field, boolean setDocsWithField) 
			throws IOException {
		return getInts(reader, field, null, setDocsWithField);
	}

	@Override
	public int[] getInts(IAtomicReader reader, String field, 
			ISortField.IntParser parser, boolean setDocsWithField) throws IOException {
		return (int[]) mCaches.get(Integer.TYPE).get(reader, 
				new Entry(field, parser), setDocsWithField);
	}

	@Override
	public Bits getDocsWithField(IAtomicReader reader, String field)
			throws IOException {
		return (Bits) mCaches.get(DocsWithFieldCache.class).get(reader, 
				new Entry(field, null), false);
	}

	@Override
	public float[] getFloats(IAtomicReader reader, String field, boolean setDocsWithField)
			throws IOException {
		return getFloats(reader, field, null, setDocsWithField);
	}

	@Override
	public float[] getFloats(IAtomicReader reader, String field, 
			ISortField.FloatParser parser, boolean setDocsWithField) throws IOException {
		return (float[]) mCaches.get(Float.TYPE).get(reader, 
				new Entry(field, parser), setDocsWithField);
	}

	@Override
	public long[] getLongs(IAtomicReader reader, String field, boolean setDocsWithField) 
			throws IOException {
		return getLongs(reader, field, null, setDocsWithField);
	}
  
	@Override
	public long[] getLongs(IAtomicReader reader, String field, 
			ISortField.LongParser parser, boolean setDocsWithField) throws IOException {
		return (long[]) mCaches.get(Long.TYPE).get(reader, 
				new Entry(field, parser), setDocsWithField);
	}

	@Override
	public double[] getDoubles(IAtomicReader reader, String field, boolean setDocsWithField)
			throws IOException {
		return getDoubles(reader, field, null, setDocsWithField);
	}

	@Override
	public double[] getDoubles(IAtomicReader reader, String field, 
			ISortField.DoubleParser parser, boolean setDocsWithField) throws IOException {
		return (double[]) mCaches.get(Double.TYPE).get(reader, 
				new Entry(field, parser), setDocsWithField);
	}

	@Override
	public IDocTermsIndex getTermsIndex(IAtomicReader reader, String field) throws IOException {
		return getTermsIndex(reader, field, 0); //PackedInts.FAST);
	}

	@Override
	public IDocTermsIndex getTermsIndex(IAtomicReader reader, String field, 
			float acceptableOverheadRatio) throws IOException {
		return (IDocTermsIndex) mCaches.get(IDocTermsIndex.class).get(reader, 
				new Entry(field, acceptableOverheadRatio), false);
	}

	// TODO: this if DocTermsIndex was already created, we
	// should share it...
	@Override
	public IDocTerms getTerms(IAtomicReader reader, String field) throws IOException {
		return getTerms(reader, field, 0); //PackedInts.FAST);
	}

	@Override
	public IDocTerms getTerms(IAtomicReader reader, String field, 
			float acceptableOverheadRatio) throws IOException {
		return (IDocTerms) mCaches.get(IDocTerms.class).get(reader, 
				new Entry(field, acceptableOverheadRatio), false);
	}

	@Override
	public IDocTermOrds getDocTermOrds(IAtomicReader reader, String field) 
			throws IOException {
		return (IDocTermOrds) mCaches.get(IDocTermOrds.class).get(reader, 
				new Entry(field, null), false);
	}

}

