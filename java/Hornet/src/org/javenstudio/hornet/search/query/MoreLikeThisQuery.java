package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.search.Query;

/**
 * A simple wrapper for MoreLikeThis for use in scenarios where a Query object is required eg
 * in custom QueryParser extensions. At query.rewrite() time the reader is used to construct the
 * actual MoreLikeThis object and obtain the real Query object.
 */
public class MoreLikeThisQuery extends Query {

	private String mLikeText;
	private String[] mMoreLikeFields;
	private IAnalyzer mAnalyzer;
	private final String mFieldName;
	private float mPercentTermsToMatch = 0.3f;
	private int mMinTermFrequency = 1;
	private int mMaxQueryTerms = 5;
	private Set<?> mStopWords = null;
	private int mMinDocFreq = -1;

	/**
	 * @param moreLikeFields fields used for similarity measure
	 */
	public MoreLikeThisQuery(String likeText, String[] moreLikeFields, 
			IAnalyzer analyzer, String fieldName) {
		mLikeText = likeText;
		mMoreLikeFields = moreLikeFields;
		mAnalyzer = analyzer;
		mFieldName = fieldName;
	}

	@Override
	public Query rewrite(IIndexReader reader) throws IOException {
		MoreLikeThis mlt = new MoreLikeThis(reader);

		mlt.setFieldNames(mMoreLikeFields);
		mlt.setAnalyzer(mAnalyzer);
		mlt.setMinTermFreq(mMinTermFrequency);
		
		if (mMinDocFreq >= 0) 
			mlt.setMinDocFreq(mMinDocFreq);
		
		mlt.setMaxQueryTerms(mMaxQueryTerms);
		mlt.setStopWords(mStopWords);
		
		BooleanQuery bq = (BooleanQuery) mlt.like(new StringReader(mLikeText), mFieldName);
		IBooleanClause[] clauses = bq.getClauses();
		//make at least half the terms match
		bq.setMinimumNumberShouldMatch((int) (clauses.length * mPercentTermsToMatch));
		
		return bq;
	}

	/** 
	 * (non-Javadoc)
	 * @see Query#toString(java.lang.String)
	 */
	@Override
	public String toString(String field) {
		return "like:" + mLikeText;
	}

	public float getPercentTermsToMatch() {
		return mPercentTermsToMatch;
	}

	public void setPercentTermsToMatch(float percentTermsToMatch) {
		mPercentTermsToMatch = percentTermsToMatch;
	}

	public IAnalyzer getAnalyzer() {
		return mAnalyzer;
	}

	public void setAnalyzer(IAnalyzer analyzer) {
		mAnalyzer = analyzer;
	}

	public String getLikeText() {
		return mLikeText;
	}

	public void setLikeText(String likeText) {
		mLikeText = likeText;
	}

	public int getMaxQueryTerms() {
		return mMaxQueryTerms;
	}

	public void setMaxQueryTerms(int maxQueryTerms) {
		mMaxQueryTerms = maxQueryTerms;
	}

	public int getMinTermFrequency() {
		return mMinTermFrequency;
	}

	public void setMinTermFrequency(int minTermFrequency) {
		mMinTermFrequency = minTermFrequency;
	}

	public String[] getMoreLikeFields() {
		return mMoreLikeFields;
	}

	public void setMoreLikeFields(String[] moreLikeFields) {
		mMoreLikeFields = moreLikeFields;
	}

	public Set<?> getStopWords() {
		return mStopWords;
	}

	public void setStopWords(Set<?> stopWords) {
		mStopWords = stopWords;
	}

	public int getMinDocFreq() {
		return mMinDocFreq;
	}

	public void setMinDocFreq(int minDocFreq) {
		mMinDocFreq = minDocFreq;
	}
	
}
