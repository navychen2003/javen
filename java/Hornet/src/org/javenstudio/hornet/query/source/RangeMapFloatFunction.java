package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>LinearFloatFunction</code> implements a linear function over
 * another {@link ValueSource}.
 * <br>
 * Normally Used as an argument to a {@link FunctionQuery}
 *
 */
public class RangeMapFloatFunction extends ValueSource {
	
	protected final ValueSource mSource;
	protected final float mMin;
	protected final float mMax;
	protected final float mTarget;
	protected final Float mDefaultVal;

	public RangeMapFloatFunction(ValueSource source, 
			float min, float max, float target, Float def) {
		mSource = source;
		mMin = min;
		mMax = max;
		mTarget = target;
		mDefaultVal = def;
	}

	@Override
	public String getDescription() {
		return "map(" + mSource.getDescription() + "," + mMin + "," + mMax + "," + mTarget + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals =  mSource.getValues(context, readerContext);
		
		return new FloatDocValues(this) {
				@Override
				public float floatVal(int doc) {
					float val = vals.floatVal(doc);
					return (val >= mMin && val <= mMax) ? mTarget : 
						(mDefaultVal == null ? val : mDefaultVal);
				}
				
				@Override
				public String toString(int doc) {
					return "map(" + vals.toString(doc) + ",min=" + mMin + ",max=" + mMax 
							+ ",target=" + mTarget + ")";
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
		int h = mSource.hashCode();
		
		h ^= (h << 10) | (h >>> 23);
		h += Float.floatToIntBits(mMin);
		h ^= (h << 14) | (h >>> 19);
		h += Float.floatToIntBits(mMax);
		h ^= (h << 13) | (h >>> 20);
		h += Float.floatToIntBits(mTarget);
		
		if (mDefaultVal != null)
			h += mDefaultVal.hashCode();
		
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || RangeMapFloatFunction.class != o.getClass()) 
			return false;
		
		RangeMapFloatFunction other = (RangeMapFloatFunction)o;
		return  this.mMin == other.mMin
				&& this.mMax == other.mMax
				&& this.mTarget == other.mTarget
				&& this.mSource.equals(other.mSource)
				&& (this.mDefaultVal == other.mDefaultVal 
				|| (this.mDefaultVal != null && this.mDefaultVal.equals(other.mDefaultVal)));
	}
	
}
