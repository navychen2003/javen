package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.search.SortField;

public abstract class ValueSource {

	/**
	 * Gets the values for this reader and the context that was previously
	 * passed to createWeight()
	 */
	public abstract FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException;

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	/**
	 * description of field, used in explain()
	 */
	public abstract String getDescription();

	/**
	 * Implementations should propagate createWeight to sub-ValueSources 
	 * which can optionally store weight info in the context. 
	 * The context object will be passed to getValues()
	 * where this info can be retrieved.
	 */
	public void createWeight(ValueSourceContext context, ISearcher searcher) 
			throws IOException {
		// do nothing
	}
	
	/**
	 * EXPERIMENTAL: This method is subject to change.
	 * <p>
	 * Get the SortField for this ValueSource.  
	 * Uses the {@link #getValues(ValueSourceContext, IAtomicReaderRef)}
	 * to populate the SortField.
	 *
	 * @param reverse true if this is a reverse sort.
	 * @return The {@link org.apache.lucene.search.SortField} for the ValueSource
	 * @throws IOException if there was a problem reading the values.
	 */
	public SortField getSortField(boolean reverse) throws IOException {
		return new ValueSourceSortField(this, reverse);
	}
	
	@Override
	public String toString() {
		return getDescription();
	}
	
}
