package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.LongDocValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.ValueSourceScorer;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueLong;

/**
 * Obtains float field values from the {@link FieldCache}
 * using <code>getFloats()</code>
 * and makes those values available as other numeric types, casting as needed.
 *
 */
public class LongFieldSource extends FieldCacheSource {

	protected final ISortField.LongParser mParser;

	public LongFieldSource(String field) {
		this(field, null);
	}

	public LongFieldSource(String field, ISortField.LongParser parser) {
		super(field);
		mParser = parser;
	}

	@Override
	public String getDescription() {
		return "long(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final long[] arr = mCache.getLongs(readerContext.getReader(), mField, mParser, true);
		final Bits valid = mCache.getDocsWithField(readerContext.getReader(), mField);
    
		return new LongDocValues(this) {
			@Override
			public long longVal(int doc) {
				return arr[doc];
			}

			@Override
			public boolean exists(int doc) {
				return valid.get(doc);
			}

			@Override
			public Object objectVal(int doc) {
				return valid.get(doc) ? longToObject(arr[doc]) : null;
			}

			@Override
			public ValueSourceScorer getRangeScorer(IIndexReader reader, 
					String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
				long lower,upper;
				// instead of using separate comparison functions, adjust the endpoints.

				if (lowerVal == null) {
					lower = Long.MIN_VALUE;
				} else {
					lower = externalToLong(lowerVal);
					if (!includeLower && lower < Long.MAX_VALUE) 
						lower++;
				}

				if (upperVal==null) {
					upper = Long.MAX_VALUE;
				} else {
					upper = externalToLong(upperVal);
					if (!includeUpper && upper > Long.MIN_VALUE) 
						upper--;
				}

				final long ll = lower;
				final long uu = upper;

				return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						long val = arr[doc];
						// only check for deleted if it's the default value
						// if (val==0 && reader.isDeleted(doc)) return false;
						return val >= ll && val <= uu;
					}
				};
			}

			@Override
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final long[] longArr = arr;
					private final MutableValueLong mval = newMutableValueLong();

					@Override
					public MutableValue getValue() {
						return mval;
					}

					@Override
					public void fillValue(int doc) {
						mval.set(longArr[doc]);
						mval.setExists(valid.get(doc));
					}
				};
			}
		};
	}

	protected MutableValueLong newMutableValueLong() {
		return new MutableValueLong();  
	}

	public long externalToLong(String extVal) {
		return Long.parseLong(extVal);
	}

	public Object longToObject(long val) {
		return val;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		LongFieldSource other = (LongFieldSource) o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? this.getClass().hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
