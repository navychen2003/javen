package org.javenstudio.falcon.search.transformer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.search.TopDocsCollector;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.search.AdvancedSort;
import org.javenstudio.hornet.search.collector.TopFieldCollector;
import org.javenstudio.hornet.search.collector.TopScoreDocCollector;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.component.SearchCommand;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.FilterCollector;
import org.javenstudio.falcon.search.query.QueryBuilder;

/**
 *
 */
public class QueryFieldCommand implements SearchCommand<QueryFieldResult> {

	public static class Builder {

		private String mQueryString;
		private IQuery mQuery;
		private ISort mSort;
		private DocSet mDocSet;
		
		private Integer mDocsToCollect;
		private boolean mNeedScores;

		public Builder setSort(ISort sort) {
			mSort = sort;
			return this;
		}

		public Builder setQuery(IQuery query) {
			mQuery = query;
			return this;
		}

		/**
		 * Sets the group query from the specified groupQueryString.
		 * The groupQueryString is parsed into a query.
		 *
		 * @param groupQueryString The group query string to parse
		 * @param request The current request
		 * @return this
		 * @throws ParseException If parsing the groupQueryString failed
		 */
		public Builder setQuery(String groupQueryString, ISearchRequest request) 
				throws ErrorException {
			QueryBuilder parser = request.getSearchCore().getQueryFactory()
					.getQueryBuilder(groupQueryString, null, request);
			
			mQueryString = groupQueryString;
			
			return setQuery(parser.getQuery());
		}

		public Builder setDocSet(DocSet docSet) {
			mDocSet = docSet;
			return this;
		}

		/**
		 * Sets the docSet based on the created {@link DocSet}
		 *
		 * @param searcher The searcher executing the
		 * @return this
		 * @throws IOException If I/O related errors occur.
		 */
		public Builder setDocSet(Searcher searcher) throws ErrorException {
			return setDocSet(searcher.getDocSet(mQuery));
		}

		public Builder setDocsToCollect(int docsToCollect) {
			mDocsToCollect = docsToCollect;
			return this;
		}

		public Builder setNeedScores(boolean needScores) {
			mNeedScores = needScores;
			return this;
		}

		public QueryFieldCommand build() {
			if (mSort == null || mQuery == null || mDocSet == null || mDocsToCollect == null) 
				throw new IllegalStateException("All fields must be set");

			return new QueryFieldCommand(mSort, mQuery, 
					mDocsToCollect, mNeedScores, mDocSet, mQueryString);
		}
	}

	private final ISort mSort;
	private final IQuery mQuery;
	private final DocSet mDocSet;
	
	private final String mQueryString;
	private final int mDocsToCollect;
	private final boolean mNeedScores;
	
	private TopDocsCollector<?> mCollector;
	private FilterCollector mFilterCollector;

	private QueryFieldCommand(ISort sort, IQuery query, int docsToCollect, boolean needScores, 
			DocSet docSet, String queryString) {
		mSort = sort;
		mQuery = query;
		mDocsToCollect = docsToCollect;
		mNeedScores = needScores;
		mDocSet = docSet;
		mQueryString = queryString;
	}

	@Override
	public List<ICollector> createCollectors() throws ErrorException {
		try {
			if (mSort == null || mSort == AdvancedSort.RELEVANCE) {
				mCollector = TopScoreDocCollector.create(mDocsToCollect, true);
				
			} else {
				mCollector = TopFieldCollector.create(mSort, mDocsToCollect, 
						true, mNeedScores, mNeedScores, true);
			}
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
		
		mFilterCollector = new FilterCollector(mDocSet, mCollector);
		
		return Arrays.asList((ICollector) mFilterCollector);
	}

	@Override
	public QueryFieldResult getResult() {
		return new QueryFieldResult(mCollector.getTopDocs(), 
				mFilterCollector.getMatches());
	}

	@Override
	public String getKey() {
		return mQueryString != null ? mQueryString : mQuery.toString();
	}

	@Override
	public ISort getGroupSort() {
		return mSort;
	}

	@Override
	public ISort getSortWithinGroup() {
		return null;
	}
	
}
