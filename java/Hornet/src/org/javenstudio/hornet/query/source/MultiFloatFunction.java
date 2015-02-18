package org.javenstudio.hornet.query.source;

import java.util.Arrays;
import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Abstract {@link ValueSource} implementation which wraps multiple ValueSources
 * and applies an extendible float function to their values.
 */
public abstract class MultiFloatFunction extends ValueSource {
	
	protected final ValueSource[] mSources;

	public MultiFloatFunction(ValueSource[] sources) {
		mSources = sources;
	}

	protected abstract String getName();
	protected abstract float callFunc(int doc, FunctionValues[] valsArr);

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('(');
		
		boolean firstTime = true;
		for (ValueSource source : mSources) {
			if (firstTime) 
				firstTime = false;
			else 
				sb.append(',');
			
			sb.append(source);
		}
		
		sb.append(')');
		return sb.toString();
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues[] valsArr = new FunctionValues[mSources.length];
		for (int i=0; i < mSources.length; i++) {
			valsArr[i] = mSources[i].getValues(context, readerContext);
		}

		return new FloatDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return callFunc(doc, valsArr);
			}
			
			@Override
			public String toString(int doc) {
				StringBuilder sb = new StringBuilder();
				sb.append(getName()).append('(');
				
				boolean firstTime = true;
				for (FunctionValues vals : valsArr) {
					if (firstTime) 
						firstTime = false;
					else 
						sb.append(',');
					
					sb.append(vals.toString(doc));
				}
				
				sb.append(')');
				return sb.toString();
			}
		};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		for (ValueSource source : mSources) {
			source.createWeight(context, searcher);
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(mSources) + getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) 
			return false;
		
		MultiFloatFunction other = (MultiFloatFunction)o;
		return this.getName().equals(other.getName())
				&& Arrays.equals(this.mSources, other.mSources);
	}
	
}
