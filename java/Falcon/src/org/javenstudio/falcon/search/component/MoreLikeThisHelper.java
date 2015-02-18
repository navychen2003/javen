package org.javenstudio.falcon.search.component;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.MoreLikeThis;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.RequestHelper;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.params.MoreLikeThisParams;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Helper class for MoreLikeThis that can be called from other request handlers
 */
public class MoreLikeThisHelper {
	
	// Pattern is thread safe -- TODO? share this with general 'fl' param
	static final Pattern sSplitList = Pattern.compile(",| ");
	  
	private final Searcher mSearcher;
	private final MoreLikeThis mMoreLikeThis;
	private final IIndexReader mReader;
	private final SchemaField mUniqueKeyField;
	private final boolean mNeedDocSet;
	
	private Map<String,Float> mBoostFields;
	private IQuery mRawQuery;
    private IQuery mBoostedQuery;
    private BooleanQuery mRealQuery;
    
	public MoreLikeThisHelper(Params params, Searcher searcher) throws ErrorException {
		mSearcher = searcher;
		mReader = searcher.getIndexReader();
		mUniqueKeyField = searcher.getSchema().getUniqueKeyField();
		mNeedDocSet = params.getBool(FacetParams.FACET,false);
      
		Params required = params.toRequired();
		String[] fields = sSplitList.split(required.get(MoreLikeThisParams.SIMILARITY_FIELDS));
		if (fields.length < 1) {
			throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, 
					"MoreLikeThis requires at least one similarity field: " + 
					MoreLikeThisParams.SIMILARITY_FIELDS);
		}
      
		// TODO -- after LUCENE-896, we can use , searcher.getSimilarity() );
      	mMoreLikeThis = new MoreLikeThis(mReader); 
      	mMoreLikeThis.setFieldNames(fields);
      	mMoreLikeThis.setAnalyzer(searcher.getSchema().getAnalyzer());
      
      	// configurable params
      	mMoreLikeThis.setMinTermFreq(params.getInt(MoreLikeThisParams.MIN_TERM_FREQ, 
      			MoreLikeThis.DEFAULT_MIN_TERM_FREQ));
      	mMoreLikeThis.setMinDocFreq(params.getInt(MoreLikeThisParams.MIN_DOC_FREQ, 
      			MoreLikeThis.DEFAULT_MIN_DOC_FREQ));
      	mMoreLikeThis.setMinWordLen(params.getInt(MoreLikeThisParams.MIN_WORD_LEN, 
      			MoreLikeThis.DEFAULT_MIN_WORD_LENGTH));
      	mMoreLikeThis.setMaxWordLen(params.getInt(MoreLikeThisParams.MAX_WORD_LEN, 
      			MoreLikeThis.DEFAULT_MAX_WORD_LENGTH));
      	mMoreLikeThis.setMaxQueryTerms(params.getInt(MoreLikeThisParams.MAX_QUERY_TERMS, 
      			MoreLikeThis.DEFAULT_MAX_QUERY_TERMS));
      	mMoreLikeThis.setMaxNumTokensParsed(params.getInt(MoreLikeThisParams.MAX_NUM_TOKENS_PARSED, 
      			MoreLikeThis.DEFAULT_MAX_NUM_TOKENS_PARSED));
      	mMoreLikeThis.setBoost(params.getBool(MoreLikeThisParams.BOOST, false));
      	
