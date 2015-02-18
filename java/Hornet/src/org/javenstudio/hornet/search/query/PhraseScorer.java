package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISloppySimilarityScorer;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;

/** 
 * Expert: Scoring functionality for phrase queries.
 * <br>A document is considered matching if it contains the phrase-query terms  
 * at "valid" positions. What "valid positions" are
 * depends on the type of the phrase query: for an exact phrase query terms are required 
 * to appear in adjacent locations, while for a sloppy phrase query some distance between 
 * the terms is allowed. The abstract method {@link #phraseFreq()} of extending classes
 * is invoked for each document containing all the phrase query terms, in order to 
 * compute the frequency of the phrase query in that document. A non zero frequency
 * means a match. 
 */
public abstract class PhraseScorer extends Scorer {
	
	protected final ISloppySimilarityScorer mDocScorer;
	protected PhrasePositions mMin, mMax;
	
	// phrase frequency in current doc as computed by phraseFreq().
	protected float mFreq; 

	public PhraseScorer(Weight weight, PostingsAndFreq[] postings,
			ISloppySimilarityScorer docScorer) {
		super(weight);
    	mDocScorer = docScorer;

    	// convert tps to a list of phrase positions.
    	// note: phrase-position differs from term-position in that its position
    	// reflects the phrase offset: pp.pos = tp.pos - offset.
    	// this allows to easily identify a matching (exact) phrase 
    	// when all PhrasePositions have exactly the same position.
    	if (postings.length > 0) {
    		mMin = new PhrasePositions(postings[0].getPostings(), 
    				postings[0].getPosition(), 0, postings[0].getTerms());
    		mMax = mMin;
    		mMax.mDoc = -1;
    		
    		for (int i = 1; i < postings.length; i++) {
    			PhrasePositions pp = new PhrasePositions(postings[i].getPostings(), 
    					postings[i].getPosition(), i, postings[i].getTerms());
    			mMax.mNext = pp;
    			mMax = pp;
    			mMax.mDoc = -1;
    		}
    		
    		mMax.mNext = mMin; // make it cyclic for easier manipulation
    	}
	}

	@Override
	public int getDocID() {
		return mMax.mDoc; 
	}

	@Override
	public int nextDoc() throws IOException {
		return advance(mMax.mDoc);
	}
  
	@Override
	public float getScore() throws IOException {
		return mDocScorer.score(mMax.mDoc, mFreq);
	}

	private boolean advanceMin(int target) throws IOException {
		if (!mMin.skipTo(target)) { 
			mMax.mDoc = NO_MORE_DOCS; // for further calls to docID() 
			return false;
		}
		
		mMin = mMin.mNext; // cyclic
		mMax = mMax.mNext; // cyclic
		
		return true;
	}
  
	@Override
	public int advance(int target) throws IOException {
		mFreq = 0.0f;
		
		if (!advanceMin(target)) 
			return NO_MORE_DOCS;
    
		boolean restart = false;
		
		while (mFreq == 0.0f) {
			while (mMin.mDoc < mMax.mDoc || restart) {
				restart = false;
				
				if (!advanceMin(mMax.mDoc)) 
					return NO_MORE_DOCS;
			}
			
			// found a doc with all of the terms
			mFreq = phraseFreq(); // check for phrase
			restart = true;
		} 

		// found a match
		return mMax.mDoc;
	}
  
	/**
	 * phrase frequency in current doc as computed by phraseFreq().
	 */
	@Override
	public final float getFreq() {
		return mFreq;
	}

	/**
	 * For a document containing all the phrase query terms, compute the
	 * frequency of the phrase in that document. 
	 * A non zero frequency means a match.
	 * <br>Note, that containing all phrase terms does not guarantee a match 
	 * - they have to be found in matching locations.  
	 * @return frequency of the phrase in current doc, 0 if not found. 
	 */
	protected abstract float phraseFreq() throws IOException;

	@Override
	public String toString() { 
		return "scorer(" + mWeight + ")"; 
	}
 
}
