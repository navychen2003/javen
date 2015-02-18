package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.LongDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>TotalTermFreqValueSource</code> returns the total term freq 
 * (sum of term freqs across all documents).
 * Returns -1 if frequencies were omitted for the field, or if 
 * the codec doesn't support this statistic.
 * 
 */
public class TotalTermFreqValueSource extends ValueSource {
	
	protected final String mField;
	protected final String mIndexedField;
	protected final String mValue;
	protected final BytesRef mIndexedBytes;

	public TotalTermFreqValueSource(String field, String val, 
			String indexedField, BytesRef indexedBytes) {
		mField = field;
		mValue = val;
		mIndexedField = indexedField;
		mIndexedBytes = indexedBytes;
	}

	public String getName() {
		return "totaltermfreq";
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mField + ',' + mValue + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return (FunctionValues)context.get(this);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		long totalTermFreq = 0;
		
		for (IAtomicReaderRef readerContext : searcher.getTopReaderContext().getLeaves()) {
			long val = readerContext.getReader().getTotalTermFreq(
					new Term(mIndexedField, mIndexedBytes));
			
			if (val == -1) {
				totalTermFreq = -1;
				break;
				
			} else {
				totalTermFreq += val;
			}
		}
		
		final long ttf = totalTermFreq;
		context.put(this, new LongDocValues(this) {
				@Override
				public long longVal(int doc) {
					return ttf;
				}
			});
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() + mIndexedField.hashCode()*29 + mIndexedBytes.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		TotalTermFreqValueSource other = (TotalTermFreqValueSource)o;
		return this.mIndexedField.equals(other.mIndexedField) && 
				this.mIndexedBytes.equals(other.mIndexedBytes);
	}
	
}
