package org.javenstudio.falcon.search.schema.type;

import java.util.Map;
import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TextFieldHelper;

/** 
 * <code>TextField</code> is the basic type for configurable text analysis.
 * Analyzers for field types using this implementation should be defined in the schema.
 *
 */
public class TextFieldType extends SchemaFieldType {
	
	protected boolean mAutoGeneratePhraseQueries;

	/**
	 * Analyzer set by schema for text types to use when searching fields
	 * of this type, subclasses can set analyzer themselves or override
	 * getAnalyzer()
	 * This analyzer is used to process wildcard, prefix, regex and other multiterm queries. It
	 * assembles a list of tokenizer +filters that "make sense" for this, primarily accent folding and
	 * lowercasing filters, and charfilters.
	 *
	 * @see #getMultiTermAnalyzer
	 * @see #setMultiTermAnalyzer
	 */
	protected IAnalyzer mMultiTermAnalyzer = null;

	@Override
	public void init(IndexSchema schema, Map<String,String> args) throws ErrorException {
		mProperties |= TOKENIZED;
		if (schema.getVersion() > 1.1f) 
			mProperties &= ~OMIT_TF_POSITIONS;
		
		if (schema.getVersion() > 1.3f) 
			mAutoGeneratePhraseQueries = false;
		else 
			mAutoGeneratePhraseQueries = true;
		
		String autoGeneratePhraseQueriesStr = args.remove("autoGeneratePhraseQueries");
		if (autoGeneratePhraseQueriesStr != null)
			mAutoGeneratePhraseQueries = Boolean.parseBoolean(autoGeneratePhraseQueriesStr);
		
		super.init(schema, args);    
	}

	/**
	 * Returns the Analyzer to be used when searching fields of this type when mult-term queries are specified.
	 * <p>
	 * This method may be called many times, at any time.
	 * </p>
	 * @see #getAnalyzer
	 */
	public IAnalyzer getMultiTermAnalyzer() {
		return mMultiTermAnalyzer;
	}

	public void setMultiTermAnalyzer(IAnalyzer analyzer) {
		mMultiTermAnalyzer = analyzer;
		
		if (analyzer == null) 
			throw new NullPointerException("Analyzer input null");
	}

	public boolean getAutoGeneratePhraseQueries() {
		return mAutoGeneratePhraseQueries;
	}

	@Override
	public ISortField getSortField(SchemaField field, boolean reverse) 
			throws ErrorException {
		// :TODO: maybe warn if isTokenized(), 
		// but doesn't use LimitTokenCountFilter in it's chain? 
		return getStringSort(field, reverse);
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeString(name, f.getStringValue(), true);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public IQuery getFieldQuery(QueryBuilder parser, SchemaField field, 
			String externalVal) throws ErrorException {
		return TextFieldHelper.parseFieldQuery(parser, getQueryAnalyzer(), 
				field.getName(), externalVal);
	}

	@Override
	public Object toObject(SchemaField sf, BytesRef term) {
		return term.utf8ToString();
	}

	@Override
	public void setAnalyzer(IAnalyzer analyzer) {
		mAnalyzer = analyzer;
	}

	@Override
	public void setQueryAnalyzer(IAnalyzer analyzer) {
		mQueryAnalyzer = analyzer;
	}

	@Override
	public IQuery getRangeQuery(QueryBuilder parser, SchemaField field, String part1, String part2, 
			boolean minInclusive, boolean maxInclusive) throws ErrorException {
		IAnalyzer multiAnalyzer = getMultiTermAnalyzer();
		BytesRef lower = TextFieldHelper.analyzeMultiTerm(field.getName(), part1, multiAnalyzer);
		BytesRef upper = TextFieldHelper.analyzeMultiTerm(field.getName(), part2, multiAnalyzer);
		return new TermRangeQuery(field.getName(), lower, upper, minInclusive, maxInclusive);
	}

}
