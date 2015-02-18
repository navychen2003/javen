package org.javenstudio.hornet.search.scorer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.DocsAndFreqs;

/** Scorer for conjunctions, sets of terms, all of which are required. */
public final class MatchOnlyConjunctionTermScorer extends ConjunctionTermScorer {
	
	public MatchOnlyConjunctionTermScorer(IWeight weight, float coord,
			DocsAndFreqs[] docsAndFreqs) throws IOException {
		super(weight, coord, docsAndFreqs);
	}

	@Override
	public float getScore() throws IOException {
		float sum = 0.0f;
		for (DocsAndFreqs docs : mDocsAndFreqs) {
			sum += docs.docScorer.score(mLastDoc, 1);
		}
		return sum * mCoord;
	}
	
}
