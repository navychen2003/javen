package org.javenstudio.falcon.search.facet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.BoundedTreeSet;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.DateParser;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.RequiredParams;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.FacetEntry;
import org.javenstudio.hornet.grouping.GroupedFacetResult;
import org.javenstudio.hornet.grouping.collector.TermAllGroupsCollector;
import org.javenstudio.hornet.grouping.collector.TermGroupFacetCollector;
import org.javenstudio.hornet.index.term.MultiDocsEnum;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.grouping.Grouping;
import org.javenstudio.falcon.search.grouping.GroupingSpecification;
import org.javenstudio.falcon.search.hits.BitDocSet;
import org.javenstudio.falcon.search.hits.DocSet;
import org.javenstudio.falcon.search.hits.DocsEnumState;
import org.javenstudio.falcon.search.hits.HashDocSet;
import org.javenstudio.falcon.search.hits.SortedIntDocSet;
import org.javenstudio.falcon.search.hits.UnInvertedField;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.params.GroupParams;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.type.BoolFieldType;
import org.javenstudio.falcon.search.schema.type.DateFieldType;

/**
 * A class that generates simple Facet information for a request.
 *
 * More advanced facet implementations may compose or subclass this class 
 * to leverage any of it's functionality.
 */
public class SimpleFacets {

	/** The main set of documents all facet counts should be relative to */
	protected DocSet mDocs;
	
	/** Configuration params behavior should be driven by */
	protected Params mParams;
	protected Params mRequired;
	
	/** Searcher to use for all calculations */
	protected Searcher mSearcher;
	protected ISearchRequest mRequest;
	protected ResponseBuilder mBuilder;
	protected NamedMap<Object> mFacetResponse;

	// per-facet values
	protected Params mLocalParams; 	// localParams on this particular facet command
	protected String mFacetValue;	// the field to or query to facet on (minus local params)
	protected DocSet mBase;      	// the base docset for this particular facet
	protected String mKey;        	// what name should the results be stored under
	
	protected int mThreads;

	public SimpleFacets(ISearchRequest req, DocSet docs, Params params) 
			throws ErrorException {
		this(req, docs, params, null);
	}

	public SimpleFacets(Params params, ResponseBuilder rb) 
			throws ErrorException {
		this(rb.getRequest(), rb.getResults().getDocSet(), params, rb);
	}
	
	public SimpleFacets(ISearchRequest req, DocSet docs, Params params, 
			ResponseBuilder rb) throws ErrorException {
		mRequest = req;
		mSearcher = req.getSearcher();
		mBase = mDocs = docs;
		mParams = params;
		mRequired = new RequiredParams(params);
		mBuilder = rb;
	}

	private void parseParams(String type, String param) throws ErrorException {
		mLocalParams = QueryParsing.getLocalParams(
				param, mRequest.getParams());
		
		mBase = mDocs;
		mFacetValue = param;
		mKey = param;
		mThreads = -1;

		if (mLocalParams == null) 
			return;

		// remove local params unless it's a query
		if (type != FacetParams.FACET_QUERY) // TODO Cut over to an Enum here
			mFacetValue = mLocalParams.get(CommonParams.VALUE);

		// reset set the default key now that localParams have been removed
		mKey = mFacetValue;

		// allow explicit set of the key
		mKey = mLocalParams.get(CommonParams.OUTPUT_KEY, mKey);

		String threadStr = mLocalParams.get(CommonParams.THREADS);
		if (threadStr != null) 
			mThreads = Integer.parseInt(threadStr);

		// figure out if we need a new base DocSet
		String excludeStr = mLocalParams.get(CommonParams.EXCLUDE);
		if (excludeStr == null) 
			return;

		Map<?,?> tagMap = (Map<?,?>)mRequest.getContextMap().get("tags");
		
		if (tagMap != null && mBuilder != null) {
			List<String> excludeTagList = StrHelper.splitSmart(excludeStr,',');
			IdentityHashMap<IQuery,Boolean> excludeSet = new IdentityHashMap<IQuery,Boolean>();
			
			for (String excludeTag : excludeTagList) {
				Object olst = tagMap.get(excludeTag);
				// tagMap has entries of List<String,List<QParser>>, but subject to change in the future
				if (!(olst instanceof Collection)) 
					continue;
				
				for (Object o : (Collection<?>)olst) {
					if (!(o instanceof QueryBuilder)) 
						continue;
					
					QueryBuilder qp = (QueryBuilder)o;
					excludeSet.put(qp.getQuery(), Boolean.TRUE);
				}
			}
			
			if (excludeSet.size() == 0) 
				return;

			List<IQuery> qlist = new ArrayList<IQuery>();

			// add the base query
			if (!excludeSet.containsKey(mBuilder.getQuery())) 
				qlist.add(mBuilder.getQuery());

			// add the filters
			if (mBuilder.getFilters() != null) {
				for (IQuery q : mBuilder.getFilters()) {
					if (!excludeSet.containsKey(q)) 
						qlist.add(q);
				}
			}

			// get the new base docset for this facet
			DocSet base = mSearcher.getDocSet(qlist);
			
			if (mBuilder.isGrouping() && mBuilder.getGroupingSpec().isTruncateGroups()) {
				Grouping grouping = new Grouping(mSearcher, null, 
						mBuilder.getQueryCommand(), false, 0, false);
				
				if (mBuilder.getGroupingSpec().getFields().length > 0) {
					grouping.addFieldCommand(mBuilder.getGroupingSpec().getFields()[0], mRequest);
					
				} else if (mBuilder.getGroupingSpec().getFunctions().length > 0) {
					grouping.addFunctionCommand(mBuilder.getGroupingSpec().getFunctions()[0], mRequest);
					
				} else {
					mBase = base;
					return;
				}
				
				AbstractAllGroupHeadsCollector<?> allGroupHeadsCollector = 
						grouping.getCommands().get(0).createAllGroupCollector();
				
				mSearcher.search(new MatchAllDocsQuery(), 
						base.getTopFilter(), allGroupHeadsCollector);
				
				int maxDoc = mSearcher.getMaxDoc();
				IFixedBitSet fixedBitSet = allGroupHeadsCollector.retrieveGroupHeads(maxDoc);
				long[] bits = fixedBitSet.getBitsArray();
				
				mBase = new BitDocSet(new OpenBitSet(bits, bits.length));
				
			} else {
				mBase = base;
			}
		}
	}

