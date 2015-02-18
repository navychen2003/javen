package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IQuery;

/** A clause in a BooleanQuery. */
public class BooleanClause implements IBooleanClause {

	/** The query whose matching documents are combined by the boolean query. */
	private IQuery mQuery;
	private Occur mOccur;

	/** Constructs a BooleanClause. */ 
	public BooleanClause(IQuery query, Occur occur) {
		mQuery = query;
		mOccur = occur;
	}

	public Occur getOccur() { return mOccur; }
	public void setOccur(Occur occur) { mOccur = occur; }

	public IQuery getQuery() { return mQuery; }
	public void setQuery(IQuery query) { mQuery = query; }
  
	public boolean isProhibited() {
		return Occur.MUST_NOT == mOccur;
	}

	public boolean isRequired() {
		return Occur.MUST == mOccur;
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof BooleanClause))
			return false;
		BooleanClause other = (BooleanClause)o;
		return this.mQuery.equals(other.mQuery) && this.mOccur == other.mOccur;
	}

	/** Returns a hash code value for this object.*/
	@Override
	public int hashCode() {
		return mQuery.hashCode() ^ (Occur.MUST == mOccur?1:0) ^ (Occur.MUST_NOT == mOccur?2:0);
	}

	@Override
	public String toString() {
		return mOccur.toString() + mQuery.toString();
	}
	
}
