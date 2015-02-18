package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.javenstudio.common.indexdb.document.DoubleField;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.document.FloatField;
import org.javenstudio.common.indexdb.document.IntField;
import org.javenstudio.common.indexdb.document.LongField;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.NumericType;
import org.javenstudio.common.indexdb.util.NumericUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.DoubleFieldSource;
import org.javenstudio.hornet.query.source.FloatFieldSource;
import org.javenstudio.hornet.query.source.IntFieldSource;
import org.javenstudio.hornet.query.source.LongFieldSource;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.search.query.NumericRangeQuery;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.analysis.TrieTokenizerFactory;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.type.DateFieldType;
import org.javenstudio.falcon.search.schema.type.TrieDateFieldType;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;

/**
 * Provides field types to support for index's {@link
 * IntField}, {@link LongField}, {@link FloatField} and
 * {@link DoubleField}.
 * See {@link NumericRangeQuery} for more details.
 * It supports integer, float, long, double and date types.
 * <p/>
 * For each number being added to this field, multiple terms are generated as per 
 * the algorithm described in the above
 * link. The possible number of terms increases dramatically with lower precision steps. For
 * the fast range search to work, trie fields must be indexed.
 * <p/>
 * Trie fields are sortable in numerical order and can be used in function queries.
 * <p/>
 * Note that if you use a precisionStep of 32 for int/float and 64 for long/double/date, 
 * then multiple terms will not be
 * generated, range search will be no faster than any other number field, 
 * but sorting will still be possible.
 *
 * @see NumericRangeQuery
 * @since 1.4
 */
public class TrieFieldType extends PrimitiveFieldType {
	
	/**
	 * Used for handling date types following the same semantics as DateField
	 */
	static final DateFieldType sDateField = new DateFieldType();
	
	public static final int DEFAULT_PRECISION_STEP = 8;

	// the one passed in or defaulted
	protected int mPrecisionStepArg = TrieFieldType.DEFAULT_PRECISION_STEP;
	protected int mPrecisionStep; // normalized
	protected TrieTypes mType;
	protected Object mMissingValue;

	@Override
	public void init(IndexSchema schema, Map<String, String> args) 
			throws ErrorException {
		super.init(schema, args);
		
		String p = args.remove("precisionStep");
		if (p != null) 
			mPrecisionStepArg = Integer.parseInt(p);
		
		// normalize the precisionStep
		mPrecisionStep = mPrecisionStepArg;
		if (mPrecisionStep <= 0 || mPrecisionStep >= 64) 
			mPrecisionStep = Integer.MAX_VALUE;
		
		String t = args.remove("type");
		if (t != null) {
			try {
				mType = TrieTypes.valueOf(t.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"Invalid type specified in schema.xml for field: " + args.get("name"), e);
			}
		}
  
		CharFilterFactory[] filterFactories = new CharFilterFactory[0];
		TokenFilterFactory[] tokenFilterFactories = new TokenFilterFactory[0];
		
		mAnalyzer = new TokenizerChain(new TrieTokenizerFactory(mType, mPrecisionStep), 
				tokenFilterFactories, filterFactories);
		
		// for query time we only need one token, so we use the biggest possible precisionStep:
		mQueryAnalyzer = new TokenizerChain(new TrieTokenizerFactory(mType, Integer.MAX_VALUE), 
				tokenFilterFactories, filterFactories);
	}

