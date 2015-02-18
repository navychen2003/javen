package org.javenstudio.falcon.search.query.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.hits.FieldParams;
import org.javenstudio.falcon.search.params.DisMaxParams;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.util.Params;

/**
 * Simple container for configuration information used when parsing queries
 */
public class ExtendedDismaxConfiguration {
	
	/** shorten the class references for utilities */
	private static interface DMParams extends DisMaxParams {
		
		/**
	     * User fields. The fields that can be used by the end user 
	     * to create field-specific queries.
	     */
	    public static String UF = "uf";
	    
	    /**
	     * Lowercase Operators. If set to true, 'or' and 'and' will 
	     * be considered OR and AND, otherwise
	     * lowercase operators will be considered terms to search for.
	     */
	    public static String LOWERCASE_OPS = "lowercaseOperators";

	    /**
	     * Multiplicative boost. Boost functions which scores 
	     * are going to be multiplied to the score
	     * of the main query (instead of just added, like with bf)
	     */
	    public static String MULT_BOOST = "boost";

	    /**
	     * If set to true, stopwords are removed from the query.
	     */
	    public static String STOPWORDS = "stopwords";
	    
	}
	
    /** 
     * The field names specified by 'qf' that (most) clauses will 
     * be queried against 
     */
    protected Map<String,Float> mQueryFields;
    
    /** 
     * The field names specified by 'uf' that users are 
     * allowed to include literally in their query string.  The Float
     * boost values will be applied automatically to any clause using that 
     * field name. '*' will be treated as an alias for any 
     * field that exists in the schema. Wildcards are allowed to
     * express dynamicFields.
     */
    protected ExtendedUserFields mUserFields;
    
    protected String[] mBoostParams;
    protected String[] mMultBoosts;
    protected Params mParams;
    protected String mMinShouldMatch;
    
    protected List<FieldParams> mAllPhraseFields;
    protected float mTiebreaker;
    protected int mQuerySlop;
    protected boolean mStopwords;
    protected String mAltQ;
    protected boolean mLowercaseOperators;
    protected String[] mBoostFuncs;
    
    public ExtendedDismaxConfiguration(Params localParams,
    		Params params, ISearchRequest req) throws ErrorException {
    	mParams = Params.wrapDefaults(localParams, params);
    	mMinShouldMatch = DisMaxQueryBuilder.parseMinShouldMatch(
    			req.getSearchCore().getSchema(), mParams);
    	mUserFields = new ExtendedUserFields(
    			QueryUtils.parseFieldBoosts(mParams.getParams(DMParams.UF)));
        mQueryFields = DisMaxQueryBuilder.parseQueryFields(
        		req.getSearchCore().getSchema(), mParams);
        
        // Phrase slop array
        int pslop[] = new int[4];
        pslop[0] = mParams.getInt(DisMaxParams.PS, 0);
        pslop[2] = mParams.getInt(DisMaxParams.PS2, pslop[0]);
        pslop[3] = mParams.getInt(DisMaxParams.PS3, pslop[0]);
      
        List<FieldParams> phraseFields = QueryUtils.parseFieldBoostsAndSlop(
        		mParams.getParams(DMParams.PF), 0, pslop[0]);
        List<FieldParams> phraseFields2 = QueryUtils.parseFieldBoostsAndSlop(
        		mParams.getParams(DMParams.PF2), 2, pslop[2]);
        List<FieldParams> phraseFields3 = QueryUtils.parseFieldBoostsAndSlop(
        		mParams.getParams(DMParams.PF3), 3, pslop[3]);
      
        mAllPhraseFields = new ArrayList<FieldParams>(phraseFields.size() 
        		+ phraseFields2.size() + phraseFields3.size());
        mAllPhraseFields.addAll(phraseFields);
        mAllPhraseFields.addAll(phraseFields2);
        mAllPhraseFields.addAll(phraseFields3);
      
        mTiebreaker = mParams.getFloat(DisMaxParams.TIE, 0.0f);
        mQuerySlop = mParams.getInt(DisMaxParams.QS, 0);
        mStopwords = mParams.getBool(DMParams.STOPWORDS, true);
        mAltQ = mParams.get(DisMaxParams.ALTQ);
        mLowercaseOperators = mParams.getBool(DMParams.LOWERCASE_OPS, true);
      
        // Boosting Query
        mBoostParams = mParams.getParams(DisMaxParams.BQ);
        mBoostFuncs = mParams.getParams(DisMaxParams.BF);
        mMultBoosts = mParams.getParams(DMParams.MULT_BOOST);
    }
    
    /**
     * @return true if there are valid multiplicative boost queries
     */
    public boolean hasMultiplicativeBoosts() {
    	return mMultBoosts!=null && mMultBoosts.length > 0;
    }
    
    /**
     * @return true if there are valid boost functions
     */
    public boolean hasBoostFunctions() {
    	return mBoostFuncs != null && mBoostFuncs.length != 0;
    }
    
    /**
     * @return true if there are valid boost params
     */
    public boolean hasBoostParams() {
    	return mBoostParams != null && mBoostParams.length > 0;
    }
    
    public List<FieldParams> getAllPhraseFields() {
    	return mAllPhraseFields;
    }
    
}
