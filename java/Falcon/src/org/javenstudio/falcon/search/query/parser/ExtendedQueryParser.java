package org.javenstudio.falcon.search.query.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryUtils;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.DisjunctionMaxQuery;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.MultiPhraseQuery;
import org.javenstudio.hornet.search.query.PhraseQuery;
import org.javenstudio.panda.analysis.StopFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.query.ParseException;
import org.javenstudio.panda.query.QueryParser;

/**
 * A subclass of QueryParser that supports aliasing fields for
 * constructing DisjunctionMaxQueries.
 */
public class ExtendedQueryParser extends DefaultQueryParser {
	
	protected enum QType {
		FIELD, PHRASE, PREFIX, WILDCARD, FUZZY, RANGE
	}
	
    /** A simple container for storing alias info */
    protected class Alias {
    	public float mTie;
    	public Map<String,Float> mFields;
    }
    
    static final ParseException sUnknownFieldException = new ParseException("UnknownField");
    static {
    	sUnknownFieldException.fillInStackTrace();
    }
    
    protected boolean mMakeDismax = true;
    private boolean mDisableCoord = true;
    protected boolean mAllowWildcard = true;
    // minimum number of clauses per phrase query...
    protected int mMinClauseSize = 0; 
    // used when constructing boosting part of query via sloppy phrases
    // allow exceptions to be thrown (for example on a missing field)
    protected boolean mExceptions; 
    
    private Map<String, IAnalyzer> mNonStopFilterAnalyzerPerField;
    private boolean mRemoveStopFilter;
    // for inner boolean queries produced from a single fieldQuery
    protected String mMinShouldMatch; 
    
    /**
     * Where we store a map from field name we expect to see in our query
     * string, to Alias object containing the fields to use in our
     * DisjunctionMaxQuery and the tiebreaker to use.
     */
    private Map<String,Alias> mAliases = new HashMap<String,Alias>(3);
    
    private QType mType;
    private String mField;
    private String mValue;
    private String mValue2;
    private boolean mBool;
    private boolean mBool2;
    private float mFloat;
    private int mSlop;
    
    public ExtendedQueryParser(QueryBuilder parser, String defaultField) {
    	super(parser, defaultField);
    	// don't trust that our parent class won't ever change it's default
    	setDefaultOperator(QueryParser.Operator.OR);
    }
    
    public void setRemoveStopFilter(boolean remove) {
    	mRemoveStopFilter = remove;
    }
    
    @Override
    protected IQuery getBooleanQuery(List<IBooleanClause> clauses, 
    		boolean disableCoord) throws ParseException {
    	IQuery q = super.getBooleanQuery(clauses, disableCoord);
    	if (q != null) 
    		q = QueryUtils.makeQueryable(q);
    	
    	return q;
    }
    
    /**
     * Add an alias to this query parser.
     *
     * @param field the field name that should trigger alias mapping
     * @param fieldBoosts the mapping from fieldname to boost value that
     *                    should be used to build up the clauses of the
     *                    DisjunctionMaxQuery.
     * @param tiebreaker to the tiebreaker to be used in the
     *                   DisjunctionMaxQuery
     * @see SolrPluginUtils#parseFieldBoosts
     */
    public void addAlias(String field, float tiebreaker,
    		Map<String,Float> fieldBoosts) {
    	Alias a = new Alias();
    	a.mTie = tiebreaker;
    	a.mFields = fieldBoosts;
    	mAliases.put(field, a);
    }
    
    /**
     * Returns the aliases found for a field.
     * Returns null if there are no aliases for the field
     * @return Alias
     */
    protected Alias getAlias(String field) {
    	return mAliases.get(field);
    }
    
    @Override
    protected IQuery getFieldQuery(String field, String val, 
    		boolean quoted) throws ParseException {
    	mType = QType.FIELD;
    	mField = field;
    	mValue= val;
    	mSlop = getPhraseSlop(); // unspecified
    	
    	return getAliasedQuery();
    }
    
    @Override
    protected IQuery getFieldQuery(String field, String val, 
    		int slop) throws ParseException {
    	mType = QType.PHRASE;
    	mField = field;
    	mValue = val;
    	mSlop = slop;
    	
    	return getAliasedQuery();
    }
    
    @Override
    protected IQuery getPrefixQuery(String field, String val) 
    		throws ParseException {
    	if (val.equals("") && field.equals("*")) 
    		return new MatchAllDocsQuery();
      
    	mType = QType.PREFIX;
    	mField = field;
    	mValue = val;
    	
    	return getAliasedQuery();
    }
    
