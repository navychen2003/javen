package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.LongDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>SumTotalTermFreqValueSource</code> returns the number of tokens.
 * (sum of term freqs across all documents, across all terms).
 * Returns -1 if frequencies were omitted for the field, or if 
 * the codec doesn't support this statistic.
 * 
 */
public class SumTotalTermFreqValueSource extends ValueSource {
	
	protected final String mIndexedField;

	public SumTotalTermFreqValueSource(String indexedField) {
		mIndexedField = indexedField;
	}

	public String getName() {
		return "sumtotaltermfreq";
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mIndexedField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return (FunctionValues)context.get(this);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		long sumTotalTermFreq = 0;
		
		for (IAtomicReaderRef readerContext : searcher.getTopReaderContext().getLeaves()) {
			IFields fields = readerContext.getReader().getFields();
			if (fields == null) continue;
			
			ITerms terms = fields.getTerms(mIndexedField);
			if (terms == null) continue;
			
			long v = terms.getSumTotalTermFreq();
			if (v == -1) {
				sumTotalTermFreq = -1;
				break;
				
			} else {
				sumTotalTermFreq += v;
			}
		}
		
		final long ttf = sumTotalTermFreq;
		context.put(this, new LongDocValues(this) {
				@Override
				public long longVal(int doc) {
					return ttf;
				}
			});
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() + mIndexedField.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		SumTotalTermFreqValueSource other = (SumTotalTermFreqValueSource)o;
		return this.mIndexedField.equals(other.mIndexedField);
	}
	
}
