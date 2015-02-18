package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.QueryDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>QueryValueSource</code> returns the relevance score of the query
 */
public class QueryValueSource extends ValueSource {
	
	private final IQuery mQuery;
	private final float mDefaultValue;

	public QueryValueSource(IQuery q, float defVal) {
		mQuery = q;
		mDefaultValue = defVal;
	}

	public IQuery getQuery() { return mQuery; }
	public float getDefaultValue() { return mDefaultValue; }

	@Override
	public String getDescription() {
		return "query(" + mQuery + ",def=" + mDefaultValue + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext fcontext, 
			IAtomicReaderRef readerContext) throws IOException {
		return new QueryDocValues(this, readerContext, fcontext);
	}

	@Override
	public int hashCode() {
		return mQuery.hashCode() * 29;
	}

	@Override
	public boolean equals(Object o) {
		if (QueryValueSource.class != o.getClass()) 
			return false;
		
		QueryValueSource other = (QueryValueSource)o;
		
		return this.mQuery.equals(other.mQuery) && 
				this.mDefaultValue == other.mDefaultValue;
	}

	@Override
	public void createWeight(ValueSourceContext context, ISearcher searcher) throws IOException {
		IWeight w = searcher.createNormalizedWeight(mQuery);
		context.put(this, w);
	}
	
}