    @Override
    protected IQuery newFieldQuery(IAnalyzer analyzer, String field, 
    		String queryText, boolean quoted) throws ParseException {
    	try {
    		IAnalyzer actualAnalyzer;
    		if (mRemoveStopFilter) {
    			if (mNonStopFilterAnalyzerPerField == null) 
    				mNonStopFilterAnalyzerPerField = new HashMap<String, IAnalyzer>();
	        
    			actualAnalyzer = mNonStopFilterAnalyzerPerField.get(field);
    			if (actualAnalyzer == null) 
	         	actualAnalyzer = noStopwordFilterAnalyzer(field);
	        
    		} else {
    			actualAnalyzer = getSchema().getFieldType(field).getQueryAnalyzer();
    		}
    		
    		return super.newFieldQuery(actualAnalyzer, field, queryText, quoted);
    	} catch (ErrorException ex) { 
    		throw new ParseException(ex.toString(), ex);
    	}
    }
    
    @Override
    protected IQuery getRangeQuery(String field, String a, String b, 
    		boolean startInclusive, boolean endInclusive) throws ParseException {
    	mType = QType.RANGE;
    	mField = field;
    	mValue = a;
    	mValue2 = b;
    	mBool = startInclusive;
    	mBool2 = endInclusive;
    	
    	return getAliasedQuery();
    }
    
    @Override
    protected IQuery getWildcardQuery(String field, String val) throws ParseException {
    	if (val.equals("*")) {
    		if (field.equals("*")) 
    			return new MatchAllDocsQuery();
    		else 
    			return getPrefixQuery(field,"");
    	}
    	
    	mType = QType.WILDCARD;
    	mField = field;
    	mValue = val;
    	
    	return getAliasedQuery();
    }
    
    @Override
    protected IQuery getFuzzyQuery(String field, String val, 
    		float minSimilarity) throws ParseException {
    	mType = QType.FUZZY;
    	mField = field;
    	mValue = val;
    	mFloat = minSimilarity;
    	
    	return getAliasedQuery();
    }
    
    /**
     * Delegates to the super class unless the field has been specified
     * as an alias -- in which case we recurse on each of
     * the aliased fields, and the results are composed into a
     * DisjunctionMaxQuery.  (so yes: aliases which point at other
     * aliases should work)
     */
    protected IQuery getAliasedQuery() throws ParseException {
    	Alias a = mAliases.get(mField);
    	validateCyclicAliasing(mField);
    	
    	if (a != null) {
    		List<IQuery> lst = getQueries(a);
    		if (lst == null || lst.size() == 0)
    			return getQuery();
    		
    		// make a DisjunctionMaxQuery in this case too... it will stop
    		// the "mm" processing from making everything required in the case
    		// that the query expanded to multiple clauses.
    		// DisMaxQuery.rewrite() removes itself if there is just a single clause anyway.
    		// if (lst.size()==1) return lst.get(0);
        
    		if (mMakeDismax) {
    			DisjunctionMaxQuery q = new DisjunctionMaxQuery(lst, a.mTie);
    			return q;
    			
    		} else {
    			// should we disable coord?
    			BooleanQuery q = new BooleanQuery(mDisableCoord);
    			for (IQuery sub : lst) {
    				q.add(sub, IBooleanClause.Occur.SHOULD);
    			}
    			return q;
    		}
    		
    	} else {
    		// verify that a fielded query is actually on a field that exists... if not,
    		// then throw an exception to get us out of here, and we'll treat it like a
    		// literal when we try the escape+re-parse.
    		if (mExceptions) {
    			SchemaFieldType ft = getSchema().getFieldTypeNoEx(mField);
    			if (ft == null && MagicFieldName.get(mField) == null) 
    				throw new ParseException("unknown field: " + mField);
    		}
        
    		return getQuery();
    	}
	}
    
    /**
     * Validate there is no cyclic referencing in the aliasing
     */
    private void validateCyclicAliasing(String field) throws ParseException {
    	Set<String> set = new HashSet<String>();
    	set.add(field);
    	
    	if (validateField(field, set)) 
    		throw new ParseException("Field aliases lead to a cycle");
    }
    
    private boolean validateField(String field, Set<String> set) {
    	if (this.getAlias(field) == null) 
    		return false;
      
    	boolean hascycle = false;
    	
    	for (String referencedField : this.getAlias(field).mFields.keySet()) {
    		if (!set.add(referencedField)) {
    			hascycle = true;
    			
    		} else {
    			if (validateField(referencedField, set)) 
    				hascycle = true;
    			
    			set.remove(referencedField);
    		}
    	}
    	return hascycle;
    }
    