      	mBoostFields = RequestHelper.parseFieldBoosts(params.getParams(MoreLikeThisParams.QF));
	}
    
	public MoreLikeThis getMoreLikeThis() { return mMoreLikeThis; }
    public IQuery getRawQuery() { return mRawQuery; }
    public IQuery getBoostedQuery() { return mBoostedQuery; }
    public IQuery getRealQuery() { return mRealQuery; }
    
    private IQuery getBoostedQuery(IQuery mltquery) {
    	BooleanQuery boostedQuery = ((BooleanQuery)mltquery).clone();
    	if (mBoostFields.size() > 0) {
    		List<?> clauses = boostedQuery.clauses();
    		for( Object o : clauses ) {
    			TermQuery q = (TermQuery)((BooleanClause)o).getQuery();
    			Float b = mBoostFields.get(q.getTerm().getField());
    			if (b != null) 
    				q.setBoost(b*q.getBoost());
    		}
    	}
    	return boostedQuery;
    }
    
    public DocListAndSet getMoreLikeThis(int id, int start, int rows, 
    		List<IQuery> filters, List<InterestingTerm> terms, int flags) throws ErrorException {
    	try {
    		IDocument doc = mReader.getDocument(id);
    		
    		mRawQuery = mMoreLikeThis.like(id);
    		mBoostedQuery = getBoostedQuery(mRawQuery);
    		
    		if (terms != null) 
    			fillInterestingTermsFromMLTQuery(mRawQuery, terms);
	      
    		// exclude current document from results
    		mRealQuery = new BooleanQuery();
    		mRealQuery.add(mBoostedQuery, BooleanClause.Occur.MUST);
    		mRealQuery.add(new TermQuery(new Term(mUniqueKeyField.getName(), 
    				mUniqueKeyField.getType().storedToIndexed(
    						(Fieldable)doc.getField(mUniqueKeyField.getName())))), 
    				BooleanClause.Occur.MUST_NOT);
	      
    		DocListAndSet results = new DocListAndSet();
    		if (mNeedDocSet) {
    			results = mSearcher.getDocListAndSet(mRealQuery, 
    					filters, null, start, rows, flags);
    		} else {
    			results.setDocList(mSearcher.getDocList(mRealQuery, 
    					filters, null, start, rows, flags));
    		}
    		
    		return results;
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
	}

    public DocListAndSet getMoreLikeThis(Reader reader, int start, int rows, 
    		List<IQuery> filters, List<InterestingTerm> terms, int flags) throws ErrorException {
    	try {
    		// analyzing with the first field: previous (stupid) behavior
    		mRawQuery = mMoreLikeThis.like(reader, mMoreLikeThis.getFieldNames()[0]);
    		mBoostedQuery = getBoostedQuery(mRawQuery);
    		
    		if (terms != null) 
    			fillInterestingTermsFromMLTQuery(mBoostedQuery, terms);
	      
    		DocListAndSet results = new DocListAndSet();
    		if (mNeedDocSet) {
    			results = mSearcher.getDocListAndSet(mBoostedQuery, 
    					filters, null, start, rows, flags);
    		} else {
    			results.setDocList(mSearcher.getDocList(mBoostedQuery, 
    					filters, null, start, rows, flags));
    		}
    		
    		return results;
    	} catch (IOException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
	}

    @Deprecated
    public NamedList<DocList> getMoreLikeThese(DocList docs, int rows, 
    		int flags) throws ErrorException {
    	IndexSchema schema = mSearcher.getSchema();
    	
    	NamedList<DocList> mlt = new NamedMap<DocList>();
    	DocIterator iterator = docs.iterator();
    	
    	while (iterator.hasNext()) {
    		int id = iterator.nextDoc();
        
    		try {
    			DocListAndSet sim = getMoreLikeThis( id, 0, rows, null, null, flags );
    			String name = schema.getPrintableUniqueKey(mReader.getDocument( id ) );
	
    			mlt.add(name, sim.getDocList());
    		} catch (IOException ex) { 
    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    		}
    	}
    	
    	return mlt;
    }
    
    private void fillInterestingTermsFromMLTQuery(IQuery query, List<InterestingTerm> terms) { 
    	List<?> clauses = ((BooleanQuery)query).clauses();
    	for (Object o : clauses) {
    		TermQuery q = (TermQuery)((BooleanClause)o).getQuery();
    		
    		InterestingTerm it = new InterestingTerm(
    				q.getTerm(), q.getBoost());
    		
    		terms.add(it);
    	} 
    	// alternatively we could use
    	// mltquery.extractTerms( terms );
	}
    
}
