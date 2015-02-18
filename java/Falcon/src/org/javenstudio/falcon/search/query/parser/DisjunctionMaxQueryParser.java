package org.javenstudio.falcon.search.query.parser;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.hornet.search.query.DisjunctionMaxQuery;
import org.javenstudio.panda.query.ParseException;
import org.javenstudio.panda.query.QueryParser;

/**
 * A subclass of QueryParser that supports aliasing fields for
 * constructing DisjunctionMaxQueries.
 */
public class DisjunctionMaxQueryParser extends BaseQueryParser {

    /** 
     * A simple container for storing alias info
     * @see #aliases
     */
    protected static class Alias {
    	protected float mTie;
    	protected Map<String,Float> mFields;
    }

    /**
     * Where we store a map from field name we expect to see in our query
     * string, to Alias object containing the fields to use in our
     * DisjunctionMaxQuery and the tiebreaker to use.
     */
    protected Map<String,Alias> mAliases = new HashMap<String,Alias>(3);
    
    public DisjunctionMaxQueryParser(QueryBuilder qp, String defaultField) {
    	super(qp, defaultField);
    	// don't trust that our parent class won't ever change it's default
    	setDefaultOperator(QueryParser.Operator.OR);
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
     * @see PluginUtils#parseFieldBoosts
     */
    public void addAlias(String field, float tiebreaker,
    		Map<String,Float> fieldBoosts) {
    	Alias a = new Alias();
    	a.mTie = tiebreaker;
    	a.mFields = fieldBoosts;
    	
    	mAliases.put(field, a);
    }

    /**
     * Delegates to the super class unless the field has been specified
     * as an alias -- in which case we recurse on each of
     * the aliased fields, and the results are composed into a
     * DisjunctionMaxQuery.  (so yes: aliases which point at other
     * aliases should work)
     */
    @Override
    protected IQuery getFieldQuery(String field, String queryText, boolean quoted)
    		throws ParseException {
    	if (mAliases.containsKey(field)) {

    		Alias a = mAliases.get(field);
    		DisjunctionMaxQuery q = new DisjunctionMaxQuery(a.mTie);

    		/** 
    		 * we might not get any valid queries from delegation,
    		 * in which case we should return null
    		 */
    		boolean ok = false;

    		for (String f : a.mFields.keySet()) {
    			IQuery sub = getFieldQuery(f, queryText, quoted);
        	
    			if (sub != null) {
    				if (a.mFields.get(f) != null) 
    					sub.setBoost(a.mFields.get(f));
            
    				q.add(sub);
    				ok = true;
    			}
    		}
    	  
    		return ok ? q : null;

    	} else {
    		try {
    			return super.getFieldQuery(field, queryText, quoted);
    		} catch (Exception e) {
    			return null;
    		}
    	}
	}

}
