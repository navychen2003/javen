package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.ValueSourceScorer;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueDouble;

/**
 * Obtains float field values from the {@link FieldCache}
 * using <code>getFloats()</code>
 * and makes those values available as other numeric types, casting as needed.
 *
 */
public class DoubleFieldSource extends FieldCacheSource {

	protected final ISortField.DoubleParser mParser;

	public DoubleFieldSource(String field) {
		this(field, null);
	}

	public DoubleFieldSource(String field, ISortField.DoubleParser parser) {
		super(field);
		mParser = parser;
	}

	@Override
	public String getDescription() {
		return "double(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final double[] arr = mCache.getDoubles(readerContext.getReader(), mField, mParser, true);
		final Bits valid = mCache.getDocsWithField(readerContext.getReader(), mField);
		
		return new DoubleDocValues(this) {
			@Override
			public double doubleVal(int doc) {
				return arr[doc];
			}

			@Override
			public boolean exists(int doc) {
				return valid.get(doc);
			}

			@Override
			public ValueSourceScorer getRangeScorer(IIndexReader reader, 
					String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
				double lower, upper;

				if (lowerVal == null) 
					lower = Double.NEGATIVE_INFINITY;
				else 
					lower = Double.parseDouble(lowerVal);
        
				if (upperVal == null) 
					upper = Double.POSITIVE_INFINITY;
				else 
					upper = Double.parseDouble(upperVal);
        
				final double l = lower;
				final double u = upper;

				if (includeLower && includeUpper) {
					return new ValueSourceScorer(reader, this) {
							@Override
							public boolean matchesValue(int doc) {
								double docVal = doubleVal(doc);
								return docVal >= l && docVal <= u;
							}
						};
						
				} else if (includeLower && !includeUpper) {
					return new ValueSourceScorer(reader, this) {
							@Override
							public boolean matchesValue(int doc) {
								double docVal = doubleVal(doc);
								return docVal >= l && docVal < u;
							}
						};
						
				} else if (!includeLower && includeUpper) {
					return new ValueSourceScorer(reader, this) {
							@Override
							public boolean matchesValue(int doc) {
								double docVal = doubleVal(doc);
								return docVal > l && docVal <= u;
							}
						};
						
				} else {
					return new ValueSourceScorer(reader, this) {
							@Override
							public boolean matchesValue(int doc) {
								double docVal = doubleVal(doc);
								return docVal > l && docVal < u;
							}
						};
				}
			}

			@Override
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final double[] mDoubleArr = arr;
					private final MutableValueDouble mVal = new MutableValueDouble();

					@Override
					public MutableValue getValue() {
						return mVal;
					}

					@Override
					public void fillValue(int doc) {
						mVal.set(mDoubleArr[doc]);
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
		
		DoubleFieldSource other = (DoubleFieldSource) o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? Double.class.hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
