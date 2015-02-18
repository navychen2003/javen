package org.javenstudio.hornet.search.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Caches all docs, and optionally also scores, coming from
 * a search, and is then able to replay them to another
 * collector.  You specify the max RAM this class may use.
 * Once the collection is done, call {@link #isCached}. If
 * this returns true, you can use {@link #replay(Collector)}
 * against a new collector.  If it returns false, this means
 * too much RAM was required and you must instead re-run the
 * original search.
 *
 * <p><b>NOTE</b>: this class consumes 4 (or 8 bytes, if
 * scoring is cached) per collected document.  If the result
 * set is large this can easily be a very substantial amount
 * of RAM!
 * 
 * <p><b>NOTE</b>: this class caches at least 128 documents
 * before checking RAM limits.
 * 
 * <p>See the Lucene <tt>modules/grouping</tt> module for more
 * details including a full code example.</p>
 *
 */
public abstract class CachingCollector extends Collector {
  
	/**
	 * Creates a {@link CachingCollector} which does not wrap another collector.
	 * The cached documents and scores can later be {@link #replay(Collector)
	 * replayed}.
	 * 
	 * @param acceptDocsOutOfOrder
	 *          whether documents are allowed to be collected out-of-order
	 */
	public static CachingCollector create(final boolean acceptDocsOutOfOrder, 
			boolean cacheScores, double maxRAMMB) {
		Collector other = new Collector() {
				@Override
				public boolean acceptsDocsOutOfOrder() {
					return acceptDocsOutOfOrder;
				}
	      
				@Override
				public void setScorer(IScorer scorer) {}
	
				@Override
				public void collect(int doc) {}
	
				@Override
				public void setNextReader(IAtomicReaderRef context) {}
			};
		
		return create(other, cacheScores, maxRAMMB);
	}

	/**
	 * Create a new {@link CachingCollector} that wraps the given collector and
	 * caches documents and scores up to the specified RAM threshold.
	 * 
	 * @param other
	 *          the Collector to wrap and delegate calls to.
	 * @param cacheScores
	 *          whether to cache scores in addition to document IDs. Note that
	 *          this increases the RAM consumed per doc
	 * @param maxRAMMB
	 *          the maximum RAM in MB to consume for caching the documents and
	 *          scores. If the collector exceeds the threshold, no documents and
	 *          scores are cached.
	 */
	public static CachingCollector create(Collector other, boolean cacheScores, double maxRAMMB) {
		return cacheScores ? new ScoreCachingCollector(other, maxRAMMB) : 
			new NoScoreCachingCollector(other, maxRAMMB);
	}

	/**
	 * Create a new {@link CachingCollector} that wraps the given collector and
	 * caches documents and scores up to the specified max docs threshold.
	 *
	 * @param other
	 *          the Collector to wrap and delegate calls to.
	 * @param cacheScores
	 *          whether to cache scores in addition to document IDs. Note that
	 *          this increases the RAM consumed per doc
	 * @param maxDocsToCache
	 *          the maximum number of documents for caching the documents and
	 *          possible the scores. If the collector exceeds the threshold,
	 *          no documents and scores are cached.
	 */
	public static CachingCollector create(ICollector other, boolean cacheScores, int maxDocsToCache) {
		return cacheScores ? new ScoreCachingCollector(other, maxDocsToCache) : 
			new NoScoreCachingCollector(other, maxDocsToCache);
	}
	
	
	// TODO: would be nice if a collector defined a
	// needsScores() method so we can specialize / do checks
	// up front. This is only relevant for the ScoreCaching
	// version -- if the wrapped Collector does not need
	// scores, it can avoid cachedScorer entirely.
	protected final ICollector mOther;
  
	protected final List<SegStart> mCachedSegs = new ArrayList<SegStart>();
	protected final List<int[]> mCachedDocs;
	protected final int mMaxDocsToCache;
  
	protected IAtomicReaderRef mLastReaderContext;
  
	protected int[] mCurDocs;
	protected int mUpto;
	protected int mBase;
	protected int mLastDocBase;
  
  
	// Prevent extension from non-internal classes
	protected CachingCollector(ICollector other, double maxRAMMB, boolean cacheScores) {
		mOther = other;
    
		mCachedDocs = new ArrayList<int[]>();
		mCurDocs = new int[INITIAL_ARRAY_SIZE];
		mCachedDocs.add(mCurDocs);

		int bytesPerDoc = JvmUtil.NUM_BYTES_INT;
		if (cacheScores) 
			bytesPerDoc += JvmUtil.NUM_BYTES_FLOAT;
		
		mMaxDocsToCache = (int) ((maxRAMMB * 1024 * 1024) / bytesPerDoc);
	}

	protected CachingCollector(ICollector other, int maxDocsToCache) {
		mOther = other;

		mCachedDocs = new ArrayList<int[]>();
		mCurDocs = new int[INITIAL_ARRAY_SIZE];
		mCachedDocs.add(mCurDocs);
		mMaxDocsToCache = maxDocsToCache;
	}
  
	@Override
	public boolean acceptsDocsOutOfOrder() {
		return mOther.acceptsDocsOutOfOrder();
	}

	public boolean isCached() {
		return mCurDocs != null;
	}

	@Override  
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mOther.setNextReader(context);
		if (mLastReaderContext != null) 
			mCachedSegs.add(new SegStart(mLastReaderContext, mBase+mUpto));
		
		mLastReaderContext = context;
	}

	/** Reused by the specialized inner classes. */
	protected void replayInit(ICollector other) {
		if (!isCached()) 
			throw new IllegalStateException("cannot replay: cache was cleared because too much RAM was required");
    
		if (!other.acceptsDocsOutOfOrder() && mOther.acceptsDocsOutOfOrder()) {
			throw new IllegalArgumentException("cannot replay: given collector does not support "
					+ "out-of-order collection, while the wrapped collector does. "
					+ "Therefore cached documents may be out-of-order.");
		}
    
		if (mLastReaderContext != null) {
			mCachedSegs.add(new SegStart(mLastReaderContext, mBase + mUpto));
			mLastReaderContext = null;
		}
	}

	/**
	 * Replays the cached doc IDs (and scores) to the given Collector. If this
	 * instance does not cache scores, then Scorer is not set on
	 * {@code other.setScorer} as well as scores are not replayed.
	 * 
	 * @throws IllegalStateException
	 *           if this collector is not cached (i.e., if the RAM limits were too
	 *           low for the number of documents + scores to cache).
	 * @throws IllegalArgumentException
	 *           if the given Collect's does not support out-of-order collection,
	 *           while the collector passed to the ctor does.
	 */
	public abstract void replay(ICollector other) throws IOException;
  
	
	// Max out at 512K arrays
	private static final int MAX_ARRAY_SIZE = 512 * 1024;
	private static final int INITIAL_ARRAY_SIZE = 128;
	private static final int[] EMPTY_INT_ARRAY = new int[0];

	
	private static class SegStart {
		public final IAtomicReaderRef mReaderContext;
		public final int mEnd;

		public SegStart(IAtomicReaderRef readerContext, int end) {
			mReaderContext = readerContext;
			mEnd = end;
		}
	}
	
	private static final class CachedScorer extends Scorer {
	    
		// NOTE: these members are package-private b/c that way accessing them from
		// the outer class does not incur access check by the JVM. The same
		// situation would be if they were defined in the outer class as private
		// members.
		private int mDoc;
		private float mScore;
    
		private CachedScorer() { super(null); }

		@Override
		public final float getScore() { return mScore; }
    
		@Override
		public final int advance(int target) { throw new UnsupportedOperationException(); }
    
		@Override
		public final int getDocID() { return mDoc; }
    
		@Override
		public final float getFreq() { throw new UnsupportedOperationException(); }
    
		@Override
		public final int nextDoc() { throw new UnsupportedOperationException(); }
	}

	// A CachingCollector which caches scores
	private static final class ScoreCachingCollector extends CachingCollector {

		private final CachedScorer mCachedScorer;
		private final List<float[]> mCachedScores;

		private IScorer mScorer;
		private float[] mCurScores;

		public ScoreCachingCollector(ICollector other, double maxRAMMB) {
			super(other, maxRAMMB, true);

			mCachedScorer = new CachedScorer();
			mCachedScores = new ArrayList<float[]>();
			mCurScores = new float[INITIAL_ARRAY_SIZE];
			mCachedScores.add(mCurScores);
		}

		public ScoreCachingCollector(ICollector other, int maxDocsToCache) {
			super(other, maxDocsToCache);

			mCachedScorer = new CachedScorer();
			mCachedScores = new ArrayList<float[]>();
			mCurScores = new float[INITIAL_ARRAY_SIZE];
			mCachedScores.add(mCurScores);
		}
    
		@Override
		public void collect(int doc) throws IOException {
			if (mCurDocs == null) {
				// Cache was too large
				mCachedScorer.mScore = mScorer.getScore();
				mCachedScorer.mDoc = doc;
				mOther.collect(doc);
				
				return;
			}

			// Allocate a bigger array or abort caching
			if (mUpto == mCurDocs.length) {
				mBase += mUpto;
        
				// Compute next array length - don't allocate too big arrays
				int nextLength = 8 * mCurDocs.length;
				if (nextLength > MAX_ARRAY_SIZE) 
					nextLength = MAX_ARRAY_SIZE;
				
				if (mBase + nextLength > mMaxDocsToCache) {
					// try to allocate a smaller array
					nextLength = mMaxDocsToCache - mBase;
					
					if (nextLength <= 0) {
						// Too many docs to collect -- clear cache
						mCurDocs = null;
						mCurScores = null;
						mCachedSegs.clear();
						mCachedDocs.clear();
						mCachedScores.clear();
						
						mCachedScorer.mScore = mScorer.getScore();
						mCachedScorer.mDoc = doc;
						mOther.collect(doc);
						
						return;
					}
				}
        
				mCurDocs = new int[nextLength];
				mCachedDocs.add(mCurDocs);
				mCurScores = new float[nextLength];
				mCachedScores.add(mCurScores);
				mUpto = 0;
			}
      
			mCurDocs[mUpto] = doc;
			mCachedScorer.mScore = mCurScores[mUpto] = mScorer.getScore();
			mUpto++;
			mCachedScorer.mDoc = doc;
			mOther.collect(doc);
		}

		@Override
		public void replay(ICollector other) throws IOException {
			replayInit(other);
      
			int curUpto = 0;
			int curBase = 0;
			int chunkUpto = 0;
			
			mCurDocs = EMPTY_INT_ARRAY;
			
			for (SegStart seg : mCachedSegs) {
				other.setNextReader(seg.mReaderContext);
				other.setScorer(mCachedScorer);
				
				while (curBase + curUpto < seg.mEnd) {
					if (curUpto == mCurDocs.length) {
						curBase += mCurDocs.length;
						
						mCurDocs = mCachedDocs.get(chunkUpto);
						mCurScores = mCachedScores.get(chunkUpto);
						
						chunkUpto ++;
						curUpto = 0;
					}
					
					mCachedScorer.mScore = mCurScores[curUpto];
					mCachedScorer.mDoc = mCurDocs[curUpto];
					
					other.collect(mCurDocs[curUpto++]);
				}
			}
		}

		@Override
		public void setScorer(IScorer scorer) throws IOException {
			mScorer = scorer;
			mOther.setScorer(mCachedScorer);
		}

		@Override
		public String toString() {
			if (isCached()) 
				return "CachingCollector(" + (mBase + mUpto) + " docs & scores cached)";
			else 
				return "CachingCollector(cache was cleared)";
		}
	}

	// A CachingCollector which does not cache scores
	private static final class NoScoreCachingCollector extends CachingCollector {
    
		public NoScoreCachingCollector(ICollector other, double maxRAMMB) {
			super(other, maxRAMMB, false);
		}

		public NoScoreCachingCollector(ICollector other, int maxDocsToCache) {
			super(other, maxDocsToCache);
		}

		@Override
		public void collect(int doc) throws IOException {
			if (mCurDocs == null) {
				// Cache was too large
				mOther.collect(doc);
				
				return;
			}

			// Allocate a bigger array or abort caching
			if (mUpto == mCurDocs.length) {
				mBase += mUpto;
        
				// Compute next array length - don't allocate too big arrays
				int nextLength = 8 * mCurDocs.length;
				if (nextLength > MAX_ARRAY_SIZE) 
					nextLength = MAX_ARRAY_SIZE;
				
				if (mBase + nextLength > mMaxDocsToCache) {
					// try to allocate a smaller array
					nextLength = mMaxDocsToCache - mBase;
					
					if (nextLength <= 0) {
						// Too many docs to collect -- clear cache
						mCurDocs = null;
						mCachedSegs.clear();
						mCachedDocs.clear();
						mOther.collect(doc);
						
						return;
					}
				}
        
				mCurDocs = new int[nextLength];
				mCachedDocs.add(mCurDocs);
				mUpto = 0;
			}
      
			mCurDocs[mUpto] = doc;
			mUpto ++;
			mOther.collect(doc);
		}

		@Override
		public void replay(ICollector other) throws IOException {
			replayInit(other);
      
			int curUpto = 0;
			int curbase = 0;
			int chunkUpto = 0;
			
			mCurDocs = EMPTY_INT_ARRAY;
			
			for (SegStart seg : mCachedSegs) {
				other.setNextReader(seg.mReaderContext);
				
				while (curbase + curUpto < seg.mEnd) {
					if (curUpto == mCurDocs.length) {
						curbase += mCurDocs.length;
						mCurDocs = mCachedDocs.get(chunkUpto);
						
						chunkUpto ++;
						curUpto = 0;
					}
					
					other.collect(mCurDocs[curUpto++]);
				}
			}
		}

		@Override
		public void setScorer(IScorer scorer) throws IOException {
			mOther.setScorer(scorer);
		}

		@Override
		public String toString() {
			if (isCached()) 
				return "CachingCollector(" + (mBase + mUpto) + " docs cached)";
			else 
				return "CachingCollector(cache was cleared)";
		}
	}
	
}