	/**
	 * Looks at various Params to determing if any simple Facet Constraint count
	 * computations are desired.
	 *
	 * @see #getFacetQueryCounts
	 * @see #getFacetFieldCounts
	 * @see #getFacetDateCounts
	 * @see #getFacetRangeCounts
	 * @see FacetParams#FACET
	 * @return a NamedList of Facet Count info or null
	 */
	public NamedList<Object> getFacetCounts() throws ErrorException {
		// if someone called this method, benefit of the doubt: assume true
		if (!mParams.getBool(FacetParams.FACET, true))
			return null;

		mFacetResponse = new NamedMap<Object>();
		mFacetResponse.add("facet_queries", getFacetQueryCounts());
		mFacetResponse.add("facet_fields", getFacetFieldCounts());
		mFacetResponse.add("facet_dates", getFacetDateCounts());
		mFacetResponse.add("facet_ranges", getFacetRangeCounts());

		return mFacetResponse;
	}

	/**
	 * Returns a list of facet counts for each of the facet queries 
	 * specified in the params
	 *
	 * @see FacetParams#FACET_QUERY
	 */
	public NamedList<Integer> getFacetQueryCounts() throws ErrorException {
		NamedList<Integer> res = new NamedMap<Integer>();

		/** 
		 * Ignore CommonParams.DF - could have init param facet.query assuming
		 * the schema default with query param DF intented to only affect Q.
		 * If user doesn't want schema default for facet.query, they should be
		 * explicit.
		 */
		//QueryParser qp = searcher.getSchema().getQueryParser(null);

		String[] facetQs = mParams.getParams(FacetParams.FACET_QUERY);

		if (facetQs != null && facetQs.length != 0) {
			for (String q : facetQs) {
				parseParams(FacetParams.FACET_QUERY, q);

				// TODO: slight optimization would prevent double-parsing of any localParams
				IQuery qobj = mBuilder.getSearchCore().getQueryFactory()
						.getQueryBuilder(q, null, mRequest).getQuery();

				if (mParams.getBool(GroupParams.GROUP_FACET, false)) 
					res.add(mKey, getGroupedFacetQueryCount(qobj));
				else 
					res.add(mKey, mSearcher.getNumDocs(qobj, mBase));
			}
		}

		return res;
	}
  
	/**
	 * Returns a grouped facet count for the facet query
	 *
	 * @see FacetParams#FACET_QUERY
	 */
	public int getGroupedFacetQueryCount(IQuery facetQuery) throws ErrorException {
		GroupingSpecification groupingSpecification = mBuilder.getGroupingSpec();
		String groupField  = groupingSpecification != null ? 
				groupingSpecification.getFields()[0] : null;
				
		if (groupField == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Specify the group.field as parameter or local parameter"
					);
		}

