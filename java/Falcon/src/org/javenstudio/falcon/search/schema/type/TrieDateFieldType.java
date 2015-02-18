package org.javenstudio.falcon.search.schema.type;

import java.util.Map;
import java.util.Date;

import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.NumericRangeQuery;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.TrieTypes;

public class TrieDateFieldType extends DateFieldType {

	protected final TrieFieldType mWrappedField = new TrieFieldType() {{
			mType = TrieTypes.DATE;
		}};

	public final TrieFieldType getWrappedField() { 
		return mWrappedField;
	}
	
	@Override
	public void init(IndexSchema schema, Map<String, String> args) 
			throws ErrorException {
		mWrappedField.init(schema, args);
		mAnalyzer = mWrappedField.getAnalyzer();
		mQueryAnalyzer = mWrappedField.getQueryAnalyzer();
	}

	@Override
	public Date toObject(Fieldable f) throws ErrorException {
		return (Date) mWrappedField.toObject(f);
	}

	@Override
	public Object toObject(SchemaField sf, BytesRef term) 
			throws ErrorException{
		return mWrappedField.toObject(sf, term);
	}

	@Override
	public SortField getSortField(SchemaField field, boolean top) 
			throws ErrorException {
		return mWrappedField.getSortField(field, top);
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		return mWrappedField.getValueSource(field, parser);
	}
	
	/**
	 * @return the precisionStep used to index values into the field
	 */
	public int getPrecisionStep() {
		return mWrappedField.getPrecisionStep();
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) 
			throws ErrorException {
		mWrappedField.write(writer, name, f);
	}

	@Override
	public boolean isTokenized() {
		return mWrappedField.isTokenized();
	}

	@Override
	public boolean isMultiValuedFieldCache() {
		return mWrappedField.isMultiValuedFieldCache();
	}

	@Override
	public String storedToReadable(Fieldable f) throws ErrorException {
		return mWrappedField.storedToReadable(f);
	}

	@Override
	public String readableToIndexed(String val) throws ErrorException {  
		return mWrappedField.readableToIndexed(val);
	}

	@Override
	public String toInternal(String val) throws ErrorException {
		return mWrappedField.toInternal(val);
	}

	@Override
	public String toExternal(Fieldable f) throws ErrorException {
		return mWrappedField.toExternal(f);
	}

	@Override
	public String indexedToReadable(String indexedForm) throws ErrorException {
		return mWrappedField.indexedToReadable(indexedForm);
	}
	
	@Override
	public CharsRef indexedToReadable(BytesRef input, CharsRef charsRef)
			throws ErrorException {
		// TODO: this could be more efficient, but the sortable types 
		// should be deprecated instead
		return mWrappedField.indexedToReadable(input, charsRef);
	}

	@Override
	public String storedToIndexed(Fieldable f) throws ErrorException {
		return mWrappedField.storedToIndexed(f);
	}

	@Override
	public Fieldable createField(SchemaField field, Object value, float boost) 
			throws ErrorException {
		return mWrappedField.createField(field, value, boost);
	}

	@Override
	public Query getRangeQuery(QueryBuilder parser, SchemaField field, String min, String max, 
			boolean minInclusive, boolean maxInclusive) throws ErrorException {
		return mWrappedField.getRangeQuery(parser, field, min, max, minInclusive, maxInclusive);
	}
  
	@Override
	public Query getRangeQuery(QueryBuilder parser, SchemaField sf, Date min, Date max, 
			boolean minInclusive, boolean maxInclusive) {
		return NumericRangeQuery.newLongRange(sf.getName(), mWrappedField.getPrecisionStep(),
				min == null ? null : min.getTime(),
				max == null ? null : max.getTime(),
				minInclusive, maxInclusive);
	}
	
}
