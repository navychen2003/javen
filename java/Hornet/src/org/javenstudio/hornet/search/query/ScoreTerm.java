package org.javenstudio.hornet.search.query;

import java.util.Comparator;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.term.TermContext;

public final class ScoreTerm implements Comparable<ScoreTerm> {
	
	private final Comparator<BytesRef> mTermComp;
	private final BytesRef mBytes = new BytesRef();
	private final TermContext mTermState;
	private float mBoost = 1.0f;
	
	public ScoreTerm(Comparator<BytesRef> termComp, TermContext termState) {
		mTermComp = termComp;
		mTermState = termState;
	}

	public final Comparator<BytesRef> getTermComparator() { return mTermComp; }
	
	public final TermContext getTermState() { return mTermState; }
	public final BytesRef getBytes() { return mBytes; }
	public final float getBoost() { return mBoost; }
	
	public final void setBoost(float boost) { mBoost = boost; }
	
	@Override
	public int compareTo(ScoreTerm other) {
		if (mBoost == other.mBoost)
			return mTermComp.compare(other.mBytes, this.mBytes);
		else
			return Float.compare(this.mBoost, other.mBoost);
	}
	
}
