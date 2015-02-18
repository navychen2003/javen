package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.StringDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Pass a the field value through as a String, no matter the type 
 * // Q: doesn't this mean it's a "string"?
 *
 */
public class LiteralValueSource extends ValueSource {
	
	protected final String mValue;
	protected final BytesRef mBytesRef;

	public LiteralValueSource(String val) {
		mValue = val;
		mBytesRef = new BytesRef(val);
	}

	/** returns the literal value */
	public String getValue() {
		return mValue;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new StringDocValues(this) {
				@Override
				public String stringVal(int doc) {
					return mValue;
				}
	
				@Override
				public boolean bytesVal(int doc, BytesRef target) {
					target.copyBytes(mBytesRef);
					return true;
				}
	
				@Override
				public String toString(int doc) {
					return mValue;
				}
			};
	}

	@Override
	public String getDescription() {
		return "literal(" + mValue + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LiteralValueSource)) 
			return false;

		LiteralValueSource that = (LiteralValueSource) o;
		return mValue.equals(that.mValue);
	}

	public static final int sHashCode = LiteralValueSource.class.hashCode();
	
	@Override
	public int hashCode() {
		return sHashCode + mValue.hashCode();
	}
	
}
