package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.similarity.PerFieldSimilarityWrapper;
import org.javenstudio.hornet.search.similarity.TFIDFSimilarity;

/** 
 * Function that returns {@link TFIDFSimilarity #idf(long, long)}
 * for every document.
 * <p>
 * Note that the configured Similarity for the field must be
 * a subclass of {@link TFIDFSimilarity}
 */
public class IDFValueSource extends DocFreqValueSource {
	
	public IDFValueSource(String field, String val, 
			String indexedField, BytesRef indexedBytes) {
		super(field, val, indexedField, indexedBytes);
	}

	@Override
	public String getName() {
		return "idf";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		ISearcher searcher = (ISearcher)context.get("searcher");
		
		TFIDFSimilarity sim = asTFIDF(searcher.getSimilarity(), mField);
		if (sim == null) 
			throw new UnsupportedOperationException("requires a TFIDFSimilarity (such as DefaultSimilarity)");
		
		int docfreq = searcher.getIndexReader().getDocFreq(
				new Term(mIndexedField, mIndexedBytes));
		
		float idf = sim.idf(docfreq, searcher.getIndexReader().getMaxDoc());
		
		return new ConstDoubleDocValues(idf, this);
	}
  
	// tries extra hard to cast the sim to TFIDFSimilarity
	static TFIDFSimilarity asTFIDF(ISimilarity sim, String field) {
		while (sim instanceof PerFieldSimilarityWrapper) {
			sim = ((PerFieldSimilarityWrapper)sim).get(field);
		}
		
		if (sim instanceof TFIDFSimilarity) 
			return (TFIDFSimilarity)sim;
		else 
			return null;
	}
	
}
