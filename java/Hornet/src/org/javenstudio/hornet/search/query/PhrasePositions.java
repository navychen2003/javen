package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.ITerm;

/**
 * Position of a term in a document that takes into account the term offset within the phrase. 
 */
public final class PhrasePositions {
	
	private final IDocsAndPositionsEnum mPostings;	// stream of docs & positions
	private final ITerm[] mTerms; 					// for repetitions initialization 
	private final int mOrd;              			// unique across all PhrasePositions instances
	
	protected PhrasePositions mNext;  	// used to make lists
	
	protected int mDoc;              	// current doc
	protected int mPosition;         	// position in doc
	protected int mCount;            	// remaining pos in this doc
	protected int mOffset;           	// position in phrase
	
	protected int mRptGroup = -1; 		// >=0 indicates that this is a repeating PP
	protected int mRptInd; 				// index in the rptGroup
	
	public PhrasePositions(IDocsAndPositionsEnum postings, 
			int o, int ord, ITerm[] terms) {
		mPostings = postings;
		mOffset = o;
		mOrd = ord;
		mTerms = terms;
	}

	public final ITerm[] getTerms() { return mTerms; }
	public final int getOrd() { return mOrd; }
	
	public final PhrasePositions getNext() { return mNext; }
	
	public final int getDoc() { return mDoc; }
	public final int getPosition() { return mPosition; }
	public final int getCount() { return mCount; }
	public final int getOffset() { return mOffset; }
	
	// increments to next doc
	public final boolean next() throws IOException { 
		mDoc = mPostings.nextDoc();
		if (mDoc == IDocIdSetIterator.NO_MORE_DOCS) 
			return false;
    
		return true;
	}

	public final boolean skipTo(int target) throws IOException {
		mDoc = mPostings.advance(target);
		if (mDoc == IDocIdSetIterator.NO_MORE_DOCS) 
			return false;
    
		return true;
	}

	public final void firstPosition() throws IOException {
		mCount = mPostings.getFreq();  // read first pos
		nextPosition();
	}

	/**
	 * Go to next location of this term current document, and set 
	 * <code>position</code> as <code>location - offset</code>, so that a 
	 * matching exact phrase is easily identified when all PhrasePositions 
	 * have exactly the same <code>position</code>.
	 */
	public final boolean nextPosition() throws IOException {
		if (mCount-- > 0) {  // read subsequent pos's
			mPosition = mPostings.nextPosition() - mOffset;
			return true;
		}
		
		return false;
	}
  
	/** for debug purposes */
	@Override
	public String toString() {
		String s = "PhrasePositions{doc=" + mDoc + ",offset=" + mOffset 
				+ ",pos=" + mPosition + ",count=" + mCount;
		
		if (mRptGroup >= 0) 
			s += ",rpt=" + mRptGroup + ",ind=" + mRptInd;
		
		s += "}";
		
		return s;
	}
	
}
