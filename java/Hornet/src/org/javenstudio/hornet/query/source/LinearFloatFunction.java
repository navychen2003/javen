package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>LinearFloatFunction</code> implements a linear function over
 * another {@link ValueSource}.
 * <br>
 * Normally Used as an argument to a {@link FunctionQuery}
 *
 */
public class LinearFloatFunction extends ValueSource {
	
	protected final ValueSource mSource;
	protected final float mSlope;
	protected final float mIntercept;

	public LinearFloatFunction(ValueSource source, float slope, float intercept) {
		mSource = source;
		mSlope = slope;
		mIntercept = intercept;
	}
  
	@Override
	public String getDescription() {
		return mSlope + "*float(" + mSource.getDescription() + ")+" + mIntercept;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals = mSource.getValues(context, readerContext);
		
		return new FloatDocValues(this) {
				@Override
				public float floatVal(int doc) {
					return vals.floatVal(doc) * mSlope + mIntercept;
				}
				
				@Override
				public String toString(int doc) {
					return mSlope + "*float(" + vals.toString(doc) + ")+" + mIntercept;
				}
			};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSource.createWeight(context, searcher);
	}

	@Override
	public int hashCode() {
		int h = Float.floatToIntBits(mSlope);
		h = (h >>> 2) | (h << 30);
		h += Float.floatToIntBits(mIntercept);
		h ^= (h << 14) | (h >>> 19);
		return h + mSource.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || LinearFloatFunction.class != o.getClass()) 
			return false;
		
		LinearFloatFunction other = (LinearFloatFunction)o;
		return  this.mSlope == other.mSlope
				&& this.mIntercept == other.mIntercept
				&& this.mSource.equals(other.mSource);
	}
	
}
