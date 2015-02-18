package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.search.Query;

/**
 * A rewrite method that first translates each term into
 * {@link BooleanClause.Occur#SHOULD} clause in a BooleanQuery, but the scores
 * are only computed as the boost.
 * <p>
 * This rewrite method only uses the top scoring terms so it will not overflow
 * the boolean max clause count.
 * 
 * @see #setRewriteMethod
 */
public final class BoostOnlyBooleanQueryRewrite 
		extends TopTermsRewrite<BooleanQuery> {
  
	/** 
	 * Create a TopTermsBoostOnlyBooleanQueryRewrite for 
	 * at most <code>size</code> terms.
	 * <p>
	 * NOTE: if {@link BooleanQuery#getMaxClauseCount} is smaller than 
	 * <code>size</code>, then it will be used instead. 
	 */
	public BoostOnlyBooleanQueryRewrite(int size) {
		super(size);
	}
  
	@Override
	protected int getMaxSize() {
		return BooleanQuery.getMaxClauseCount();
	}
  
	@Override
	protected BooleanQuery getTopLevelQuery() {
		return new BooleanQuery(true);
	}
  
	@Override
	protected void addClause(BooleanQuery topLevel, ITerm term, int docFreq, 
			float boost, ITermContext states) {
		final Query q = new ConstantScoreQuery(new TermQuery(term, states));
		q.setBoost(boost);
		topLevel.add(q, BooleanClause.Occur.SHOULD);
	}
  
}