	@Override
	public Object toObject(Fieldable f) throws ErrorException {
		final Number val = f.getNumericValue();
		if (val != null) 
			return (mType == TrieTypes.DATE) ? new Date(val.longValue()) : val;
			
		// the following code is "deprecated" and only to support pre-3.2 indexes 
		// using the old BinaryField encoding:
		final BytesRef bytes = f.getBinaryValue();
		if (bytes == null) 
			return toBadFieldString(f);
		
		switch (mType) {
		case INTEGER:
			return toInt(bytes.getBytes(), bytes.getOffset());
			
		case FLOAT:
			return Float.intBitsToFloat(toInt(bytes.getBytes(), bytes.getOffset()));
			
		case LONG:
			return toLong(bytes.getBytes(), bytes.getOffset());
			
		case DOUBLE:
			return Double.longBitsToDouble(toLong(bytes.getBytes(), bytes.getOffset()));
			
		case DATE:
			return new Date(toLong(bytes.getBytes(), bytes.getOffset()));
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + f.getName());
		}
	}

	@Override
	public SortField getSortField(SchemaField field, boolean top) throws ErrorException {
		field.checkSortability();

		Object missingValue = null;
		boolean sortMissingLast  = field.isSortMissingLast();
		boolean sortMissingFirst = field.isSortMissingFirst();
    
		switch (mType) {
		case INTEGER:
			if (sortMissingLast) 
				missingValue = top ? Integer.MIN_VALUE : Integer.MAX_VALUE;
			else if(sortMissingFirst) 
				missingValue = top ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			
			return new AdvancedSortField(field.getName(), 
					FieldCache.NUMERIC_UTILS_INT_PARSER, top).setMissingValue(missingValue);
      
		case FLOAT:
			if (sortMissingLast) 
				missingValue = top ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
			else if (sortMissingFirst) 
				missingValue = top ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
			
			return new AdvancedSortField( field.getName(), 
					FieldCache.NUMERIC_UTILS_FLOAT_PARSER, top).setMissingValue(missingValue);
      
		case DATE: // fallthrough
		case LONG:
			if (sortMissingLast) 
				missingValue = top ? Long.MIN_VALUE : Long.MAX_VALUE;
			else if (sortMissingFirst) 
				missingValue = top ? Long.MAX_VALUE : Long.MIN_VALUE;
			
			return new AdvancedSortField( field.getName(), 
					FieldCache.NUMERIC_UTILS_LONG_PARSER, top).setMissingValue(missingValue);
        
		case DOUBLE:
			if (sortMissingLast) 
				missingValue = top ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			else if (sortMissingFirst) 
				missingValue = top ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
			
			return new AdvancedSortField( field.getName(), 
					FieldCache.NUMERIC_UTILS_DOUBLE_PARSER, top).setMissingValue(missingValue);
        
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + field.getName());
		}
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder qparser) throws ErrorException {
		field.checkFieldCacheSource(qparser);
		
		switch (mType) {
		case INTEGER:
			return new IntFieldSource(field.getName(), FieldCache.NUMERIC_UTILS_INT_PARSER);
			
		case FLOAT:
			return new FloatFieldSource(field.getName(), FieldCache.NUMERIC_UTILS_FLOAT_PARSER);
			
		case DATE:
			return new TrieDateFieldSource(field.getName(), FieldCache.NUMERIC_UTILS_LONG_PARSER);
			
		case LONG:
			return new LongFieldSource(field.getName(), FieldCache.NUMERIC_UTILS_LONG_PARSER);
			
		case DOUBLE:
			return new DoubleFieldSource(field.getName(), FieldCache.NUMERIC_UTILS_DOUBLE_PARSER);
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + field.getName());
		}
	}
	
	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeVal(name, toObject(f));
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public boolean isTokenized() {
		return true;
	}

	@Override
	public boolean isMultiValuedFieldCache() {
		return false;
	}

	/**
	 * @return the precisionStep used to index values into the field
	 */
	public int getPrecisionStep() {
		return mPrecisionStepArg;
	}

	/**
	 * @return the type of this field
	 */
	public TrieTypes getType() {
		return mType;
	}

	@Override
	public Query getRangeQuery(QueryBuilder parser, SchemaField field, String min, String max, 
			boolean minInclusive, boolean maxInclusive) throws ErrorException {
		int ps = mPrecisionStep;
		Query query = null;
		
		switch (mType) {
		case INTEGER:
			query = NumericRangeQuery.newIntRange(field.getName(), ps,
					min == null ? null : Integer.parseInt(min),
					max == null ? null : Integer.parseInt(max),
					minInclusive, maxInclusive);
			break;
			
		case FLOAT:
			query = NumericRangeQuery.newFloatRange(field.getName(), ps,
					min == null ? null : Float.parseFloat(min),
					max == null ? null : Float.parseFloat(max),
					minInclusive, maxInclusive);
			break;
			
		case LONG:
			query = NumericRangeQuery.newLongRange(field.getName(), ps,
					min == null ? null : Long.parseLong(min),
					max == null ? null : Long.parseLong(max),
					minInclusive, maxInclusive);
			break;
			
		case DOUBLE:
			query = NumericRangeQuery.newDoubleRange(field.getName(), ps,
					min == null ? null : Double.parseDouble(min),
					max == null ? null : Double.parseDouble(max),
					minInclusive, maxInclusive);
			break;
			
		case DATE:
			query = NumericRangeQuery.newLongRange(field.getName(), ps,
					min == null ? null : sDateField.parseMath(null, min).getTime(),
					max == null ? null : sDateField.parseMath(null, max).getTime(),
					minInclusive, maxInclusive);
			break;
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field");
		}

		return query;
	}

	@Deprecated
	public static int toInt(byte[] arr, int offset) {
		return (arr[offset]<<24) | ((arr[offset+1]&0xff)<<16) | ((arr[offset+2]&0xff)<<8) | (arr[offset+3]&0xff);
	}
  
	@Deprecated
	public static long toLong(byte[] arr, int offset) {
		int high = (arr[offset]<<24) | ((arr[offset+1]&0xff)<<16) | ((arr[offset+2]&0xff)<<8) | (arr[offset+3]&0xff);
		int low = (arr[offset+4]<<24) | ((arr[offset+5]&0xff)<<16) | ((arr[offset+6]&0xff)<<8) | (arr[offset+7]&0xff);
		return (((long)high)<<32) | (low&0x0ffffffffL);
	}

	@Override
	public String storedToReadable(Fieldable f) throws ErrorException {
		return toExternal(f);
	}

	@Override
	public String readableToIndexed(String val) throws ErrorException {
		// TODO: Numeric should never be handled as String, that may break in future indexdb versions! 
		// Change to use BytesRef for term texts!
		final BytesRef bytes = new BytesRef(NumericUtil.BUF_SIZE_LONG);
		readableToIndexed(val, bytes);
		return bytes.utf8ToString();
	}

	@Override
	public void readableToIndexed(CharSequence val, BytesRef result) throws ErrorException {
		String s = val.toString();
		
		switch (mType) {
		case INTEGER:
			NumericUtil.intToPrefixCoded(Integer.parseInt(s), 
					0, result);
			break;
			
		case FLOAT:
			NumericUtil.intToPrefixCoded(NumericUtil.floatToSortableInt(Float.parseFloat(s)), 
					0, result);
			break;
			
		case LONG:
			NumericUtil.longToPrefixCoded(Long.parseLong(s), 0, result);
			break;
			
		case DOUBLE:
			NumericUtil.longToPrefixCoded(NumericUtil.doubleToSortableLong(Double.parseDouble(s)), 
					0, result);
			break;
			
		case DATE:
			NumericUtil.longToPrefixCoded(sDateField.parseMath(null, s).getTime(), 
					0, result);
			break;
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}
	}

	@Override
	public String toInternal(String val) throws ErrorException {
		return readableToIndexed(val);
	}

	static String toBadFieldString(Fieldable f) {
		String s = f.getStringValue();
		return "ERROR:SCHEMA-INDEX-MISMATCH,stringValue="+s;
	}

	@Override
	public String toExternal(Fieldable f) throws ErrorException {
		return (mType == TrieTypes.DATE) ? sDateField.toExternal((Date) toObject(f)) : 
			toObject(f).toString();
	}

	@Override
	public String indexedToReadable(String indexedForm) throws ErrorException {
		final BytesRef formBytes = new BytesRef(indexedForm);
		switch (mType) {
		case INTEGER:
			return Integer.toString(NumericUtil.prefixCodedToInt(formBytes));
			
		case FLOAT:
			return Float.toString(NumericUtil.sortableIntToFloat(
					NumericUtil.prefixCodedToInt(formBytes)));
			
		case LONG:
			return Long.toString(NumericUtil.prefixCodedToLong(formBytes));
			
		case DOUBLE:
			return Double.toString(NumericUtil.sortableLongToDouble(
					NumericUtil.prefixCodedToLong(formBytes)));
			
		case DATE:
			return sDateField.toExternal(new Date(NumericUtil.prefixCodedToLong(formBytes)));
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}
	}

	@Override
	public CharsRef indexedToReadable(BytesRef indexedForm, CharsRef charsRef) 
			throws ErrorException {
		final String value;
		switch (mType) {
		case INTEGER:
			value = Integer.toString(NumericUtil.prefixCodedToInt(indexedForm));
			break;
			
		case FLOAT:
			value = Float.toString(NumericUtil.sortableIntToFloat(
					NumericUtil.prefixCodedToInt(indexedForm)));
			break;
			
		case LONG:
			value = Long.toString(NumericUtil.prefixCodedToLong(indexedForm) );
			break;
			
		case DOUBLE:
			value = Double.toString(NumericUtil.sortableLongToDouble(
					NumericUtil.prefixCodedToLong(indexedForm)));
			break;
			
		case DATE:
			value = sDateField.toExternal(new Date(NumericUtil.prefixCodedToLong(indexedForm)));
			break;
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}
		
		charsRef.grow(value.length());
		charsRef.mLength = value.length();
		value.getChars(0, charsRef.mLength, charsRef.mChars, 0);
		
		return charsRef;
	}

	@Override
	public Object toObject(SchemaField sf, BytesRef term) throws ErrorException {
		switch (mType) {
		case INTEGER:
			return NumericUtil.prefixCodedToInt(term);
		case FLOAT:
			return NumericUtil.sortableIntToFloat(NumericUtil.prefixCodedToInt(term));
		case LONG:
			return NumericUtil.prefixCodedToLong(term);
		case DOUBLE:
			return NumericUtil.sortableLongToDouble(NumericUtil.prefixCodedToLong(term));
		case DATE:
			return new Date(NumericUtil.prefixCodedToLong(term));
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}
	}

	@Override
	public String storedToIndexed(Fieldable f) throws ErrorException {
		final BytesRef bytes = new BytesRef(NumericUtil.BUF_SIZE_LONG);
		final Number val = f.getNumericValue();
		
		if (val != null) {
			switch (mType) {
			case INTEGER:
				NumericUtil.intToPrefixCoded(val.intValue(), 0, bytes);
				break;
			case FLOAT:
				NumericUtil.intToPrefixCoded(NumericUtil.floatToSortableInt(val.floatValue()), 
						0, bytes);
				break;
			case LONG: //fallthrough!
			case DATE:
				NumericUtil.longToPrefixCoded(val.longValue(), 0, bytes);
				break;
			case DOUBLE:
				NumericUtil.longToPrefixCoded(NumericUtil.doubleToSortableLong(val.doubleValue()), 
						0, bytes);
				break;
			default:
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown type for trie field: " + f.getName());
			}
		} else {
			// the following code is "deprecated" and only to support pre-3.2 indexes
			// using the old BinaryField encoding:
			final BytesRef bytesRef = f.getBinaryValue();
			
			if (bytesRef == null) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Invalid field contents: " + f.getName());
			}
			
			switch (mType) {
			case INTEGER:
				NumericUtil.intToPrefixCoded(toInt(bytesRef.getBytes(), bytesRef.getOffset()), 
						0, bytes);
				break;
				
			case FLOAT: {
				// WARNING: Code Duplication! Keep in sync with o.a.l.util.NumericUtil!
				// copied from NumericUtil to not convert to/from float two times
				// code in next 2 lines is identical to: 
				// int v = NumericUtil.floatToSortableInt(Float.intBitsToFloat(toInt(arr)));
				int v = toInt(bytesRef.getBytes(), bytesRef.getOffset());
				if (v < 0) v ^= 0x7fffffff;
				NumericUtil.intToPrefixCoded(v, 0, bytes);
				break;
			}
			
			case LONG: //fallthrough!
			case DATE:
				NumericUtil.longToPrefixCoded(toLong(bytesRef.getBytes(), bytesRef.getOffset()), 
						0, bytes);
				break;
				
			case DOUBLE: {
				// WARNING: Code Duplication! Keep in sync with o.a.l.util.NumericUtil!
				// copied from NumericUtil to not convert to/from double two times
				// code in next 2 lines is identical to: 
				// long v = NumericUtil.doubleToSortableLong(Double.longBitsToDouble(toLong(arr)));
				long v = toLong(bytesRef.getBytes(), bytesRef.getOffset());
				if (v < 0) v ^= 0x7fffffffffffffffL;
				NumericUtil.longToPrefixCoded(v, 0, bytes);
				break;
			}
			
			default:
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown type for trie field: " + f.getName());
			}
		}
		
		return bytes.utf8ToString();
	}
  
	@Override
	public Fieldable createField(SchemaField field, Object value, float boost) 
			throws ErrorException {
		boolean indexed = field.isIndexed();
		boolean stored = field.isStored();

		if (!indexed && !stored) {
			if (LOG.isWarnEnabled())
				LOG.warn("Ignoring unindexed/unstored field: " + field);
			
			return null;
		}
    
		FieldType ft = new FieldType();
		ft.setStored(stored);
		ft.setTokenized(true);
		ft.setIndexed(indexed);
		ft.setOmitNorms(field.isOmitNorms());
		ft.setIndexOptions(getIndexOptions(field, value.toString()));

		switch (mType) {
		case INTEGER:
			ft.setNumericType(NumericType.INT);
			break;
		case FLOAT:
			ft.setNumericType(NumericType.FLOAT);
			break;
		case LONG:
			ft.setNumericType(NumericType.LONG);
			break;
		case DOUBLE:
			ft.setNumericType(NumericType.DOUBLE);
			break;
		case DATE:
			ft.setNumericType(NumericType.LONG);
			break;
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}
		
		ft.setNumericPrecisionStep(mPrecisionStep);

		final Fieldable f;
		switch (mType) {
		case INTEGER:
			int i = (value instanceof Number) ? ((Number)value).intValue() : 
				Integer.parseInt(value.toString());
			f = new IntField(field.getName(), i, ft);
			break;
			
		case FLOAT:
			float fl = (value instanceof Number) ? ((Number)value).floatValue() : 
				Float.parseFloat(value.toString());
			f = new FloatField(field.getName(), fl, ft);
			break;
			
		case LONG:
			long l = (value instanceof Number) ? ((Number)value).longValue() : 
				Long.parseLong(value.toString());
			f = new LongField(field.getName(), l, ft);
			break;
			
		case DOUBLE:
			double d = (value instanceof Number) ? ((Number)value).doubleValue() : 
				Double.parseDouble(value.toString());
			f = new DoubleField(field.getName(), d, ft);
			break;
			
		case DATE:
			Date date = (value instanceof Date) ? ((Date)value) : 
				sDateField.parseMath(null, value.toString());
			f = new LongField(field.getName(), date.getTime(), ft);
			break;
			
		default:
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown type for trie field: " + mType);
		}

		f.setBoost(boost);
		
		return f;
	}

	static final String INT_PREFIX = new String(new char[]{NumericUtil.SHIFT_START_INT});
	static final String LONG_PREFIX = new String(new char[]{NumericUtil.SHIFT_START_LONG});

	/** 
	 * expert internal use, subject to change.
	 * Returns null if no prefix or prefix not needed, or the prefix of the main value of a trie field
	 * that indexes multiple precisions per value.
	 */
	public static String getMainValuePrefix(SchemaFieldType ft) throws ErrorException {
		if (ft instanceof TrieDateFieldType)
			ft = ((TrieDateFieldType) ft).getWrappedField();
		
		if (ft instanceof TrieFieldType) {
			final TrieFieldType trie = (TrieFieldType)ft;
			if (trie.mPrecisionStep  == Integer.MAX_VALUE)
				return null;
			
			switch (trie.mType) {
			case INTEGER:
			case FLOAT:
				return INT_PREFIX;
			case LONG:
			case DOUBLE:
			case DATE:
				return LONG_PREFIX;
			default:
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Unknown type for trie field: " + trie.mType);
			}
		}
		
		return null;
	}
	
}

