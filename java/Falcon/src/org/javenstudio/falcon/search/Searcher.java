package org.javenstudio.falcon.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.ITopDocs;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.hornet.index.segment.SlowCompositeReaderWrapper;
import org.javenstudio.hornet.index.term.MultiDocsEnum;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.collector.TimeExceededException;
import org.javenstudio.hornet.search.collector.TimeLimitingCollector;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.falcon.search.cache.SearchCache;
import org.javenstudio.falcon.search.cache.SearchCacheConfig;
import org.javenstudio.falcon.search.filter.IndexFilter;
import org.javenstudio.falcon.search.filter.PostFilter;
import org.javenstudio.falcon.search.filter.ProcessedFilter;
import org.javenstudio.falcon.search.hits.BitDocSet;
import org.javenstudio.falcon.search.hits.DelegatingCollector;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.DocSetCollector;
import org.javenstudio.falcon.search.hits.DocSetDelegateCollector;
import org.javenstudio.falcon.search.hits.DocSlice;
import org.javenstudio.falcon.search.hits.DocsEnumState;
import org.javenstudio.falcon.search.hits.QueryCommand;
import org.javenstudio.falcon.search.hits.QueryResult;
import org.javenstudio.falcon.search.hits.QueryResultKey;
import org.javenstudio.falcon.search.hits.SetNonLazyFieldSelector;
import org.javenstudio.falcon.search.hits.SortedIntDocSet;
import org.javenstudio.falcon.search.hits.UnInvertedField;
import org.javenstudio.falcon.search.query.ExtendedQuery;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.query.WrappedQuery;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.store.DirectoryFactory;

public class Searcher implements InfoMBean {
	static final Logger LOG = Logger.getLogger(Searcher.class);

	public static final int NO_CHECK_QCACHE       = 0x80000000;
	public static final int GET_DOCSET            = 0x40000000;
	public static final int NO_CHECK_FILTERCACHE  = 0x20000000;
	public static final int NO_SET_QCACHE         = 0x10000000;
	
	// get the documents actually returned in a response
	public static final int GET_DOCLIST           = 0x02; 
	public static final int GET_SCORES            = 0x01;
	
	@SuppressWarnings("rawtypes")
	private static final SearchCache[] NO_CACHES = new SearchCache[0];
	@SuppressWarnings("rawtypes")
	private static final Map<String, SearchCache> NO_GENERIC_CACHES = 
			new HashMap<String, SearchCache>();
	
	// These should *only* be used for debugging or monitoring purposes
	//private static final AtomicLong sNumOpens = new AtomicLong();
	private static final AtomicLong sNumCloses = new AtomicLong();
	private static final AtomicLong sNumCounter = new AtomicLong();
	
	private static Query sMatchAllDocsQuery = new MatchAllDocsQuery();
	
	private static Comparator<IQuery> sSortByCost = new Comparator<IQuery>() {
		@Override
		public int compare(IQuery q1, IQuery q2) {
			return ((ExtendedQuery)q1).getCost() - ((ExtendedQuery)q2).getCost();
		}
	};
	
	private final SearchControl mControl;
	private final DirectoryFactory mDirectoryFactory;
	private final IDirectoryReader mDirectoryReader;
	private final IAtomicReader mAtomicReader;
	private final IndexSearcher mSearcher;
	private final IndexSchema mSchema;
	private final String mName;
	
	private final SearchCache<IQuery,DocSet> mFilterCache;
	private final SearchCache<QueryResultKey,DocList> mQueryResultCache;
	private final SearchCache<Integer,IDocument> mDocumentCache;
	private final SearchCache<String,UnInvertedField> mFieldValueCache;
	
	// list of all caches associated with this searcher.
	@SuppressWarnings("rawtypes")
	private final SearchCache[] mCacheList;
	
	// map of generic caches - not synchronized since it's read-only after the constructor.
	@SuppressWarnings("rawtypes")
	private final Map<String, SearchCache> mCacheMap;
	
	private final boolean mUseFilterForSortedQuery;
	private final boolean mEnableLazyFieldLoading;
	private final boolean mEnableCache;
	private final boolean mCloseReader;
	
	private final int mQueryResultWindowSize;
	private final int mQueryResultMaxDocsCached;
	
