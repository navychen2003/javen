package org.javenstudio.falcon.search.component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.Field;
import org.javenstudio.common.indexdb.document.StringField;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.hornet.grouping.GroupDocs;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.grouping.TopGroups;
import org.javenstudio.hornet.search.AdvancedSort;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.RequestHelper;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchReturnFields;
import org.javenstudio.falcon.search.grouping.Grouping;
import org.javenstudio.falcon.search.grouping.GroupingSpecification;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.hits.DocSlice;
import org.javenstudio.falcon.search.hits.QueryCommand;
import org.javenstudio.falcon.search.hits.QueryResult;
import org.javenstudio.falcon.search.hits.ShardDoc;
import org.javenstudio.falcon.search.hits.ShardFieldSortedHitQueue;
import org.javenstudio.falcon.search.hits.SortSpec;
import org.javenstudio.falcon.search.params.GroupParams;
import org.javenstudio.falcon.search.params.MoreLikeThisParams;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.query.QueryBuilderFactory;
import org.javenstudio.falcon.search.query.QueryParsing;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardRequestFactory;
import org.javenstudio.falcon.search.shard.ShardResponse;
import org.javenstudio.falcon.search.shard.ShardResponseProcessor;
import org.javenstudio.falcon.search.transformer.EndResultTransformer;
import org.javenstudio.falcon.search.transformer.GroupedEndResultTransformer;
import org.javenstudio.falcon.search.transformer.MainEndResultTransformer;
import org.javenstudio.falcon.search.transformer.QueryFieldCommand;
import org.javenstudio.falcon.search.transformer.ResultSource;
import org.javenstudio.falcon.search.transformer.SearchGroupShardResponseProcessor;
import org.javenstudio.falcon.search.transformer.SearchGroupsFieldCommand;
import org.javenstudio.falcon.search.transformer.SearchGroupsRequestFactory;
import org.javenstudio.falcon.search.transformer.SearchGroupsResultTransformer;
import org.javenstudio.falcon.search.transformer.SimpleEndResultTransformer;
import org.javenstudio.falcon.search.transformer.StoredFieldsShardRequestFactory;
import org.javenstudio.falcon.search.transformer.StoredFieldsShardResponseProcessor;
import org.javenstudio.falcon.search.transformer.TopGroupsFieldCommand;
import org.javenstudio.falcon.search.transformer.TopGroupsResultTransformer;
import org.javenstudio.falcon.search.transformer.TopGroupsShardRequestFactory;
import org.javenstudio.falcon.search.transformer.TopGroupsShardResponseProcessor;

/**
 * TODO!
 * 
 * @since 1.3
 */
public class QueryComponent extends SearchComponent {
	static final Logger LOG = Logger.getLogger(QueryComponent.class);
	
	public static final String COMPONENT_NAME = "query";
  
	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		ISearchRequest req = rb.getRequest();
		Params params = req.getParams();
		if (!params.getBool(COMPONENT_NAME, true)) 
			return;
		
		ISearchResponse rsp = rb.getResponse();
		SearchReturnFields returnFields = rsp.getReturnFields(); 
		
		int flags = 0;
		if (returnFields.wantsScore()) 
			flags |= Searcher.GET_SCORES;
    
		rb.setFieldFlags(flags);

		String defType = params.get(QueryParsing.DEFTYPE, QueryBuilderFactory.DEFAULT_QTYPE);

		// get it from the response builder to give a different 
		// component a chance to set it.
		String queryString = rb.getQueryString();
		
		if (queryString == null) {
			// this is the normal way it's set.
			queryString = params.get(CommonParams.Q);
			rb.setQueryString(queryString);
		}

