package org.javenstudio.falcon.search.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.query.FunctionQuery;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.collector.CachingCollector;
import org.javenstudio.hornet.search.collector.MultiCollector;
import org.javenstudio.hornet.search.collector.TimeExceededException;
import org.javenstudio.hornet.search.collector.TimeLimitingCollector;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.filter.ProcessedFilter;
import org.javenstudio.falcon.search.hits.BitDocSet;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.DocSetCollector;
import org.javenstudio.falcon.search.hits.DocSetDelegateCollector;
import org.javenstudio.falcon.search.hits.DocSlice;
import org.javenstudio.falcon.search.hits.QueryCommand;
import org.javenstudio.falcon.search.hits.QueryResult;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.StringFieldSource;

/**
 * Basic Lightning Grouping infrastructure.
 * Warning NOT thread save!
 *
 */
public class Grouping {
	private final static Logger LOG = Logger.getLogger(Grouping.class);

	public static enum Format {
		/** Grouped result. Each group has its own result set. */
		GROUPED,

		/** Flat result. All documents of all groups are put in one list. */
		SIMPLE
	}

	public static enum TotalCount {
		/** Computations should be based on groups. */
		GROUPED,

		/** Computations should be based on plain documents, so not taking grouping into account. */
		UNGROUPED
	}
  
	protected final Searcher mSearcher;
	protected final QueryResult mResult;
	protected final QueryCommand mCommand;
	
	protected final List<GroupingCommand<?>> mCommands = 
			new ArrayList<GroupingCommand<?>>();
	
	protected final boolean mIsMain;
	protected final boolean mCacheSecondPassSearch;
	protected final int mMaxDocsPercentageToCache;
	
	protected NamedList<Object> mGrouped = new NamedMap<Object>();
	
	// used for tracking unique docs when we need a doclist
	protected Set<Integer> mIdSet = new LinkedHashSet<Integer>(); 
	
	// output if one of the grouping commands should be used as the main result.
	public DocList mMainResult;
	
	protected TimeLimitingCollector mTimeLimitingCollector;
	
	protected IQuery mQuery;
	protected DocSet mFilter;
	protected IFilter mIndexFilter;
	protected ISort mSort;
	protected ISort mGroupSort;
	
	protected int mLimitDefault;
	protected int mDocsPerGroupDefault;
	protected int mGroupOffsetDefault;
	
	protected Format mDefaultFormat;
	protected TotalCount mDefaultTotalCount;

	protected int mMaxDoc;
	// max number of matches from any grouping command
	protected int mMaxMatches; 
	// max score seen in any doclist
	protected float mMaxScore = Float.NEGATIVE_INFINITY; 
	
	protected boolean mNeedScores;
	protected boolean mGetDocSet;
	protected boolean mGetGroupedDocSet;
	// doclist needed for debugging or highlighting
	protected boolean mGetDocList; 
	
	protected boolean mSignalCacheWarning = false;
	
	/**
	 * @param cacheSecondPassSearch    
	 * Whether to cache the documents and scores from the first pass search for the second
	 * pass search.
	 * @param maxDocsPercentageToCache 
	 * The maximum number of documents in a percentage relative from maxdoc
	 * that is allowed in the cache. When this threshold is met,
	 * the cache is not used in the second pass search.
	 */
	public Grouping(Searcher searcher, QueryResult qr, QueryCommand cmd,
			boolean cacheSecondPassSearch, int maxDocsPercentageToCache, boolean main) {
		mSearcher = searcher;
		mResult = qr;
		mCommand = cmd;
		mCacheSecondPassSearch = cacheSecondPassSearch;
		mMaxDocsPercentageToCache = maxDocsPercentageToCache;
		mIsMain = main;
	}

	public DocList getMainResult() { return mMainResult; }
	
	public void add(GroupingCommand<?> groupingCommand) {
		mCommands.add(groupingCommand);
	}

