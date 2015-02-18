package org.javenstudio.common.indexdb.search;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollectionStatistics;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITermStatistics;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.ITopSortDocs;
import org.javenstudio.common.indexdb.IWeight;

/** 
 * Implements search over a single IndexReader.
 *
 * <p>Applications usually need only call the inherited
 * {@link #search(Query,int)}
 * or {@link #search(Query,Filter,int)} methods. For
 * performance reasons, if your index is unchanging, you
 * should share a single IndexSearcher instance across
 * multiple searches instead of creating a new one
 * per-search.  If your index has changed and you wish to
 * see the changes reflected in searching, you should
 * use {@link DirectoryReader#openIfChanged(DirectoryReader)}
 * to obtain a new reader and
 * then create a new IndexSearcher from that.  Also, for
 * low-latency turnaround it's best to use a near-real-time
 * reader ({@link DirectoryReader#open(IndexWriter,boolean)}).
 * Once you have a new {@link IndexReader}, it's relatively
 * cheap to create a new IndexSearcher from it.
 * 
 * <a name="thread-safety"></a><p><b>NOTE</b>: <code>{@link
 * IndexSearcher}</code> instances are completely
 * thread safe, meaning multiple threads can call any of its
 * methods, concurrently.  If your application requires
 * external synchronization, you should <b>not</b>
 * synchronize on the <code>IndexSearcher</code> instance;
 * use your own (non-Indexdb) objects instead.</p>
 */
public abstract class IndexSearcher implements ISearcher {
	
	protected final IIndexReader mReader; 
  
	// NOTE: these members might change in incompatible ways
	// in the next release
	protected final IIndexReaderRef mReaderContext;
	protected final List<IAtomicReaderRef> mLeafContexts;
  
	/** The Similarity implementation used by this searcher. */
	protected ISimilarity mSimilarity;
	
