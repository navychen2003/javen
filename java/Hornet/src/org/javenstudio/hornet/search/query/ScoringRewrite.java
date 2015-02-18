package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.TooManyClauses;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.term.TermContext;

/** 
 * Base rewrite method that translates each term into a query, and keeps
 * the scores as computed by the query.
 * <p>
 * Only public to be accessible by spans package. 
 */
public abstract class ScoringRewrite<Q extends IQuery> 
		extends TermCollectingRewrite<Q> {

	/** 
	 * A rewrite method that first translates each term into
	 *  {@link BooleanClause.Occur#SHOULD} clause in a
	 *  BooleanQuery, and keeps the scores as computed by the
	 *  query.  Note that typically such scores are
	 *  meaningless to the user, and require non-trivial CPU
	 *  to compute, so it's almost always better to use {@link
	 *  MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} instead.
	 *
	 *  <p><b>NOTE</b>: This rewrite method will hit {@link
	 *  BooleanQuery.TooManyClauses} if the number of terms
	 *  exceeds {@link BooleanQuery#getMaxClauseCount}.
	 *
	 *  @see MultiTermQuery#setRewriteMethod 
	 */
	public final static ScoringRewrite<BooleanQuery> SCORING_BOOLEAN_QUERY_REWRITE = 
			new ScoringRewrite<BooleanQuery>() {
		@Override
		protected BooleanQuery getTopLevelQuery() {
			return new BooleanQuery(true);
		}
    
		@Override
		protected void addClause(BooleanQuery topLevel, ITerm term, int docCount,
				float boost, ITermContext states) {
			final TermQuery tq = new TermQuery(term, states);
			tq.setBoost(boost);
			topLevel.add(tq, BooleanClause.Occur.SHOULD);
		}
    
		@Override
		protected void checkMaxClauseCount(int count) {
			if (count > BooleanQuery.getMaxClauseCount())
				throw new TooManyClauses(count);
		}
	};
  
	/** 
	 * Like {@link #SCORING_BOOLEAN_QUERY_REWRITE} except
	 *  scores are not computed.  Instead, each matching
	 *  document receives a constant score equal to the
	 *  query's boost.
	 * 
	 *  <p><b>NOTE</b>: This rewrite method will hit {@link
	 *  BooleanQuery.TooManyClauses} if the number of terms
	 *  exceeds {@link BooleanQuery#getMaxClauseCount}.
	 *
	 *  @see MultiTermQuery#setRewriteMethod 
	 */
	public final static RewriteMethod CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE = 
			new RewriteMethod() {
		@Override
		public IQuery rewrite(IIndexReader reader, MultiTermQuery query) throws IOException {
			final BooleanQuery bq = (BooleanQuery)SCORING_BOOLEAN_QUERY_REWRITE.rewrite(reader, query);
			// TODO: if empty boolean query return NullQuery?
			if (bq.clauses().isEmpty())
				return bq;
			
			// strip the scores off
			final Query result = new ConstantScoreQuery(bq);
			result.setBoost(query.getBoost());
			
			return result;
		}
	};

	/** 
	 * This method is called after every new term to check if the number of max clauses
	 * (e.g. in BooleanQuery) is not exceeded. Throws the corresponding {@link RuntimeException}. 
	 */
	protected abstract void checkMaxClauseCount(int count) throws IOException;
  
	@Override
	public final Q rewrite(final IIndexReader reader, final MultiTermQuery query) throws IOException {
		final Q result = getTopLevelQuery();
		final ParallelArraysTermCollector<Q> col = new ParallelArraysTermCollector<Q>(this);
		
		collectTerms(reader, query, col);
    
		final int size = col.getTerms().size();
		if (size > 0) {
			final int sort[] = col.getTerms().sort(col.getTermsEnum().getComparator());
			final float[] boost = col.getTermArray().getBoosts();
			final TermContext[] termStates = col.getTermArray().getTermStates();
			
			for (int i = 0; i < size; i++) {
				final int pos = sort[i];
				final Term term = new Term(query.getFieldName(), col.getTerms().get(pos, new BytesRef()));
				assert reader.getDocFreq(term) == termStates[pos].getDocFreq();
				
				addClause(result, term, termStates[pos].getDocFreq(), 
						query.getBoost() * boost[pos], termStates[pos]);
			}
		}
		
		return result;
	}
	
}