	private long mOpenTime = System.currentTimeMillis();
	private long mWarmupTime = 0;
	private long mRegisterTime = 0;
	private boolean mClosed = false;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	Searcher(SearchControl control, DirectoryFactory factory, 
			IndexSchema schema, IDirectoryReader reader, boolean closeReader, 
			boolean enableCache, boolean reserveDirectory, boolean realtime) 
			throws ErrorException {
		mControl = control;
		mDirectoryFactory = factory;
		mSchema = schema;
		mEnableCache = enableCache;
		mCloseReader = closeReader;
		
		try { 
			mName = "Searcher-" + sNumCounter.incrementAndGet() + "@" 
					+ control.getSearchCore().getName() + "/" + Thread.currentThread().getName() 
					+ (realtime ? "/realtime" : "");
			
			LOG.info("Opening " + mName);
			
			final ISearchConfig conf = control.getSearchCore().getSearchConfig();
			
			mEnableLazyFieldLoading = conf.isEnableLazyFieldLoading();
			mUseFilterForSortedQuery = conf.useFilterForSortedQuery();
			mQueryResultWindowSize = conf.getQueryResultWindowSize();
			mQueryResultMaxDocsCached = conf.getQueryResultMaxDocsCached();
					
			mDirectoryReader = reader;
			mAtomicReader = SlowCompositeReaderWrapper.wrap(reader);
			mSearcher = SearchHelper.createSearcher(reader);
			mSearcher.setSimilarity(mSchema.getSimilarity());
			
			if (mEnableCache) { 
				List<SearchCache> clist = new ArrayList<SearchCache>();
				
				mFieldValueCache = (SearchCache<String,UnInvertedField>) 
						newCacheInstance(clist, conf.getFieldValueCacheConfig());
				
				mFilterCache = (SearchCache<IQuery,DocSet>) 
						newCacheInstance(clist, conf.getFilterCacheConfig());
				
				mQueryResultCache = (SearchCache<QueryResultKey,DocList>) 
						newCacheInstance(clist, conf.getQueryResultCacheConfig());
				
				mDocumentCache = (SearchCache<Integer,IDocument>) 
						newCacheInstance(clist, conf.getDocumentCacheConfig());
				
				SearchCacheConfig[] cacheConfs = conf.getUserCacheConfigs();
				if (cacheConfs != null) { 
					mCacheMap = new HashMap<String, SearchCache>(cacheConfs.length);
					for (SearchCacheConfig cacheConf : cacheConfs) { 
						SearchCache cache = newCacheInstance(clist, cacheConf); 
						if (cache != null) 
							mCacheMap.put(cache.getName(), cache);
					}
				} else 
					mCacheMap = NO_GENERIC_CACHES;
				
				mCacheList = clist.toArray(new SearchCache[clist.size()]);
				
			} else { 
				mCacheList = NO_CACHES;
				mCacheMap = NO_GENERIC_CACHES;
				
				mFieldValueCache = null;
				mFilterCache = null; 
				mQueryResultCache = null;
				mDocumentCache = null;
			}
			
		} catch (IOException ex) { 
			mClosed = true;
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
		control.getSearchCore().registerInfoMBean(this);
	}
	
	@SuppressWarnings("rawtypes")
	private SearchCache newCacheInstance(List<SearchCache> clist, 
			SearchCacheConfig conf) throws ErrorException { 
		SearchCache cache = (conf != null) ? conf.newInstance(this) : null;
		if (cache != null) 
			clist.add(cache);
		
		return cache;
	}
	
	@Override
	public String toString() { return getName(); }
	
	public String getName() { return mName; }
	
	public ISearchCore getSearchCore() { return mControl.getSearchCore(); }
	public IndexSchema getSchema() { return mSchema; }
	
	public IDirectoryReader getDirectoryReader() { 
		return mDirectoryReader; //(IDirectoryReader)mSearcher.getIndexReader();
	}
	
	public IIndexReader getIndexReader() { 
		return mSearcher.getIndexReader();
	}
	
	public final IAtomicReader getAtomicReader() {
		return mAtomicReader;
	}
	
	public IIndexReaderRef getTopReaderContext() {
		return mSearcher.getTopReaderContext();
	}
	
	public int getMaxDoc() { 
		return mDirectoryReader.getMaxDoc();
	}
	
	public ValueSourceContext createValueSourceContext() { 
		return ValueSourceContext.create(mSearcher);
	}
	
	public void createValueSourceWeight(ValueSourceContext context, 
			ValueSource source) throws ErrorException { 
		try {
			source.createWeight(context, mSearcher);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public SearchCache<String,UnInvertedField> getFieldValueCache() {
		return mFieldValueCache;
	}
	
	public boolean isEnableLazyFieldLoading() { 
		return mEnableLazyFieldLoading; 
	}
	
	/**
	 * Warm this searcher based on an old one (primarily for auto-cache warming).
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void warm(Searcher old) throws ErrorException { 
	    // Make sure this is first!  filters can help queryResults execute!
	    long warmingStartTime = System.currentTimeMillis();
	    
	    // warm the caches in order...
	    ModifiableParams params = new ModifiableParams();
	    params.add("warming", "true");
	    
	    for (int i=0; i < mCacheList.length; i++) { 
	    	SearchCache theCache = mCacheList[i]; 
	    	SearchCache oldCache = (i < old.mCacheList.length) ? old.mCacheList[i] : null;
	    	
	    	if (LOG.isDebugEnabled())
	    		LOG.debug("autowarming " + this + " from " + old + "\n\t" + oldCache);
	    	
	    	ISearchRequest req = getSearchCore().createLocalRequest(this, params);
		    ISearchResponse rsp = getSearchCore().createLocalResponse(this, req);
		    SearchRequestInfo.setRequestInfo(new SearchRequestInfo(req, rsp));
		    
		    try { 
		    	theCache.warm(this, oldCache);
		    } finally { 
		    	try { 
		    		req.close();
		    	} finally { 
		    		SearchRequestInfo.clearRequestInfo();
		    	}
		    }
		    
		    if (LOG.isDebugEnabled())
		    	LOG.debug("autowarming result for " + this + "\n\t" + theCache);
	    }
	    
	    mWarmupTime = System.currentTimeMillis() - warmingStartTime;
	}
	
	/** Register sub-objects such as caches */
	@SuppressWarnings("rawtypes")
	public void register() throws ErrorException { 
	    // register self, called at ctor
		//control.getSearchCore().registerInfoMBean(this);
	    
	    for (SearchCache cache : mCacheList) {
	    	cache.setState(SearchCache.State.LIVE);
	    	getSearchCore().registerInfoMBean(cache);
	    }
	    
	    mRegisterTime = System.currentTimeMillis();
	}
	
	/**
	 * Free's resources associated with this searcher.
	 *
	 * In particular, the underlying reader and any cache's in use are closed.
	 */
	@SuppressWarnings("rawtypes")
	public void close() throws ErrorException {
		mClosed = true;
		
		try { 
			getSearchCore().removeInfoMBean(this);
	
		    // super.close();
		    // can't use super.close() since it just calls reader.close() 
			// and that may only be called once
		    // per reader (even if incRef() was previously called).
		    if (mCloseReader) 
		    	mDirectoryReader.decreaseRef();
	
		    for (SearchCache cache : mCacheList) {
		    	getSearchCore().removeInfoMBean(cache);
		    	cache.close();
		    }
	
		    mDirectoryFactory.release(getIndexReader().getDirectory());
		   
		    // do this at the end so it only gets done if there are no exceptions
		    sNumCloses.incrementAndGet();
		    
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	/** 
	 * Visit a document's fields using a {@link StoredFieldVisitor}
	 *  This method does not currently use the document cache.
	 * 
	 * @see IndexReader#document(int, StoredFieldVisitor) 
	 */
	public void document(int docID, IFieldVisitor fieldVisitor) 
			throws ErrorException { 
		try { 
			mSearcher.document(docID, fieldVisitor);
		} catch (Throwable ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	/**
	 * Retrieve the {@link Document} instance corresponding to the document id.
	 *
	 * Note: The document will have all fields accessable, but if a field
	 * filter is provided, only the provided fields will be loaded (the 
	 * remainder will be available lazily).
	 */
	public IDocument getDocument(int i, Set<String> fields) throws ErrorException {
		IDocument d;
		
		try {
			if (mDocumentCache != null) {
				d = mDocumentCache.get(i);
				if (d != null) 
					return d;
			}
	
			if (!mEnableLazyFieldLoading || fields == null) {
				d = getIndexReader().getDocument(i);
				
			} else {
				final SetNonLazyFieldSelector visitor = 
						new SetNonLazyFieldSelector(fields, getIndexReader(), i);
				
				getIndexReader().document(i, visitor);
				d = visitor.getDocument();
			}
	
			if (mDocumentCache != null) 
				mDocumentCache.put(i, d);
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}

		return d;
	}
	
	public void search(IQuery query, IFilter filter, ICollector results) 
			throws ErrorException {
		try { 
			mSearcher.search(query, filter, results);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	public IExplanation explain(IQuery query, int doc) throws ErrorException { 
		try { 
			return mSearcher.explain(query, doc);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	/** Returns a weighted sort according to this searcher */
	public ISort weightSort(ISort sort) throws ErrorException {
		try {
			return (sort != null) ? sort.rewrite(mSearcher) : null;
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	public QueryResult search(QueryResult qr, QueryCommand cmd) 
			throws ErrorException { 
		getDocListC(qr, cmd); 
		return qr;
	}
  
	public IDocument getDoc(int doc) throws ErrorException { 
		return getDoc(doc, (Set<String>)null);
	}
  
	/**
	 * Retrieve the {@link Document} instance corresponding to the document id.
	 *
	 * Note: The document will have all fields accessable, but if a field
	 * filter is provided, only the provided fields will be loaded (the 
	 * remainder will be available lazily).
	 */
	public IDocument getDoc(int i, Set<String> fields) throws ErrorException {
		IDocument d;
	    if (mDocumentCache != null) {
	    	d = mDocumentCache.get(i);
	    	if (d != null) 
	    		return d;
	    }

	    try { 
		    if (!mEnableLazyFieldLoading || fields == null) {
		    	d = getIndexReader().getDocument(i);
		    	
		    } else {
		    	final SetNonLazyFieldSelector visitor = 
		    			new SetNonLazyFieldSelector(fields, getIndexReader(), i);
		    	
		    	getIndexReader().document(i, visitor);
		    	d = visitor.getDocument();
		    }
	    } catch (IOException ex) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    }

	    if (mDocumentCache != null) 
	    	mDocumentCache.put(i, d);

	    return d;
	}
  
	/**
	 * Returns documents matching both <code>query</code> and <code>filter</code>
	 * and sorted by <code>sort</code>.
	 * <p>
	 * This method is cache aware and may retrieve <code>filter</code> from
	 * the cache or make an insertion into the cache as a result of this call.
	 * <p>
	 * FUTURE: The returned DocList may be retrieved from a cache.
	 *
	 * @param filter   may be null
	 * @param lsort    criteria by which to sort (if null, query relevance is used)
	 * @param offset   offset into the list of documents to return
	 * @param len      maximum number of documents to return
	 * @return DocList meeting the specified criteria, should <b>not</b> be modified by the caller.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public DocList getDocList(IQuery query, IQuery filter, ISort lsort, 
			int offset, int len) throws ErrorException {
		QueryCommand qc = new QueryCommand();
	    qc.setQuery(query)
	      	.setFilterList(filter)
	      	.setSort(lsort)
	      	.setOffset(offset)
	      	.setLength(len);
	    
	    QueryResult qr = new QueryResult();
	    search(qr, qc);
	    
	    return qr.getDocList();
	}
  
	/**
	 * Returns documents matching both <code>query</code> and <code>filter</code>
	 * and sorted by <code>sort</code>.
	 * FUTURE: The returned DocList may be retrieved from a cache.
	 *
	 * @param filter   may be null
	 * @param lsort    criteria by which to sort (if null, query relevance is used)
	 * @param offset   offset into the list of documents to return
	 * @param len      maximum number of documents to return
	 * @return DocList meeting the specified criteria, should <b>not</b> be modified by the caller.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public DocList getDocList(IQuery query, DocSet filter, ISort lsort, 
			int offset, int len) throws ErrorException {
	    QueryCommand qc = new QueryCommand();
	    qc.setQuery(query)
	    	.setFilter(filter)
	      	.setSort(lsort)
	      	.setOffset(offset)
	      	.setLength(len);
	    
	    QueryResult qr = new QueryResult();
	    search(qr, qc);
	    
	    return qr.getDocList();
	}
  
	/**
	 * Returns documents matching both <code>query</code> and the 
	 * intersection of the <code>filterList</code>, sorted by <code>sort</code>.
	 * <p>
	 * This method is cache aware and may retrieve <code>filter</code> from
	 * the cache or make an insertion into the cache as a result of this call.
	 * <p>
	 * FUTURE: The returned DocList may be retrieved from a cache.
	 *
	 * @param filterList may be null
	 * @param lsort    criteria by which to sort (if null, query relevance is used)
	 * @param offset   offset into the list of documents to return
	 * @param len      maximum number of documents to return
	 * @return DocList meeting the specified criteria, should <b>not</b> be modified by the caller.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public DocList getDocList(IQuery query, List<IQuery> filterList, ISort lsort, 
			int offset, int len, int flags) throws ErrorException { 
		QueryCommand qc = new QueryCommand();
	    qc.setQuery(query)
	      	.setFilterList(filterList)
	      	.setSort(lsort)
	      	.setOffset(offset)
	      	.setLength(len)
	      	.setFlags(flags);
	    
	    QueryResult qr = new QueryResult();
	    search(qr, qc);
	    
	    return qr.getDocList();
	}
  
	/**
	 * Returns the set of document ids matching a query.
	 * This method is cache-aware and attempts to retrieve the answer from the cache if possible.
	 * If the answer was not cached, it may have been inserted into the cache as a result of this call.
	 * This method can handle negative queries.
	 * <p>
	 * The DocSet returned should <b>not</b> be modified.
	 */
	public DocSet getDocSet(IQuery query) throws ErrorException {
		if (query instanceof ExtendedQuery) {
			ExtendedQuery eq = (ExtendedQuery)query;
			if (!eq.getCache()) {
				if (query instanceof WrappedQuery) 
					query = ((WrappedQuery)query).getWrappedQuery();
				
				query = QueryUtils.makeQueryable(query);
				return getDocSetNC(query, null);
			}
		}

		// Get the absolute value (positive version) of this query.  If we
		// get back the same reference, we know it's positive.
		IQuery absQ = QueryUtils.getAbs(query);
		boolean positive = (query == absQ);

		if (mFilterCache != null) {
			DocSet absAnswer = mFilterCache.get(absQ);
			if (absAnswer!=null) {
				if (positive) 
					return absAnswer;
				else 
					return getPositiveDocSet(sMatchAllDocsQuery).andNot(absAnswer);
			}
		}

		DocSet absAnswer = getDocSetNC(absQ, null);
		DocSet answer = positive ? absAnswer : 
			getPositiveDocSet(sMatchAllDocsQuery).andNot(absAnswer);

		if (mFilterCache != null) {
			// cache negative queries as positive
			mFilterCache.put(absQ, absAnswer);
		}

		return answer;
	}
	
	/**
	 * Returns the set of document ids matching all queries.
	 * This method is cache-aware and attempts to retrieve the answer from the cache if possible.
	 * If the answer was not cached, it may have been inserted into the cache as a result of this call.
	 * This method can handle negative queries.
	 * <p>
	 * The DocSet returned should <b>not</b> be modified.
	 */
	public DocSet getDocSet(List<IQuery> queries) throws ErrorException {
	    ProcessedFilter pf = getProcessedFilter(null, queries);
	    if (pf.getAnswer() != null) 
	    	return pf.getAnswer();

	    DocSetCollector setCollector = new DocSetCollector(getMaxDoc()>>6, getMaxDoc());
	    Collector collector = setCollector;
	    
	    if (pf.getPostFilter() != null) {
	    	pf.getPostFilter().setLastDelegate(collector);
	    	collector = pf.getPostFilter();
	    }

	    for (final IAtomicReaderRef leaf : mSearcher.getLeafContexts()) {
	    	final IAtomicReader reader = leaf.getReader();
	    	// TODO: the filter may already only have liveDocs...
	    	final Bits liveDocs = reader.getLiveDocs(); 
	    	
	    	try { 
		    	IDocIdSet idSet = null;
		    	if (pf.getFilter() != null) {
		    		idSet = pf.getFilter().getDocIdSet(leaf, liveDocs);
		    		if (idSet == null) 
		    			continue;
		    	}
		    	
		    	IDocIdSetIterator idIter = null;
		    	if (idSet != null) {
		    		idIter = idSet.iterator();
		    		if (idIter == null) 
		    			continue;
		    	}
	
		    	collector.setNextReader(leaf);
		    	int max = reader.getMaxDoc();
	
		    	if (idIter == null) {
		    		for (int docid = 0; docid < max; docid++) {
		    			if (liveDocs != null && !liveDocs.get(docid)) 
		    				continue;
		    			
		    			collector.collect(docid);
		    		}
		    	} else {
		    		for (int docid = -1; (docid = idIter.advance(docid+1)) < max; ) {
		    			collector.collect(docid);
		    		}
		    	}
		    	
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    	}
	    }

	    return setCollector.getDocSet();
	}
  
	public DocSet getDocSet(DocsEnumState deState) throws ErrorException {
		DocSet result = null;
		
		try {
		    int largestPossible = deState.getTermsEnum().getDocFreq();
		    boolean useCache = mFilterCache != null && largestPossible >= deState.getMinSetSizeCached();
		    TermQuery key = null;
	
		    if (useCache) {
		    	key = new TermQuery(new Term(deState.getFieldName(), 
		    			BytesRef.deepCopyOf(deState.getTermsEnum().getTerm())));
		    	
		    	result = mFilterCache.get(key);
		    	if (result != null) 
		    		return result;
		    }
	
		    int smallSetSize = getMaxDoc()>>6;
		    int scratchSize = Math.min(smallSetSize, largestPossible);
		    
		    if (deState.getScratches() == null || deState.getScratches().length < scratchSize)
		    	deState.setScratches(new int[scratchSize]);
	
		    final int[] docs = deState.getScratches();
		    OpenBitSet obs = null;
		    int upto = 0;
		    int bitsSet = 0;
		    
		    IDocsEnum docsEnum = deState.getTermsEnum().getDocs(
		    		deState.getLiveDocs(), deState.getDocsEnum(), 0);
		    
		    if (deState.getDocsEnum() == null) 
		    	deState.setDocsEnum(docsEnum);
		    
		    if (docsEnum instanceof MultiDocsEnum) {
		    	MultiDocsEnum.EnumWithSlice[] subs = ((MultiDocsEnum)docsEnum).getSubs();
		    	int numSubs = ((MultiDocsEnum)docsEnum).getNumSubs();
		    	
		    	for (int subindex = 0; subindex < numSubs; subindex++) {
		    		MultiDocsEnum.EnumWithSlice sub = subs[subindex];
		    		if (sub.getDocsEnum() == null) 
		    			continue;
		    		
		    		int base = sub.getSlice().getStart();
		    		int docid;
		        
		    		if (largestPossible > docs.length) {
		    			if (obs == null) 
		    				obs = new OpenBitSet(getMaxDoc());
		    			
		    			while ((docid = sub.getDocsEnum().nextDoc()) != 
		    					IDocIdSetIterator.NO_MORE_DOCS) {
		    				obs.fastSet(docid + base);
		    				bitsSet++;
		    			}
		    		} else {
		    			while ((docid = sub.getDocsEnum().nextDoc()) != 
		    					IDocIdSetIterator.NO_MORE_DOCS) {
		    				docs[upto++] = docid + base;
		    			}
		    		}
		    	}
		    } else {
		    	int docid;
		    	if (largestPossible > docs.length) {
		    		if (obs == null) 
		    			obs = new OpenBitSet(getMaxDoc());
		    		
		    		while ((docid = docsEnum.nextDoc()) != IDocIdSetIterator.NO_MORE_DOCS) {
		    			obs.fastSet(docid);
		    			bitsSet++;
		    		}
		    	} else {
		    		while ((docid = docsEnum.nextDoc()) != IDocIdSetIterator.NO_MORE_DOCS) {
		    			docs[upto++] = docid;
		    		}
		    	}
		    }
	
		    if (obs != null) {
		    	for (int i=0; i < upto; i++) {
		    		obs.fastSet(docs[i]);  
		    	}
		    	
		    	bitsSet += upto;
		    	result = new BitDocSet(obs, bitsSet);
		    	
		    } else {
		    	result = (upto == 0) ? DocSet.EMPTY : 
		    		new SortedIntDocSet(Arrays.copyOf(docs, upto));
		    }
	
		    if (useCache) 
		    	mFilterCache.put(key, result);
		    
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
	    return result;
	}
  
	/**
	 * Returns the set of document ids matching both the query and the filter.
	 * This method is cache-aware and attempts to retrieve the answer from the cache if possible.
	 * If the answer was not cached, it may have been inserted into the cache as a result of this call.
	 * <p>
	 *
	 * @param filter may be null
	 * @return DocSet meeting the specified criteria, should <b>not</b> be modified by the caller.
	 */
	public DocSet getDocSet(IQuery query, DocSet filter) throws ErrorException {
		if (filter == null) 
			return getDocSet(query);

	    if (query instanceof ExtendedQuery) {
	    	ExtendedQuery eq = (ExtendedQuery)query;
	    	if (!eq.getCache()) {
	    		if (query instanceof WrappedQuery) 
	    			query = ((WrappedQuery)query).getWrappedQuery();
	    		
	    		query = QueryUtils.makeQueryable(query);
	    		return getDocSetNC(query, filter);
	    	}
	    }

	    // Negative query if absolute value different from original
	    IQuery absQ = QueryUtils.getAbs(query);
	    boolean positive = (absQ == query);

	    DocSet first;
	    if (mFilterCache != null) {
	    	first = mFilterCache.get(absQ);
	    	if (first == null) {
	    		first = getDocSetNC(absQ,null);
	    		mFilterCache.put(absQ,first);
	    	}
	    	
	    	return positive ? first.intersection(filter) : 
	    		filter.andNot(first);
	    }

	    // If there isn't a cache, then do a single filtered query if positive.
	    return positive ? getDocSetNC(absQ,filter) : 
	    	filter.andNot(getPositiveDocSet(absQ));
	}
  
	/**
	 * Returns documents matching both <code>query</code> and the intersection 
	 * of <code>filterList</code>, sorted by <code>sort</code>.  
	 * Also returns the compete set of documents
	 * matching <code>query</code> and <code>filter</code> 
	 * (regardless of <code>offset</code> and <code>len</code>).
	 * <p>
	 * This method is cache aware and may retrieve <code>filter</code> from
	 * the cache or make an insertion into the cache as a result of this call.
	 * <p>
	 * FUTURE: The returned DocList may be retrieved from a cache.
	 * <p>
	 * The DocList and DocSet returned should <b>not</b> be modified.
	 *
	 * @param filterList   may be null
	 * @param lsort    criteria by which to sort (if null, query relevance is used)
	 * @param offset   offset into the list of documents to return
	 * @param len      maximum number of documents to return
	 * @param flags    user supplied flags for the result set
	 * @return DocListAndSet meeting the specified criteria, should <b>not</b> be modified by the caller.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public DocListAndSet getDocListAndSet(IQuery query, List<IQuery> filterList, 
			ISort lsort, int offset, int len, int flags) throws ErrorException { 
	    QueryCommand qc = new QueryCommand();
	    qc.setQuery(query)
	      	.setFilterList(filterList)
	      	.setSort(lsort)
	      	.setOffset(offset)
	      	.setLength(len)
	      	.setNeedDocSet(true);
	    
	    QueryResult qr = new QueryResult();
	    search(qr, qc);
	    
	    return qr.getDocListAndSet();
	}
  
	/**
	 * Returns the number of documents that match both <code>a</code> and <code>b</code>.
	 * <p>
	 * This method is cache-aware and may check as well as modify the cache.
	 *
	 * @return the number of documents in the intersection between <code>a</code> and <code>b</code>.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public int getNumDocs(IQuery a, IQuery b) throws ErrorException {
	    IQuery absA = QueryUtils.getAbs(a);
	    IQuery absB = QueryUtils.getAbs(b);
	    
	    DocSet positiveA = getPositiveDocSet(absA);
	    DocSet positiveB = getPositiveDocSet(absB);

	    // Negative query if absolute value different from original
	    if (a == absA) {
	    	if (b == absB) 
	    		return positiveA.intersectionSize(positiveB);
	    	
	    	return positiveA.andNotSize(positiveB);
	    }
	    
	    if (b == absB) 
	    	return positiveB.andNotSize(positiveA);

	    // if both negative, we need to create a temp DocSet since we
	    // don't have a counting method that takes three.
	    DocSet all = getPositiveDocSet(sMatchAllDocsQuery);

	    // -a -b == *:*.andNot(a).andNotSize(b) == *.*.andNotSize(a.union(b))
	    // we use the last form since the intermediate DocSet should normally be smaller.
	    return all.andNotSize(positiveA.union(positiveB));
	}
  
	/**
	 * Returns the number of documents that match both <code>a</code> and <code>b</code>.
	 * <p>
	 * This method is cache-aware and may check as well as modify the cache.
	 *
	 * @return the number of documents in the intersection between <code>a</code> and <code>b</code>.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public int getNumDocs(IQuery a, DocSet b) throws ErrorException {
	    // Negative query if absolute value different from original
	    IQuery absQ = QueryUtils.getAbs(a);
	    DocSet positiveA = getPositiveDocSet(absQ);
	    
	    return a == absQ ? b.intersectionSize(positiveA) : b.andNotSize(positiveA);
	}
  
	public int getNumDocs(DocSet a, DocsEnumState deState) throws ErrorException {
	    // Negative query if absolute value different from original
	    return a.intersectionSize(getDocSet(deState));
	}
  
	/**
	 * Returns the first document number containing the term <code>t</code>
	 * Returns -1 if no document was found.
	 * This method is primarily intended for clients that want to fetch
	 * documents using a unique identifier."
	 * @return the first document number containing the term
	 */
	public int getFirstMatch(ITerm t) throws ErrorException {
		try { 
		    IFields fields = mAtomicReader.getFields();
		    if (fields == null) return -1;
		    
		    ITerms terms = fields.getTerms(t.getField());
		    if (terms == null) return -1;
		    
		    BytesRef termBytes = t.getBytes();
		    final ITermsEnum termsEnum = terms.iterator(null);
		    if (!termsEnum.seekExact(termBytes, false)) 
		    	return -1;
		    
		    IDocsEnum docs = termsEnum.getDocs(mAtomicReader.getLiveDocs(), null, 0);
		    if (docs == null) return -1;
		    
		    int id = docs.nextDoc();
		    return id == IDocIdSetIterator.NO_MORE_DOCS ? -1 : id;
		    
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	/** 
	 * lookup the docid by the unique key field, and return the id *within* 
	 * the leaf reader in the low 32 bits, and the index of the leaf reader in the high 32 bits.
	 * -1 is returned if not found.
	 */
	public long lookupId(BytesRef idBytes) throws ErrorException {
		String field = mSchema.getUniqueKeyField().getName();

		for (int i=0, c = mSearcher.getLeafContexts().size(); i<c; i++) {
			final IAtomicReaderRef leaf = mSearcher.getLeafContexts().get(i);
			final IAtomicReader reader = leaf.getReader();

			try { 
				final ITerms terms = reader.getTerms(field);
				if (terms == null) continue;
	      
				ITermsEnum te = terms.iterator(null);
				if (te.seekExact(idBytes, true)) {
					IDocsEnum docs = te.getDocs(reader.getLiveDocs(), null, 0);
					
					int id = docs.nextDoc();
					if (id == IDocIdSetIterator.NO_MORE_DOCS) 
						continue;
					
					assert docs.nextDoc() == IDocIdSetIterator.NO_MORE_DOCS;
	
					return (((long)i) << 32) | id;
				}
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}

		return -1;
	}
	
	// only handle positive (non negative) queries
	protected DocSet getPositiveDocSet(IQuery q) throws ErrorException {
		DocSet answer;
	    if (mFilterCache != null) {
	    	answer = mFilterCache.get(q);
	    	if (answer != null) 
	    		return answer;
	    }
	    
	    answer = getDocSetNC(q, null);
	    if (mFilterCache != null) mFilterCache.put(
	        q,answer);
	    
	    return answer;
	}
	
	public ProcessedFilter getProcessedFilter(DocSet setFilter, 
			List<IQuery> queries) throws ErrorException {
	    ProcessedFilter pf = new ProcessedFilter();
	    if (queries == null || queries.size() == 0) {
	    	if (setFilter != null)
	    		pf.setFilter(setFilter.getTopFilter());
	    	
	    	return pf;
	    }

	    DocSet answer = null;

	    boolean[] neg = new boolean[queries.size()+1];
	    DocSet[] sets = new DocSet[queries.size()+1];
	    
	    List<IQuery> notCached = null;
	    List<IQuery> postFilters = null;

	    int end = 0;
	    int smallestIndex = -1;
	    int smallestCount = Integer.MAX_VALUE;

	    if (setFilter != null) {
	    	answer = sets[end++] = setFilter;
	    	smallestIndex = end;
	    }

	    for (IQuery q : queries) {
	    	if (q instanceof ExtendedQuery) {
	    		ExtendedQuery eq = (ExtendedQuery)q;
	    		
	    		if (!eq.getCache()) {
	    			if (eq.getCost() >= 100 && eq instanceof PostFilter) {
	    				if (postFilters == null) 
	    					postFilters = new ArrayList<IQuery>(sets.length-end);
	    				
	    				postFilters.add(q);
	    				
	    			} else {
	    				if (notCached == null) 
	    					notCached = new ArrayList<IQuery>(sets.length-end);
	    				
	    				notCached.add(q);
	    			}
	    			
	    			continue;
	    		}
	    	}

	    	IQuery posQuery = QueryUtils.getAbs(q);
	    	sets[end] = getPositiveDocSet(posQuery);
	    	
	    	// Negative query if absolute value different from original
	    	if (q == posQuery) {
	    		neg[end] = false;
	    		
	    		// keep track of the smallest positive set.
	    		// This optimization is only worth it if size() is cached, which it would
	    		// be if we don't do any set operations.
	    		int sz = sets[end].size();
	    		
	    		if (sz < smallestCount) {
	    			smallestCount = sz;
	    			smallestIndex = end;
	    			answer = sets[end];
	    		}
	    	} else {
	    		neg[end] = true;
	    	}

	    	end++;
	    }

	    // Are all of our normal cached filters negative?
	    if (end > 0 && answer == null) 
	    	answer = getPositiveDocSet(sMatchAllDocsQuery);

	    // do negative queries first to shrink set size
	    for (int i=0; i < end; i++) {
	    	if (neg[i]) 
	    		answer = answer.andNot(sets[i]);
	    }

	    for (int i=0; i < end; i++) {
	    	if (!neg[i] && i != smallestIndex) 
	    		answer = answer.intersection(sets[i]);
	    }

	    if (notCached != null) {
	    	Collections.sort(notCached, sSortByCost);
	    	List<IWeight> weights = new ArrayList<IWeight>(notCached.size());
	    	
	    	for (IQuery q : notCached) {
	    		IQuery qq = QueryUtils.makeQueryable(q);
	    		
	    		try {
	    			weights.add(mSearcher.createNormalizedWeight(qq));
	    		} catch (IOException ex) { 
	    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		}
	    	}
	    	
	    	pf.setFilter(new IndexFilter(answer, weights));
	    	
	    } else {
	    	if (postFilters == null) {
	    		if (answer == null) 
	    			answer = getPositiveDocSet(sMatchAllDocsQuery);
	    		
	    		// "answer" is the only part of the filter, so set it.
	    		pf.setAnswer(answer);
	    	}

	    	if (answer != null) 
	    		pf.setFilter(answer.getTopFilter());
	    }

	    if (postFilters != null) {
	    	Collections.sort(postFilters, sSortByCost);
	    	
	    	for (int i = postFilters.size()-1; i >= 0; i--) {
	    		DelegatingCollector prev = pf.getPostFilter();
	    		pf.setPostFilter(((PostFilter)postFilters.get(i)).getFilterCollector(mSearcher));
	    		
	    		if (prev != null) 
	    			pf.getPostFilter().setDelegate(prev);
	    	}
	    }

	    return pf;
	}
	
	// query must be positive
	protected DocSet getDocSetNC(IQuery query, DocSet filter) throws ErrorException {
	    DocSetCollector collector = new DocSetCollector(getMaxDoc()>>6, getMaxDoc());

	    try {
		    if (filter == null) {
		    	if (query instanceof TermQuery) {
		    		ITerm t = ((TermQuery)query).getTerm();
		    		
		    		for (final IAtomicReaderRef leaf : mSearcher.getLeafContexts()) {
		    			final IAtomicReader reader = leaf.getReader();
		    			collector.setNextReader(leaf);
		    			
		    			IFields fields = reader.getFields();
		    			ITerms terms = fields.getTerms(t.getField());
		    			BytesRef termBytes = t.getBytes();
		          
		    			Bits liveDocs = reader.getLiveDocs();
		    			IDocsEnum docsEnum = null;
		    			
		    			if (terms != null) {
		    				final ITermsEnum termsEnum = terms.iterator(null);
		    				if (termsEnum.seekExact(termBytes, false)) 
		    					docsEnum = termsEnum.getDocs(liveDocs, null, 0);
		    			}
	
		    			if (docsEnum != null) {
		    				int docid;
		    				while ((docid = docsEnum.nextDoc()) != IDocIdSetIterator.NO_MORE_DOCS) {
		    					collector.collect(docid);
		    				}
		    			}
		    		}
		    	} else {
		    		mSearcher.search(query, null, collector);
		    	}
		    	
		    } else {
		    	IFilter indexFilter = filter.getTopFilter();
		    	mSearcher.search(query, indexFilter, collector);
		    }
		    
	    } catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
	    
	    return collector.getDocSet();
	}
	
	/**
	 * getDocList version that uses+populates query and filter caches.
	 * In the event of a timeout, the cache is not populated.
	 */
	private void getDocListC(QueryResult qr, QueryCommand cmd) throws ErrorException {
	    DocListAndSet out = new DocListAndSet();
	    QueryResultKey key = null;
	    qr.setDocListAndSet(out);
	    
	    int maxDocRequested = cmd.getOffset() + cmd.getLength();
	    // check for overflow, and check for # docs in index
	    if (maxDocRequested < 0 || maxDocRequested > getMaxDoc()) 
	    	maxDocRequested = getMaxDoc();
	    
	    int supersetMaxDoc = maxDocRequested;
	    int flags = cmd.getFlags();
	    
	    DocList superset = null;
	    IQuery q = cmd.getQuery();
	    
	    if (q instanceof ExtendedQuery) {
	    	ExtendedQuery eq = (ExtendedQuery)q;
	    	if (!eq.getCache()) {
	    		flags |= (NO_CHECK_QCACHE | NO_SET_QCACHE | NO_CHECK_FILTERCACHE);
	    	}
	    }

	    // we can try and look up the complete query in the cache.
	    // we can't do that if filter!=null though (we don't want to
	    // do hashCode() and equals() for a big DocSet).
	    if (mQueryResultCache != null && cmd.getFilter() == null && 
	       (flags & (NO_CHECK_QCACHE|NO_SET_QCACHE)) != ((NO_CHECK_QCACHE|NO_SET_QCACHE))) { 
	    	
	    	// all of the current flags can be reused during warming,
	        // so set all of them on the cache key.
	        key = new QueryResultKey(q, cmd.getFilterList(), cmd.getSort(), flags);
	        
	        if ((flags & NO_CHECK_QCACHE)==0) {
	        	superset = mQueryResultCache.get(key);

	        	if (superset != null) {
	        		// check that the cache entry has scores recorded if we need them
	        		if ((flags & GET_SCORES)==0 || superset.hasScores()) {
	        			// NOTE: subset() returns null if the DocList has fewer docs than
	        			// requested
	        			out.setDocList(superset.subset(cmd.getOffset(),cmd.getLength()));
	        		}
	        	}
	        	
	        	if (out.getDocList() != null) {
	        		// found the docList in the cache... now check if we need the docset too.
	        		// OPT: possible future optimization - if the doclist contains all the matches,
	        		// use it to make the docset instead of rerunning the query.
	        		if (out.getDocSet() == null && ((flags & GET_DOCSET) != 0)) {
	        			if (cmd.getFilterList() == null) {
	        				out.setDocSet(getDocSet(cmd.getQuery()));
	        				
	        			} else {
	        				List<IQuery> newList = new ArrayList<IQuery>(cmd.getFilterList().size()+1);
	        				newList.add(cmd.getQuery());
	        				newList.addAll(cmd.getFilterList());
	        				
	        				out.setDocSet(getDocSet(newList));
	        			}
	        		}
	        		
	        		return;
	        	}
	        }

	        // If we are going to generate the result, bump up to the
	        // next resultWindowSize for better caching.

	        if ((flags & NO_SET_QCACHE) == 0) {
	        	// handle 0 special case as well as avoid idiv in the common case.
	        	if (maxDocRequested < mQueryResultWindowSize) {
	        		supersetMaxDoc = mQueryResultWindowSize;
	        		
	        	} else {
	        		supersetMaxDoc = ((maxDocRequested -1)/mQueryResultWindowSize + 1)*mQueryResultWindowSize;
	        		if (supersetMaxDoc < 0) 
	        			supersetMaxDoc = maxDocRequested;
	        	}
	        } else {
	        	// we won't be caching the result
	        	key = null; 
	        }
	    }

	    // OK, so now we need to generate an answer.
	    // One way to do that would be to check if we have an unordered list
	    // of results for the base query.  If so, we can apply the filters and then
	    // sort by the resulting set.  This can only be used if:
	    // - the sort doesn't contain score
	    // - we don't want score returned.

	    // check if we should try and use the filter cache
	    boolean useFilterCache = false;
	    
	    if ((flags & (GET_SCORES|NO_CHECK_FILTERCACHE)) == 0 && 
	    	mUseFilterForSortedQuery && cmd.getSort() != null && mFilterCache != null) {
	    	
	    	useFilterCache = true;
	    	ISortField[] sfields = cmd.getSort().getSortFields();
	    	
	    	for (ISortField sf : sfields) {
	    		if (sf.getType() == ISortField.Type.SCORE) {
	    			useFilterCache = false;
	    			break;
	    		}
	    	}
	    }

	    // disable useFilterCache optimization temporarily
	    if (useFilterCache) {
	    	// now actually use the filter cache.
	    	// for large filters that match few documents, this may be
	    	// slower than simply re-executing the query.
	    	if (out.getDocSet() == null) {
	    		out.setDocSet(getDocSet(cmd.getQuery(),cmd.getFilter()));
	    		
	    		DocSet bigFilt = getDocSet(cmd.getFilterList());
	    		if (bigFilt != null) 
	    			out.setDocSet(out.getDocSet().intersection(bigFilt));
	    	}
	    	
	    	// todo: there could be a sortDocSet that could take a list of
	    	// the filters instead of anding them first...
	    	// perhaps there should be a multi-docset-iterator
	    	superset = sortDocSet(out.getDocSet(), cmd.getSort(), supersetMaxDoc);
	    	out.setDocList(superset.subset(cmd.getOffset(), cmd.getLength()));
	    	
	    } else {
	    	// do it the normal way...
	    	cmd.setSupersetMaxDoc(supersetMaxDoc);
	    	
	    	if ((flags & GET_DOCSET)!=0) {
	    		// this currently conflates returning the docset for the base query vs
	    		// the base query and all filters.
	    		DocSet qDocSet = getDocListAndSetNC(qr, cmd);
	    		
	    		// cache the docSet matching the query w/o filtering
	    		if (qDocSet != null && mFilterCache != null && !qr.isPartialResults()) 
	    			mFilterCache.put(cmd.getQuery(), qDocSet);
	    		
	    	} else {
	    		getDocListNC(qr, cmd);
	    		//Parameters: cmd.getQuery(),theFilt,cmd.getSort(),0,
	    		//  supersetMaxDoc,cmd.getFlags(),cmd.getTimeAllowed(),responseHeader);
	    	}

	    	superset = out.getDocList();
	    	out.setDocList(superset.subset(cmd.getOffset(), cmd.getLength()));
	    }

	    // lastly, put the superset in the cache if the size is less than or equal
	    // to queryResultMaxDocsCached
	    if (key != null && superset.size() <= mQueryResultMaxDocsCached && !qr.isPartialResults()) {
	    	mQueryResultCache.put(key, superset);
	    }
	}
  
	protected DocList sortDocSet(DocSet set, ISort sort, int nDocs) throws ErrorException {
	    if (nDocs == 0) 
	    	return new DocSlice(0, 0, new int[0], null, 0, 0f);

	    // bit of a hack to tell if a set is sorted - do it better in the future.
	    boolean inOrder = (set instanceof BitDocSet) || (set instanceof SortedIntDocSet);
	    
	    TopDocsCollector<?> topCollector = null; 
	    try {
		    topCollector = TopFieldCollector.create(weightSort(sort), 
		    		nDocs, false, false, false, inOrder);
	    } catch (IOException ex) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    }

	    DocIterator iter = set.iterator();
	    
	    int base = 0;
	    int end = 0;
	    int readerIndex = 0;

	    while (iter.hasNext()) {
	    	int doc = iter.nextDoc();
	    	
	    	try { 
		    	while (doc >= end) {
		    		IAtomicReaderRef leaf = mSearcher.getLeafContexts().get(readerIndex++);
		    		base = leaf.getDocBase();
		    		end = base + leaf.getReader().getMaxDoc();
		    		
		    		topCollector.setNextReader(leaf);
		    		// we should never need to set the scorer given the settings for the collector
		    	}
		    	
		    	topCollector.collect(doc-base);
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    	}
	    }
	    
	    ITopDocs topDocs = topCollector.getTopDocs(0, nDocs);

	    int nDocsReturned = topDocs.getScoreDocs().length;
	    int[] ids = new int[nDocsReturned];

	    for (int i=0; i < nDocsReturned; i++) {
	    	IScoreDoc scoreDoc = topDocs.getScoreDocs()[i];
	    	ids[i] = scoreDoc.getDoc();
	    }

	    return new DocSlice(0, nDocsReturned, ids, null, 
	    		topDocs.getTotalHits(), 0.0f);
	}
	
	// any DocSet returned is for the query only, without any filtering... that way it may
	// be cached if desired.
	private DocSet getDocListAndSetNC(QueryResult qr,QueryCommand cmd) throws ErrorException {
	    int len = cmd.getSupersetMaxDoc();
	    int last = len;
	    if (last < 0 || last > getMaxDoc()) 
	    	last = getMaxDoc();
	    
	    final int lastDocRequested = last;
	    
	    int nDocsReturned;
	    int totalHits;
	    float maxScore;
	    int[] ids;
	    float[] scores;
	    DocSet set;

	    boolean needScores = (cmd.getFlags() & GET_SCORES) != 0;
	    
	    int maxDoc = getMaxDoc();
	    int smallSetSize = maxDoc>>6;

	    ProcessedFilter pf = getProcessedFilter(cmd.getFilter(), cmd.getFilterList());
	    final IFilter indexFilter = pf.getFilter();

	    IQuery query = QueryUtils.makeQueryable(cmd.getQuery());
	    final long timeAllowed = cmd.getTimeAllowed();

	    // handle zero case...
	    if (lastDocRequested <= 0) {
	    	final float[] topscore = new float[] { Float.NEGATIVE_INFINITY };

	    	Collector collector;
	    	DocSetCollector setCollector;

	    	if (!needScores) {
	    		collector = setCollector = new DocSetCollector(smallSetSize, maxDoc);
	    		
	    	} else {
	    		collector = setCollector = new DocSetDelegateCollector(smallSetSize, maxDoc, 
	    			new Collector() {
	    				private IScorer mScorer;
	    				
	    				@Override
	    				public void setScorer(IScorer scorer) {
	    					this.mScorer = scorer;
	    				}
	    				
	    				@Override
	    				public void collect(int doc) throws IOException {
	    					float score = this.mScorer.getScore();
	    					if (score > topscore[0]) 
	    						topscore[0] = score;
	    				}
	    				
	    				@Override
	    				public void setNextReader(IAtomicReaderRef context) {
	    				}
	    				
	    				@Override
	    				public boolean acceptsDocsOutOfOrder() {
	    					return false;
	    				}
	    			});
	    	}

	    	if (timeAllowed > 0) {
	    		collector = new TimeLimitingCollector(collector, 
	    				TimeLimitingCollector.getGlobalCounter(), timeAllowed);
	    	}
	    	
	    	if (pf.getPostFilter() != null) {
	    		pf.getPostFilter().setLastDelegate(collector);
	    		collector = pf.getPostFilter();
	    	}

	    	try {
	    		mSearcher.search(query, indexFilter, collector);
	    		
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		
	    	} catch (TimeExceededException ex) {
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Query: " + query + "; " + ex.getMessage());
	    		
	    		qr.setPartialResults(true);
	    	}

	    	set = setCollector.getDocSet();

	    	nDocsReturned = 0;
	    	ids = new int[nDocsReturned];
	    	scores = new float[nDocsReturned];
	    	
	    	totalHits = set.size();
	    	maxScore = totalHits>0 ? topscore[0] : 0.0f;
	    	
	    } else {
	    	TopDocsCollector<?> topCollector;

	    	if (cmd.getSort() == null) {
	    		topCollector = TopScoreDocCollector.create(len, true);
	    		
	    	} else {
	    		try { 
		    		topCollector = TopFieldCollector.create(weightSort(cmd.getSort()), 
		    				len, false, needScores, needScores, true);
	    		} catch (IOException ex) { 
	    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		}
	    	}

	    	DocSetCollector setCollector = new DocSetDelegateCollector(
	    			maxDoc>>6, maxDoc, topCollector);
	    	
	    	Collector collector = setCollector;
	    	if (timeAllowed > 0) {
	    		collector = new TimeLimitingCollector(collector, 
	    				TimeLimitingCollector.getGlobalCounter(), timeAllowed);
	    	}
	    	
	    	if (pf.getPostFilter() != null) {
	    		pf.getPostFilter().setLastDelegate(collector);
	    		collector = pf.getPostFilter();
	    	}
	    	
	    	try {
	    		mSearcher.search(query, indexFilter, collector);
	    		
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		
	    	} catch (TimeExceededException ex) {
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Query: " + query + "; " + ex.getMessage());
	    		
	    		qr.setPartialResults(true);
	    	}

	    	set = setCollector.getDocSet();      

	    	totalHits = topCollector.getTotalHits();
	    	assert(totalHits == set.size());

	    	ITopDocs topDocs = topCollector.getTopDocs(0, len);
	    	maxScore = totalHits>0 ? topDocs.getMaxScore() : 0.0f;
	    	nDocsReturned = topDocs.getScoreDocs().length;

	    	ids = new int[nDocsReturned];
	    	scores = (cmd.getFlags()&GET_SCORES)!=0 ? new float[nDocsReturned] : null;
	    	
	    	for (int i=0; i < nDocsReturned; i++) {
	    		IScoreDoc scoreDoc = topDocs.getScoreDocs()[i];
	    		ids[i] = scoreDoc.getDoc();
	    		
	    		if (scores != null) 
	    			scores[i] = scoreDoc.getScore();
	    	}
	    }

	    int sliceLen = Math.min(lastDocRequested,nDocsReturned);
	    if (sliceLen < 0) sliceLen = 0;

	    qr.setDocList(new DocSlice(0,sliceLen,ids,scores,totalHits,maxScore));
	    // TODO: if we collect results before the filter, we just need to intersect with
	    // that filter to generate the DocSet for qr.setDocSet()
	    qr.setDocSet(set);

	    // TODO: currently we don't generate the DocSet for the base query,
	    // but the QueryDocSet == CompleteDocSet if filter==null.
	    return pf.getFilter() == null && pf.getPostFilter() == null ? 
	    		qr.getDocSet() : null;
	}
	
	private void getDocListNC(QueryResult qr,QueryCommand cmd) throws ErrorException {
	    final long timeAllowed = cmd.getTimeAllowed();
	    
	    int len = cmd.getSupersetMaxDoc();
	    int last = len;
	    if (last < 0 || last > getMaxDoc()) 
	    	last = getMaxDoc();
	    
	    final int lastDocRequested = last;
	    
	    int nDocsReturned;
	    int totalHits;
	    float maxScore;
	    int[] ids;
	    float[] scores;

	    boolean needScores = (cmd.getFlags() & GET_SCORES) != 0;

	    IQuery query = QueryUtils.makeQueryable(cmd.getQuery());

	    ProcessedFilter pf = getProcessedFilter(cmd.getFilter(), cmd.getFilterList());
	    final IFilter indexFilter = pf.getFilter();

	    // handle zero case...
	    if (lastDocRequested <= 0) {
	    	final float[] topscore = new float[] { Float.NEGATIVE_INFINITY };
	    	final int[] numHits = new int[1];

	    	Collector collector;

	    	if (!needScores) {
	    		collector = new Collector () {
		    			@Override
		    			public void setScorer(IScorer scorer) {
		    			}
		    			@Override
		    			public void collect(int doc) {
		    				numHits[0]++;
		    			}
		    			@Override
		    			public void setNextReader(IAtomicReaderRef context) {
		    			}
		    			@Override
		    			public boolean acceptsDocsOutOfOrder() {
		    				return true;
		    			}
		    		};
	    	} else {
	    		collector = new Collector() {
		    			private IScorer mScorer;
		    			@Override
		    			public void setScorer(IScorer scorer) {
		    				this.mScorer = scorer;
		    			}
		    			@Override
		    			public void collect(int doc) throws IOException {
		    				numHits[0]++;
		    				float score = this.mScorer.getScore();
		    				if (score > topscore[0]) 
		    					topscore[0] = score;            
		    			}
		    			@Override
		    			public void setNextReader(IAtomicReaderRef context) {
		    			}
		    			@Override
		    			public boolean acceptsDocsOutOfOrder() {
		    				return true;
		    			}
		    		};
	    	}
	      
	    	if (timeAllowed > 0) {
	    		collector = new TimeLimitingCollector(collector, 
	    				TimeLimitingCollector.getGlobalCounter(), timeAllowed);
	    	}
	      
	    	if (pf.getPostFilter() != null) {
	    		pf.getPostFilter().setLastDelegate(collector);
	    		collector = pf.getPostFilter();
	    	}

	    	try {
	    		mSearcher.search(query, indexFilter, collector);
	    	  
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    	  
	    	} catch (TimeExceededException ex) {
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Query: " + query + "; " + ex.getMessage());
	    	  
	    		qr.setPartialResults(true);
	    	}

	    	nDocsReturned=0;
	    	ids = new int[nDocsReturned];
	    	scores = new float[nDocsReturned];
	    	
	    	totalHits = numHits[0];
	    	maxScore = totalHits>0 ? topscore[0] : 0.0f;
	    	
	    } else {
	    	TopDocsCollector<?> topCollector;
	    	
	    	if (cmd.getSort() == null) {
	    		if (cmd.getScoreDoc() != null) {
	    			//create the Collector with InOrderPagingCollector
	    			topCollector = TopScoreDocCollector.create(len, cmd.getScoreDoc(), true); 
	    		} else {
	    			topCollector = TopScoreDocCollector.create(len, true);
	    		}
	    	} else {
	    		try { 
		    		topCollector = TopFieldCollector.create(weightSort(cmd.getSort()), 
		    				len, false, needScores, needScores, true);
	    		} catch (IOException ex) { 
	    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		}
	    	}
	    	
	    	Collector collector = topCollector;
	    	if (timeAllowed > 0) {
	    		collector = new TimeLimitingCollector(collector, 
	    				TimeLimitingCollector.getGlobalCounter(), timeAllowed);
	    	}
	    	
	    	if (pf.getPostFilter() != null) {
	    		pf.getPostFilter().setLastDelegate(collector);
	    		collector = pf.getPostFilter();
	    	}
	    	
	    	try {
	    		mSearcher.search(query, indexFilter, collector);
	    		
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    		
	    	} catch (TimeExceededException ex) {
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("Query: " + query + "; " + ex.getMessage());
	    		
	    		qr.setPartialResults(true);
	    	}

	    	totalHits = topCollector.getTotalHits();
	    	ITopDocs topDocs = topCollector.getTopDocs(0, len);
	    	
	    	maxScore = totalHits>0 ? topDocs.getMaxScore() : 0.0f;
	    	nDocsReturned = topDocs.getScoreDocs().length;
	    	
	    	ids = new int[nDocsReturned];
	    	scores = (cmd.getFlags()&GET_SCORES)!=0 ? new float[nDocsReturned] : null;
	    	
	    	for (int i=0; i < nDocsReturned; i++) {
	    		IScoreDoc scoreDoc = topDocs.getScoreDocs()[i];
	    		ids[i] = scoreDoc.getDoc();
	    		
	    		if (scores != null) 
	    			scores[i] = scoreDoc.getScore();
	    	}
	    }

	    int sliceLen = Math.min(lastDocRequested,nDocsReturned);
	    if (sliceLen < 0) sliceLen = 0;
	    
	    qr.setDocList(new DocSlice(0, 
	    		sliceLen, ids, scores, totalHits, maxScore));
	}

	public boolean isClosed() { 
		return mClosed || mDirectoryReader == null || mDirectoryReader.isClosed();
	}
	
	@Override
	public String getMBeanKey() {
		return getName();
	}

	@Override
	public String getMBeanName() {
		return getClass().getName();
	}

	@Override
	public String getMBeanVersion() {
		return "1.0";
	}

	@Override
	public String getMBeanDescription() {
		return "Index Searcher";
	}

	@Override
	public String getMBeanCategory() {
		return InfoMBean.CATEGORY_CORE;
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
	    NamedList<Object> lst = new NamedMap<Object>();
	    lst.add("searcherName", mName);
	    lst.add("caching", mEnableCache);
	    
	    boolean closed = true;
	    if (!isClosed()) {
	    	closed = false;
	    	
		    lst.add("numDocs", mDirectoryReader.getNumDocs());
		    lst.add("maxDoc", mDirectoryReader.getMaxDoc());
		    lst.add("reader", mDirectoryReader.toString());
		    lst.add("readerDir", mDirectoryReader.getDirectory());
		    lst.add("indexVersion", mDirectoryReader.getVersion());
	    }
	    
	    lst.add("openedAt", new Date(mOpenTime));
	    if (mRegisterTime != 0) 
	    	lst.add("registeredAt", new Date(mRegisterTime));
	    if (mWarmupTime != 0)
	    	lst.add("warmupTime", "" + mWarmupTime + " ms");
	    
	    if (closed)
	    	lst.add("closed", "true");
	    
	    return lst;
	}
	
}
