package org.javenstudio.falcon.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.query.FunctionQuery;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.ConstantScoreQuery;
import org.javenstudio.hornet.search.query.FuzzyQuery;
import org.javenstudio.hornet.search.query.NumericRangeQuery;
import org.javenstudio.hornet.search.query.PrefixQuery;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.panda.query.QueryParser;

/**
 * Collection of static utilities useful for query parsing.
 *
 */
public class QueryParsing {
	
	public static final String OP = "q.op";  // the Params used to override the QueryParser "default operator"
	public static final String V = "v";      // value of this parameter
	public static final String F = "f";      // field that a query or command pertains to
	public static final String TYPE = "type";// parser for this query or command
	public static final String DEFTYPE = "defType"; // default parser for any direct subqueries
	
	public static final String LOCALPARAM_START = "{!";
	public static final char LOCALPARAM_END = '}';
	
	public static final String DOCID = "_docid_";
	public static final String SCORE = "score";

	// true if the value was specified by the "v" param (i.e. v=myval, or v=$param)
	public static final String VAL_EXPLICIT = "__VAL_EXPLICIT__";


	/**
	 * Returns the "preferred" default operator for use by Query Parsers,
	 * based on the settings in the IndexSchema which may be overridden using 
	 * an optional String override value.
	 *
	 * @see IndexSchema#getQueryParserDefaultOperator()
	 * @see #OP
	 */
	public static QueryParser.Operator getQueryParserDefaultOperator(
			final IndexSchema sch, final String override) {
		String val = override;
		if (null == val) 
			val = sch.getQueryParserDefaultOperator();
		
		return "AND".equals(val) ? QueryParser.Operator.AND : QueryParser.Operator.OR;
	}

	/**
	 * Returns the effective default field based on the 'df' param or
	 * hardcoded schema default.  May be null if either exists specified.
	 * @see CommonParams#DF
	 * @see IndexSchema#getDefaultSearchFieldName
	 */
	public static String getDefaultField(final IndexSchema s, final String df) {
		return df != null ? df : s.getDefaultSearchFieldName();
	}

	// note to self: something needs to detect infinite recursion when parsing queries
	public static int parseLocalParams(String txt, int start, Map<String, String> target, 
			Params params) throws ErrorException {
		return parseLocalParams(txt, start, target, params, 
				LOCALPARAM_START, LOCALPARAM_END);
	}

