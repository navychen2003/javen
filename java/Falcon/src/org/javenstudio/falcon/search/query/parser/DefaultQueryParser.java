package org.javenstudio.falcon.search.query.parser;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TextFieldHelper;
import org.javenstudio.falcon.search.schema.type.TextFieldType;
import org.javenstudio.panda.analysis.ReversedWildcardFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.query.ParseException;
import org.javenstudio.panda.query.QueryParser;

/**
 * A variation on the QueryParser which knows about the field 
 * types and query time analyzers configured in Solr's schema.xml.
 *
 * <p>
 * This class also deviates from the QueryParser by using 
 * ConstantScore versions of RangeQuery and PrefixQuery to prevent 
 * TooManyClauses exceptions.
 * </p> 
 *
 * <p>
 * If the magic field name "<code>_val_</code>" is used in a term or 
 * phrase query, the value is parsed as a function.
 * </p>
 */
public class DefaultQueryParser extends QueryParser {
	static Logger LOG = Logger.getLogger(DefaultQueryParser.class);

	/** 
	 * Identifies the list of all known "magic fields" that trigger 
	 * special parsing behavior
	 */
	public static enum MagicFieldName {
		VAL("_val_", "func"), QUERY("_query_", null);
    
		private final String mField;
		private final String mSubParser;
		
		MagicFieldName(final String field, final String subParser) {
			mField = field;
			mSubParser = subParser;
		}
		
		public final String getFieldName() { return mField; }
		public final String getSubParser() { return mSubParser; }
		
		@Override
		public String toString() {
			return mField;
		}
		
		static final Map<String,MagicFieldName> sLookup = new HashMap<String,MagicFieldName>();
		static {
			for (MagicFieldName s : EnumSet.allOf(MagicFieldName.class)) {
				sLookup.put(s.toString(), s);
			}
		}
		
		public static MagicFieldName get(final String field) {
			return sLookup.get(field);
		}
	}

	// implementation detail - caching ReversedWildcardFilterFactory based on type
	private Map<SchemaFieldType, ReversedWildcardFilterFactory> mLeadingWildcards;

	private final IndexSchema mSchema;
	private final QueryBuilder mBuilder;
	private final String mDefaultField;
  
	public DefaultQueryParser(QueryBuilder builder, String defaultField) {
		this(builder, defaultField, 
				builder.getSearchCore().getSchema().getQueryAnalyzer());
	}

	public DefaultQueryParser(QueryBuilder builder, String defaultField, IAnalyzer analyzer) {
		super(defaultField, analyzer);
		
		mSchema = builder.getSearchCore().getSchema();
		mBuilder = builder;
		mDefaultField = defaultField;
		
		setEnablePositionIncrements(true);
		setLowercaseExpandedTerms(false);
		setAllowLeadingWildcard(true);
	}

	public final IndexSchema getSchema() { return mSchema; }
	
	protected ReversedWildcardFilterFactory getReversedWildcardFilterFactory(
			SchemaFieldType fieldType) {
		if (mLeadingWildcards == null) 
			mLeadingWildcards = new HashMap<SchemaFieldType, ReversedWildcardFilterFactory>();
		
		ReversedWildcardFilterFactory fac = mLeadingWildcards.get(fieldType);
		if (fac == null && mLeadingWildcards.containsKey(fac)) 
			return fac;

		IAnalyzer a = fieldType.getAnalyzer();
		if (a instanceof TokenizerChain) {
			// examine the indexing analysis chain if it supports leading wildcards
			TokenizerChain tc = (TokenizerChain)a;
			TokenFilterFactory[] factories = tc.getTokenFilterFactories();
			
			for (TokenFilterFactory factory : factories) {
				if (factory instanceof ReversedWildcardFilterFactory) {
					fac = (ReversedWildcardFilterFactory)factory;
					break;
				}
			}
		}

		mLeadingWildcards.put(fieldType, fac);
		return fac;
	}

	private void checkNullField(String field) throws ParseException {
		if (field == null && mDefaultField == null) {
			throw new ParseException("no field name specified in query " 
					+ "and no default specified via 'df' param");
		}
	}

