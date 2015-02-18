package org.javenstudio.hornet.search.scorer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Scorer;

/** 
 * Expert: A <code>Scorer</code> for documents matching a
 * <code>Term</code>.  It treats all documents as having
 * one occurrenc (tf=1) for the term.
 */
public final class MatchOnlyTermScorer extends Scorer {
	
	private final IDocsEnum mDocsEnum;
	private final IExactSimilarityScorer mDocScorer;
  
	/**
	 * Construct a <code>TermScorer</code>.
	 * 
	 * @param weight
	 *          The weight of the <code>Term</code> in the query.
	 * @param td
	 *          An iterator over the documents matching the <code>Term</code>.
	 * @param docScorer
	 *          The </code>Similarity.ExactSimScorer</code> implementation 
	 *          to be used for score computations.
	 */
	public MatchOnlyTermScorer(IWeight weight, IDocsEnum td, IExactSimilarityScorer docScorer) 
			throws IOException {
		super(weight);
		mDocScorer = docScorer;
		mDocsEnum = td;
	}

	@Override
	public int getDocID() {
		return mDocsEnum.getDocID();
	}

	@Override
	public float getFreq() {
		return 1.0f;
	}

	/**
	 * Advances to the next document matching the query. <br>
	 * 
	 * @return the document matching the query or NO_MORE_DOCS if there are no more documents.
	 */
	@Override
	public int nextDoc() throws IOException {
		return mDocsEnum.nextDoc();
	}

	@Override
	public float getScore() {
		assert getDocID() != NO_MORE_DOCS;
		return mDocScorer.score(mDocsEnum.getDocID(), 1);
	}

	/**
	 * Advances to the first match beyond the current whose document number is
	 * greater than or equal to a given target. <br>
	 * The implementation uses {@link DocsEnum#advance(int)}.
	 * 
	 * @param target
	 *          The target document number.
	 * @return the matching document or NO_MORE_DOCS if none exist.
	 */
	@Override
	public int advance(int target) throws IOException {
		return mDocsEnum.advance(target);
	}

	/** Returns a string representation of this <code>TermScorer</code>. */
	@Override
	public String toString() { 
		return "MatchOnlyTermScorer(" + mWeight + ")"; 
	}
	
}
