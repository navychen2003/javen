package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.similarity.TFIDFSimilarity;

/** 
 * Function that returns {@link TFIDFSimilarity#tf(int)}
 * for every document.
 * <p>
 * Note that the configured Similarity for the field must be
 * a subclass of {@link TFIDFSimilarity}
 */
public class TFValueSource extends TermFreqValueSource {
	
	public TFValueSource(String field, String val, 
			String indexedField, BytesRef indexedBytes) {
		super(field, val, indexedField, indexedBytes);
	}

	@Override
	public String getName() {
		return "tf";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final IFields fields = readerContext.getReader().getFields();
		final ITerms terms = fields.getTerms(mIndexedField);
		
		final ISearcher searcher = (ISearcher)context.get("searcher");
		final TFIDFSimilarity similarity = IDFValueSource.asTFIDF(
				searcher.getSimilarity(), mIndexedField);
		if (similarity == null) {
			throw new UnsupportedOperationException(
					"requires a TFIDFSimilarity (such as DefaultSimilarity)");
		}

		return new TFFloatDocValues(this, terms, similarity);
	}
	
	private class TFFloatDocValues extends FloatDocValues { 
		
		private final ITerms mTerms;
		private final TFIDFSimilarity mSimilarity;
		private IDocsEnum mDocs;
		private int mAtDoc;
		private int mLastDocRequested = -1;

		public TFFloatDocValues(ValueSource source, 
				ITerms terms, TFIDFSimilarity similarity) throws IOException { 
			super(source);
			mTerms = terms;
			mSimilarity = similarity;
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
		public float floatVal(int doc) {
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
					return mSimilarity.tf(0);
				}

				// a match!
				return mSimilarity.tf(mDocs.getFreq());
			} catch (IOException e) {
				throw new RuntimeException("caught exception in function " 
						+ getDescription() + " : doc=" + doc, e);
			}
		}
	}
	
}
