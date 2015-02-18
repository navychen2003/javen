package org.javenstudio.hornet.search.scorer;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.DocsAndFreqs;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.util.ArrayUtil;

/** Scorer for conjunctions, sets of terms, all of which are required. */
public class ConjunctionTermScorer extends Scorer {
	
	protected final float mCoord;
	protected int mLastDoc = -1;
	protected final DocsAndFreqs[] mDocsAndFreqs;
	private final DocsAndFreqs mLead;

	public ConjunctionTermScorer(IWeight weight, float coord,
			DocsAndFreqs[] docsAndFreqs) throws IOException {
		super(weight);
		mCoord = coord;
		mDocsAndFreqs = docsAndFreqs;
		
		// Sort the array the first time to allow the least frequent DocsEnum to
		// lead the matching.
		ArrayUtil.mergeSort(docsAndFreqs, new Comparator<DocsAndFreqs>() {
				public int compare(DocsAndFreqs o1, DocsAndFreqs o2) {
					return o1.docFreq - o2.docFreq;
				}
			});

		mLead = docsAndFreqs[0]; // least frequent DocsEnum leads the intersection
	}

	private int doNext(int doc) throws IOException {
		do {
			if (mLead.doc == DocIdSetIterator.NO_MORE_DOCS) 
				return NO_MORE_DOCS;
			
			advanceHead: do {
				for (int i = 1; i < mDocsAndFreqs.length; i++) {
					if (mDocsAndFreqs[i].doc < doc) 
						mDocsAndFreqs[i].doc = mDocsAndFreqs[i].docs.advance(doc);
					
					if (mDocsAndFreqs[i].doc > doc) {
						// DocsEnum beyond the current doc - break and advance lead
						break advanceHead;
					}
				}
				
				// success - all DocsEnums are on the same doc
				return doc;
			} while (true);
			
			// advance head for next iteration
			doc = mLead.doc = mLead.docs.nextDoc(); 
			
		} while (true);
	}

	@Override
	public int advance(int target) throws IOException {
		mLead.doc = mLead.docs.advance(target);
		return mLastDoc = doNext(mLead.doc);
	}

	@Override
	public int getDocID() {
		return mLastDoc;
	}

	@Override
	public int nextDoc() throws IOException {
		mLead.doc = mLead.docs.nextDoc();
		return mLastDoc = doNext(mLead.doc);
	}

	@Override
	public float getScore() throws IOException {
		float sum = 0.0f;
		for (DocsAndFreqs docs : mDocsAndFreqs) {
			sum += docs.docScorer.score(mLastDoc, docs.docs.getFreq());
		}
		return sum * mCoord;
	}
	
}
