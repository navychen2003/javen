package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.IntDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Function that returns {@link DocsEnum#freq()} for the
 * supplied term in every document.
 * <p>
 * If the term does not exist in the document, returns 0.
 * If frequencies are omitted, returns 1.
 */
public class TermFreqValueSource extends DocFreqValueSource {
	
	public TermFreqValueSource(String field, String val, 
			String indexedField, BytesRef indexedBytes) {
		super(field, val, indexedField, indexedBytes);
	}

	@Override
	public String getName() {
		return "termfreq";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		IFields fields = readerContext.getReader().getFields();
		final ITerms terms = fields.getTerms(mIndexedField);

		return new TermFreqIntDocValues(this, terms);
	}
	
	private class TermFreqIntDocValues extends IntDocValues { 
		
		private final ITerms mTerms;
		private IDocsEnum mDocs;
		private int mAtDoc;
		private int mLastDocRequested = -1;

		public TermFreqIntDocValues(ValueSource source, 
				ITerms terms) throws IOException { 
			super(source);
			mTerms = terms;
			reset();
		}
		
		public void reset() throws IOException {
			// no one should call us for deleted docs?
			mDocs = null;
			if (mTerms != null) {
				final ITermsEnum termsEnum = mTerms.iterator(null);
				if (termsEnum.seekExact(mIndexedBytes, false)) 
					mDocs = termsEnum.getDocs(null, null);
			}

			if (mDocs == null) {
				mDocs = new DocsEnum() {
					@Override
					public int getFreq() {
						return 0;
					}

					@Override
					public int getDocID() {
						return IDocIdSetIterator.NO_MORE_DOCS;
					}

					@Override
					public int nextDoc() {
						return IDocIdSetIterator.NO_MORE_DOCS;
					}

					@Override
					public int advance(int target) {
						return IDocIdSetIterator.NO_MORE_DOCS;
					}
				};
			}
			
			mAtDoc = -1;
		}

		@Override
		public int intVal(int doc) {
			try {
				if (doc < mLastDocRequested) {
					// out-of-order access.... reset
					reset();
				}
				
				mLastDocRequested = doc;

				if (mAtDoc < doc) 
					mAtDoc = mDocs.advance(doc);

				if (mAtDoc > doc) {
					// term doesn't match this document... either because we hit the
					// end, or because the next doc is after this doc.
					return 0;
				}

				// a match!
				return mDocs.getFreq();
			} catch (IOException e) {
				throw new RuntimeException("caught exception in function " 
						+ getDescription() + " : doc=" + doc, e);
			}
		}
	}
	
}
