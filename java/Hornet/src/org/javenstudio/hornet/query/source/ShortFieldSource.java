package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Obtains short field values from the {@link FieldCache}
 * using <code>getShorts()</code>
 * and makes those values available as other numeric types, casting as needed.
 */
public class ShortFieldSource extends FieldCacheSource {

	private final ISortField.ShortParser mParser;

	public ShortFieldSource(String field) {
		this(field, null);
	}

	public ShortFieldSource(String field, ISortField.ShortParser parser) {
		super(field);
		this.mParser = parser;
	}

	@Override
	public String getDescription() {
		return "short(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final short[] arr = mCache.getShorts(readerContext.getReader(), mField, mParser, false);
    
		return new FunctionValues() {
			@Override
			public byte byteVal(int doc) {
				return (byte) arr[doc];
			}

			@Override
			public short shortVal(int doc) {
				return arr[doc];
			}

			@Override
			public float floatVal(int doc) {
				return (float) arr[doc];
			}

			@Override
			public int intVal(int doc) {
				return (int) arr[doc];
			}

			@Override
			public long longVal(int doc) {
				return (long) arr[doc];
			}

			@Override
			public double doubleVal(int doc) {
				return (double) arr[doc];
			}

			@Override
			public String stringVal(int doc) {
				return Short.toString(arr[doc]);
			}

			@Override
			public String toString(int doc) {
				return getDescription() + '=' + shortVal(doc);
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		ShortFieldSource other = (ShortFieldSource) o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? Short.class.hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
