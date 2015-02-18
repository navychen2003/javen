package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.similarity.TFIDFSimilarity;

/** 
 * Function that returns {@link TFIDFSimilarity#decodeNormValue(byte)}
 * for every document.
 * <p>
 * Note that the configured Similarity for the field must be
 * a subclass of {@link TFIDFSimilarity}
 */
public class NormValueSource extends ValueSource {
	
	protected final String mField;
	
	public NormValueSource(String field) {
		mField = field;
	}

	public String getName() {
		return "norm";
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mField + ')';
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		context.put("searcher", searcher);
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, IAtomicReaderRef readerContext) throws IOException {
		final ISearcher searcher = (ISearcher)context.get("searcher");
		final TFIDFSimilarity similarity = IDFValueSource.asTFIDF(
				searcher.getSimilarity(), mField);
		if (similarity == null) {
			throw new UnsupportedOperationException(
					"requires a TFIDFSimilarity (such as DefaultSimilarity)");
		}
		
		//DocValues dv = readerContext.getReader().normValues(mField);
		//if (dv == null) 
			return new ConstDoubleDocValues(0.0, this);
    
		//final byte[] norms = (byte[]) dv.getSource().getArray();

		//return new FloatDocValues(this) {
		//		@Override
		//		public float floatVal(int doc) {
		//			return similarity.decodeNormValue(norms[doc]);
		//		}
		//	};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		return this.mField.equals(((NormValueSource)o).mField);
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + mField.hashCode();
	}
	
}
