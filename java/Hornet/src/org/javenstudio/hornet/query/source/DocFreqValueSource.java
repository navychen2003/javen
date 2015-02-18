package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>DocFreqValueSource</code> returns the number of documents containing the term.
 * 
 */
public class DocFreqValueSource extends ValueSource {
	
	protected final String mField;
	protected final String mIndexedField;
	protected final String mValue;
	protected final BytesRef mIndexedBytes;

	public DocFreqValueSource(String field, String val, 
			String indexedField, BytesRef indexedBytes) {
		mField = field;
		mValue = val;
		mIndexedField = indexedField;
		mIndexedBytes = indexedBytes;
	}

	public String getName() {
		return "docfreq";
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mField + ',' + mValue + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
    	ISearcher searcher = (ISearcher)context.get("searcher");
    	
    	int docfreq = searcher.getIndexReader().getDocFreq(
    			new Term(mIndexedField, mIndexedBytes));
    	
    	return new ConstIntDocValues(docfreq, this);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		context.put("searcher",searcher);
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
		
		DocFreqValueSource other = (DocFreqValueSource)o;
		return this.mIndexedField.equals(other.mIndexedField) && 
				this.mIndexedBytes.equals(other.mIndexedBytes);
	}
	
}
