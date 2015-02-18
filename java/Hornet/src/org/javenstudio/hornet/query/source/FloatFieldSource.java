package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueFloat;

/**
 * Obtains float field values from the {@link FieldCache}
 * using <code>getFloats()</code>
 * and makes those values available as other numeric types, casting as needed.
 *
 */
public class FloatFieldSource extends FieldCacheSource {

	protected final ISortField.FloatParser mParser;

	public FloatFieldSource(String field) {
		this(field, null);
	}

	public FloatFieldSource(String field, ISortField.FloatParser parser) {
		super(field);
		mParser = parser;
	}

	@Override
	public String getDescription() {
		return "float(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final float[] arr = mCache.getFloats(readerContext.getReader(), mField, mParser, true);
		final Bits valid = mCache.getDocsWithField(readerContext.getReader(), mField);

		return new FloatDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return arr[doc];
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
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final float[] mFloatArr = arr;
					private final MutableValueFloat mVal = new MutableValueFloat();

					@Override
					public MutableValue getValue() {
						return mVal;
					}

					@Override
					public void fillValue(int doc) {
						mVal.set(mFloatArr[doc]);
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
		
		FloatFieldSource other = (FloatFieldSource)o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? Float.class.hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