		QueryBuilder parser = getSearchCore().getQueryFactory()
				.getQueryBuilder(rb.getQueryString(), defType, req);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("QueryBuilder: " + (parser != null ? parser.getClass().getName() : "null") 
					+ " defType=" + defType);
		}
		
		IQuery q = parser.getQuery();
		// normalize a null query to a query that matches nothing
		if (q == null) { 
			q = new BooleanQuery(); 
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("normalize a null query to a query that matches nothing");
		}
		
		rb.setQuery(q);
		rb.setSortSpec(parser.getSort(true));
		rb.setQueryBuilder(parser);
		rb.setScoreDoc(parser.getPaging());
      
		String[] fqs = req.getParams().getParams(CommonParams.FQ);
		
		if (fqs != null && fqs.length != 0) {
			List<IQuery> filters = rb.getFilters();
			if (filters == null) 
				filters = new ArrayList<IQuery>(fqs.length);
        
			for (String fq : fqs) {
				if (fq != null && fq.trim().length() != 0) {
					QueryBuilder fqp = getSearchCore().getQueryFactory()
							.getQueryBuilder(fq, null, req);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("filter QueryBuilder: " + (fqp != null ? fqp.getClass().getName() : "null") 
								+ " fq=" + fq);
					}
					
					filters.add(fqp.getQuery());
				}
			}
			
			// only set the filters if they are not empty otherwise
			// fq=&someotherParam= will trigger all docs filter for every request 
			// if filter cache is disabled
			if (!filters.isEmpty()) 
				rb.setFilters(filters);
		}

		boolean grouping = params.getBool(GroupParams.GROUP, false);
		if (!grouping) 
			return;

		QueryCommand cmd = rb.getQueryCommand();
		Searcher searcher = rb.getSearcher();
		
		GroupingSpecification groupingSpec = new GroupingSpecification();
		rb.setGroupingSpec(groupingSpec);

		//TODO: move weighting of sort
		ISort groupSort = searcher.weightSort(cmd.getSort());
		if (groupSort == null) 
			groupSort = AdvancedSort.RELEVANCE;
    
		// groupSort defaults to sort
		String groupSortStr = params.get(GroupParams.GROUP_SORT);
		
		//TODO: move weighting of sort
		ISort sortWithinGroup = (groupSortStr == null) ? groupSort : 
    		searcher.weightSort(QueryParsing.parseSort(groupSortStr, req, 
    				getSearchCore().getQueryFactory()));
		
		if (sortWithinGroup == null) 
			sortWithinGroup = AdvancedSort.RELEVANCE;
    
		groupingSpec.setSortWithinGroup(sortWithinGroup);
		groupingSpec.setGroupSort(groupSort);

		String formatStr = params.get(GroupParams.GROUP_FORMAT, 
				Grouping.Format.GROUPED.name());
		
		Grouping.Format responseFormat;
		try {
			responseFormat = Grouping.Format.valueOf(formatStr);
		} catch (IllegalArgumentException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					String.format(Locale.ROOT, "Illegal %s parameter", GroupParams.GROUP_FORMAT));
		}
		
		groupingSpec.setResponseFormat(responseFormat);

		groupingSpec.setFields(params.getParams(GroupParams.GROUP_FIELD));
		groupingSpec.setQueries(params.getParams(GroupParams.GROUP_QUERY));
		groupingSpec.setFunctions(params.getParams(GroupParams.GROUP_FUNC));
		groupingSpec.setGroupOffset(params.getInt(GroupParams.GROUP_OFFSET, 0));
		groupingSpec.setGroupLimit(params.getInt(GroupParams.GROUP_LIMIT, 1));
		
		groupingSpec.setOffset(rb.getSortSpec().getOffset());
		groupingSpec.setLimit(rb.getSortSpec().getCount());
		
		groupingSpec.setIncludeGroupCount(params.getBool(GroupParams.GROUP_TOTAL_COUNT, false));
		groupingSpec.setIsMain(params.getBool(GroupParams.GROUP_MAIN, false));
		groupingSpec.setNeedScore((cmd.getFlags() & Searcher.GET_SCORES) != 0);
		groupingSpec.setTruncateGroups(params.getBool(GroupParams.GROUP_TRUNCATE, false));
	}

	/**
	 * Actually run the query
	 */
	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		ISearchRequest req = rb.getRequest();
		ISearchResponse rsp = rb.getResponse();
	  
		Params params = req.getParams();
		if (!params.getBool(COMPONENT_NAME, true)) 
			return;
	  
		Searcher searcher = req.getSearcher();

		if (rb.getQueryCommand().getOffset() < 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"'start' parameter cannot be negative");
		}

		// -1 as flag if not set.
		long timeAllowed = (long)params.getInt(CommonParams.TIME_ALLOWED, -1);

		// Optional: This could also be implemented by the top-level searcher sending
		// a filter that lists the ids... that would be transparent to
		// the request handler, but would be more expensive (and would preserve score
		// too if desired).
		String ids = params.get(ShardParams.IDS);
	  
		if (ids != null) {
			SchemaField idField = req.getSearchCore().getSchema().getUniqueKeyField();
			List<String> idArr = StrHelper.splitSmart(ids, ",", true);
		  
			int[] indexIds = new int[idArr.size()];
			int docs = 0;
		  
			for (int i=0; i < idArr.size(); i++) {
				int id = searcher.getFirstMatch(new Term(idField.getName(), 
						idField.getType().toInternal(idArr.get(i))));
			  
				if (id >= 0)
					indexIds[docs++] = id;
			}

			DocListAndSet res = new DocListAndSet();
			res.setDocList(new DocSlice(0, docs, indexIds, null, docs, 0));
		  
			if (rb.isNeedDocSet()) {
				// TODO: create a cache for this!
				List<IQuery> queries = new ArrayList<IQuery>();
				queries.add(rb.getQuery());
			  
				List<IQuery> filters = rb.getFilters();
				if (filters != null) 
					queries.addAll(filters);
			  
				res.setDocSet(searcher.getDocSet(queries));
			}
		  
			rb.setResults(res);
      
			QueryData ctx = new QueryData(rb.getResults().getDocList());
			rsp.add("response", ctx);
		  
			return;
		}

		QueryCommand cmd = rb.getQueryCommand();
		cmd.setTimeAllowed(timeAllowed);
		
		QueryResult result = new QueryResult();

		// grouping / field collapsing
		GroupingSpecification groupingSpec = rb.getGroupingSpec();
		
		if (groupingSpec != null) {
			try {
				boolean needScores = (cmd.getFlags() & Searcher.GET_SCORES) != 0;
				if (params.getBool(GroupParams.GROUP_DISTRIBUTED_FIRST, false)) {
					SearchCommands.Builder topsGroupsActionBuilder = new SearchCommands.Builder()
							.setQueryCommand(cmd)
							.setNeedDocSet(false) // Order matters here
							.setIncludeHitCount(true)
							.setSearcher(searcher);

					for (String field : groupingSpec.getFields()) {
						topsGroupsActionBuilder.addSearchCommand(new SearchGroupsFieldCommand.Builder()
								.setField(searcher.getSchema().getField(field))
								.setGroupSort(groupingSpec.getGroupSort())
								.setTopNGroups(cmd.getOffset() + cmd.getLength())
								.setIncludeGroupCount(groupingSpec.isIncludeGroupCount())
								.build());
					}

					SearchCommands commandHandler = topsGroupsActionBuilder.build();
					commandHandler.execute();
					
					SearchGroupsResultTransformer serializer = new SearchGroupsResultTransformer(searcher);
					rsp.add("firstPhase", commandHandler.processResult(result, serializer));
					
					rsp.add("totalHitCount", commandHandler.getTotalHitCount());
					rb.setResult(result);
					
					return;
					
				} else if (params.getBool(GroupParams.GROUP_DISTRIBUTED_SECOND, false)) {
					SearchCommands.Builder secondPhaseBuilder = new SearchCommands.Builder()
						.setQueryCommand(cmd)
						.setTruncateGroups(groupingSpec.isTruncateGroups() && 
								groupingSpec.getFields().length > 0)
						.setSearcher(searcher);

					for (String field : groupingSpec.getFields()) {
						String[] topGroupsParam = params.getParams(
								GroupParams.GROUP_DISTRIBUTED_TOPGROUPS_PREFIX + field);
						if (topGroupsParam == null) 
							topGroupsParam = new String[0];
						
						List<SearchGroup<BytesRef>> topGroups = 
								new ArrayList<SearchGroup<BytesRef>>(topGroupsParam.length);
						
						for (String topGroup : topGroupsParam) {
							SearchGroup<BytesRef> searchGroup = new SearchGroup<BytesRef>();
							if (!topGroup.equals(TopGroupsShardRequestFactory.GROUP_NULL_VALUE)) {
								searchGroup.setGroupValue(new BytesRef(searcher.getSchema()
										.getField(field).getType().readableToIndexed(topGroup)));
							}
							topGroups.add(searchGroup);
						}

						secondPhaseBuilder.addSearchCommand(new TopGroupsFieldCommand.Builder()
								.setField(searcher.getSchema().getField(field))
								.setGroupSort(groupingSpec.getGroupSort())
								.setSortWithinGroup(groupingSpec.getSortWithinGroup())
								.setFirstPhaseGroups(topGroups)
								.setMaxDocPerGroup(groupingSpec.getGroupOffset() + groupingSpec.getGroupLimit())
								.setNeedScores(needScores)
								.setNeedMaxScore(needScores)
								.build());
					}

					for (String query : groupingSpec.getQueries()) {
						secondPhaseBuilder.addSearchCommand(new QueryFieldCommand.Builder()
								.setDocsToCollect(groupingSpec.getOffset() + groupingSpec.getLimit())
								.setSort(groupingSpec.getGroupSort())
								.setQuery(query, rb.getRequest())
								.setDocSet(searcher)
								.build());
					}

					SearchCommands commandHandler = secondPhaseBuilder.build();
					commandHandler.execute();
					
					TopGroupsResultTransformer serializer = new TopGroupsResultTransformer(rb);
					rsp.add("secondPhase", commandHandler.processResult(result, serializer));
					rb.setResult(result);
					
					return;
				}

				int maxDocsPercentageToCache = params.getInt(GroupParams.GROUP_CACHE_PERCENTAGE, 0);
				boolean cacheSecondPassSearch = maxDocsPercentageToCache >= 1 && maxDocsPercentageToCache <= 100;
				
				Grouping.TotalCount defaultTotalCount = groupingSpec.isIncludeGroupCount() ?
						Grouping.TotalCount.GROUPED : Grouping.TotalCount.UNGROUPED;
				
				int limitDefault = cmd.getLength(); // this is normally from "rows"
				Grouping grouping = new Grouping(searcher, result, cmd, 
						cacheSecondPassSearch, maxDocsPercentageToCache, groupingSpec.isMain());
				
				grouping.setSort(groupingSpec.getGroupSort())
						.setGroupSort(groupingSpec.getSortWithinGroup())
						.setDefaultFormat(groupingSpec.getResponseFormat())
						.setLimitDefault(limitDefault)
						.setDefaultTotalCount(defaultTotalCount)
						.setDocsPerGroupDefault(groupingSpec.getGroupLimit())
						.setGroupOffsetDefault(groupingSpec.getGroupOffset())
						.setGetGroupedDocSet(groupingSpec.isTruncateGroups());

				if (groupingSpec.getFields() != null) {
					for (String field : groupingSpec.getFields()) {
						grouping.addFieldCommand(field, rb.getRequest());
					}
				}

				if (groupingSpec.getFunctions() != null) {
					for (String groupByStr : groupingSpec.getFunctions()) {
						grouping.addFunctionCommand(groupByStr, rb.getRequest());
					}
				}

				if (groupingSpec.getQueries() != null) {
					for (String groupByStr : groupingSpec.getQueries()) {
						grouping.addQueryCommand(groupByStr, rb.getRequest());
					}
				}

				if (rb.isDoHighlights() || rb.isDebug() || params.getBool(MoreLikeThisParams.MLT, false)) {
					// we need a single list of the returned docs
					cmd.setFlags(Searcher.GET_DOCLIST);
				}

				grouping.execute();
				
				if (grouping.isSignalCacheWarning()) {
					rsp.add("cacheWarning", String.format(Locale.ROOT, 
							"Cache limit of %d percent relative to maxdoc has exceeded. " +
							"Please increase cache size or disable caching.", 
							maxDocsPercentageToCache));
				}
				
				rb.setResult(result);

				if (grouping.getMainResult() != null) {
					QueryData ctx = new QueryData(grouping.getMainResult());
					rsp.add("response", ctx);
					rsp.getToLog().add("hits", grouping.getMainResult().matches());
					
				} else if (!grouping.getCommands().isEmpty()) { 
					// Can never be empty since grouping.execute() checks for this.
					rsp.add("grouped", result.getGroupedResults());
					rsp.getToLog().add("hits", grouping.getCommands().get(0).getMatches());
				}
				
				return;
				
			} catch (Exception e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
			}
		}

		// normal search result
		searcher.search(result, cmd);
		rb.setResult(result);

		QueryData ctx = new QueryData(rb.getResults().getDocList(), rb.getQuery());
		rsp.add("response", ctx);
		rsp.getToLog().add("hits", rb.getResults().getDocList().matches());

		doFieldSortValues(rb, searcher);
		doPrefetch(rb);
	}

	protected void doFieldSortValues(ResponseBuilder rb, Searcher searcher) 
			throws ErrorException {
		ISearchRequest req = rb.getRequest();
		ISearchResponse rsp = rb.getResponse();
		
		final CharsRef spare = new CharsRef();
		
		// The query cache doesn't currently store sort field values, and Searcher doesn't
		// currently have an option to return sort field values.  Because of this, we
		// take the documents given and re-derive the sort values.
		boolean fsv = req.getParams().getBool(ResponseBuilder.FIELD_SORT_VALUES, false);
		
		if (fsv) {
			ISort sort = searcher.weightSort(rb.getSortSpec().getSort());
			ISortField[] sortFields = (sort == null) ? 
					new ISortField[]{AdvancedSortField.FIELD_SCORE} : sort.getSortFields();
					
			// order is important for the sort fields
			NamedList<Object[]> sortVals = new NamedList<Object[]>(); 
			StringField field = new StringField("dummy", "", Field.Store.NO); // a dummy Field
			
			IIndexReaderRef topReaderContext = searcher.getTopReaderContext();
			List<IAtomicReaderRef> leaves = topReaderContext.getLeaves();
			
			IAtomicReaderRef currentLeaf = null;
			if (leaves.size() == 1) {
				// if there is a single segment, use that subReader and avoid looking up each time
				currentLeaf = leaves.get(0);
				leaves = null;
			}

			DocList docList = rb.getResults().getDocList();

			// sort ids from lowest to highest so we can access them in order
			int nDocs = docList.size();
			long[] sortedIds = new long[nDocs];
			
			DocIterator it = rb.getResults().getDocList().iterator();
			for (int i=0; i < nDocs; i++) {
				sortedIds[i] = (((long)it.nextDoc()) << 32) | i;
			}
			
			Arrays.sort(sortedIds);

			for (ISortField sortField: sortFields) {
				ISortField.Type type = sortField.getType();
				if (type == ISortField.Type.SCORE || type == ISortField.Type.DOC) 
					continue;

				IFieldComparator<?> comparator = null;

				String fieldname = sortField.getField();
				SchemaFieldType ft = (fieldname == null) ? null : 
					req.getSearchCore().getSchema().getFieldTypeNoEx(fieldname);

				Object[] vals = new Object[nDocs];
				int lastIdx = -1;
				int idx = 0;

				for (long idAndPos : sortedIds) {
					int doc = (int)(idAndPos >>> 32);
					int position = (int)idAndPos;

					if (leaves != null) {
						idx = ReaderUtil.subIndex(doc, leaves);
						currentLeaf = leaves.get(idx);
						
						if (idx != lastIdx) {
							// we switched segments.  invalidate comparator.
							comparator = null;
						}
					}

					try {
						if (comparator == null) {
							comparator = sortField.getComparator(1,0);
							comparator = comparator.setNextReader(currentLeaf);
						}
	
						doc -= currentLeaf.getDocBase();  // adjust for what segment this is in
						comparator.copy(0, doc);
						
					} catch (IOException ex) { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
					}
					
					Object val = comparator.getValue(0);

					// Sortable float, double, int, long types all just use a string
					// comparator. For these, we need to put the type into a readable
					// format.  One reason for this is that XML can't represent all
					// string values (or even all unicode code points).
					// indexedToReadable() should be a no-op and should
					// thus be harmless anyway (for all current ways anyway)
					if (val instanceof String) {
						field.setStringValue((String)val);
						val = ft.toObject(field);
					}

					// Must do the same conversion when sorting by a
					// String field in index, which returns the terms
					// data as BytesRef:
					if (val instanceof BytesRef) {
						UnicodeUtil.UTF8toUTF16((BytesRef)val, spare);
						field.setStringValue(spare.toString());
						val = ft.toObject(field);
					}

					vals[position] = val;
				}

				sortVals.add(fieldname, vals);
			}

			rsp.add("sort_values", sortVals);
		}
	}

	protected void doPrefetch(ResponseBuilder rb) throws ErrorException {
		ISearchRequest req = rb.getRequest();
		ISearchResponse rsp = rb.getResponse();
		
		//pre-fetch returned documents
		DocList docList = rb.getResults().getDocList();
		
		if (!req.getParams().getBool(ShardParams.IS_SHARD,false) && 
			docList != null && docList.size() <= 50) {
			RequestHelper.optimizePreFetchDocs(rb, docList, rb.getQuery(), req, rsp);
		}
	}

	@Override  
	public int distributedProcess(ResponseBuilder rb) throws ErrorException {
		if (rb.isGrouping()) 
			return groupedDistributedProcess(rb);
		else 
			return regularDistributedProcess(rb);
	}

	private int groupedDistributedProcess(ResponseBuilder rb) throws ErrorException {
		int nextStage = ResponseBuilder.STAGE_DONE;
		ShardRequestFactory shardRequestFactory = null;

		if (rb.getStage() < ResponseBuilder.STAGE_PARSE_QUERY) {
			nextStage = ResponseBuilder.STAGE_PARSE_QUERY;
			
		} else if (rb.getStage() == ResponseBuilder.STAGE_PARSE_QUERY) {
			createDistributedIdf(rb);
			nextStage = ResponseBuilder.STAGE_TOP_GROUPS;
			
		} else if (rb.getStage() < ResponseBuilder.STAGE_TOP_GROUPS) {
			nextStage = ResponseBuilder.STAGE_TOP_GROUPS;
			
		} else if (rb.getStage() == ResponseBuilder.STAGE_TOP_GROUPS) {
			shardRequestFactory = new SearchGroupsRequestFactory();
			nextStage = ResponseBuilder.STAGE_EXECUTE_QUERY;
			
		} else if (rb.getStage() < ResponseBuilder.STAGE_EXECUTE_QUERY) {
			nextStage = ResponseBuilder.STAGE_EXECUTE_QUERY;
			
		} else if (rb.getStage() == ResponseBuilder.STAGE_EXECUTE_QUERY) {
			shardRequestFactory = new TopGroupsShardRequestFactory();
			nextStage = ResponseBuilder.STAGE_GET_FIELDS;
			
		} else if (rb.getStage() < ResponseBuilder.STAGE_GET_FIELDS) {
			nextStage = ResponseBuilder.STAGE_GET_FIELDS;
			
		} else if (rb.getStage() == ResponseBuilder.STAGE_GET_FIELDS) {
			shardRequestFactory = new StoredFieldsShardRequestFactory();
			nextStage = ResponseBuilder.STAGE_DONE;
		}

		if (shardRequestFactory != null) {
			for (ShardRequest shardRequest : shardRequestFactory.constructRequest(rb)) {
				rb.addRequest(this, shardRequest);
			}
		}
		
		return nextStage;
	}

	private int regularDistributedProcess(ResponseBuilder rb) throws ErrorException {
		if (rb.getStage() < ResponseBuilder.STAGE_PARSE_QUERY)
			return ResponseBuilder.STAGE_PARSE_QUERY;
		
		if (rb.getStage() == ResponseBuilder.STAGE_PARSE_QUERY) {
			createDistributedIdf(rb);
			return ResponseBuilder.STAGE_EXECUTE_QUERY;
		}
		
		if (rb.getStage() < ResponseBuilder.STAGE_EXECUTE_QUERY) 
			return ResponseBuilder.STAGE_EXECUTE_QUERY;
		
		if (rb.getStage() == ResponseBuilder.STAGE_EXECUTE_QUERY) {
			createMainQuery(rb);
			return ResponseBuilder.STAGE_GET_FIELDS;
		}
		
		if (rb.getStage() < ResponseBuilder.STAGE_GET_FIELDS) 
			return ResponseBuilder.STAGE_GET_FIELDS;
		
		if (rb.getStage() == ResponseBuilder.STAGE_GET_FIELDS) {
			createRetrieveDocs(rb);
			return ResponseBuilder.STAGE_DONE;
		}
		
		return ResponseBuilder.STAGE_DONE;
	}

	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) 
			throws ErrorException {
		if (rb.isGrouping()) 
			handleGroupedResponses(rb, sreq);
		else 
			handleRegularResponses(rb, sreq);
	}

	private void handleGroupedResponses(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		ShardResponseProcessor responseProcessor = null;
		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_TOP_GROUPS) != 0) {
			responseProcessor = new SearchGroupShardResponseProcessor();
			
		} else if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) {
			responseProcessor = new TopGroupsShardResponseProcessor();
			
		} else if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
			responseProcessor = new StoredFieldsShardResponseProcessor();
		}

		if (responseProcessor != null) 
			responseProcessor.process(rb, sreq);
	}

	private void handleRegularResponses(ResponseBuilder rb, ShardRequest sreq) 
			throws ErrorException {
		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) 
			mergeIds(rb, sreq);
    
		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FIELDS) != 0) 
			returnFields(rb, sreq);
	}

	@Override
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		if (rb.getStage() != ResponseBuilder.STAGE_GET_FIELDS) 
			return;
		
		if (rb.isGrouping()) 
			groupedFinishStage(rb);
		else 
			regularFinishStage(rb);
	}

	private static final EndResultTransformer MAIN_END_RESULT_TRANSFORMER = 
			new MainEndResultTransformer();
	private static final EndResultTransformer SIMPLE_END_RESULT_TRANSFORMER = 
			new SimpleEndResultTransformer();

	@SuppressWarnings("unchecked")
	private void groupedFinishStage(final ResponseBuilder rb) throws ErrorException {
		// To have same response as non-distributed request.
		GroupingSpecification groupSpec = rb.getGroupingSpec();
		if (rb.isMergedTopGroupsEmpty()) {
			for (String field : groupSpec.getFields()) {
				rb.addMergedTopGroup(field, new TopGroups<BytesRef>(null, null, 0, 0, 
						new GroupDocs[]{}, Float.NaN));
			}
			rb.setResultIds(new HashMap<Object, ShardDoc>());
		}

		ResultSource resultDocumentSource = new ResultSource() {
				@Override
				public ResultItem retrieve(IScoreDoc doc) {
					ShardDoc shardDoc = (ShardDoc) doc;
					return rb.getRetrievedDocument(shardDoc.getId());
				}
			};
		
		EndResultTransformer endResultTransformer;
		if (groupSpec.isMain()) {
			endResultTransformer = MAIN_END_RESULT_TRANSFORMER;
			
		} else if (Grouping.Format.GROUPED == groupSpec.getResponseFormat()) {
			endResultTransformer = new GroupedEndResultTransformer(rb.getRequest().getSearcher());
			
		} else if (Grouping.Format.SIMPLE == groupSpec.getResponseFormat() && !groupSpec.isMain()) {
			endResultTransformer = SIMPLE_END_RESULT_TRANSFORMER;
			
		} else {
			return;
		}
		
		Map<String, Object> combinedMap = new LinkedHashMap<String, Object>();
		combinedMap.putAll(rb.getMergedTopGroups());
		combinedMap.putAll(rb.getMergedQueryCommandResults());
		
		endResultTransformer.transform(combinedMap, rb, resultDocumentSource);
	}

	private void regularFinishStage(ResponseBuilder rb) {
		// We may not have been able to retrieve all the docs due to an
		// index change.  Remove any null documents.
		for (Iterator<ResultItem> iter = rb.getResponseDocs().iterator(); iter.hasNext();) {
			if (iter.next() == null) {
				iter.remove();
				rb.getResponseDocs().setNumFound(rb.getResponseDocs().getNumFound()-1);
			}
		}

		rb.getResponse().add("response", rb.getResponseDocs());
	}

	private void createDistributedIdf(ResponseBuilder rb) {
		// TODO
	}

	private void createMainQuery(ResponseBuilder rb) throws ErrorException {
		ShardRequest sreq = new ShardRequest();
		sreq.setPurpose(ShardRequest.PURPOSE_GET_TOP_IDS);

		sreq.setParams(new ModifiableParams(rb.getRequest().getParams()));
		// TODO: base on current params or original params?

		// don't pass through any shards param
		sreq.getParams().remove(ShardParams.SHARDS);

		// set the start (offset) to 0 for each shard request so we can properly merge
		// results from the start.
		if (rb.getShardsStart() > -1) {
			// if the client set shards.start set this explicitly
			sreq.getParams().set(CommonParams.START, rb.getShardsStart());
		} else {
			sreq.getParams().set(CommonParams.START, "0");
		}
		
		// TODO: should we even use the SortSpec?  That's obtained from the QParser, and
		// perhaps we shouldn't attempt to parse the query at this level?
		// Alternate Idea: instead of specifying all these things at the upper level,
		// we could just specify that this is a shard request.
		if (rb.getShardsRows() > -1) {
			// if the client set shards.rows set this explicity
			sreq.getParams().set(CommonParams.ROWS, rb.getShardsRows());
			
		} else {
			sreq.getParams().set(CommonParams.ROWS, 
					rb.getSortSpec().getOffset() + rb.getSortSpec().getCount());
		}

		// in this first phase, request only the unique key field
		// and any fields needed for merging.
		sreq.getParams().set(ResponseBuilder.FIELD_SORT_VALUES, "true");

		if ((rb.getFieldFlags() & Searcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore()) {
			sreq.getParams().set(CommonParams.FL, 
					rb.getRequest().getSearchCore().getSchema().getUniqueKeyField().getName() + ",score");
			
		} else {
			sreq.getParams().set(CommonParams.FL, 
					rb.getRequest().getSearchCore().getSchema().getUniqueKeyField().getName());      
		}

		rb.addRequest(this, sreq);
	}

	private void mergeIds(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		SortSpec ss = rb.getSortSpec();
		ISort sort = ss.getSort();

		ISortField[] sortFields = null;
		if (sort != null) 
			sortFields = sort.getSortFields();
		else 
			sortFields = new ISortField[]{AdvancedSortField.FIELD_SCORE};
		
		SchemaField uniqueKeyField = 
				rb.getSearchCore().getSchema().getUniqueKeyField();

		// id to shard mapping, to eliminate any accidental dups
		Map<Object,String> uniqueDoc = new HashMap<Object,String>();    

		// Merge the docs via a priority queue so we don't have to sort *all* of the
		// documents... we only need to order the top (rows+start)
		ShardFieldSortedHitQueue queue = new ShardFieldSortedHitQueue(sortFields, 
				ss.getOffset() + ss.getCount());

		NamedList<Object> shardInfo = null;
		if (rb.getRequest().getParams().getBool(ShardParams.SHARDS_INFO, false)) {
			shardInfo = new NamedMap<Object>();
			rb.getResponse().getValues().add(ShardParams.SHARDS_INFO, shardInfo);
		}
      
		long numFound = 0;
		Float maxScore = null;
		boolean partialResults = false;
		
		for (ShardResponse srsp : sreq.getResponses()) {
			ResultList docs = null;

			if (shardInfo != null) {
				NamedMap<Object> nl = new NamedMap<Object>();
          
				Throwable t = srsp.getException();
				if (t != null) {
					nl.add("error", t.toString());
					
					StringWriter trace = new StringWriter();
					t.printStackTrace(new PrintWriter(trace));
					nl.add("trace", trace.toString());
					
				} else {
					docs = (ResultList)srsp.getResponse().getValue("response");
					nl.add("numFound", docs.getNumFound());
					nl.add("maxScore", docs.getMaxScore());
				}
				
				if (srsp.getResponse() != null) 
					nl.add("time", srsp.getResponse().getElapsedTime());
				
				shardInfo.add(srsp.getShard(), nl);
			}
			
			// now that we've added the shard info, let's only proceed if we have no error.
			if (srsp.getException() != null) 
				continue;

			if (docs == null) { 
				// could have been initialized in the shards info block above
				docs = (ResultList)srsp.getResponse().getValue("response");
			}
        
			NamedList<?> responseHeader = (NamedList<?>)srsp.getResponse().getValue("responseHeader");
			if (responseHeader != null && Boolean.TRUE.equals(responseHeader.get("partialResults"))) 
				partialResults = true;
        
			// calculate global maxScore and numDocsFound
			if (docs.getMaxScore() != null) {
				maxScore = (maxScore == null) ? docs.getMaxScore() : 
					Math.max(maxScore, docs.getMaxScore());
			}
			
			numFound += docs.getNumFound();

			NamedList<?> sortFieldValues = (NamedList<?>)(srsp.getResponse().getValue("sort_values"));

			// go through every doc in this response, construct a ShardDoc, and
			// put it in the priority queue so it can be ordered.
			for (int i=0; i < docs.size(); i++) {
				ResultItem doc = docs.get(i);
				Object id = doc.getFieldValue(uniqueKeyField.getName());

				String prevShard = uniqueDoc.put(id, srsp.getShard());
				if (prevShard != null) {
					// duplicate detected
					numFound --;

					// For now, just always use the first encountered since we can't currently
					// remove the previous one added to the priority queue.  If we switched
					// to the Java5 PriorityQueue, this would be easier.
					continue;
					
					// make which duplicate is used deterministic based on shard
					// if (prevShard.compareTo(srsp.shard) >= 0) {
					//  TODO: remove previous from priority queue
					//  continue;
					// }
				}

				ShardDoc shardDoc = new ShardDoc();
				shardDoc.setId(id);
				shardDoc.setShard(srsp.getShard());
				shardDoc.setOrderInShard(i);
          
				Object scoreObj = doc.getFieldValue("score");
				if (scoreObj != null) {
					if (scoreObj instanceof String) 
						shardDoc.setScore(Float.parseFloat((String)scoreObj));
					else 
						shardDoc.setScore((Float)scoreObj);
				}

				shardDoc.setSortFieldValues(sortFieldValues);
				queue.insertWithOverflow(shardDoc);
			}
		}
      
		// The queue now has 0 -> queuesize docs, where queuesize <= start + rows
		// So we want to pop the last documents off the queue to get
		// the docs offset -> queuesize
		int resultSize = queue.size() - ss.getOffset();
		resultSize = Math.max(0, resultSize);  // there may not be any docs in range

		Map<Object,ShardDoc> resultIds = new HashMap<Object,ShardDoc>();
		
		for (int i = resultSize-1; i >= 0; i--) {
			ShardDoc shardDoc = queue.pop();
			shardDoc.setPositionInResponse(i);
			// Need the toString() for correlation with other lists that must
			// be strings (like keys in highlighting, explain, etc)
			resultIds.put(shardDoc.getId().toString(), shardDoc);
		}

		// Add hits for distributed requests
		//rb.rsp.addToLog("hits", numFound);

		ResultList responseDocs = new ResultList();
		if (maxScore != null) 
			responseDocs.setMaxScore(maxScore);
		
		responseDocs.setNumFound(numFound);
		responseDocs.setStart(ss.getOffset());
		
		// size appropriately
		for (int i=0; i < resultSize; i++) { 
			responseDocs.add(null); 
		}

		// save these results in a private area so we can access them
		// again when retrieving stored fields.
		// TODO: use ResponseBuilder (w/ comments) or the request context?
		rb.setResultIds(resultIds);
		rb.setResponseDocs(responseDocs);
		
		if (partialResults) 
			rb.getResponse().getResponseHeader().add("partialResults", Boolean.TRUE);
	}

	private void createRetrieveDocs(ResponseBuilder rb) throws ErrorException {
		// TODO: in a system with nTiers > 2, we could be passed "ids" here
		// unless those requests always go to the final destination shard

		// for each shard, collect the documents for that shard.
		Map<String, Collection<ShardDoc>> shardMap = 
				new HashMap<String,Collection<ShardDoc>>();
		
		for (ShardDoc sdoc : rb.getResultIds().values()) {
			Collection<ShardDoc> shardDocs = shardMap.get(sdoc.getShard());
			if (shardDocs == null) {
				shardDocs = new ArrayList<ShardDoc>();
				shardMap.put(sdoc.getShard(), shardDocs);
			}
			
			shardDocs.add(sdoc);
		}

		SchemaField uniqueField = rb.getSearchCore().getSchema().getUniqueKeyField();

		// Now create a request for each shard to retrieve the stored fields
		for (Collection<ShardDoc> shardDocs : shardMap.values()) {
			ShardRequest sreq = new ShardRequest();
			sreq.setPurpose(ShardRequest.PURPOSE_GET_FIELDS);

			sreq.setShards(new String[] {shardDocs.iterator().next().getShard()});
			sreq.setParams(new ModifiableParams());

			// add original params
			sreq.getParams().add(rb.getRequest().getParams());

			// no need for a sort, we already have order
			sreq.getParams().remove(CommonParams.SORT);

			// we already have the field sort values
			sreq.getParams().remove(ResponseBuilder.FIELD_SORT_VALUES);

			if (!rb.getResponse().getReturnFields().wantsField(uniqueField.getName())) 
				sreq.getParams().add(CommonParams.FL, uniqueField.getName());
    
			ArrayList<String> ids = new ArrayList<String>(shardDocs.size());
			
			for (ShardDoc shardDoc : shardDocs) {
				// TODO: depending on the type, we may need more tha a simple toString()?
				ids.add(shardDoc.getId().toString());
			}
			
			sreq.getParams().add(ShardParams.IDS, StrHelper.join(ids, ','));

			rb.addRequest(this, sreq);
		}
	}

	private void returnFields(ResponseBuilder rb, ShardRequest sreq) throws ErrorException {
		// Keep in mind that this could also be a shard in a multi-tiered system.
		// TODO: if a multi-tiered system, it seems like some requests
		// could/should bypass middlemen (like retrieving stored fields)
		// TODO: merge fsv to if requested

		if ((sreq.getPurpose() & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
			boolean returnScores = (rb.getFieldFlags() & Searcher.GET_SCORES) != 0;

			assert(sreq.getResponses().size() == 1);
			ShardResponse srsp = sreq.getResponses().get(0);
			ResultList docs = (ResultList)srsp.getResponse().getValue("response");

			String keyFieldName = rb.getSearchCore().getSchema().getUniqueKeyField().getName();
			boolean removeKeyField = !rb.getResponse().getReturnFields().wantsField(keyFieldName);

			for (ResultItem doc : docs) {
				Object id = doc.getFieldValue(keyFieldName);
				ShardDoc sdoc = rb.getResultIds().get(id.toString());
				
				if (sdoc != null) {
					if (returnScores && sdoc.getShardScore() != null) 
						doc.setField("score", sdoc.getShardScore());
					
					if (removeKeyField) 
						doc.removeFields(keyFieldName);
          
					rb.getResponseDocs().set(sdoc.getPositionInResponse(), doc);
				}
			}
		}
	}

}
