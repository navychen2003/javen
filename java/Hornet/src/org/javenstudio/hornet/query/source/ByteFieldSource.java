package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Obtains int field values from the {@link FieldCache}
 * using <code>getInts()</code>
 * and makes those values available as other numeric types, casting as needed. *
 *
 */
public class ByteFieldSource extends FieldCacheSource {

	private final ISortField.ByteParser mParser;

	public ByteFieldSource(String field) {
		this(field, null);
	}

	public ByteFieldSource(String field, ISortField.ByteParser parser) {
		super(field);
		mParser = parser;
	}

	@Override
	public String getDescription() {
		return "byte(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final byte[] arr = mCache.getBytes(readerContext.getReader(), mField, mParser, false);
    
		return new FunctionValues() {
			@Override
			public byte byteVal(int doc) {
				return arr[doc];
			}

			@Override
			public short shortVal(int doc) {
				return (short) arr[doc];
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
				return Byte.toString(arr[doc]);
			}

			@Override
			public String toString(int doc) {
				return getDescription() + '=' + byteVal(doc);
			}

			@Override
			public Object objectVal(int doc) {
				return arr[doc];  // TODO: valid?
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		ByteFieldSource other = (ByteFieldSource) o;
		return super.equals(other) && (this.mParser == null ? other.mParser == null :
			this.mParser.getClass() == other.mParser.getClass());
	}

	@Override
	public int hashCode() {
		int h = mParser == null ? Byte.class.hashCode() : mParser.getClass().hashCode();
		h += super.hashCode();
		return h;
	}
	
}