	/**
	 * Adds a field command based on the specified field.
	 * If the field is not compatible with {@link CommandField} it invokes the
	 * {@link #addFunctionCommand(ISearchRequest)} method.
	 *
	 * @param field The fieldname to group by.
	 */
	public void addFieldCommand(String field, ISearchRequest request) 
			throws ErrorException {
		// Throws an exception when field doesn't exist. Bad request.
		SchemaField schemaField = mSearcher.getSchema().getField(field); 
		SchemaFieldType fieldType = schemaField.getType();
		
		ValueSource valueSource = fieldType.getValueSource(schemaField, null);
		if (!(valueSource instanceof StringFieldSource)) {
			addFunctionCommand(field, request);
			return;
		}

		GroupingCommandField gc = new GroupingCommandField(this);
		gc.mGroupSort = mGroupSort;
		gc.mGroupBy = field;
		gc.mKey = field;
		gc.mNumGroups = mLimitDefault;
		gc.mDocsPerGroup = mDocsPerGroupDefault;
		gc.mGroupOffset = mGroupOffsetDefault;
		gc.mOffset = mCommand.getOffset();
		gc.mSort = mSort;
		gc.mFormat = mDefaultFormat;
		gc.mTotalCount = mDefaultTotalCount;

		if (mIsMain) {
			gc.mIsMain = true;
			gc.mFormat = Grouping.Format.SIMPLE;
		}

		if (gc.mFormat == Grouping.Format.SIMPLE) 
			gc.mGroupOffset = 0;  // doesn't make sense
		
		mCommands.add(gc);
	}

	public void addFunctionCommand(String groupByStr, ISearchRequest request) 
			throws ErrorException {
		QueryBuilder parser = mSearcher.getSearchCore().getQueryFactory()
				.getQueryBuilder(groupByStr, "func", request);
		
		IQuery q = parser.getQuery();
		final GroupingCommand<?> gc;
		
		if (q instanceof FunctionQuery) {
			ValueSource valueSource = ((FunctionQuery) q).getValueSource();
			if (valueSource instanceof StringFieldSource) {
				String field = ((StringFieldSource) valueSource).getField();
				
				GroupingCommandField commandField = new GroupingCommandField(this);
				commandField.mGroupBy = field;
				
				gc = commandField;
			} else {
				GroupingCommandFunc commandFunc = new GroupingCommandFunc(this);
				commandFunc.mGroupBy = valueSource;
				
				gc = commandFunc;
			}
		} else {
			GroupingCommandFunc commandFunc = new GroupingCommandFunc(this);
			commandFunc.mGroupBy = new QueryValueSource(q, 0.0f);
			
			gc = commandFunc;
		}
		
		gc.mGroupSort = mGroupSort;
		gc.mKey = groupByStr;
		gc.mNumGroups = mLimitDefault;
		gc.mDocsPerGroup = mDocsPerGroupDefault;
		gc.mGroupOffset = mGroupOffsetDefault;
		gc.mOffset = mCommand.getOffset();
		gc.mSort = mSort;
		gc.mFormat = mDefaultFormat;
		gc.mTotalCount = mDefaultTotalCount;

		if (mIsMain) {
			gc.mIsMain = true;
			gc.mFormat = Grouping.Format.SIMPLE;
		}

		if (gc.mFormat == Grouping.Format.SIMPLE) 
			gc.mGroupOffset = 0;  // doesn't make sense

		mCommands.add(gc);
	}

	public void addQueryCommand(String groupByStr, ISearchRequest request) 
			throws ErrorException {
		QueryBuilder parser = mSearcher.getSearchCore().getQueryFactory()
				.getQueryBuilder(groupByStr, null, request);
		
		IQuery gq = parser.getQuery();
		GroupingCommandQuery gc = new GroupingCommandQuery(this);
		
		gc.mQuery = gq;
		gc.mGroupSort = mGroupSort;
		gc.mKey = groupByStr;
		gc.mNumGroups = mLimitDefault;
		gc.mDocsPerGroup = mDocsPerGroupDefault;
		gc.mGroupOffset = mGroupOffsetDefault;

		// these two params will only be used if this is for the main result set
		gc.mOffset = mCommand.getOffset();
		gc.mNumGroups = mLimitDefault;
		gc.mFormat = mDefaultFormat;

		if (mIsMain) {
			gc.mIsMain = true;
			gc.mFormat = Grouping.Format.SIMPLE;
		}
		
		if (gc.mFormat == Grouping.Format.SIMPLE) {
			gc.mDocsPerGroup = gc.mNumGroups;  // doesn't make sense to limit to one
			gc.mGroupOffset = gc.mOffset;
		}

		mCommands.add(gc);
	}

	public Grouping setSort(ISort sort) {
		if (sort == null) 
			throw new NullPointerException("sort is null");
		
		mSort = sort;
		return this;
	}

	public Grouping setGroupSort(ISort groupSort) {
		if (groupSort == null) 
			throw new NullPointerException("groupSort is null");
		
		mGroupSort = groupSort;
		return this;
	}

