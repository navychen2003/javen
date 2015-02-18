package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Abstract {@link ValueSource} implementation which wraps two ValueSources
 * and applies an extendible float function to their values.
 **/
public abstract class DualFloatFunction extends ValueSource {
	
	protected final ValueSource mSourceA;
	protected final ValueSource mSourceB;

	/**
	 * @param   a  the base.
	 * @param   b  the exponent.
	 */
	public DualFloatFunction(ValueSource a, ValueSource b) {
		mSourceA = a;
		mSourceB = b;
	}

	protected abstract String getName();
	protected abstract float callFunc(int doc, FunctionValues aVals, FunctionValues bVals);

	@Override
	public String getDescription() {
		return getName() + "(" + mSourceA.getDescription() + "," + mSourceB.getDescription() + ")";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues aVals =  mSourceA.getValues(context, readerContext);
		final FunctionValues bVals =  mSourceB.getValues(context, readerContext);
		
		return new FloatDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return callFunc(doc, aVals, bVals);
			}

			@Override
			public String toString(int doc) {
				return getName() + '(' + aVals.toString(doc) + ',' + bVals.toString(doc) + ')';
			}
		};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSourceA.createWeight(context,searcher);
		mSourceB.createWeight(context,searcher);
	}

	@Override
	public int hashCode() {
		int h = mSourceA.hashCode();
		h ^= (h << 13) | (h >>> 20);
		h += mSourceB.hashCode();
		h ^= (h << 23) | (h >>> 10);
		h += getName().hashCode();
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) 
			return false;
		
		DualFloatFunction other = (DualFloatFunction)o;
		return this.mSourceA.equals(other.mSourceA)
				&& this.mSourceB.equals(other.mSourceB);
	}
	
}