	public static int parseLocalParams(String txt, int start, Map<String, String> target, 
			Params params, String startString, char endChar) throws ErrorException {
		int off = start;
		if (!txt.startsWith(startString, off)) 
			return start;
		
		StringParser p = new StringParser(txt, start, txt.length());
		p.increasePos(startString.length()); // skip over "{!"

		for (; ;) {
			//if (p.getPos() >= txt.length()) 
			//	throw new ParseException("Missing '}' parsing local params '" + txt + '"');
			
			char ch = p.peek();
			if (ch == endChar) 
				return p.getPos() + 1;

			String id = p.getId();
			if (id.length() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Expected ending character '" + endChar + "' parsing local params '" + txt + '"');
			}
			
			String val = null;

			ch = p.peek();
			if (ch != '=') {
				// single word... treat {!func} as type=func for easy lookup
				val = id;
				id = TYPE;
				
			} else {
				// saw equals, so read value
				p.increasePos(1);
				ch = p.peek();
				
				boolean deref = false;
				if (ch == '$') {
					p.increasePos(1);
					ch = p.peek();
					
					// dereference whatever value is read by treating it as a variable name
					deref = true; 
				}

				if (ch == '\"' || ch == '\'') {
					val = p.getQuotedString();
					
				} else {
					// read unquoted literal ended by whitespace or endChar (normally '}')
					// there is no escaping.
					int valStart = p.getPos();
					
					for (; ;) {
						if (p.getPos() >= p.getEnd()) {
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"Missing end to unquoted value starting at " + valStart + " str='" + txt + "'");
						}
						
						char c = p.getValue().charAt(p.getPos());
						if (c == endChar || Character.isWhitespace(c)) {
							val = p.getValue().substring(valStart, p.getPos());
							break;
						}
						
						p.increasePos(1);
					}
				}

				if (deref) {  // dereference parameter
					if (params != null) 
						val = params.get(val);
				}
			}
			
			if (target != null) 
				target.put(id, val);
		}
	}

	public static String encodeLocalParamVal(String val) {
		int len = val.length();
		int i = 0;
		
		if (len > 0 && val.charAt(0) != '$') {
			for (; i < len; i++) {
				char ch = val.charAt(i);
				if (Character.isWhitespace(ch) || ch=='}') 
					break;
			}
		}

		if (i >= len) return val;

		// We need to enclose in quotes... but now we need to escape
		StringBuilder sb = new StringBuilder(val.length() + 4);
		sb.append('\'');
		
		for (i=0; i < len; i++) {
			char ch = val.charAt(i);
			if (ch == '\'') 
				sb.append('\\');
      
			sb.append(ch);
		}
		
		sb.append('\'');
		return sb.toString();
	}
  
	/**
	 * "foo" returns null
	 * "{!prefix f=myfield}yes" returns type="prefix",f="myfield",v="yes"
	 * "{!prefix f=myfield v=$p}" returns type="prefix",f="myfield",v=params.get("p")
	 */
	public static Params getLocalParams(String txt, Params params) 
			throws ErrorException {
		if (txt == null || !txt.startsWith(LOCALPARAM_START)) 
			return null;
    
		Map<String, String> localParams = new HashMap<String, String>();
		int start = QueryParsing.parseLocalParams(txt, 0, localParams, params);

		String val = localParams.get(V);
		if (val == null) {
			val = txt.substring(start);
			localParams.put(V, val);
			
		} else {
			// localParams.put(VAL_EXPLICIT, "true");
		}
		
		return new MapParams(localParams);
	}

	/**
	 * Returns null if the sortSpec is the standard sort desc.
	 * <p/>
	 * <p>
	 * The form of the sort specification string currently parsed is:
	 * </p>
	 * <pre>
	 * SortSpec ::= SingleSort [, SingleSort]*
	 * SingleSort ::= &lt;fieldname&gt; SortDirection
	 * SortDirection ::= top | desc | bottom | asc
	 * </pre>
	 * Examples:
	 * <pre>
	 *   score desc               #normal sort by score (will return null)
	 *   weight bottom            #sort by weight ascending
	 *   weight desc              #sort by weight descending
	 *   height desc,weight desc  #sort by height descending, and use weight descending to break any ties
	 *   height desc,weight asc   #sort by height descending, using weight ascending as a tiebreaker
	 * </pre>
	 */
	public static ISort parseSort(String sortSpec, ISearchRequest req) 
			throws ErrorException {
		return parseSort(sortSpec, req, req.getSearchCore().getQueryFactory());
	}
	
	public static ISort parseSort(String sortSpec, ISearchRequest req, 
			QueryBuilderFactory factory) throws ErrorException {
		if (sortSpec == null || sortSpec.length() == 0) 
			return null;
		
		List<ISortField> lst = new ArrayList<ISortField>(4);

		try {
			StringParser sp = new StringParser(sortSpec);
			
			while (sp.getPos() < sp.getEnd()) {
				sp.eatws();

				final int start = sp.getPos();

				// short circuit test for a really simple field name
				String field = sp.getId(null);
				Exception qParserException = null;

				if (field == null || !Character.isWhitespace(sp.peekChar())) {
					// let's try it as a function instead
					field = null;
					String funcStr = sp.getValue().substring(start);

					QueryBuilder parser = factory.getQueryBuilder(funcStr, 
							FunctionQueryBuilderPlugin.NAME, req);
					IQuery q = null;
					
					try {
						if (parser instanceof FunctionQueryBuilder) {
							FunctionQueryBuilder fparser = (FunctionQueryBuilder)parser;
							fparser.setParseMultipleSources(false);
							fparser.setParseToEnd(false);
              
							q = fparser.getQuery();
              
							if (fparser.mLocalParams != null) {
								if (fparser.mValFollowedParams) {
									// need to find the end of the function query via the string parser
									int leftOver = fparser.getParser().getEnd() - fparser.getParser().getPos();
									// reset our parser to the same amount of leftover
									sp.setPos(sp.getEnd() - leftOver);
									
								} else {
									// the value was via the "v" param in localParams, so we need to find
									// the end of the local params themselves to pick up where we left off
									sp.setPos(start + fparser.mLocalParamsEnd);
								}
								
							} else {
								// need to find the end of the function query via the string parser
								int leftOver = fparser.getParser().getEnd() - fparser.getParser().getPos();
								// reset our parser to the same amount of leftover
								sp.setPos(sp.getEnd() - leftOver); 
							}
							
						} else {
							// A QParser that's not for function queries.
							// It must have been specified via local params.
							q = parser.getQuery();

							assert parser.getLocalParams() != null;
							sp.setPos(start + parser.mLocalParamsEnd);
						}

						Boolean top = sp.getSortDirection();
						if (top != null) {
							// we have a Query and a valid direction
							if (q instanceof FunctionQuery) 
								lst.add(((FunctionQuery)q).getValueSource().getSortField(top));
							else 
								lst.add((new QueryValueSource(q, 0.0f)).getSortField(top));
							
							continue;
						}
						
					} catch (IOException ioe) {
						throw ioe;
					} catch (Exception e) {
						// hang onto this in case the string isn't a full field name either
						qParserException = e;
					}
				}

				// if we made it here, we either have a "simple" field name,
				// or there was a problem parsing the string as a complex func/quer

				if (field == null) {
					// try again, simple rules for a field name with no whitespace
					sp.setPos(start);
					field = sp.getSimpleString();
				}
				
				Boolean top = sp.getSortDirection();
				if (top == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Can't determine a Sort Order (asc or desc) in sort spec " + sp);
				}
        
				if (SCORE.equals(field)) {
					if (top) 
						lst.add(AdvancedSortField.FIELD_SCORE);
					else 
						lst.add(new AdvancedSortField(null, SortField.Type.SCORE, true));
					
				} else if (DOCID.equals(field)) {
					lst.add(new AdvancedSortField(null, SortField.Type.DOC, top));
					
				} else {
					// try to find the field
					SchemaField sf = factory.getSearchCore().getSchema().getFieldOrNull(field);
					if (null == sf) {
						if (null != qParserException) {
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
									"sort param could not be parsed as a query, and is not a "+
									"field that exists in the index: " + field,
									qParserException);
						}
						
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
								"sort param field can't be found: " + field);
					}
					
					lst.add(sf.getSortField(top));
				}
			}

		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"error in sort: " + sortSpec, e);
		}

		// normalize a sort on score desc to null
		if (lst.size() == 1 && lst.get(0) == AdvancedSortField.FIELD_SCORE) 
			return null;

		return new Sort(lst.toArray(new SortField[lst.size()]));
	}

	static SchemaFieldType writeFieldName(String name, IndexSchema schema, 
			Appendable out, int flags) throws IOException {
		SchemaFieldType ft = schema.getFieldTypeNoEx(name);
		out.append(name);
		
		if (ft == null) 
			out.append("(UNKNOWN FIELD " + name + ')');
		
		out.append(':');
		return ft;
	}

	static void writeFieldVal(String val, SchemaFieldType ft, 
			Appendable out, int flags) throws IOException {
		if (ft != null) {
			try {
				out.append(ft.indexedToReadable(val));
			} catch (Exception e) {
				out.append("EXCEPTION(val=");
				out.append(val);
				out.append(")");
			}
		} else {
			out.append(val);
		}
	}

	static void writeFieldVal(BytesRef val, SchemaFieldType ft, 
			Appendable out, int flags) throws IOException {
		if (ft != null) {
			try {
				CharsRef readable = new CharsRef();
				ft.indexedToReadable(val, readable);
				out.append(readable);
			} catch (Exception e) {
				out.append("EXCEPTION(val=");
				out.append(val.utf8ToString());
				out.append(")");
			}
		} else {
			out.append(val.utf8ToString());
		}
	}

	/**
	 * @see #toString(Query,IndexSchema)
	 */
	@SuppressWarnings("unused")
	public static void toString(IQuery query, IndexSchema schema, 
			Appendable out, int flags) throws IOException {
		boolean writeBoost = true;

		if (query instanceof TermQuery) {
			TermQuery q = (TermQuery) query;
			ITerm t = q.getTerm();
			
			SchemaFieldType ft = writeFieldName(t.getField(), schema, out, flags);
			writeFieldVal(t.getBytes(), ft, out, flags);
			
		} else if (query instanceof TermRangeQuery) {
			TermRangeQuery q = (TermRangeQuery) query;
			String fname = q.getFieldName();
			
			SchemaFieldType ft = writeFieldName(fname, schema, out, flags);
			out.append(q.includesLower() ? '[' : '{');
			
			BytesRef lt = q.getLowerTerm();
			BytesRef ut = q.getUpperTerm();
			if (lt == null) 
				out.append('*');
			else 
				writeFieldVal(lt, ft, out, flags);
			
			out.append(" TO ");
			if (ut == null) 
				out.append('*');
			else 
				writeFieldVal(ut, ft, out, flags);
			
			out.append(q.includesUpper() ? ']' : '}');
			
		} else if (query instanceof NumericRangeQuery) {
			NumericRangeQuery<?> q = (NumericRangeQuery<?>) query;
			String fname = q.getFieldName();
			
			SchemaFieldType ft = writeFieldName(fname, schema, out, flags);
			out.append(q.includesMin() ? '[' : '{');
			
			Number lt = q.getMin();
			Number ut = q.getMax();
			if (lt == null) 
				out.append('*');
			else 
				out.append(lt.toString());
			
			out.append(" TO ");
			if (ut == null) 
				out.append('*');
			else 
				out.append(ut.toString());
			
			out.append(q.includesMax() ? ']' : '}');
			
		} else if (query instanceof BooleanQuery) {
			BooleanQuery q = (BooleanQuery) query;
			boolean needParens = false;

			if (q.getBoost() != 1.0 || q.getMinimumNumberShouldMatch() != 0 || 
				q.isCoordDisabled()) {
				needParens = true;
			}
			if (needParens) 
				out.append('(');
			
			boolean first = true;
			for (IBooleanClause c : q.clauses()) {
				if (!first) 
					out.append(' ');
				else 
					first = false;
				
				if (c.isProhibited()) 
					out.append('-');
				else if (c.isRequired()) 
					out.append('+');
				
				IQuery subQuery = (IQuery)c.getQuery();
				boolean wrapQuery = false;

				// TODO: may need to put parens around other types
				// of queries too, depending on future syntax.
				if (subQuery instanceof BooleanQuery) 
					wrapQuery = true;
        
				if (wrapQuery) 
					out.append('(');
        
				toString(subQuery, schema, out, flags);

				if (wrapQuery) 
					out.append(')');
			}

			if (needParens) 
				out.append(')');
      
			if (q.getMinimumNumberShouldMatch() > 0) {
				out.append('~');
				out.append(Integer.toString(q.getMinimumNumberShouldMatch()));
			}
			
			if (q.isCoordDisabled()) 
				out.append("/no_coord");
      
		} else if (query instanceof PrefixQuery) {
			PrefixQuery pq = (PrefixQuery) query;
			ITerm prefix = pq.getPrefix();
			
			SchemaFieldType ft = writeFieldName(prefix.getField(), schema, out, flags);
			out.append(prefix.getText());
			out.append('*');
			
		//} else if (query instanceof WildcardQuery) {
			//  out.append(query.toString());
			//  writeBoost = false;
			
		} else if (query instanceof FuzzyQuery) {
			out.append(query.toString());
			writeBoost = false;
			
		} else if (query instanceof ConstantScoreQuery) {
			out.append(query.toString());
			writeBoost = false;
			
		} else {
			out.append(query.getClass().getSimpleName() + '(' + query.toString() + ')');
			writeBoost = false;
		}

		if (writeBoost && query.getBoost() != 1.0f) {
			out.append("^");
			out.append(Float.toString(query.getBoost()));
		}
	}

	/**
	 * Formats a Query for debugging, using the IndexSchema to make
	 * complex field types readable.
	 * <p/>
	 * <p>
	 * The benefit of using this method instead of calling
	 * <code>Query.toString</code> directly is that it knows about the data
	 * types of each field, so any field which is encoded in a particularly
	 * complex way is still readable. The downside is that it only knows
	 * about built in Query types, and will not be able to format custom
	 * Query classes.
	 * </p>
	 */
	public static String toString(IQuery query, IndexSchema schema) {
		try {
			StringBuilder sb = new StringBuilder();
			toString(query, schema, sb, 0);
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds a list of String which are stringified versions of a list of Queries
	 */
	public static List<String> toString(List<IQuery> queries, IndexSchema schema) {
		List<String> out = new ArrayList<String>(queries.size());
		for (IQuery q : queries) {
			out.add(QueryParsing.toString(q, schema));
		}
		return out;
	}

}