	protected String analyzeIfMultitermTermText(String field, String part, 
			SchemaFieldType fieldType) throws ParseException {
		if (part == null) return part;

		try {
			SchemaField sf = mSchema.getFieldOrNull((field));
			if (sf == null || !(fieldType instanceof TextFieldType)) 
				return part;
			
			IAnalyzer analyzer = ((TextFieldType)fieldType).getMultiTermAnalyzer();
			if (analyzer == null) 
				return part;
			
			String out = TextFieldHelper.analyzeMultiTerm(field, part, 
					analyzer).utf8ToString();
			
			return out;
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
	}

	@Override
	protected IQuery getFieldQuery(String field, String queryText, 
			boolean quoted) throws ParseException {
		checkNullField(field);
		// intercept magic field name of "_" to use as a hook for our
		// own functions.
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("getFieldQuery: field=" + field + " queryText=" + queryText + " quoted=" + quoted);
		
		try {
			if (field.charAt(0) == '_' && mBuilder != null) {
				MagicFieldName magic = MagicFieldName.get(field);
				if (magic != null) {
					QueryBuilder nested = mBuilder.subQuery(queryText, magic.getSubParser());
					return nested.getQuery();
				} 
			}
			
			SchemaField sf = mSchema.getFieldOrNull(field);
			if (sf != null) {
				SchemaFieldType ft = sf.getType();
				// delegate to type for everything except tokenized fields
				if (ft.isTokenized()) {
					return super.getFieldQuery(field, queryText, 
							quoted || (ft instanceof TextFieldType && 
									((TextFieldType)ft).getAutoGeneratePhraseQueries()));
					
				} else {
					return sf.getType().getFieldQuery(mBuilder, sf, queryText);
				}
			}
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
		
		// default to a normal field query
		return super.getFieldQuery(field, queryText, quoted);
	}

	@Override
	protected IQuery getRangeQuery(String field, String part1, String part2, 
			boolean startInclusive, boolean endInclusive) throws ParseException {
		checkNullField(field);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getFieldQuery: field=" + field + " part1=" + part1 + " part2=" + part2 
					+ " startInclusive=" + startInclusive + " endInclusive=" + endInclusive);
		}
		
		try {
			SchemaField sf = mSchema.getField(field);
			return sf.getType().getRangeQuery(mBuilder, sf, part1, part2, 
					startInclusive, endInclusive);
			
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
	}

	@Override
	protected IQuery getPrefixQuery(String field, String termStr) 
			throws ParseException {
		checkNullField(field);

		if (LOG.isDebugEnabled()) 
			LOG.debug("getPrefixQuery: field=" + field + " termStr=" + termStr);
		
		try {
			termStr = analyzeIfMultitermTermText(field, termStr, 
					mSchema.getFieldType(field));
	
			// has always used constant scoring for prefix queries. 
			// This should return constant scoring by default.
			return newPrefixQuery(new Term(field, termStr));
			
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
	}
	
	@Override
	protected IQuery getWildcardQuery(String field, String termStr) 
			throws ParseException {
		checkNullField(field);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("getWildcardQuery: field=" + field + " termStr=" + termStr);
		
		// *:* -> MatchAllDocsQuery
		if ("*".equals(field) && "*".equals(termStr)) 
			return newMatchAllDocsQuery();
		
		SchemaFieldType fieldType = null;
		ReversedWildcardFilterFactory factory = null;
		
		try {
			fieldType = mSchema.getFieldType(field);
			termStr = analyzeIfMultitermTermText(field, termStr, fieldType);
			
			// can we use reversed wildcards in this field?
			factory = getReversedWildcardFilterFactory(fieldType);
			
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
		
		if (factory != null) {
			/*
			Term term = new Term(field, termStr);
			// fsa representing the query
			Automaton automaton = WildcardQuery.toAutomaton(term);
			
			// TODO: we should likely use the automaton to calculate shouldReverse, too.
			if (factory.shouldReverse(termStr)) {
				automaton = BasicOperations.concatenate(automaton, 
						BasicAutomata.makeChar(factory.getMarkerChar()));
				SpecialOperations.reverse(automaton);
				
			} else { 
				// reverse wildcardfilter is active: remove false positives
				// fsa representing false positives (markerChar*)
				Automaton falsePositives = BasicOperations.concatenate(
						BasicAutomata.makeChar(factory.getMarkerChar()), 
						BasicAutomata.makeAnyString());
				// subtract these away
				automaton = BasicOperations.minus(automaton, falsePositives);
			}
			
			return new AutomatonQuery(term, automaton) {
				// override toString so its completely transparent
				@Override
				public String toString(String field) {
					StringBuilder buffer = new StringBuilder();
					if (!getField().equals(field)) {
						buffer.append(getField());
						buffer.append(":");
					}
					buffer.append(term.text());
					buffer.append(ToStringUtils.boost(getBoost()));
					return buffer.toString();
				}
			};
			*/
		}

		// has always used constant scoring for wildcard queries.
		// This should return constant scoring by default.
		return newWildcardQuery(new Term(field, termStr));
	}

	@Override
	protected IQuery getRegexpQuery(String field, String termStr) throws ParseException {
		checkNullField(field);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("getRegexpQuery: field=" + field + " termStr=" + termStr);
		
		try {
			termStr = analyzeIfMultitermTermText(field, termStr, 
					mSchema.getFieldType(field));
			
		} catch (ErrorException ex) { 
			throw new ParseException(ex.toString(), ex);
		}
		
		return newRegexpQuery(new Term(field, termStr));
	}
	
}
