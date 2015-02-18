package org.javenstudio.hornet.search.scorer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Scorer;

/** 
 * A Scorer for queries with a required subscorer
 * and an excluding (prohibited) sub DocIdSetIterator.
 * <br>
 * This <code>Scorer</code> implements {@link Scorer#advance(int)},
 * and it uses the skipTo() on the given scorers.
 */
public class ReqExclScorer extends Scorer {
	
	private IScorer mReqScorer;
	private IDocIdSetIterator mExclDisi;
	private int mDoc = -1;

	/** 
	 * Construct a <code>ReqExclScorer</code>.
	 * @param reqScorer The scorer that must match, except where
	 * @param exclDisi indicates exclusion.
	 */
	public ReqExclScorer(IScorer reqScorer, IDocIdSetIterator exclDisi) {
		super(reqScorer.getWeight());
		mReqScorer = reqScorer;
		mExclDisi = exclDisi;
	}

	@Override
	public int nextDoc() throws IOException {
		if (mReqScorer == null) 
			return mDoc;
		
		mDoc = mReqScorer.nextDoc();
		if (mDoc == NO_MORE_DOCS) {
			mReqScorer = null; // exhausted, nothing left
			return mDoc;
		}
		
		if (mExclDisi == null) 
			return mDoc;
		
		return mDoc = toNonExcluded();
	}
  
	/** 
	 * Advance to non excluded doc.
	 * <br>On entry:
	 * <ul>
	 * <li>reqScorer != null,
	 * <li>exclScorer != null,
	 * <li>reqScorer was advanced once via next() or skipTo()
	 *      and reqScorer.doc() may still be excluded.
	 * </ul>
	 * Advances reqScorer a non excluded required doc, if any.
	 * @return true iff there is a non excluded required doc.
	 */
	private int toNonExcluded() throws IOException {
		int exclDoc = mExclDisi.getDocID();
		int reqDoc = mReqScorer.getDocID(); // may be excluded
		
		do {  
			if (reqDoc < exclDoc) {
				return reqDoc; // reqScorer advanced to before exclScorer, ie. not excluded
				
			} else if (reqDoc > exclDoc) {
				exclDoc = mExclDisi.advance(reqDoc);
				if (exclDoc == NO_MORE_DOCS) {
					mExclDisi = null; // exhausted, no more exclusions
					return reqDoc;
				}
				if (exclDoc > reqDoc) 
					return reqDoc; // not excluded
			}
		} while ((reqDoc = mReqScorer.nextDoc()) != NO_MORE_DOCS);
		
		mReqScorer = null; // exhausted, nothing left
		
		return NO_MORE_DOCS;
	}

	@Override
	public int getDocID() {
		return mDoc;
	}

	/** 
	 * Returns the score of the current document matching the query.
	 * Initially invalid, until {@link #nextDoc()} is called the first time.
	 * @return The score of the required scorer.
	 */
	@Override
	public float getScore() throws IOException {
		// reqScorer may be null when next() or skipTo() already return false
		return mReqScorer.getScore(); 
	}
  
	@Override
	public int advance(int target) throws IOException {
		if (mReqScorer == null) 
			return mDoc = NO_MORE_DOCS;
		
		if (mExclDisi == null) 
			return mDoc = mReqScorer.advance(target);
		
		if (mReqScorer.advance(target) == NO_MORE_DOCS) {
			mReqScorer = null;
			return mDoc = NO_MORE_DOCS;
		}
		
		return mDoc = toNonExcluded();
	}
	
}
