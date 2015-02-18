package org.javenstudio.falcon.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.ValueSourceScorer;
import org.javenstudio.falcon.search.filter.PostFilter;
import org.javenstudio.falcon.search.filter.ValueSourceRangeFilter;
import org.javenstudio.falcon.search.hits.DelegatingCollector;

// This class works as either a normal constant score query, 
// or as a PostFilter using a collector
public class FunctionRangeQuery extends BaseConstantScoreQuery 
		implements PostFilter {
	
	private final ValueSourceRangeFilter mRangeFilter;

	public FunctionRangeQuery(ValueSourceRangeFilter filter) {
		super(filter);
		mRangeFilter = filter;
	}

	@Override
	public DelegatingCollector getFilterCollector(ISearcher searcher) {
		ValueSourceContext fcontext = ValueSourceContext.create(searcher);
		return new FunctionRangeCollector(fcontext);
	}

	class FunctionRangeCollector extends DelegatingCollector {
		private final ValueSourceContext mContext;
		private ValueSourceScorer mScorer;
		private int mMaxdoc;

		public FunctionRangeCollector(ValueSourceContext fcontext) {
			mContext = fcontext;
		}

		@Override
		public void collect(int doc) throws IOException {
			if (doc < mMaxdoc && mScorer.matches(doc)) 
				mDelegate.collect(doc);
		}

		@Override
		public void setNextReader(IAtomicReaderRef context) throws IOException {
			mMaxdoc = context.getReader().getMaxDoc();
			FunctionValues dv = mRangeFilter.getValueSource().getValues(mContext, context);
			
			mScorer = dv.getRangeScorer(context.getReader(), 
					mRangeFilter.getLowerVal(), mRangeFilter.getUpperVal(), 
					mRangeFilter.isIncludeLower(), mRangeFilter.isIncludeUpper());
			
			super.setNextReader(context);
		}
	}
	
}
