package org.javenstudio.hornet.search;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.ITopSortDocs;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.ScoreDoc;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.TopDocs;
import org.javenstudio.common.indexdb.search.TopFieldDocs;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;
import org.javenstudio.hornet.search.hits.HitQueue;
import org.javenstudio.hornet.search.query.FilteredQuery;
import org.javenstudio.hornet.search.similarity.DefaultSimilarity;

public class AdvancedIndexSearcher extends IndexSearcher {

	// the default Similarity
	static final ISimilarity sDefaultSimilarity = new DefaultSimilarity();
	
	/**
	 * Expert: returns a default Similarity instance.
	 * In general, this method is only called to initialize searchers and writers.
	 * User code and query implementations should respect
	 * {@link IndexSearcher#getSimilarity()}.
	 */
	public static ISimilarity getDefaultSimilarity() {
		return sDefaultSimilarity;
	}
	
	// These are only used for multi-threaded search
	private final ExecutorService mExecutor;
	
	// used with executor - each slice holds a set of leafs executed within one thread
	private final LeafSlice[] mLeafSlices;
	
	/** Creates a searcher searching the provided index. */
	public AdvancedIndexSearcher(IIndexReader r) {
		this(r, null);
	}

	/** 
	 * Runs searches for each segment separately, using the
	 *  provided ExecutorService.  IndexSearcher will not
	 *  shutdown/awaitTermination this ExecutorService on
	 *  close; you must do so, eventually, on your own.  NOTE:
	 *  if you are using {@link NIOFSDirectory}, do not use
	 *  the shutdownNow method of ExecutorService as this uses
	 *  Thread.interrupt under-the-hood which can silently
	 *  close file descriptors (see <a
	 *  href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
	 */
	public AdvancedIndexSearcher(IIndexReader r, ExecutorService executor) {
		this(r.getReaderContext(), executor);
	}
	
	/**
	 * Creates a searcher searching the provided top-level {@link IIndexReaderRef}.
	 *
	 * @see IIndexReaderRef
	 * @see IndexReader#getTopReaderContext()
	 */
	public AdvancedIndexSearcher(IIndexReaderRef context) {
		this(context, null);
	}
	
	/**
	 * Creates a searcher searching the provided top-level {@link IIndexReaderRef}.
	 * <p>
	 * Given a non-<code>null</code> {@link ExecutorService} this method runs
	 * searches for each segment separately, using the provided ExecutorService.
	 * IndexSearcher will not shutdown/awaitTermination this ExecutorService on
	 * close; you must do so, eventually, on your own. NOTE: if you are using
	 * {@link NIOFSDirectory}, do not use the shutdownNow method of
	 * ExecutorService as this uses Thread.interrupt under-the-hood which can
	 * silently close file descriptors (see <a
	 * href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
	 * 
	 * @see IIndexReaderRef
	 * @see IndexReader#getTopReaderContext()
	 */
	public AdvancedIndexSearcher(IIndexReaderRef context, ExecutorService executor) {
		super(context, getDefaultSimilarity());
		mExecutor = executor;
		mLeafSlices = (executor == null) ? null : getSlices(mLeafContexts);
	}
	
	@Override
	protected IQuery wrapFilter(IQuery query, IFilter filter) {
		return (filter == null) ? query : new FilteredQuery(query, filter);
	}
	
	/**
	 * Expert: Creates an array of leaf slices each holding a subset of the given leaves.
	 * Each {@link LeafSlice} is executed in a single thread. By default there
	 * will be one {@link LeafSlice} per leaf ({@link IAtomicReaderRef}).
	 */
	protected LeafSlice[] getSlices(List<IAtomicReaderRef> leaves) {
		LeafSlice[] slices = new LeafSlice[leaves.size()];
		for (int i = 0; i < slices.length; i++) {
			slices[i] = new LeafSlice(leaves.get(i));
		}
		return slices;
	}
	
