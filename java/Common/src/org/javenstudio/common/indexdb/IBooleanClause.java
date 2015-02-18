package org.javenstudio.common.indexdb;

public interface IBooleanClause {

	/** Specifies how clauses are to occur in matching documents. */
	public static enum Occur {

		/** Use this operator for clauses that <i>must</i> appear in the matching documents. */
		MUST     { @Override public String toString() { return "+"; } },

		/** 
		 * Use this operator for clauses that <i>should</i> appear in the 
		 * matching documents. For a BooleanQuery with no <code>MUST</code> 
		 * clauses one or more <code>SHOULD</code> clauses must match a document 
		 * for the BooleanQuery to match.
		 * @see BooleanQuery#setMinimumNumberShouldMatch
		 */
		SHOULD   { @Override public String toString() { return "";  } },

		/** 
		 * Use this operator for clauses that <i>must not</i> appear in the matching documents.
		 * Note that it is not possible to search for queries that only consist
		 * of a <code>MUST_NOT</code> clause. 
		 */
		MUST_NOT { @Override public String toString() { return "-"; } };

	}
	
	public Occur getOccur();
	public IQuery getQuery();
	
	public boolean isProhibited();
	public boolean isRequired();
	
}
