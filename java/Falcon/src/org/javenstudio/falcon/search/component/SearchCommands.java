package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.collector.TermAllGroupHeadsCollector;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.collector.MultiCollector;
import org.javenstudio.hornet.search.collector.TimeExceededException;
import org.javenstudio.hornet.search.collector.TimeLimitingCollector;
import org.javenstudio.hornet.search.collector.TotalHitCountCollector;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.filter.ProcessedFilter;
import org.javenstudio.falcon.search.hits.BitDocSet;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.DocSetCollector;
import org.javenstudio.falcon.search.hits.DocSetDelegateCollector;
import org.javenstudio.falcon.search.hits.QueryCommand;
import org.javenstudio.falcon.search.hits.QueryResult;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.shard.ShardTransformer;

/**
 * Responsible for executing a search with a number of {@link SearchCommand} instances.
 * A typical search can have more then one {@link SearchCommand} instances.
 *
 */
public class SearchCommands {
	static final Logger LOG = Logger.getLogger(SearchCommands.class);

	public static class Builder {
		private List<SearchCommand<?>> mCommands = 
				new ArrayList<SearchCommand<?>>();
		
		private QueryCommand mQueryCommand;
		private Searcher mSearcher;
		
		private boolean mNeedDocSet = false;
		private boolean mTruncateGroups = false;
		private boolean mIncludeHitCount = false;

		public Builder setQueryCommand(QueryCommand queryCommand) {
			mQueryCommand = queryCommand;
			mNeedDocSet = (queryCommand.getFlags() & Searcher.GET_DOCSET) != 0;
			return this;
		}

		public Builder addSearchCommand(SearchCommand<?> commandField) {
			mCommands.add(commandField);
			return this;
		}

		public Builder setSearcher(Searcher searcher) {
			mSearcher = searcher;
			return this;
		}

		/**
		 * Sets whether to compute a {@link DocSet}.
		 * May override the value set by {@link #setQueryCommand(QueryCommand)}.
		 *
		 * @param needDocSet Whether to compute a {@link DocSet}
		 * @return this
		 */
		public Builder setNeedDocSet(boolean needDocSet) {
			mNeedDocSet = needDocSet;
			return this;
		}

		public Builder setTruncateGroups(boolean truncateGroups) {
			mTruncateGroups = truncateGroups;
			return this;
		}

		public Builder setIncludeHitCount(boolean includeHitCount) {
			mIncludeHitCount = includeHitCount;
			return this;
		}

		public SearchCommands build() {
			if (mQueryCommand == null || mSearcher == null) 
				throw new IllegalStateException("All fields must be set");
			
			return new SearchCommands(mQueryCommand, mCommands, mSearcher, 
					mNeedDocSet, mTruncateGroups, mIncludeHitCount);
		}
	}

	private final QueryCommand mQueryCommand;
	private final List<SearchCommand<?>> mCommands;
	private final Searcher mSearcher;
	
	private final boolean mNeedDocset;
	private final boolean mTruncateGroups;
	private final boolean mIncludeHitCount;
	
	private boolean mPartialResults = false;
	private int mTotalHitCount;
	private DocSet mDocSet;
	
	private SearchCommands(QueryCommand queryCommand,
			List<SearchCommand<?>> commands, Searcher searcher,
			boolean needDocset, boolean truncateGroups, boolean includeHitCount) {
		mQueryCommand = queryCommand;
		mCommands = commands;
		mSearcher = searcher;
		mNeedDocset = needDocset;
		mTruncateGroups = truncateGroups;
		mIncludeHitCount = includeHitCount;
	}

	public int getTotalHitCount() { return mTotalHitCount; }
	