	/** 
	 * Expert: Low-level search implementation.  Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null.
	 *
	 * <p>Applications should usually call {@link IndexSearcher#search(Query,int)} or
	 * {@link IndexSearcher#search(Query,Filter,int)} instead.
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	protected ITopDocs search(IWeight weight, IScoreDoc after, int nDocs) throws IOException {
		if (mExecutor == null) 
			return search(mLeafContexts, weight, after, nDocs);
			
		final HitQueue hq = new HitQueue(nDocs, false);
		final Lock lock = new ReentrantLock();
		final ExecutionHelper<TopDocs> runner = new ExecutionHelper<TopDocs>(mExecutor);

		for (int i = 0; i < mLeafSlices.length; i++) { // search each sub
			runner.submit(new SearcherCallableNoSort(lock, this, 
					mLeafSlices[i], (Weight)weight, (ScoreDoc)after, nDocs, hq));
		}

		int totalHits = 0;
		float maxScore = Float.NEGATIVE_INFINITY;
		
		for (final TopDocs topDocs : runner) {
			if (topDocs.getTotalHits() != 0) {
				totalHits += topDocs.getTotalHits();
				maxScore = Math.max(maxScore, topDocs.getMaxScore());
			}
		}

		final IScoreDoc[] scoreDocs = new IScoreDoc[hq.size()];
		for (int i = hq.size() - 1; i >= 0; i--) { // put docs in array
			scoreDocs[i] = hq.pop();
		}

		return new TopDocs(totalHits, scoreDocs, maxScore);
	}
	
	/**
	 * Just like {@link #search(Weight, int, Sort, boolean, boolean)}, but you choose
	 * whether or not the fields in the returned {@link FieldDoc} instances should
	 * be set by specifying fillFields.
	 *
	 * <p>NOTE: this does not compute scores by default.  If you
	 * need scores, create a {@link TopFieldCollector}
	 * instance by calling {@link TopFieldCollector#create} and
	 * then pass that to {@link #search(List, Weight,
	 * Collector)}.</p>
	 */
	@Override
	protected ITopSortDocs search(IWeight weight, FieldDoc after, int nDocs,
			ISort sort, boolean fillFields, boolean doDocScores, boolean doMaxScore) throws IOException {
		if (sort == null) throw new NullPointerException();
    
		if (mExecutor == null) { 
			// use all leaves here!
			return search(mLeafContexts, weight, after, nDocs, sort, 
					fillFields, doDocScores, doMaxScore);
		}
    
		final TopFieldCollector topCollector = TopFieldCollector.create(
				(Sort)sort, nDocs, after, fillFields, doDocScores, doMaxScore, false);

		final Lock lock = new ReentrantLock();
		final ExecutionHelper<TopFieldDocs> runner = new ExecutionHelper<TopFieldDocs>(mExecutor);
		
		for (int i = 0; i < mLeafSlices.length; i++) { // search each leaf slice
			runner.submit(new SearcherCallableWithSort(lock, this, mLeafSlices[i], 
					(Weight)weight, after, nDocs, topCollector, (Sort)sort, doDocScores, doMaxScore));
		}
		
		int totalHits = 0;
		float maxScore = Float.NEGATIVE_INFINITY;
		
		for (final TopFieldDocs topFieldDocs : runner) {
			if (topFieldDocs.getTotalHits() != 0) {
				totalHits += topFieldDocs.getTotalHits();
				maxScore = Math.max(maxScore, topFieldDocs.getMaxScore());
			}
		}

		final TopFieldDocs topDocs = (TopFieldDocs) topCollector.getTopDocs();

		return new TopFieldDocs(totalHits, topDocs.getScoreDocs(), 
				topDocs.getSortFields(), topDocs.getMaxScore());
	}
	
	/**
	 * Just like {@link #search(Weight, int, Sort, boolean, boolean)}, but you choose
	 * whether or not the fields in the returned {@link FieldDoc} instances should
	 * be set by specifying fillFields.
	 */
	@Override
	public ITopSortDocs search(List<IAtomicReaderRef> leaves, IWeight weight, FieldDoc after, 
			int nDocs, ISort sort, boolean fillFields, boolean doDocScores, boolean doMaxScore) 
			throws IOException {
		// single thread
		int limit = mReader.getMaxDoc();
		if (limit == 0) 
			limit = 1;
		
		nDocs = Math.min(nDocs, limit);

		TopFieldCollector collector = TopFieldCollector.create(
				(Sort)sort, nDocs, after, fillFields, doDocScores, doMaxScore, 
				!weight.scoresDocsOutOfOrder());
		
		search(leaves, weight, collector);
		
		return (TopFieldDocs) collector.getTopDocs();
	}
	
	/** 
	 * Expert: Low-level search implementation.  Finds the top <code>n</code>
	 * hits for <code>query</code>.
	 *
	 * <p>Applications should usually call {@link IndexSearcher#search(Query,int)} or
	 * {@link IndexSearcher#search(Query,Filter,int)} instead.
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs search(List<IAtomicReaderRef> leaves, IWeight weight, 
			IScoreDoc after, int nDocs) throws IOException {
		// single thread
		int limit = mReader.getMaxDoc();
		if (limit == 0) 
			limit = 1;
		
		nDocs = Math.min(nDocs, limit);
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				nDocs, (ScoreDoc)after, !weight.scoresDocsOutOfOrder());
		
		search(leaves, weight, collector);
		
		return collector.getTopDocs();
	}
	
	@Override
	public String toString() {
		return "AdvancedIndexSearcher(" + mReader + "; executor=" + mExecutor + ")";
	}
	
}
