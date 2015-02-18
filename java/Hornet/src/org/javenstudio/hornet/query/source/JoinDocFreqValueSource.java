package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.field.MultiFields;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.IntDocValues;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Use a field value and find the Document Frequency within another field.
 * 
 * @since solr 4.0
 */
public class JoinDocFreqValueSource extends FieldCacheSource {

	public static final String NAME = "joindf";
  
	protected final String mQField;
  
	public JoinDocFreqValueSource(String field, String qfield) {
		super(field);
		mQField = qfield;
	}

	@Override
	public String getDescription() {
		return NAME + "(" + mField +":(" + mQField + "))";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final IDocTerms terms = mCache.getTerms(readerContext.getReader(), mField, IIntsReader.FAST);
		final IIndexReader top = ReaderUtil.getTopLevel(readerContext).getReader();
		
		ITerms t = MultiFields.getTerms(top, mQField);
		final ITermsEnum termsEnum = (t == null) ? TermsEnum.EMPTY : t.iterator(null);
    
		return new IntDocValues(this) {
				final BytesRef mBytes = new BytesRef();
	
				@Override
				public int intVal(int doc) {
					try {
						terms.getTerm(doc, mBytes);
						if (termsEnum.seekExact(mBytes, true)) 
							return termsEnum.getDocFreq();
						else 
							return 0;
						
					} catch (IOException e) {
						throw new RuntimeException("caught exception in function " 
								+ getDescription() + " : doc=" + doc, e);
					}
				}
			};
	}
  
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != JoinDocFreqValueSource.class) 
			return false;
		
		JoinDocFreqValueSource other = (JoinDocFreqValueSource)o;
		if (!mQField.equals(other.mQField)) 
			return false;
		
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return mQField.hashCode() + super.hashCode();
	}
	
}
