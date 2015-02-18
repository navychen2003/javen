package org.javenstudio.common.indexdb.search;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IWeight;

/** 
 * The abstract base class for queries.
 * <p>Instantiable subclasses are:
 * <ul>
 * <li> {@link TermQuery}
 * <li> {@link MultiTermQuery}
 * <li> {@link BooleanQuery}
 * <li> {@link WildcardQuery}
 * <li> {@link PhraseQuery}
 * <li> {@link PrefixQuery}
 * <li> {@link MultiPhraseQuery}
 * <li> {@link FuzzyQuery}
 * <li> {@link TermRangeQuery}
 * <li> {@link NumericRangeQuery}
 * <li> {@link SpanQuery}
 * </ul>
 */
public abstract class Query implements IQuery {
	
	private float mBoost = 1.0f;	// query boost factor

	/** 
	 * Sets the boost for this query clause to <code>b</code>.  Documents
	 * matching this clause will (in addition to the normal weightings) have
	 * their score multiplied by <code>b</code>.
	 */
	@Override
	public void setBoost(float b) { 
		mBoost = b; 
	}

	/** 
	 * Gets the boost for this clause.  Documents matching
	 * this clause will (in addition to the normal weightings) have their score
	 * multiplied by <code>b</code>.   The boost is 1.0 by default.
	 */
	@Override
	public float getBoost() { 
		return mBoost; 
	}

	/**
	 * Expert: Constructs an appropriate Weight implementation for this query.
	 * 
	 * <p>
	 * Only implemented by primitive queries, which re-write to themselves.
	 */
	@Override
	public IWeight createWeight(ISearcher searcher) throws IOException {
		throw new UnsupportedOperationException("Query " + this + " does not implement createWeight");
	}

	/** 
	 * Expert: called to re-write queries into primitive queries. For example,
	 * a PrefixQuery will be rewritten into a BooleanQuery that consists
	 * of TermQuerys.
	 */
	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		return this;
	}
  
	/**
	 * Expert: adds all terms occurring in this query to the terms set. Only
	 * works if this query is in its {@link #rewrite rewritten} form.
	 * 
	 * @throws UnsupportedOperationException if this query is not yet rewritten
	 */
	@Override
	public void extractTerms(Set<ITerm> terms) {
		// needs to be implemented by query subclasses
		throw new UnsupportedOperationException();
	}

	/** 
	 * Prints a query to a string, with <code>field</code> assumed to be the 
	 * default field and omitted.
	 */
	public abstract String toString(String field);
	
	/** Prints a query to a string. */
	@Override
	public String toString() {
		return toString("");
	}
	
	/** Returns a clone of this query. */
	@Override
	public Query clone() {
		try {
			return (Query)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone not supported: " + e.getMessage());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(mBoost);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Query other = (Query) obj;
		if (Float.floatToIntBits(mBoost) != Float.floatToIntBits(other.mBoost))
			return false;
		return true;
	}
	
}
