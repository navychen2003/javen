package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * <code>ReciprocalFloatFunction</code> implements a reciprocal 
 * function f(x) = a/(mx+b), based on
 * the float value of a field or function as exported by {@link ValueSource}.
 * <br>
 *
 * When a and b are equal, and x>=0, this function has a maximum value of 1 
 * that drops as x increases.
 * Increasing the value of a and b together results in a movement of 
 * the entire function to a flatter part of the curve.
 * <p>These properties make this an idea function for boosting more recent documents.
 * <p>Example:<code>  recip(ms(NOW,mydatefield),3.16e-11,1,1)</code>
 * <p>A multiplier of 3.16e-11 changes the units from milliseconds to years 
 * (since there are about 3.16e10 milliseconds
 * per year).  Thus, a very recent date will yield a value close to 1/(0+1) or 1,
 * a date a year in the past will get a multiplier of about 1/(1+1) or 1/2,
 * and date two years old will yield 1/(2+1) or 1/3.
 *
 * @see FunctionQuery
 */
public class ReciprocalFloatFunction extends ValueSource {
	
	protected final ValueSource mSource;
	protected final float mValueM;
	protected final float mValueA;
	protected final float mValueB;

	/**
	 *  f(source) = a/(m*float(source)+b)
	 */
	public ReciprocalFloatFunction(ValueSource source, float m, float a, float b) {
		mSource = source;
		mValueM = m;
		mValueA = a;
		mValueB = b;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals = mSource.getValues(context, readerContext);
		
		return new FloatDocValues(this) {
				@Override
				public float floatVal(int doc) {
					return mValueA / (mValueM * vals.floatVal(doc) + mValueB);
				}
				
				@Override
				public String toString(int doc) {
					return Float.toString(mValueA) + "/("
							+ mValueM + "*float(" + vals.toString(doc) + ')'
							+ '+' + mValueB + ')';
				}
			};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSource.createWeight(context, searcher);
	}

	@Override
	public String getDescription() {
		return Float.toString(mValueA) + "/("
				+ mValueM + "*float(" + mSource.getDescription() + ")"
				+ "+" + mValueB + ')';
	}

	@Override
	public int hashCode() {
		int h = Float.floatToIntBits(mValueA) + Float.floatToIntBits(mValueM);
		h ^= (h << 13) | (h >>> 20);
		return h + (Float.floatToIntBits(mValueB)) + mSource.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || ReciprocalFloatFunction.class != o.getClass()) 
			return false;
		
		ReciprocalFloatFunction other = (ReciprocalFloatFunction)o;
		return this.mValueM == other.mValueM
				&& this.mValueA == other.mValueA
				&& this.mValueB == other.mValueB
				&& this.mSource.equals(other.mSource);
	}
	
}
