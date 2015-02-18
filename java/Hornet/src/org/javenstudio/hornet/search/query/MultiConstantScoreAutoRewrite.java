package org.javenstudio.hornet.search.query;

/** 
 * A rewrite method that tries to pick the best
 *  constant-score rewrite method based on term and
 *  document counts from the query.  If both the number of
 *  terms and documents is small enough, then {@link
 *  #CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE} is used.
 *  Otherwise, {@link #CONSTANT_SCORE_FILTER_REWRITE} is
 *  used.
 */
public class MultiConstantScoreAutoRewrite extends ConstantScoreAutoRewrite {
	
}

