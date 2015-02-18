package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefHash;

class ConstantScoreAutoRewrite extends TermCollectingRewrite<BooleanQuery> {

	// Defaults derived from rough tests with a 20.0 million
	// doc Wikipedia index.  With more than 350 terms in the
	// query, the filter method is fastest:
	public static final int DEFAULT_TERM_COUNT_CUTOFF = 350;

	// If the query will hit more than 1 in 1000 of the docs
	// in the index (0.1%), the filter method is fastest:
	public static final double DEFAULT_DOC_COUNT_PERCENT = 0.1;

	private int mTermCountCutoff = DEFAULT_TERM_COUNT_CUTOFF;
	private double mDocCountPercent = DEFAULT_DOC_COUNT_PERCENT;

	/** 
	 * If the number of terms in this query is equal to or
	 *  larger than this setting then {@link
	 *  MultiTermQuery#CONSTANT_SCORE_FILTER_REWRITE} is used. 
	 */
	public void setTermCountCutoff(int count) {
		mTermCountCutoff = count;
	}

	/** @see #setTermCountCutoff */
	public int getTermCountCutoff() {
		return mTermCountCutoff;
	}

	/** 
	 * If the number of documents to be visited in the
	 *  postings exceeds this specified percentage of the
	 *  maxDoc() for the index, then {@link
	 *  MultiTermQuery#CONSTANT_SCORE_FILTER_REWRITE} is used.
	 *  @param percent 0.0 to 100.0 
	 */
	public void setDocCountPercent(double percent) {
		mDocCountPercent = percent;
	}

	/** @see #setDocCountPercent */
	public double getDocCountPercent() {
		return mDocCountPercent;
	}

	@Override
	protected BooleanQuery getTopLevelQuery() {
		return new BooleanQuery(true);
	}
  
	@Override
	protected void addClause(BooleanQuery topLevel, ITerm term, int docFreq, 
			float boost /*ignored*/, ITermContext states) {
		topLevel.add(new TermQuery(term, states), BooleanClause.Occur.SHOULD);
	}

	@Override
	public IQuery rewrite(final IIndexReader reader, final MultiTermQuery query) throws IOException {
		// Get the enum and start visiting terms.  If we
		// exhaust the enum before hitting either of the
		// cutoffs, we use ConstantBooleanQueryRewrite; else,
		// ConstantFilterRewrite:
		final int docCountCutoff = (int) ((mDocCountPercent / 100.) * reader.getMaxDoc());
		final int termCountLimit = Math.min(BooleanQuery.getMaxClauseCount(), mTermCountCutoff);

		final CutOffTermCollector col = new CutOffTermCollector(docCountCutoff, termCountLimit);
		collectTerms(reader, query, col);
		
		final int size = col.getPendingTerms().size();
		if (col.hasCutOff()) {
			return MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE.rewrite(reader, query);
			
		} else if (size == 0) {
			return getTopLevelQuery();
			
		} else {
			final BooleanQuery bq = getTopLevelQuery();
			final BytesRefHash pendingTerms = col.getPendingTerms();
			final int sort[] = pendingTerms.sort(col.getTermsEnum().getComparator());
			
			for (int i = 0; i < size; i++) {
				final int pos = sort[i];
				// docFreq is not used for constant score here, we pass 1
				// to explicitely set a fake value, so it's not calculated
				addClause(bq, new Term(query.getFieldName(), pendingTerms.get(pos, new BytesRef())), 
						1, 1.0f, col.getTermArray().getTermStateAt(pos));
			}
			
			// Strip scores
			final Query result = new ConstantScoreQuery(bq);
			result.setBoost(query.getBoost());
			return result;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 1279;
		return (int) (prime * mTermCountCutoff + Double.doubleToLongBits(mDocCountPercent));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		ConstantScoreAutoRewrite other = (ConstantScoreAutoRewrite) obj;
		if (other.mTermCountCutoff != mTermCountCutoff) 
			return false;

		if (Double.doubleToLongBits(other.mDocCountPercent) != 
			Double.doubleToLongBits(mDocCountPercent)) 
			return false;
    
		return true;
	}
	
}
