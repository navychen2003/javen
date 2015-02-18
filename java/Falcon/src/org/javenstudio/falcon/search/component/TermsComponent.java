package org.javenstudio.falcon.search.component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.BoundedTreeSet;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.facet.FacetCountPair;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.params.TermsParams;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.type.StringFieldType;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;

/**
 * Return TermEnum information, useful for things like auto suggest.
 * 
 * <pre class="prettyprint">
 * &lt;searchComponent name="termsComponent" class="lightning.TermsComponent"/&gt;
 * 
 * &lt;requestHandler name="/terms" class="lightning.SearchHandler"&gt;
 *   &lt;lst name="defaults"&gt;
 *     &lt;bool name="terms"&gt;true&lt;/bool&gt;
 *   &lt;/lst&gt;
 *   &lt;arr name="components"&gt;
 *     &lt;str&gt;termsComponent&lt;/str&gt;
 *   &lt;/arr&gt;
 * &lt;/requestHandler&gt;</pre>
 *
 * @see TermsParams See TermEnum class
 */
public class TermsComponent extends SearchComponent {
	public static final String COMPONENT_NAME = "terms";
	
	public static final int UNLIMITED_MAX_COUNT = -1;
	
	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		Params params = rb.getRequest().getParams();
		if (params.getBool(TermsParams.TERMS, false)) 
			rb.setDoTerms(true);

		// TODO: temporary... this should go in a different component.
		String shards = params.get(ShardParams.SHARDS);
		if (shards != null) {
			rb.setDistributed(true);
			if (params.get(ShardParams.SHARDS_QT) == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"No shards.qt parameter specified");
			}
			
			List<String> lst = StrHelper.splitSmart(shards, ",", true);
			rb.setShards(lst.toArray(new String[lst.size()]));
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		try { 
			doProcess(rb); 
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
  
	private void doProcess(ResponseBuilder rb) throws IOException, ErrorException {
		Params params = rb.getRequest().getParams();
		if (!params.getBool(TermsParams.TERMS, false)) 
			return;

		String[] fields = params.getParams(TermsParams.TERMS_FIELD);

		NamedList<Object> termsResult = new NamedMap<Object>();
		rb.getResponse().add("terms", termsResult);

		if (fields == null || fields.length == 0) 
			return;

		int limit = params.getInt(TermsParams.TERMS_LIMIT, 10);
		if (limit < 0) 
			limit = Integer.MAX_VALUE;

		String lowerStr = params.get(TermsParams.TERMS_LOWER);
		String upperStr = params.get(TermsParams.TERMS_UPPER);
		
		boolean upperIncl = params.getBool(TermsParams.TERMS_UPPER_INCLUSIVE, false);
		boolean lowerIncl = params.getBool(TermsParams.TERMS_LOWER_INCLUSIVE, true);
		
		boolean sort = !TermsParams.TERMS_SORT_INDEX.equals(
				params.get(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_COUNT));
		
		int freqmin = params.getInt(TermsParams.TERMS_MINCOUNT, 1);
		int freqmax = params.getInt(TermsParams.TERMS_MAXCOUNT, UNLIMITED_MAX_COUNT);
		if (freqmax<0) 
			freqmax = Integer.MAX_VALUE;
		
		String prefix = params.get(TermsParams.TERMS_PREFIX_STR);
		String regexp = params.get(TermsParams.TERMS_REGEXP_STR);
		
		Pattern pattern = (regexp != null) ? 
				Pattern.compile(regexp, resolveRegexpFlags(params)) : null;

		boolean raw = params.getBool(TermsParams.TERMS_RAW, false);

		final IAtomicReader indexReader = rb.getSearcher().getAtomicReader();
		IFields lfields = indexReader.getFields();

		for (String field : fields) {
			NamedList<Integer> fieldTerms = new NamedList<Integer>();
			termsResult.add(field, fieldTerms);

			ITerms terms = lfields == null ? null : lfields.getTerms(field);
			if (terms == null) {
				// no terms for this field
				continue;
			}

			SchemaFieldType ft = raw ? null : 
				rb.getSearchCore().getSchema().getFieldTypeNoEx(field);
			if (ft == null) 
				ft = new StringFieldType();

			// prefix must currently be text
			BytesRef prefixBytes = prefix == null ? null : new BytesRef(prefix);

			BytesRef upperBytes = null;
			if (upperStr != null) {
				upperBytes = new BytesRef();
				ft.readableToIndexed(upperStr, upperBytes);
			}

			BytesRef lowerBytes;
			if (lowerStr == null) {
				// If no lower bound was specified, use the prefix
				lowerBytes = prefixBytes;
				
			} else {
				lowerBytes = new BytesRef();
				if (raw) {
					// TODO: how to handle binary? perhaps we don't for "raw"... or if the field exists
					// perhaps we detect if the FieldType is non-character and expect hex if so?
					lowerBytes = new BytesRef(lowerStr);
					
				} else {
					lowerBytes = new BytesRef();
					ft.readableToIndexed(lowerStr, lowerBytes);
				}
			}

			ITermsEnum termsEnum = terms.iterator(null);
			BytesRef term = null;

			if (lowerBytes != null) {
				if (termsEnum.seekCeil(lowerBytes, true) == ITermsEnum.SeekStatus.END) {
					termsEnum = null;
					
				} else {
					term = termsEnum.getTerm();
					
					// Only advance the enum if we are excluding the lower bound 
					// and the lower Term actually matches
					if (lowerIncl == false && term.equals(lowerBytes)) 
						term = termsEnum.next();
				}
				
			} else {
				// position termsEnum on first term
				term = termsEnum.next();
			}

			BoundedTreeSet<FacetCountPair<BytesRef, Integer>> queue = 
					(sort ? new BoundedTreeSet<FacetCountPair<BytesRef, Integer>>(limit) : null);
			
			CharsRef external = new CharsRef();
			int i = 0;
			
			while (term != null && (i < limit || sort)) {
				boolean externalized = false; // did we fill in "external" yet for this term?

				// stop if the prefix doesn't match
				if (prefixBytes != null && !StringHelper.startsWith(term, prefixBytes)) 
					break;

				if (pattern != null) {
					// indexed text or external text?
					// TODO: support "raw" mode?
					ft.indexedToReadable(term, external);
					externalized = true;
					
					if (!pattern.matcher(external).matches()) {
						term = termsEnum.next();
						continue;
					}
				}

				if (upperBytes != null) {
					int upperCmp = term.compareTo(upperBytes);
					// if we are past the upper term, or equal to it (when don't include upper) then stop.
					if (upperCmp > 0 || (upperCmp == 0 && !upperIncl)) 
						break;
				}

				// This is a good term in the range. Check if mincount/maxcount conditions are satisfied.
				int docFreq = termsEnum.getDocFreq();
				
				if (docFreq >= freqmin && docFreq <= freqmax) {
					// add the term to the list
					if (sort) {
						queue.add(new FacetCountPair<BytesRef, Integer>(
								BytesRef.deepCopyOf(term), docFreq));
						
					} else {
						// TODO: handle raw somehow
						if (!externalized) 
							ft.indexedToReadable(term, external);
						
						fieldTerms.add(external.toString(), docFreq);
						i++;
					}
				}

				term = termsEnum.next();
			}

			if (sort) {
				for (FacetCountPair<BytesRef, Integer> item : queue) {
					if (i >= limit) break;
					
					ft.indexedToReadable(item.getKey(), external);          
					fieldTerms.add(external.toString(), item.getValue());
					
					i++;
				}
			}
		}
	}

	int resolveRegexpFlags(Params params) throws ErrorException {
		String[] flagParams = params.getParams(TermsParams.TERMS_REGEXP_FLAG);
		if (flagParams == null) 
			return 0;
		
		int flags = 0;
		for (String flagParam : flagParams) {
			try {
				flags |= TermsParams.TermsRegexpFlag.valueOf(
						flagParam.toUpperCase(Locale.ROOT)).getValue();
				
			} catch (IllegalArgumentException iae) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unknown terms regex flag '" + flagParam + "'");
			}
		}
		return flags;
	}