		TermAllGroupsCollector collector = new TermAllGroupsCollector(groupField);
		// This returns a filter that only matches documents matching with q param and fq params
		IFilter mainQueryFilter = mDocs.getTopFilter(); 

		mSearcher.search(facetQuery, mainQueryFilter, collector);

		return collector.getGroupCount();
	}

	public NamedList<Integer> getTermCounts(String field) throws ErrorException {
		int offset = mParams.getFieldInt(field, FacetParams.FACET_OFFSET, 0);
		int limit = mParams.getFieldInt(field, FacetParams.FACET_LIMIT, 100);
		if (limit == 0) 
			return new NamedList<Integer>();
		
		Integer mincount = mParams.getFieldInt(field, FacetParams.FACET_MINCOUNT);
		if (mincount == null) {
			Boolean zeros = mParams.getFieldBool(field, FacetParams.FACET_ZEROS);
			// mincount = (zeros!=null && zeros) ? 0 : 1;
			mincount = (zeros!=null && !zeros) ? 1 : 0;
			// current default is to include zeros.
		}
		
		boolean missing = mParams.getFieldBool(field, FacetParams.FACET_MISSING, false);
		// default to sorting if there is a limit.
		String sort = mParams.getFieldParam(field, FacetParams.FACET_SORT, 
				limit>0 ? FacetParams.FACET_SORT_COUNT : FacetParams.FACET_SORT_INDEX);
		String prefix = mParams.getFieldParam(field,FacetParams.FACET_PREFIX);

		NamedList<Integer> counts;
		SchemaField sf = mSearcher.getSchema().getField(field);
		SchemaFieldType ft = sf.getType();

		// determine what type of faceting method to use
		String method = mParams.getFieldParam(field, FacetParams.FACET_METHOD);
		boolean enumMethod = FacetParams.FACET_METHOD_enum.equals(method);

		// TODO: default to per-segment or not?
		boolean per_segment = FacetParams.FACET_METHOD_fcs.equals(method);

		if (method == null && ft instanceof BoolFieldType) {
			// Always use filters for booleans... we know the number of values is very small.
			enumMethod = true;
		}
		
		boolean multiToken = sf.isMultiValued() || ft.isMultiValuedFieldCache();

		if (TrieFieldType.getMainValuePrefix(ft) != null) {
			// A TrieField with multiple parts indexed per value... currently only
			// UnInvertedField can handle this case, so force it's use.
			enumMethod = false;
			multiToken = true;
		}

		if (mParams.getFieldBool(field, GroupParams.GROUP_FACET, false)) {
			counts = getGroupedCounts(mSearcher, mBase, field, multiToken, offset, limit, 
					mincount, missing, sort, prefix);
			
		} else {
			// unless the enum method is explicitly specified, use a counting method.
			if (enumMethod) {
				counts = getFacetTermEnumCounts(mSearcher, mBase, field, offset, limit, 
						mincount, missing, sort, prefix);
				
			} else {
				if (multiToken) {
					UnInvertedField uif = UnInvertedField.getUnInvertedField(field, mSearcher);
					counts = uif.getCounts(mSearcher, mBase, offset, limit, mincount, missing, sort, prefix);
					
				} else {
					// TODO: future logic could use filters instead of the fieldcache if
					// the number of terms in the field is small enough.
					if (per_segment) {
						SingleValuedFaceting ps = new SingleValuedFaceting(mSearcher, mBase, 
								field, offset, limit, mincount, missing, sort, prefix);
						
						Executor executor = (mThreads == 0) ? 
								FacetHelper.sDirectExecutor : FacetHelper.sFacetExecutor;
						
						ps.setNumThreads(mThreads);
						counts = ps.getFacetCounts(executor);
						
					} else {
						counts = FacetHelper.getFieldCacheCounts(mSearcher, mBase, 
								field, offset, limit, mincount, missing, sort, prefix);
					}
				}
			}
		}

		return counts;
	}

	public NamedList<Integer> getGroupedCounts(Searcher searcher,
			DocSet base, String field, boolean multiToken, int offset, int limit,
			int mincount, boolean missing, String sort, String prefix) 
			throws ErrorException {
		GroupingSpecification groupingSpecification = mBuilder.getGroupingSpec();
		String groupField  = (groupingSpecification != null) ? 
				groupingSpecification.getFields()[0] : null;
				
		if (groupField == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Specify the group.field as parameter or local parameter");
		}

		GroupedFacetResult result = null; 
		BytesRef prefixBR = (prefix != null) ? new BytesRef(prefix) : null;
    
		try {
			TermGroupFacetCollector collector = 
					TermGroupFacetCollector.createTermGroupFacetCollector(
							groupField, field, multiToken, prefixBR, 128);
			
			searcher.search(new MatchAllDocsQuery(), base.getTopFilter(), collector);
			
			boolean orderByCount = sort.equals(FacetParams.FACET_SORT_COUNT) || 
					sort.equals(FacetParams.FACET_SORT_COUNT_LEGACY);
			
			result = collector.mergeSegmentResults(
					offset + limit, mincount, orderByCount);
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
    
		CharsRef charsRef = new CharsRef();
		NamedList<Integer> facetCounts = new NamedList<Integer>();
		
		SchemaFieldType facetFieldType = searcher.getSchema().getFieldType(field);
		List<FacetEntry> scopedEntries = result.getFacetEntries(offset, limit);
		
		for (FacetEntry facetEntry : scopedEntries) {
			facetFieldType.indexedToReadable(facetEntry.getValue(), charsRef);
			facetCounts.add(charsRef.toString(), facetEntry.getCount());
		}

		if (missing) 
			facetCounts.add(null, result.getTotalMissingCount());

		return facetCounts;
	}
  
	/**
	 * Returns a list of value constraints and the associated facet counts 
	 * for each facet field specified in the params.
	 *
	 * @see FacetParams#FACET_FIELD
	 * @see #getFieldMissingCount
	 * @see #getFacetTermEnumCounts
	 */
	public NamedList<Object> getFacetFieldCounts() throws ErrorException {
		NamedList<Object> res = new NamedMap<Object>();
		String[] facetFs = mParams.getParams(FacetParams.FACET_FIELD);
		
		if (facetFs != null) {
			for (String f : facetFs) {
				parseParams(FacetParams.FACET_FIELD, f);
				
				String termList = (mLocalParams == null) ? null : 
					mLocalParams.get(CommonParams.TERMS);
				
				if (termList != null) 
					res.add(mKey, getListedTermCounts(mFacetValue, termList));
				else 
					res.add(mKey, getTermCounts(mFacetValue));
			}
		}
		
		return res;
	}

	private NamedList<Integer> getListedTermCounts(String field, String termList) 
			throws ErrorException {
		SchemaFieldType ft = mSearcher.getSchema().getFieldType(field);
		List<String> terms = StrHelper.splitSmart(termList, ",", true);
		
		NamedList<Integer> res = new NamedList<Integer>();
		for (String term : terms) {
			String internal = ft.toInternal(term);
			int count = mSearcher.getNumDocs(
					new TermQuery(new Term(field, internal)), mBase);
			res.add(term, count);
		}
		
		return res;
	}

	/**
	 * Returns a list of terms in the specified field along with the 
	 * corresponding count of documents in the set that match that constraint.
	 * This method uses the FilterCache to get the intersection count between <code>docs</code>
	 * and the DocSet for each term in the filter.
	 *
	 * @see FacetParams#FACET_LIMIT
	 * @see FacetParams#FACET_ZEROS
	 * @see FacetParams#FACET_MISSING
	 */
	public NamedList<Integer> getFacetTermEnumCounts(Searcher searcher, 
			DocSet docs, String field, int offset, int limit, int mincount, boolean missing, 
			String sort, String prefix) throws ErrorException {
		try { 
			return doGetFacetTermEnumCounts(searcher, docs, field, offset, limit, 
					mincount, missing, sort, prefix);
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
  
	private NamedList<Integer> doGetFacetTermEnumCounts(Searcher searcher, 
			DocSet docs, String field, int offset, int limit, int mincount, boolean missing, 
			String sort, String prefix) throws IOException, ErrorException {
		/** 
		 * :TODO: potential optimization...
		 * cache the Terms with the highest docFreq and try them first
		 * don't enum if we get our max from them
		 */
		// Minimum term docFreq in order to use the filterCache for that term.
		int minDfFilterCache = mParams.getFieldInt(field, FacetParams.FACET_ENUM_CACHE_MINDF, 0);

		// make sure we have a set that is fast for random access, if we will use it for that
		DocSet fastForRandomSet = docs;
		if (minDfFilterCache>0 && docs instanceof SortedIntDocSet) {
			SortedIntDocSet sset = (SortedIntDocSet)docs;
			fastForRandomSet = new HashDocSet(sset.getDocs(), 0, sset.size());
		}

		IndexSchema schema = searcher.getSchema();
		IAtomicReader r = searcher.getAtomicReader();
		SchemaFieldType ft = schema.getFieldType(field);

		boolean sortByCount = sort.equals("count") || sort.equals("true");
		
		final int maxsize = (limit >= 0) ? offset+limit : Integer.MAX_VALUE-1;
		final BoundedTreeSet<FacetCountPair<BytesRef,Integer>> queue = 
				sortByCount ? new BoundedTreeSet<FacetCountPair<BytesRef,Integer>>(maxsize) : null;
		final NamedList<Integer> res = new NamedList<Integer>();

		int min = mincount-1; // the smallest value in the top 'N' values    
		int off = offset;
		int lim = limit>=0 ? limit : Integer.MAX_VALUE;

		BytesRef startTermBytes = null;
		if (prefix != null) {
			String indexedPrefix = ft.toInternal(prefix);
			startTermBytes = new BytesRef(indexedPrefix);
		}

		IFields fields = r.getFields();
		ITerms terms = (fields == null) ? null : fields.getTerms(field);
		
		ITermsEnum termsEnum = null;
		DocsEnumState deState = null;
		BytesRef term = null;
		
		if (terms != null) {
			termsEnum = terms.iterator(null);

			// TODO: OPT: if seek(ord) is supported for this termsEnum, then we could use it for
			// facet.offset when sorting by index order.

			if (startTermBytes != null) {
				if (termsEnum.seekCeil(startTermBytes, true) == ITermsEnum.SeekStatus.END) 
					termsEnum = null;
				else 
					term = termsEnum.getTerm();
				
			} else {
				// position termsEnum on first term
				term = termsEnum.next();
			}
		}

		IDocsEnum docsEnum = null;
		CharsRef charsRef = new CharsRef(10);

		if (docs.size() >= mincount) {
			while (term != null) {
				if (startTermBytes != null && !StringHelper.startsWith(term, startTermBytes))
					break;

				int df = termsEnum.getDocFreq();

				// If we are sorting, we can use df>min (rather than >=) since we
				// are going in index order.  For certain term distributions this can
				// make a large difference (for example, many terms with df=1).
				if (df > 0 && df > min) {
					int c;

					if (df >= minDfFilterCache) {
						// use the filter cache

						if (deState == null) {
							deState = new DocsEnumState(field);
							deState.setLiveDocs(r.getLiveDocs());
							deState.setTermsEnum(termsEnum);
							deState.setDocsEnum(docsEnum);
						}

						c = searcher.getNumDocs(docs, deState);
						docsEnum = deState.getDocsEnum();
						
					} else {
						// iterate over TermDocs to calculate the intersection

						// TODO: specialize when base docset is a bitset or hash set (skipDocs)?  
						// or does it matter for this?
						// TODO: do this per-segment for better efficiency (MultiDocsEnum just 
						// uses base class impl)
						// TODO: would passing deleted docs lead to better efficiency over checking 
						// the fastForRandomSet?
						docsEnum = termsEnum.getDocs(null, docsEnum, 0);
						c = 0;

						if (docsEnum instanceof MultiDocsEnum) {
							MultiDocsEnum.EnumWithSlice[] subs = ((MultiDocsEnum)docsEnum).getSubs();
							int numSubs = ((MultiDocsEnum)docsEnum).getNumSubs();
							
							for (int subindex = 0; subindex < numSubs; subindex++) {
								MultiDocsEnum.EnumWithSlice sub = subs[subindex];
								if (sub.getDocsEnum() == null) 
									continue;
								
								int base = sub.getSlice().getStart();
								int docid;
								
								while ((docid = sub.getDocsEnum().nextDoc()) != IDocIdSetIterator.NO_MORE_DOCS) {
									if (fastForRandomSet.exists(docid+base)) c ++;
								}
							}
						} else {
							int docid;
							while ((docid = docsEnum.nextDoc()) != IDocIdSetIterator.NO_MORE_DOCS) {
								if (fastForRandomSet.exists(docid)) c++;
							}
						}
					}

					if (sortByCount) {
						if (c > min) {
							BytesRef termCopy = BytesRef.deepCopyOf(term);
							queue.add(new FacetCountPair<BytesRef,Integer>(termCopy, c));
							
							if (queue.size() >= maxsize) 
								min = queue.last().getValue();
						}
					} else {
						if (c >= mincount && --off < 0) {
							if (--lim < 0) break;
							
							ft.indexedToReadable(term, charsRef);
							res.add(charsRef.toString(), c);
						}
					}
				}

				term = termsEnum.next();
			}
		}

		if (sortByCount) {
			for (FacetCountPair<BytesRef,Integer> p : queue) {
				if (--off >= 0) continue;
				if (--lim < 0) break;
				
				ft.indexedToReadable(p.getKey(), charsRef);
				res.add(charsRef.toString(), p.getValue());
			}
		}

		if (missing) 
			res.add(null, FacetHelper.getFieldMissingCount(searcher, docs, field));

		return res;
	}

	/**
	 * Returns a list of value constraints and the associated facet counts 
	 * for each facet date field, range, and interval specified in the
	 * Params
	 *
	 * @see FacetParams#FACET_DATE
	 * @deprecated Use getFacetRangeCounts which is more generalized
	 */
	@Deprecated
	public NamedList<Object> getFacetDateCounts() throws ErrorException {
		final NamedList<Object> resOuter = new NamedMap<Object>();
		final String[] fields = mParams.getParams(FacetParams.FACET_DATE);

		if (fields == null || fields.length == 0) 
			return resOuter;

		for (String f : fields) {
			getFacetDateCounts(f, resOuter);
		}

		return resOuter;
	}

	/**
	 * @deprecated Use getFacetRangeCounts which is more generalized
	 */
	@Deprecated
	public void getFacetDateCounts(String dateFacet, NamedList<Object> resOuter)
			throws ErrorException {
		final IndexSchema schema = mSearcher.getSchema();

		parseParams(FacetParams.FACET_DATE, dateFacet);
		String f = mFacetValue;

		final NamedList<Object> resInner = new NamedMap<Object>();
		resOuter.add(mKey, resInner);
		
		final SchemaField sf = schema.getField(f);
		
		if (!(sf.getType() instanceof DateFieldType)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Can not date facet on a field which is not a DateField: " + f);
		}
		
		final DateFieldType ft = (DateFieldType) sf.getType();
		final String startS = mRequired.getFieldParam(f, FacetParams.FACET_DATE_START);
		
		final Date start;
		try {
			start = ft.parseMath(null, startS);
		} catch (ErrorException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"date facet 'start' is not a valid Date string: " + startS, e);
		}
		
		final String endS = mRequired.getFieldParam(f, FacetParams.FACET_DATE_END);
		Date end; // not final, hardend may change this
		
		try {
			end = ft.parseMath(null, endS);
		} catch (ErrorException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"date facet 'end' is not a valid Date string: " + endS, e);
		}

		if (end.before(start)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"date facet 'end' comes before 'start': " + endS + " < " + startS);
		}

		final String gap = mRequired.getFieldParam(f, FacetParams.FACET_DATE_GAP);
		final DateParser dmp = new DateParser();

		final int minCount = mParams.getFieldInt(f,FacetParams.FACET_MINCOUNT, 0);
		String[] iStrs = mParams.getFieldParams(f,FacetParams.FACET_DATE_INCLUDE);
		
		// Legacy support for default of [lower,upper,edge] for date faceting
		// this is not handled by FacetRangeInclude.parseParam because
		// range faceting has differnet defaults
		final EnumSet<FacetRangeInclude> include = (null == iStrs || 0 == iStrs.length ) 
				? EnumSet.of(FacetRangeInclude.LOWER, 
						FacetRangeInclude.UPPER, FacetRangeInclude.EDGE)
				: FacetRangeInclude.parseParam(iStrs);

		try {
			Date low = start;
			while (low.before(end)) {
				dmp.setNow(low);
				
				String label = ft.toExternal(low);
				Date high = dmp.parseMath(gap);
				
				if (end.before(high)) {
					if (mParams.getFieldBool(f, FacetParams.FACET_DATE_HARD_END, false)) 
						high = end;
					else 
						end = high;
				}
				
				if (high.before(low)) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"date facet infinite loop (is gap negative?)");
				}
				
				if (high.equals(low)) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"date facet infinite loop: gap is effectively zero");
				}
				
				final boolean includeLower = (include.contains(FacetRangeInclude.LOWER) ||
						(include.contains(FacetRangeInclude.EDGE) && low.equals(start)));
				final boolean includeUpper = (include.contains(FacetRangeInclude.UPPER) ||
						(include.contains(FacetRangeInclude.EDGE) && high.equals(end)));

				final int count = rangeCount(sf,low,high,includeLower,includeUpper);
				if (count >= minCount) 
					resInner.add(label, count);
				
				low = high;
			}
		} catch (java.text.ParseException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"date facet 'gap' is not a valid Date Math string: " + gap, e);
		}

		// explicitly return the gap and end so all the counts
		// (including before/after/between) are meaningful - even if mincount
		// has removed the neighboring ranges
		resInner.add("gap", gap);
		resInner.add("start", start);
		resInner.add("end", end);

		final String[] othersP = mParams.getFieldParams(f,FacetParams.FACET_DATE_OTHER);
		if (othersP != null && othersP.length > 0) {
			final Set<FacetRangeOther> others = 
					EnumSet.noneOf(FacetRangeOther.class);

			for (final String o : othersP) {
				others.add(FacetRangeOther.get(o));
			}

			// no matter what other values are listed, we don't do
			// anything if "none" is specified.
			if (!others.contains(FacetRangeOther.NONE)) {
				boolean all = others.contains(FacetRangeOther.ALL);

				if (all || others.contains(FacetRangeOther.BEFORE)) {
					// include upper bound if "outer" or if first gap doesn't already include it
					resInner.add(FacetRangeOther.BEFORE.toString(),
							rangeCount(sf, null, start, false,
									(include.contains(FacetRangeInclude.OUTER) ||
											(!(include.contains(FacetRangeInclude.LOWER) ||
													include.contains(FacetRangeInclude.EDGE))))));
				}
				
				if (all || others.contains(FacetRangeOther.AFTER)) {
					// include lower bound if "outer" or if last gap doesn't already include it
					resInner.add(FacetRangeOther.AFTER.toString(),
							rangeCount(sf, end, null,
									(include.contains(FacetRangeInclude.OUTER) ||
											(! (include.contains(FacetRangeInclude.UPPER) ||
													include.contains(FacetRangeInclude.EDGE)))),
									false));
				}
				
				if (all || others.contains(FacetRangeOther.BETWEEN)) {
					resInner.add(FacetRangeOther.BETWEEN.toString(),
							rangeCount(sf, start, end,
									(include.contains(FacetRangeInclude.LOWER) ||
											include.contains(FacetRangeInclude.EDGE)),
									(include.contains(FacetRangeInclude.UPPER) ||
											include.contains(FacetRangeInclude.EDGE))));
				}
			}
		}
	}

	/**
	 * Returns a list of value constraints and the associated facet
	 * counts for each facet numerical field, range, and interval
	 * specified in the Params
	 *
	 * @see FacetParams#FACET_RANGE
	 */
	public NamedList<Object> getFacetRangeCounts() throws ErrorException {
		final NamedList<Object> resOuter = new NamedMap<Object>();
		final String[] fields = mParams.getParams(FacetParams.FACET_RANGE);

		if (fields == null || fields.length == 0) 
			return resOuter;

		for (String f : fields) {
			getFacetRangeCounts(f, resOuter);
		}

		return resOuter;
	}

	protected void getFacetRangeCounts(String facetRange, NamedList<Object> resOuter)
			throws ErrorException {
		final IndexSchema schema = mSearcher.getSchema();

		parseParams(FacetParams.FACET_RANGE, facetRange);
		String f = mFacetValue;

		final SchemaField sf = schema.getField(f);
		final SchemaFieldType ft = sf.getType();

		RangeEndpointCalculator<?> calc = null;

		if (ft instanceof TrieFieldType) {
			final TrieFieldType trie = (TrieFieldType)ft;

			switch (trie.getType()) {
			case FLOAT:
				calc = new FloatRangeCalculator(sf);
				break;
			case DOUBLE:
				calc = new DoubleRangeCalculator(sf);
				break;
			case INTEGER:
				calc = new IntegerRangeCalculator(sf);
				break;
			case LONG:
				calc = new LongRangeCalculator(sf);
				break;
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"Unable to range facet on tried field of unexpected type:" + f);
			}
			
		} else if (ft instanceof DateFieldType) {
			calc = new DateRangeCalculator(sf, null);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Unable to range facet on field:" + sf);
		}

		resOuter.add(mKey, getFacetRangeCounts(sf, calc));
	}

	private <T extends Comparable<T>> NamedList<?> getFacetRangeCounts(final SchemaField sf, 
			final RangeEndpointCalculator<T> calc) throws ErrorException {
    
		final String f = sf.getName();
		final NamedList<Object> res = new NamedMap<Object>();
		final NamedList<Integer> counts = new NamedList<Integer>();
		
		res.add("counts", counts);

		final T start = calc.getValue(
				mRequired.getFieldParam(f,FacetParams.FACET_RANGE_START));
		
		// not final, hardend may change this
		T end = calc.getValue(mRequired.getFieldParam(f,FacetParams.FACET_RANGE_END));
		
		if (end.compareTo(start) < 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"range facet 'end' comes before 'start': " + end + " < " + start);
		}
    
		final String gap = mRequired.getFieldParam(f, FacetParams.FACET_RANGE_GAP);
		
		// explicitly return the gap.  compute this early so we are more 
		// likely to catch parse errors before attempting math
		res.add("gap", calc.getGap(gap));
    
		final int minCount = mParams.getFieldInt(f, FacetParams.FACET_MINCOUNT, 0);
    
		final EnumSet<FacetRangeInclude> include = 
				FacetRangeInclude.parseParam(
						mParams.getFieldParams(f, FacetParams.FACET_RANGE_INCLUDE));
    
		T low = start;
    
		while (low.compareTo(end) < 0) {
			T high = calc.addGap(low, gap);
			if (end.compareTo(high) < 0) {
				if (mParams.getFieldBool(f,FacetParams.FACET_RANGE_HARD_END,false)) 
					high = end;
				else 
					end = high;
			}
			
			if (high.compareTo(low) < 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"range facet infinite loop (is gap negative? did the math overflow?)");
			}
			
			if (high.compareTo(low) == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"range facet infinite loop: gap is either zero, or too small relative start/end and caused underflow: " + low + " + " + gap + " = " + high );
			}
      
			final boolean includeLower = 
					(include.contains(FacetRangeInclude.LOWER) ||
					(include.contains(FacetRangeInclude.EDGE) && low.compareTo(start) == 0));
			
			final boolean includeUpper = 
					(include.contains(FacetRangeInclude.UPPER) ||
					(include.contains(FacetRangeInclude.EDGE) && high.compareTo(end) == 0));
      
			final String lowS = calc.formatValue(low);
			final String highS = calc.formatValue(high);

			final int count = rangeCount(sf, lowS, highS, includeLower, includeUpper);
			if (count >= minCount) 
				counts.add(lowS, count);
      
			low = high;
		}
    
		// explicitly return the start and end so all the counts 
		// (including before/after/between) are meaningful - even if mincount
		// has removed the neighboring ranges
		res.add("start", start);
		res.add("end", end);
    
		final String[] othersP = mParams.getFieldParams(f, FacetParams.FACET_RANGE_OTHER);
		
		if (othersP != null && othersP.length > 0) {
			Set<FacetRangeOther> others = EnumSet.noneOf(FacetRangeOther.class);
      
			for (final String o : othersP) {
				others.add(FacetRangeOther.get(o));
			}
      
			// no matter what other values are listed, we don't do
			// anything if "none" is specified.
			if (!others.contains(FacetRangeOther.NONE)) {
				boolean all = others.contains(FacetRangeOther.ALL);
				
				final String startS = calc.formatValue(start);
				final String endS = calc.formatValue(end);

				if (all || others.contains(FacetRangeOther.BEFORE)) {
					// include upper bound if "outer" or if first gap doesn't already include it
					res.add(FacetRangeOther.BEFORE.toString(),
							rangeCount(sf, null, startS, false,
									(include.contains(FacetRangeInclude.OUTER) ||
									(!(include.contains(FacetRangeInclude.LOWER) || 
											include.contains(FacetRangeInclude.EDGE))))));
          
				}
				
				if (all || others.contains(FacetRangeOther.AFTER)) {
					// include lower bound if "outer" or if last gap doesn't already include it
					res.add(FacetRangeOther.AFTER.toString(),
							rangeCount(sf, endS, null,
									(include.contains(FacetRangeInclude.OUTER) ||
											(! (include.contains(FacetRangeInclude.UPPER) ||
													include.contains(FacetRangeInclude.EDGE)))),  
									false));
				}
				
				if (all || others.contains(FacetRangeOther.BETWEEN)) {
					res.add(FacetRangeOther.BETWEEN.toString(),
							rangeCount(sf, startS, endS,
									(include.contains(FacetRangeInclude.LOWER) ||
											include.contains(FacetRangeInclude.EDGE)),
									(include.contains(FacetRangeInclude.UPPER) ||
											include.contains(FacetRangeInclude.EDGE))));
				}
			}
		}
		
		return res;
	}  
  
	/**
	 * Macro for getting the numDocs of range over docs
	 * @see Searcher#numDocs
	 * @see TermRangeQuery
	 */
	protected int rangeCount(SchemaField sf, String low, String high,
			boolean iLow, boolean iHigh) throws ErrorException {
		IQuery rangeQ = sf.getType().getRangeQuery(null, sf, low, high, iLow, iHigh);
		
		if (mParams.getBool(GroupParams.GROUP_FACET, false)) 
			return getGroupedFacetQueryCount(rangeQ);
		else 
			return mSearcher.getNumDocs(rangeQ, mBase);
	}

	/**
	 * @deprecated Use rangeCount(SchemaField,String,String,boolean,boolean) which is more generalized
	 */
	@Deprecated
	protected int rangeCount(SchemaField sf, Date low, Date high,
			boolean iLow, boolean iHigh) throws ErrorException {
		IQuery rangeQ = ((DateFieldType)(sf.getType())).getRangeQuery(null, sf, low, high, iLow, iHigh);
		return mSearcher.getNumDocs((Query)rangeQ, mBase);
	}
  
}
