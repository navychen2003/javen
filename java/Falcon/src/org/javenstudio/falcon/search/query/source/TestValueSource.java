package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.SearchRequestInfo;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;
import org.javenstudio.falcon.search.query.ValueSourceParser;

public class TestValueSource extends ValueSource {
	
	public static class Parser extends ValueSourceParser { 
		@Override
		public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
			final ValueSource source = fp.parseValueSource();
			return new TestValueSource(source);
		}
	}
	
	private ValueSource mSource;
  
	public TestValueSource(ValueSource source) {
		mSource = source;
	}
  
	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		if (context.get(this) == null) {
			SearchRequestInfo requestInfo = SearchRequestInfo.getRequestInfo();
			
			throw new IOException("testfunc: unweighted value source detected.  delegate=" 
					+ mSource + " request=" + (requestInfo == null ? "null" : requestInfo.getRequest()));
		}
		
		return mSource.getValues(context, readerContext);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		
		return o instanceof TestValueSource && 
				mSource.equals(((TestValueSource)o).mSource);
	}

	@Override
	public int hashCode() {
		return mSource.hashCode() + TestValueSource.class.hashCode();
	}

	@Override
	public String getDescription() {
		return "testfunc(" + mSource.getDescription() + ')';
	}

	@Override
	public void createWeight(ValueSourceContext context, ISearcher searcher) throws IOException {
		context.put(this, this);
	}

	@Override
	public SortField getSortField(boolean reverse) throws IOException {
		return super.getSortField(reverse);
	}
	
}
