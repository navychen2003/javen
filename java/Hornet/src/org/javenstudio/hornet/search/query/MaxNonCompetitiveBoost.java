package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Add this {@link Attribute} to a fresh {@link AttributeSource} before calling
 * {@link MultiTermQuery#getTermsEnum(Terms,AttributeSource)}.
 * {@link FuzzyQuery} is using this to control its internal behaviour
 * to only return competitive terms.
 * <p><b>Please note:</b> This attribute is intended to be added by the {@link MultiTermQuery.RewriteMethod}
 * to an empty {@link AttributeSource} that is shared for all segments
 * during query rewrite. This attribute source is passed to all segment enums
 * on {@link MultiTermQuery#getTermsEnum(Terms,AttributeSource)}.
 * {@link TopTermsRewrite} uses this attribute to
 * inform all enums about the current boost, that is not competitive.
 */
public class MaxNonCompetitiveBoost {

	private float mMaxNonCompetitiveBoost = 1.0f;
	private BytesRef mCompetitiveTerm = null;
	
	/** This is the maximum boost that would not be competitive. */
	public void setMaxNonCompetitiveBoost(float maxNonCompetitiveBoost) { 
		mMaxNonCompetitiveBoost = maxNonCompetitiveBoost;
	}
	
	/** 
	 * This is the maximum boost that would not be competitive. 
	 * Default is negative infinity, which means every term is competitive. 
	 */
	public float getMaxNonCompetitiveBoost() { 
		return mMaxNonCompetitiveBoost;
	}
	
	/** This is the term or <code>null</code> of the term that triggered the boost change. */
	public void setCompetitiveTerm(BytesRef competitiveTerm) { 
		mCompetitiveTerm = competitiveTerm;
	}
	
	/** 
	 * This is the term or <code>null</code> of the term that triggered the boost change. 
	 * Default is <code>null</code>, which means every term is competitoive. 
	 */
	public BytesRef getCompetitiveTerm() { 
		return mCompetitiveTerm;
	}
	
}
