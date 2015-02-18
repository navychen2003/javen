package org.javenstudio.falcon.search.handler;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.RequestHelper;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchReturnFields;
import org.javenstudio.falcon.search.component.InterestingTerm;
import org.javenstudio.falcon.search.component.MoreLikeThisHelper;
import org.javenstudio.falcon.search.facet.SimpleFacets;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.hits.SortSpec;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.params.MoreLikeThisParams;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryBuilderFactory;
import org.javenstudio.falcon.search.query.QueryParsing;

/**
 * MoreLikeThis --
 * 
 * Return similar documents either based on a single document 
 * or based on posted text.
 * 
 * @since 1.3
 */
public class MoreLikeThisHandler {
	static final Logger LOG = Logger.getLogger(MoreLikeThisHandler.class);
  
	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp) 
			throws ErrorException {
		try { 
			doHandleRequestBody(req, rsp);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
  
	@SuppressWarnings("unused")
	private void doHandleRequestBody(ISearchRequest req, ISearchResponse rsp) 
			throws IOException, ErrorException {
		Params params = req.getParams();

		// Set field flags
		SearchReturnFields returnFields = rsp.getReturnFields(); 
		
		int flags = 0;
		if (returnFields.wantsScore()) 
			flags |= Searcher.GET_SCORES;
    
		String defType = params.get(QueryParsing.DEFTYPE, QueryBuilderFactory.DEFAULT_QTYPE);
		String q = params.get(CommonParams.Q);
		
		IQuery query = null;
		SortSpec sortSpec = null;
		List<IQuery> filters = null;

		if (q != null) {
			QueryBuilder parser = req.getSearchCore().getQueryFactory()
					.getQueryBuilder(q, defType, req);
			query = parser.getQuery();
			sortSpec = parser.getSort(true);
		}

		String[] fqs = req.getParams().getParams(CommonParams.FQ);
		if (fqs != null && fqs.length != 0) {
			filters = new ArrayList<IQuery>();
			
			for (String fq : fqs) {
				if (fq != null && fq.trim().length()!=0) {
					QueryBuilder fqp = req.getSearchCore().getQueryFactory()
							.getQueryBuilder(fq, null, req);
					filters.add(fqp.getQuery());
				}
			}
		}

		Searcher searcher = req.getSearcher();
		MoreLikeThisHelper mlt = new MoreLikeThisHelper(params, searcher);

		// Hold on to the interesting terms if relevant
		MoreLikeThisParams.TermStyle termStyle = MoreLikeThisParams.TermStyle.get(
				params.get(MoreLikeThisParams.INTERESTING_TERMS));
		
		List<InterestingTerm> interesting = (termStyle == MoreLikeThisParams.TermStyle.NONE) ? 
				null : new ArrayList<InterestingTerm>( mlt.getMoreLikeThis().getMaxQueryTerms() );
    
		DocListAndSet mltDocs = null;

		// Parse Required Params
		// This will either have a single Reader or valid query
		Reader reader = null;
		
		try {
			if (q == null || q.trim().length() < 1) {
				Iterable<ContentStream> streams = req.getContentStreams();
				if (streams != null) {
					Iterator<ContentStream> iter = streams.iterator();
					if (iter.hasNext()) 
						reader = iter.next().getReader();
					
					if (iter.hasNext()) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
								"MoreLikeThis does not support multiple ContentStreams");
					}
				}
			}

			int start = params.getInt(CommonParams.START, 0);
			int rows = params.getInt(CommonParams.ROWS, 10);

			// Find documents MoreLikeThis - either with a reader or a query
			// --------------------------------------------------------------------------------
			if (reader != null) {
				mltDocs = mlt.getMoreLikeThis(reader, start, rows, filters,
						interesting, flags);
				
			} else if (q != null) {
				// Matching options
				boolean includeMatch = params.getBool(MoreLikeThisParams.MATCH_INCLUDE, true);
				int matchOffset = params.getInt(MoreLikeThisParams.MATCH_OFFSET, 0);
				
				// Find the base match
				DocList match = searcher.getDocList(query, null, null, matchOffset, 1,
						flags); // only get the first one...
				
				if (includeMatch) 
					rsp.add("match", match);
        
				// This is an iterator, but we only handle the first match
				DocIterator iterator = match.iterator();
				if (iterator.hasNext()) {
					// do a MoreLikeThis query for each document in results
					int id = iterator.nextDoc();
					mltDocs = mlt.getMoreLikeThis(id, start, rows, filters, interesting, flags);
				}
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"MoreLikeThis requires either a query (?q=) or text to find similar documents.");
			}
		} finally {
			if (reader != null) 
				reader.close();
		}
    
		if (mltDocs == null) 
			mltDocs = new DocListAndSet(); // avoid NPE
    
		rsp.add("response", mltDocs.getDocList());
    
		if (interesting != null) {
			if (termStyle == MoreLikeThisParams.TermStyle.DETAILS) {
				NamedList<Float> it = new NamedList<Float>();
				for (InterestingTerm t : interesting) {
					it.add(t.getTerm().toString(), t.getBoost());
				}
				rsp.add( "interestingTerms", it );
				
			} else {
				List<String> it = new ArrayList<String>(interesting.size());
				for (InterestingTerm t : interesting) {
					it.add(t.getTerm().getText());
				}
				rsp.add("interestingTerms", it);
			}
		}
    
		// maybe facet the results
		if (params.getBool(FacetParams.FACET,false)) {
			if (mltDocs.getDocSet() == null) {
				rsp.add("facet_counts", null);
			} else {
				SimpleFacets f = new SimpleFacets(req, mltDocs.getDocSet(), params );
				rsp.add("facet_counts", f.getFacetCounts());
			}
		}
		
		boolean dbg = req.getParams().getBool(CommonParams.DEBUG_QUERY, false);
		boolean dbgQuery = false, dbgResults = false;
		
		if (dbg == false) { //if it's true, we are doing everything anyway.
			String[] dbgParams = req.getParams().getParams(CommonParams.DEBUG);
			
			if (dbgParams != null) {
				for (int i = 0; i < dbgParams.length; i++) {
					if (dbgParams[i].equals(CommonParams.QUERY)){
						dbgQuery = true;
					} else if (dbgParams[i].equals(CommonParams.RESULTS)){
						dbgResults = true;
					}
				}
			}
		} else {
			dbgQuery = true;
			dbgResults = true;
		}
		
		// Copied from StandardRequestHandler... perhaps it should be added to doStandardDebug?
		if (dbg == true) {
			try {
				NamedList<Object> dbgInfo = RequestHelper.doStandardDebug(req, q, 
						mlt.getRawQuery(), mltDocs.getDocList(), dbgQuery, dbgResults);
				
				if (dbgInfo != null) {
					if (filters != null) {
						dbgInfo.add("filter_queries", req.getParams().getParams(CommonParams.FQ));
						
						List<String> fqsList = new ArrayList<String>(filters.size());
						for (IQuery fq : filters) {
							fqsList.add(QueryParsing.toString(fq, req.getSearchCore().getSchema()));
						}
						dbgInfo.add("parsed_filter_queries", fqsList);
					}
					rsp.add("debug", dbgInfo);
				}
				
			} catch (Exception e) {
				if (LOG.isDebugEnabled())
					LOG.debug("Exception during debug", e);
				
				rsp.add("exception_during_debug", e.toString());
			}
		}
	}
  
}
