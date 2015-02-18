package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.BoolDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * {@link BoolFunction} implementation which applies an extendible boolean
 * function to the values of a single wrapped {@link ValueSource}.
 *
 * Functions this can be used for include whether a field has a value or not,
 * or inverting the boolean value of the wrapped ValueSource.
 */
public abstract class SimpleBoolFunction extends BoolFunction {
	
	protected final ValueSource mSource;

	public SimpleBoolFunction(ValueSource source) {
		mSource = source;
	}

	protected abstract String getName();
	protected abstract boolean callFunc(int doc, FunctionValues vals);

	@Override
	public BoolDocValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals = mSource.getValues(context, readerContext);
		
		return new BoolDocValues(this) {
			@Override
			public boolean boolVal(int doc) {
				return callFunc(doc, vals);
			}
			
			@Override
			public String toString(int doc) {
				return getName() + '(' + vals.toString(doc) + ')';
			}
		};
	}

	@Override
	public String getDescription() {
		return getName() + '(' + mSource.getDescription() + ')';
	}

	@Override
	public int hashCode() {
		return mSource.hashCode() + getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) 
			return false;
		
		SimpleBoolFunction other = (SimpleBoolFunction)o;
		return this.mSource.equals(other.mSource);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSource.createWeight(context, searcher);
	}
	
}
