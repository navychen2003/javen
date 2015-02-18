package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.FixedBitSet;

public class OpenFixedBitSet extends FixedBitSet {

	public OpenFixedBitSet(int numBits) {
		super(numBits);
	}
	
	public OpenFixedBitSet(FixedBitSet other) {
		super(other);
	}
	
	@Override
	public DocIdSetIterator iterator() {
		return new OpenBitSetIterator(mBits, mBits.length);
	}
	
	/** 
	 * Does in-place OR of the bits provided by the
	 *  iterator. 
	 */
	@Override
	public void or(IDocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.getDocID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			or(obs.mArr, obs.mWords);
			// advance after last doc that would be accepted if standard
			// iteration is used (to exhaust it):
			obs.advance(mNumBits);
			
		} else
			super.or(iter);
	}
	
	/** 
	 * Does in-place AND of the bits provided by the
	 *  iterator. 
	 */
	@Override
	public void and(IDocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.getDocID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			and(obs.mArr, obs.mWords);
			// advance after last doc that would be accepted if standard
			// iteration is used (to exhaust it):
			obs.advance(mNumBits);
			
		} else
			super.and(iter);
	}
	
	/** 
	 * Does in-place AND NOT of the bits provided by the
	 *  iterator. 
	 */
	@Override
	public void andNot(IDocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.getDocID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			andNot(obs.mArr, obs.mWords);
			// advance after last doc that would be accepted if standard
			// iteration is used (to exhaust it):
			obs.advance(mNumBits);
			
		} else
			super.andNot(iter);
	}
	
	@Override
	public OpenFixedBitSet clone() {
		return new OpenFixedBitSet(this);
	}
	
}