	@Override
	public int distributedProcess(ResponseBuilder rb) throws ErrorException {
		if (!rb.isDoTerms()) 
			return ResponseBuilder.STAGE_DONE;

		if (rb.getStage() == ResponseBuilder.STAGE_EXECUTE_QUERY) {
			TermsResponse th = rb.getTermsHelper();
			if (th == null) {
				th = new TermsResponse(); rb.setTermsHelper(th);
				th.init(rb.getRequest().getParams());
			}
			
			ShardRequest sreq = createShardQuery(rb.getRequest().getParams());
			rb.addRequest(this, sreq);
		}

		if (rb.getStage() < ResponseBuilder.STAGE_EXECUTE_QUERY) 
			return ResponseBuilder.STAGE_EXECUTE_QUERY;
		else 
			return ResponseBuilder.STAGE_DONE;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {
		if (!rb.isDoTerms() || (sreq.getPurpose() & ShardRequest.PURPOSE_GET_TERMS) == 0) 
			return;
		
		TermsResponse th = rb.getTermsHelper();
		if (th != null) {
			for (ShardResponse srsp : sreq.getResponses()) {
				NamedList<NamedList<Number>> terms = (NamedList<NamedList<Number>>) 
						srsp.getResponse().getValue("terms");
				th.parse(terms);
			}
		}
	}

	@Override
	public void finishStage(ResponseBuilder rb) throws ErrorException {
		if (!rb.isDoTerms() || rb.getStage() != ResponseBuilder.STAGE_EXECUTE_QUERY) 
			return;

		TermsResponse ti = rb.getTermsHelper();
		NamedList<?> terms = ti.buildResponse();

		rb.getResponse().add("terms", terms);
		rb.setTermsHelper(null);
	}

	private ShardRequest createShardQuery(Params params) throws ErrorException {
		ShardRequest sreq = new ShardRequest();
		sreq.setPurpose(ShardRequest.PURPOSE_GET_TERMS);

		// base shard request on original parameters
		sreq.setParams(new ModifiableParams(params));

		// remove any limits for shards, we want them to return all possible
		// responses
		// we want this so we can calculate the correct counts
		// dont sort by count to avoid that unnecessary overhead on the shards
		sreq.getParams().remove(TermsParams.TERMS_MAXCOUNT);
		sreq.getParams().remove(TermsParams.TERMS_MINCOUNT);
		sreq.getParams().set(TermsParams.TERMS_LIMIT, -1);
		sreq.getParams().set(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_INDEX);

		return sreq;
	}

}