    protected List<IQuery> getQueries(Alias a) throws ParseException {
    	if (a == null) return null;
    	if (a.mFields.size() == 0) return null;
    	
    	List<IQuery> lst= new ArrayList<IQuery>(4);
      
    	for (String f : a.mFields.keySet()) {
    		mField = f;
    		
    		IQuery sub = getAliasedQuery();
    		if (sub != null) {
    			Float boost = a.mFields.get(f);
    			if (boost != null) 
    				sub.setBoost(boost);
          
    			lst.add(sub);
    		}
    	}
    	
    	return lst;
    }
    
    private IQuery getQuery() {
    	try {
    		switch (mType) {
    		case FIELD:  // fallthrough
    		case PHRASE:
    			IQuery query = super.getFieldQuery(mField, mValue, mType == QType.PHRASE);
    			
    			// A BooleanQuery is only possible from getFieldQuery if it came from
    			// a single whitespace separated term. In this case, check the coordination
    			// factor on the query: if its enabled, that means we aren't a set of synonyms
    			// but instead multiple terms from one whitespace-separated term, we must
    			// apply minShouldMatch here so that it works correctly with other things
    			// like aliasing.
    			if (query instanceof BooleanQuery) {
    				BooleanQuery bq = (BooleanQuery) query;
    				if (!bq.isCoordDisabled()) 
    					QueryUtils.setMinShouldMatch(bq, mMinShouldMatch);
    			}
    			
    			if (query instanceof PhraseQuery) {
    				PhraseQuery pq = (PhraseQuery)query;
    				if (mMinClauseSize > 1 && pq.getTerms().length < mMinClauseSize) 
    					return null;
    				
    				pq.setSlop(mSlop);
    				
    			} else if (query instanceof MultiPhraseQuery) {
    				MultiPhraseQuery pq = (MultiPhraseQuery)query;
    				if (mMinClauseSize > 1 && pq.getTermArrays().size() < mMinClauseSize) 
    					return null;
    				
    				pq.setSlop(mSlop);
    				
    			} else if (mMinClauseSize > 1) {
    				// if it's not a type of phrase query, it doesn't meet 
    				// the minClauseSize requirements
    				return null;
    			}
    			
    			return query;
    			
    		case PREFIX: return super.getPrefixQuery(mField, mValue);
    		case WILDCARD: return super.getWildcardQuery(mField, mValue);
    		case FUZZY: return super.getFuzzyQuery(mField, mValue, mFloat);
    		case RANGE: return super.getRangeQuery(mField, mValue, mValue2, mBool, mBool2);
    		}
    		
    		return null;
    	} catch (Throwable e) {
    		// an exception here is due to the field query not 
    		// being compatible with the input text
    		// for example, passing a string to a numeric field.
    		return null;
    	}
    }
    
    private IAnalyzer noStopwordFilterAnalyzer(String fieldName) throws ErrorException {
    	SchemaFieldType ft = getSchema().getFieldType(fieldName);
    	IAnalyzer qa = ft.getQueryAnalyzer();
    	if (!(qa instanceof TokenizerChain)) 
    		return qa;
      
    	TokenizerChain tcq = (TokenizerChain) qa;
    	IAnalyzer ia = ft.getAnalyzer();
    	if (ia == qa || !(ia instanceof TokenizerChain)) 
    		return qa;
      
    	TokenizerChain tci = (TokenizerChain) ia;
      
    	// make sure that there isn't a stop filter in the indexer
    	for (TokenFilterFactory tf : tci.getTokenFilterFactories()) {
    		if (tf instanceof StopFilterFactory) 
    			return qa;
    	}
      
    	// now if there is a stop filter in the query analyzer, remove it
    	int stopIdx = -1;
    	TokenFilterFactory[] facs = tcq.getTokenFilterFactories();
      
    	for (int i = 0; i < facs.length; i++) {
    		TokenFilterFactory tf = facs[i];
    		if (tf instanceof StopFilterFactory) {
    			stopIdx = i;
    			break;
    		}
    	}
      
    	if (stopIdx == -1) {
    		// no stop filter exists
    		return qa;
    	}
      
    	TokenFilterFactory[] newtf = new TokenFilterFactory[facs.length - 1];
    	for (int i = 0, j = 0; i < facs.length; i++) {
    		if (i == stopIdx) continue;
    		newtf[j++] = facs[i];
    	}
      
    	TokenizerChain newa = new TokenizerChain(tcq.getTokenizerFactory(), newtf);
    	newa.setPositionIncrementGap(tcq.getPositionIncrementGap(fieldName));
    	return newa;
	}
    
}