	public Grouping setLimitDefault(int limitDefault) {
		mLimitDefault = limitDefault;
		return this;
	}

	public Grouping setDocsPerGroupDefault(int docsPerGroupDefault) {
		mDocsPerGroupDefault = docsPerGroupDefault;
		return this;
	}

	public Grouping setGroupOffsetDefault(int groupOffsetDefault) {
		mGroupOffsetDefault = groupOffsetDefault;
		return this;
	}

	public Grouping setDefaultFormat(Format defaultFormat) {
		if (defaultFormat == null) 
			throw new NullPointerException("defaultFormat is null");
		
		mDefaultFormat = defaultFormat;
		return this;
	}

	public Grouping setDefaultTotalCount(TotalCount defaultTotalCount) {
		if (defaultTotalCount == null) 
			throw new NullPointerException("defaultTotalCount is null");
		
		mDefaultTotalCount = defaultTotalCount;
		return this;
	}

	public Grouping setGetGroupedDocSet(boolean getGroupedDocSet) {
		mGetGroupedDocSet = getGroupedDocSet;
		return this;
	}

	public List<GroupingCommand<?>> getCommands() {
		return mCommands;
	}

	public void execute() throws ErrorException {
		if (mCommands.isEmpty()) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Specify at least one field, function or query to group by.");
		}

		DocListAndSet out = new DocListAndSet();
		mResult.setDocListAndSet(out);

		ProcessedFilter pf = mSearcher.getProcessedFilter(
				mCommand.getFilter(), mCommand.getFilterList());
		
		final IFilter indexFilter = pf.getFilter();
		boolean cacheScores = false;
		
		mMaxDoc = mSearcher.getMaxDoc();
		mNeedScores = (mCommand.getFlags() & Searcher.GET_SCORES) != 0;
		
		// NOTE: Change this when groupSort can be specified per group
		if (!mNeedScores && !mCommands.isEmpty()) {
			if (mCommands.get(0).mGroupSort == null) {
				cacheScores = true;
				
			} else {
				for (ISortField field : mCommands.get(0).mGroupSort.getSortFields()) {
					if (field.getType() == ISortField.Type.SCORE) {
						cacheScores = true;
						break;
					}
				}
			}
			
		} else if (mNeedScores) {
			cacheScores = mNeedScores;
		}
		
		mGetDocSet = (mCommand.getFlags() & Searcher.GET_DOCSET) != 0;
		mGetDocList = (mCommand.getFlags() & Searcher.GET_DOCLIST) != 0;
		
		mQuery = QueryUtils.makeQueryable(mCommand.getQuery());

		for (GroupingCommand<?> cmd : mCommands) {
			cmd.prepare();
		}

		AbstractAllGroupHeadsCollector<?> allGroupHeadsCollector = null;
		List<ICollector> collectors = new ArrayList<ICollector>(mCommands.size());
		
		for (GroupingCommand<?> cmd : mCommands) {
			ICollector collector = cmd.createFirstPassCollector();
			if (collector != null) 
				collectors.add(collector);
			
			if (mGetGroupedDocSet && allGroupHeadsCollector == null) 
				collectors.add(allGroupHeadsCollector = cmd.createAllGroupCollector());
		}

		ICollector allCollectors = MultiCollector.wrap(
				collectors.toArray(new ICollector[collectors.size()]));
		
		DocSetCollector setCollector = null;
		CachingCollector cachedCollector = null;
		
		if (mGetDocSet && allGroupHeadsCollector == null) {
			setCollector = new DocSetDelegateCollector(mMaxDoc >> 6, mMaxDoc, allCollectors);
			allCollectors = setCollector;
		}

		if (mCacheSecondPassSearch && allCollectors != null) {
			int maxDocsToCache = (int) Math.round(mMaxDoc * (mMaxDocsPercentageToCache / 100.0d));
			// Only makes sense to cache if we cache more than zero.
			// Maybe we should have a minimum and a maximum, that defines the window we would like caching for.
			if (maxDocsToCache > 0) {
				allCollectors = cachedCollector = 
						CachingCollector.create(allCollectors, cacheScores, maxDocsToCache);
			}
		}

		if (pf.getPostFilter() != null) {
			pf.getPostFilter().setLastDelegate(allCollectors);
			allCollectors = pf.getPostFilter();
		}

		if (allCollectors != null) 
			searchWithTimeLimiter(indexFilter, allCollectors);

