package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.panda.util.StringDistance;

/**
 *
 */
public class StringDistanceFunction extends ValueSource {
	
	protected ValueSource mString1, mString2;
	protected StringDistance mDist;

	public StringDistanceFunction(ValueSource str1, ValueSource str2, 
			StringDistance measure) {
		mString1 = str1;
		mString2 = str2;
		mDist = measure;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues str1DV = mString1.getValues(context, readerContext);
		final FunctionValues str2DV = mString2.getValues(context, readerContext);
		
		return new FloatDocValues(this) {
				@Override
				public float floatVal(int doc) {
					return mDist.getDistance(str1DV.stringVal(doc), str2DV.stringVal(doc));
				}
	
				@Override
				public String toString(int doc) {
					StringBuilder sb = new StringBuilder();
					sb.append("strdist").append('(');
					sb.append(str1DV.toString(doc)).append(',').append(str2DV.toString(doc))
	                	.append(", dist=").append(mDist.getClass().getName());
					sb.append(')');
					return sb.toString();
				}
			};
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("strdist").append('(');
		sb.append(mString1).append(',').append(mString2); 
		sb.append(", dist=").append(mDist.getClass().getName());
		sb.append(')');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof StringDistanceFunction)) 
			return false;

		StringDistanceFunction that = (StringDistanceFunction) o;

		if (!mDist.equals(that.mDist)) return false;
		if (!mString1.equals(that.mString1)) return false;
		if (!mString2.equals(that.mString2)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = mString1.hashCode();
		result = 31 * result + mString2.hashCode();
		result = 31 * result + mDist.hashCode();
		return result;
	}
	
}