	/** Creates a searcher searching the provided index. */
	protected IndexSearcher(IIndexReader r, ISimilarity similarity) {
		this(r.getReaderContext(), similarity);
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
	protected IndexSearcher(IIndexReaderRef context, ISimilarity similarity) {
		assert context.isTopLevel(): "IndexSearcher's ReaderContext must be topLevel for reader" 
				+ context.getReader();
		
		mReader = context.getReader();
		mReaderContext = context;
		mLeafContexts = context.getLeaves();
		mSimilarity = similarity;
		
		if (similarity == null)
			throw new NullPointerException("Similarity input null");
	}
  
	/** Return the context for this searches */
	@Override
	public IIndexContext getContext() { 
		return mReader.getContext();
	}
	
	/** Return the {@link IndexReader} this searches. */
	@Override
	public IIndexReader getIndexReader() {
		return mReader;
	}

	public final List<IAtomicReaderRef> getLeafContexts() { 
		return mLeafContexts; 
	}
	
	/** Sugar for <code>.getIndexReader().document(docID)</code> */
	@Override
	public IDocument getDocument(int docID) throws CorruptIndexException, IOException {
		return mReader.getDocument(docID);
	}

	/** Sugar for <code>.getIndexReader().document(docID, fieldVisitor)</code> */
	@Override
	public void document(int docID, IFieldVisitor fieldVisitor) 
			throws CorruptIndexException, IOException {
		mReader.document(docID, fieldVisitor);
	}

	/** Sugar for <code>.getIndexReader().document(docID, fieldsToLoad)</code> */
	@Override
	public IDocument getDocument(int docID, Set<String> fieldsToLoad) 
			throws CorruptIndexException, IOException {
		return mReader.getDocument(docID, fieldsToLoad);
	}

	/** Expert: Set the Similarity implementation used by this Searcher. */
	@Override
	public void setSimilarity(ISimilarity similarity) {
		mSimilarity = similarity;
		
		if (similarity == null)
			throw new NullPointerException("Similarity input null");
	}

	public ISimilarity getSimilarity() {
		return mSimilarity;
	}
  
	protected abstract IQuery wrapFilter(IQuery query, IFilter filter);

	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, int n) throws IOException {
		return search(createNormalizedWeight(query), after, n);
	}
  
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null,
	 * where all results are after a previous result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, IFilter filter, int n) throws IOException {
		return search(createNormalizedWeight(wrapFilter(query, filter)), after, n);
	}
  
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs search(IQuery query, int n) throws IOException {
		return search(query, null, n);
	}

	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs search(IQuery query, IFilter filter, int n) throws IOException {
		return search(createNormalizedWeight(wrapFilter(query, filter)), null, n);
	}

	/** 
	 * Lower-level search API.
	 *
	 * <p>{@link Collector#collect(int)} is called for every matching
	 * document.
	 *
	 * @param query to match documents
	 * @param filter if non-null, used to permit documents to be collected.
	 * @param results to receive hits
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public void search(IQuery query, IFilter filter, ICollector results) throws IOException {
		search(mLeafContexts, createNormalizedWeight(wrapFilter(query, filter)), results);
	}

	/** 
	 * Lower-level search API.
	 *
	 * <p>{@link Collector#collect(int)} is called for every matching document.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public void search(IQuery query, ICollector results) throws IOException {
		search(mLeafContexts, createNormalizedWeight(query), results);
	}
  
	/** 
	 * Search implementation with arbitrary sorting.  Finds
	 * the top <code>n</code> hits for <code>query</code>, applying
	 * <code>filter</code> if non-null, and sorting the hits by the criteria in
	 * <code>sort</code>.
	 * 
	 * <p>NOTE: this does not compute scores by default; use
	 * {@link IndexSearcher#search(Query,Filter,int,Sort,boolean,boolean)} to
	 * control scoring.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopSortDocs search(IQuery query, IFilter filter, int n, ISort sort) throws IOException {
		return search(createNormalizedWeight(wrapFilter(query, filter)), n, sort, false, false);
	}

	/** 
	 * Search implementation with arbitrary sorting, plus
	 * control over whether hit scores and max score
	 * should be computed.  Finds
	 * the top <code>n</code> hits for <code>query</code>, applying
	 * <code>filter</code> if non-null, and sorting the hits by the criteria in
	 * <code>sort</code>.  If <code>doDocScores</code> is <code>true</code>
	 * then the score of each hit will be computed and
	 * returned.  If <code>doMaxScore</code> is
	 * <code>true</code> then the maximum score over all
	 * collected hits will be computed.
	 * 
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopSortDocs search(IQuery query, IFilter filter, int n, ISort sort, 
			boolean doDocScores, boolean doMaxScore) throws IOException {
		return search(createNormalizedWeight(wrapFilter(query, filter)), 
				n, sort, doDocScores, doMaxScore);
	}

	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null,
	 * where all results are after a previous result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, IFilter filter, 
			int n, ISort sort) throws IOException {
		if (after != null && !(after instanceof FieldDoc)) {
			// TODO: if we fix type safety of TopFieldDocs we can
			// remove this
			throw new IllegalArgumentException("after must be a FieldDoc; got " + after);
		}
		return search(createNormalizedWeight(wrapFilter(query, filter)), 
				(FieldDoc) after, n, sort, true, false, false);
	}

	/**
	 * Search implementation with arbitrary sorting and no filter.
	 * @param query The query to search for
	 * @param n Return only the top n results
	 * @param sort The {@link Sort} object
	 * @return The top docs, sorted according to the supplied {@link Sort} instance
	 * @throws IOException
	 */
	@Override
	public ITopSortDocs search(IQuery query, int n, ISort sort) throws IOException {
		return search(createNormalizedWeight(query), n, sort, false, false);
	}

	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, int n, ISort sort) throws IOException {
		if (after != null && !(after instanceof FieldDoc)) {
			// TODO: if we fix type safety of TopFieldDocs we can
			// remove this
			throw new IllegalArgumentException("after must be a FieldDoc; got " + after);
		}
		return search(createNormalizedWeight(query), (FieldDoc) after, n, sort, true, false, false);
	}

	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>), allowing control over
	 * whether hit scores and max score should be computed.
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.  If <code>doDocScores</code> is <code>true</code>
	 * then the score of each hit will be computed and
	 * returned.  If <code>doMaxScore</code> is
	 * <code>true</code> then the maximum score over all
	 * collected hits will be computed.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	@Override
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, IFilter filter, 
			int n, ISort sort, boolean doDocScores, boolean doMaxScore) throws IOException {
		if (after != null && !(after instanceof FieldDoc)) {
			// TODO: if we fix type safety of TopFieldDocs we can
			// remove this
			throw new IllegalArgumentException("after must be a FieldDoc; got " + after);
		}
		return search(createNormalizedWeight(wrapFilter(query, filter)), 
				(FieldDoc) after, n, sort, true, doDocScores, doMaxScore);
	}

	/** 
	 * Expert: Low-level search implementation.  Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null.
	 *
	 * <p>Applications should usually call {@link IndexSearcher#search(Query,int)} or
	 * {@link IndexSearcher#search(Query,Filter,int)} instead.
	 * @throws BooleanQuery.TooManyClauses
	 */
	protected abstract ITopDocs search(IWeight weight, IScoreDoc after, int nDocs) 
			throws IOException;

	/** 
	 * Expert: Low-level search implementation.  Finds the top <code>n</code>
	 * hits for <code>query</code>.
	 *
	 * <p>Applications should usually call {@link IndexSearcher#search(Query,int)} or
	 * {@link IndexSearcher#search(Query,Filter,int)} instead.
	 * @throws BooleanQuery.TooManyClauses
	 */
	public abstract ITopDocs search(List<IAtomicReaderRef> leaves, IWeight weight, 
			IScoreDoc after, int nDocs) throws IOException;

	/** 
	 * Expert: Low-level search implementation with arbitrary
	 * sorting and control over whether hit scores and max
	 * score should be computed.  Finds
	 * the top <code>n</code> hits for <code>query</code> and sorting the hits
	 * by the criteria in <code>sort</code>.
	 *
	 * <p>Applications should usually call {@link
	 * IndexSearcher#search(Query,Filter,int,Sort)} instead.
	 * 
	 * @throws BooleanQuery.TooManyClauses
	 */
	protected ITopSortDocs search(IWeight weight, final int nDocs, ISort sort, 
			boolean doDocScores, boolean doMaxScore) throws IOException {
		return search(weight, null, nDocs, sort, true, doDocScores, doMaxScore);
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
	protected abstract ITopSortDocs search(IWeight weight, FieldDoc after, int nDocs,
			ISort sort, boolean fillFields, boolean doDocScores, boolean doMaxScore) 
			throws IOException;
  
	/**
	 * Just like {@link #search(Weight, int, Sort, boolean, boolean)}, but you choose
	 * whether or not the fields in the returned {@link FieldDoc} instances should
	 * be set by specifying fillFields.
	 */
	public abstract ITopSortDocs search(List<IAtomicReaderRef> leaves, IWeight weight, FieldDoc after, 
			int nDocs, ISort sort, boolean fillFields, boolean doDocScores, boolean doMaxScore) 
			throws IOException;

	/**
	 * Lower-level search API.
	 * 
	 * <p>
	 * {@link Collector#collect(int)} is called for every document. <br>
	 * 
	 * <p>
	 * NOTE: this method executes the searches on all given leaves exclusively.
	 * To search across all the searchers leaves use {@link #leafContexts}.
	 * 
	 * @param leaves 
	 *          the searchers leaves to execute the searches on
	 * @param weight
	 *          to match documents
	 * @param collector
	 *          to receive hits
	 * @throws BooleanQuery.TooManyClauses
	 */
	protected void search(List<IAtomicReaderRef> leaves, IWeight weight, 
			ICollector collector) throws IOException {
		// TODO: should we make this
		// threaded...?  the Collector could be sync'd?
		// always use single thread:
		for (IAtomicReaderRef ctx : leaves) { // search each subreader
			collector.setNextReader(ctx);
			IScorer scorer = weight.getScorer(ctx, !collector.acceptsDocsOutOfOrder(), true, 
					ctx.getReader().getLiveDocs());
			if (scorer != null) 
				scorer.score(collector);
		}
	}

	/** 
	 * Expert: called to re-write queries into primitive queries.
	 * @throws BooleanQuery.TooManyClauses
	 */
	public IQuery rewrite(IQuery original) throws IOException {
		IQuery query = original;
		for (IQuery rewrittenQuery = query.rewrite(mReader); rewrittenQuery != query; 
			rewrittenQuery = query.rewrite(mReader)) {
			query = rewrittenQuery;
		}
		return query;
	}

	/** 
	 * Returns an Explanation that describes how <code>doc</code> scored against
	 * <code>query</code>.
	 *
	 * <p>This is intended to be used in developing Similarity implementations,
	 * and, for good performance, should not be displayed with every hit.
	 * Computing an explanation is as expensive as executing the query over the
	 * entire index.
	 */
	@Override
	public IExplanation explain(IQuery query, int doc) throws IOException {
		return explain(createNormalizedWeight(query), doc);
	}

	/** 
	 * Expert: low-level implementation method
	 * Returns an Explanation that describes how <code>doc</code> scored against
	 * <code>weight</code>.
	 *
	 * <p>This is intended to be used in developing Similarity implementations,
	 * and, for good performance, should not be displayed with every hit.
	 * Computing an explanation is as expensive as executing the query over the
	 * entire index.
	 * <p>Applications should call {@link IndexSearcher#explain(Query, int)}.
	 * @throws BooleanQuery.TooManyClauses
	 */
	protected IExplanation explain(IWeight weight, int doc) throws IOException {
		final int n = getContext().findReaderIndex(doc, mLeafContexts);
		final IAtomicReaderRef ctx = mLeafContexts.get(n);
		final int deBasedDoc = doc - ctx.getDocBase();
    
		return weight.explain(ctx, deBasedDoc);
	}

	/**
	 * Creates a normalized weight for a top-level {@link Query}.
	 * The query is rewritten by this method and {@link Query#createWeight} called,
	 * afterwards the {@link Weight} is normalized. The returned {@code Weight}
	 * can then directly be used to get a {@link Scorer}.
	 */
	@Override
	public IWeight createNormalizedWeight(IQuery query) throws IOException {
		query = rewrite(query);
		
		IWeight weight = query.createWeight(this);
		
		float v = weight.getValueForNormalization();
		float norm = getSimilarity().queryNorm(v);
		if (Float.isInfinite(norm) || Float.isNaN(norm)) 
			norm = 1.0f;
		
		weight.normalize(norm, 1.0f);
		
		return weight;
	}
  
	/**
	 * Returns this searchers the top-level {@link IIndexReaderRef}.
	 * @see IndexReader#getTopReaderContext() 
	 * sugar for #getReader().getTopReaderContext() 
	 */
	@Override
	public IIndexReaderRef getTopReaderContext() {
		return mReaderContext;
	}
  
	/**
	 * Returns {@link TermStatistics} for a term.
	 * 
	 * This can be overridden for example, to return a term's statistics
	 * across a distributed collection.
	 */
	@Override
	public ITermStatistics getTermStatistics(ITerm term, ITermContext context) throws IOException {
		return new TermStatistics(term.getBytes(), context.getDocFreq(), context.getTotalTermFreq());
	}
  
	/**
	 * Returns {@link CollectionStatistics} for a field.
	 * 
	 * This can be overridden for example, to return a field's statistics
	 * across a distributed collection.
	 */
	@Override
	public ICollectionStatistics getCollectionStatistics(String field) throws IOException {
		final int docCount;
		final long sumTotalTermFreq;
		final long sumDocFreq;

		assert field != null;
    
		ITerms terms = getContext().getMultiFieldsTerms(mReader, field);
		if (terms == null) {
			docCount = 0;
			sumTotalTermFreq = 0;
			sumDocFreq = 0;
		} else {
			docCount = terms.getDocCount();
			sumTotalTermFreq = terms.getSumTotalTermFreq();
			sumDocFreq = terms.getSumDocFreq();
		}
		
		return new CollectionStatistics(field, 
				mReader.getMaxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
	}
  
	@Override
	public String toString() {
		return "IndexSearcher(" + mReader + ")";
	}
  
}