		if (mGetGroupedDocSet && allGroupHeadsCollector != null) {
			IFixedBitSet fixedBitSet = allGroupHeadsCollector.retrieveGroupHeads(mMaxDoc);
			
			long[] bits = fixedBitSet.getBitsArray();
			OpenBitSet openBitSet = new OpenBitSet(bits, bits.length);
			
			mResult.setDocSet(new BitDocSet(openBitSet));
			
		} else if (mGetDocSet) {
			mResult.setDocSet(setCollector.getDocSet());
		}

		collectors.clear();
		
		for (GroupingCommand<?> cmd : mCommands) {
			ICollector collector = cmd.createSecondPassCollector();
			if (collector != null)
				collectors.add(collector);
		}

		if (!collectors.isEmpty()) {
			ICollector secondPhaseCollectors = MultiCollector.wrap(
					collectors.toArray(new ICollector[collectors.size()]));
			
			if (collectors.size() > 0) {
				if (cachedCollector != null) {
					if (cachedCollector.isCached()) {
						try {
							cachedCollector.replay(secondPhaseCollectors);
						} catch (IOException ex) { 
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
						}
						
					} else {
						mSignalCacheWarning = true;
						
						if (LOG.isWarnEnabled()) {
							LOG.warn(String.format(Locale.ROOT, 
									"The grouping cache is active, but not used because it exceeded the max cache limit of %d percent", 
									mMaxDocsPercentageToCache));
							LOG.warn("Please increase cache size or disable group caching.");
						}
						
						searchWithTimeLimiter(indexFilter, secondPhaseCollectors);
					}
					
				} else {
					if (pf.getPostFilter() != null) {
						pf.getPostFilter().setLastDelegate(secondPhaseCollectors);
						secondPhaseCollectors = pf.getPostFilter();
					}
					
					searchWithTimeLimiter(indexFilter, secondPhaseCollectors);
				}
			}
		}

		for (GroupingCommand<?> cmd : mCommands) {
			cmd.finish();
		}

    	mResult.setGroupedResults(mGrouped);

    	if (mGetDocList) {
    		int sz = mIdSet.size();
    		int[] ids = new int[sz];
    		int idx = 0;
    		
    		for (int val : mIdSet) {
    			ids[idx++] = val;
    		}
    		
    		mResult.setDocList(new DocSlice(0, sz, ids, null, 
    				mMaxMatches, mMaxScore));
    	}
	}

	/**
	 * Invokes search with the specified filter and collector.  
	 * If a time limit has been specified, wrap the collector in a TimeLimitingCollector
	 */
	private void searchWithTimeLimiter(final IFilter indexFilter, 
			ICollector collector) throws ErrorException {
		if (mCommand.getTimeAllowed() > 0) {
			if (mTimeLimitingCollector == null) {
				mTimeLimitingCollector = new TimeLimitingCollector(collector, 
						TimeLimitingCollector.getGlobalCounter(), mCommand.getTimeAllowed());
				
			} else {
				/**
				 * This is so the same timer can be used for grouping's multiple phases.   
				 * We don't want to create a new TimeLimitingCollector for each phase because that would 
				 * reset the timer for each phase.  If time runs out during the first phase, the 
				 * second phase should timeout quickly.
				 */
				mTimeLimitingCollector.setCollector(collector);
			}
			
			collector = mTimeLimitingCollector;
		}
		
		try {
			mSearcher.search(mQuery, indexFilter, collector);
			
		} catch (TimeExceededException x) {
			if (LOG.isDebugEnabled())
				LOG.debug("Query: " + mQuery + "; " + x.getMessage());
			
			mResult.setPartialResults(true);
		}
	}

	/**
	 * Returns offset + len if len equals zero or higher. Otherwise returns max.
	 *
	 * @param offset The offset
	 * @param len    The number of documents to return
	 * @param max    The number of document to return if len < 0 or if offset + len < 0
	 * @return offset + len if len equals zero or higher. Otherwise returns max
	 */
	protected int getMax(int offset, int len, int max) {
		int v = len < 0 ? max : offset + len;
		if (v < 0 || v > max) v = max;
		return v;
	}

	/**
	 * Returns whether a cache warning should be send to the client.
	 * The value <code>true</code> is returned when the cache is emptied because 
	 * the caching limits where met, otherwise
	 * <code>false</code> is returned.
	 *
	 * @return whether a cache warning should be send to the client
	 */
	public boolean isSignalCacheWarning() {
		return mSignalCacheWarning;
	}

}