	public void execute() throws ErrorException {
		final int nrOfCommands = mCommands.size();
		List<ICollector> collectors = new ArrayList<ICollector>(nrOfCommands);
		for (SearchCommand<?> command : mCommands) {
			collectors.addAll(command.createCollectors());
		}

		ProcessedFilter pf = mSearcher.getProcessedFilter(
				mQueryCommand.getFilter(), mQueryCommand.getFilterList());
		
		IFilter indexFilter = pf.getFilter();
		IQuery query = QueryUtils.makeQueryable(mQueryCommand.getQuery());

		if (mTruncateGroups) {
			mDocSet = computeGroupedDocSet(query, indexFilter, collectors);
		} else if (mNeedDocset) {
			mDocSet = computeDocSet(query, indexFilter, collectors);
		} else if (!collectors.isEmpty()) {
			searchWithTimeLimiter(query, indexFilter, 
					MultiCollector.wrap(collectors.toArray(new ICollector[nrOfCommands])));
		} else {
			searchWithTimeLimiter(query, indexFilter, null);
		}
	}

	private DocSet computeGroupedDocSet(IQuery query, IFilter indexFilter, 
			List<ICollector> collectors) throws ErrorException {
		SearchCommand<?> firstCommand = mCommands.get(0);
		AbstractAllGroupHeadsCollector<?> termAllGroupHeadsCollector =
				TermAllGroupHeadsCollector.create(firstCommand.getKey(), 
						firstCommand.getSortWithinGroup());
		
		if (collectors.isEmpty()) {
			searchWithTimeLimiter(query, indexFilter, termAllGroupHeadsCollector);
		} else {
			collectors.add(termAllGroupHeadsCollector);
			searchWithTimeLimiter(query, indexFilter, 
					MultiCollector.wrap(collectors.toArray(new ICollector[collectors.size()])));
		}

		int maxDoc = mSearcher.getMaxDoc();
		long[] bits = termAllGroupHeadsCollector.retrieveGroupHeads(maxDoc).getBitsArray();
		
		return new BitDocSet(new OpenBitSet(bits, bits.length));
	}

	private DocSet computeDocSet(IQuery query, IFilter indexFilter, 
			List<ICollector> collectors) throws ErrorException {
		int maxDoc = mSearcher.getMaxDoc();
		DocSetCollector docSetCollector;
		
		if (collectors.isEmpty()) {
			docSetCollector = new DocSetCollector(maxDoc >> 6, maxDoc);
		} else {
			ICollector wrappedCollectors = MultiCollector.wrap(
					collectors.toArray(new ICollector[collectors.size()]));
			docSetCollector = new DocSetDelegateCollector(maxDoc >> 6, maxDoc, wrappedCollectors);
		}
		
		searchWithTimeLimiter(query, indexFilter, docSetCollector);
		return docSetCollector.getDocSet();
	}

	public NamedList<?> processResult(QueryResult queryResult, 
			ShardTransformer<List<SearchCommand<?>>,?> transformer) throws ErrorException {
		if (mDocSet != null) 
			queryResult.setDocSet(mDocSet);
		
		queryResult.setPartialResults(mPartialResults);
		
		return transformer.transform(mCommands);
	}

	/**
	 * Invokes search with the specified filter and collector.  
	 * If a time limit has been specified then wrap the collector in the TimeLimitingCollector
	 */
	private void searchWithTimeLimiter(final IQuery query, final IFilter indexFilter, 
			ICollector collector) throws ErrorException {
		if (mQueryCommand.getTimeAllowed() > 0 ) {
			collector = new TimeLimitingCollector(collector, 
					TimeLimitingCollector.getGlobalCounter(), mQueryCommand.getTimeAllowed());
		}

		TotalHitCountCollector hitCountCollector = new TotalHitCountCollector();
		if (mIncludeHitCount) 
			collector = MultiCollector.wrap(collector, hitCountCollector);

		try {
			mSearcher.search(query, indexFilter, collector);
		} catch (TimeExceededException ex) {
			mPartialResults = true;
			if (LOG.isDebugEnabled())
				LOG.warn("Query: " + query + "; " + ex.getMessage());
		}

		if (mIncludeHitCount) 
			mTotalHitCount = hitCountCollector.getTotalHits();
	}

}
