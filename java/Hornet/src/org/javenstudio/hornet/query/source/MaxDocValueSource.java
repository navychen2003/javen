package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Returns the value of {@link IndexReader#maxDoc()}
 * for every document. This is the number of documents
 * including deletions.
 */
public class MaxDocValueSource extends ValueSource {
	
	public String getName() {
		return "maxdoc";
	}

	@Override
	public String getDescription() {
		return getName() + "()";
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		context.put("searcher", searcher);
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		ISearcher searcher = (ISearcher)context.get("searcher");
		return new ConstIntDocValues(searcher.getIndexReader().getMaxDoc(), this);
	}

	@Override
	public boolean equals(Object o) {
		return this.getClass() == o.getClass();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}
	
}
