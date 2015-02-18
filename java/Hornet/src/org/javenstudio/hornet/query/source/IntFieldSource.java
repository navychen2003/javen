package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.IntDocValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.ValueSourceScorer;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueInt;

/**
 * Obtains int field values from the {@link FieldCache}
 * using <code>getInts()</code>
 * and makes those values available as other numeric types, casting as needed. *
 *
 */
public class IntFieldSource extends FieldCacheSource {
	
	private final ISortField.IntParser mParser;

	public IntFieldSource(String field) {
		this(field, null);
	}

	public IntFieldSource(String field, ISortField.IntParser parser) {
		super(field);
		mParser = parser;
	}

	@Override
	public String getDescription() {
		return "int(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final int[] arr = mCache.getInts(readerContext.getReader(), mField, mParser, true);
		final Bits valid = mCache.getDocsWithField(readerContext.getReader(), mField);
    
		return new IntDocValues(this) {
			//final MutableValueInt mVal = new MutableValueInt();
      
			@Override
			public float floatVal(int doc) {
				return (float)arr[doc];
			}

			@Override
			public int intVal(int doc) {
				return arr[doc];
			}

			@Override
			public long longVal(int doc) {
				return (long)arr[doc];
			}

			@Override
			public double doubleVal(int doc) {
				return (double)arr[doc];
			}

			@Override
			public String stringVal(int doc) {
				return Float.toString(arr[doc]);
			}

			@Override
			public Object objectVal(int doc) {
				return valid.get(doc) ? arr[doc] : null;
			}

			@Override
			public boolean exists(int doc) {
				return valid.get(doc);
			}

			@Override
			public String toString(int doc) {
				return getDescription() + '=' + intVal(doc);
			}

			@Override
			public ValueSourceScorer getRangeScorer(IIndexReader reader, 
					String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
				int lower, upper;
				// instead of using separate comparison functions, adjust the endpoints.

				if (lowerVal == null) {
					lower = Integer.MIN_VALUE;
				} else {
					lower = Integer.parseInt(lowerVal);
					if (!includeLower && lower < Integer.MAX_VALUE) 
						lower++;
				}

				if (upperVal == null) {
					upper = Integer.MAX_VALUE;
				} else {
					upper = Integer.parseInt(upperVal);
					if (!includeUpper && upper > Integer.MIN_VALUE) 
						upper--;
				}

				final int ll = lower;
				final int uu = upper;

				return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						int val = arr[doc];
						// only check for deleted if it's the default value
						// if (val==0 && reader.isDeleted(doc)) return false;
						return val >= ll && val <= uu;
					}
				};
			}

			@Override
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final int[] mIntArr = arr;
					private final MutableValueInt mVal = new MutableValueInt();

					@Override
					public MutableValue getValue() {
						return mVal;
					}

					@Override
					public void fillValue(int doc) {
						mVal.set(mIntArr[doc]);
						mVal.setExists(valid.get(doc));
					}
				};
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		IntFieldSource other = (IntFieldSource)o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? Integer.class.hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
