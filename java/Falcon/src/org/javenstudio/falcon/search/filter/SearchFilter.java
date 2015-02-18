package org.javenstudio.falcon.search.filter;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.ValueSourceContext;

/** 
 * A SearchFilter extends the Filter and adds extra semantics such as passing on
 * weight context info for function queries.
 *
 * Experimental and subject to change.
 */
public abstract class SearchFilter extends Filter {

	/** 
	 * Implementations should propagate createWeight to sub-ValueSources 
	 * which can store weight info in the context.
	 * The context object will be passed to getDocIdSet() 
	 * where this info can be retrieved. 
	 */
	public abstract void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException;
  
	public abstract IDocIdSet getDocIdSet(ValueSourceContext context, 
			IAtomicReaderRef readerContext, Bits acceptDocs) throws IOException;

	@Override
	public IDocIdSet getDocIdSet(IAtomicReaderRef context, Bits acceptDocs) 
			throws IOException {
		return getDocIdSet(null, context, acceptDocs);
	}
	
}
