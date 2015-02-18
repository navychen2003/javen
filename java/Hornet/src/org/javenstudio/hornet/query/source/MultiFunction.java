package org.javenstudio.hornet.query.source;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Abstract parent class for {@link ValueSource} implementations that wrap multiple
 * ValueSources and apply their own logic.
 */
public abstract class MultiFunction extends ValueSource {
	
	protected final List<ValueSource> mSources;

	public MultiFunction(List<ValueSource> sources) {
		mSources = sources;
	}

	protected abstract String getName();

	@Override
	public String getDescription() {
		return toDescription(getName(), mSources);
	}

	public static String toDescription(String name, List<ValueSource> sources) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append('(');
		
		boolean firstTime=true;
		for (ValueSource source : sources) {
			if (firstTime) 
				firstTime = false;
			else 
				sb.append(',');
			
			sb.append(source);
		}
		
		sb.append(')');
		return sb.toString();
	}

	public static FunctionValues[] valsArr(List<ValueSource> sources, 
			ValueSourceContext fcontext, IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues[] valsArr = new FunctionValues[sources.size()];
		
		int i = 0;
		for (ValueSource source : sources) {
			valsArr[i++] = source.getValues(fcontext, readerContext);
		}
		
		return valsArr;
	}

	public class Values extends FunctionValues {
		final FunctionValues[] mValsArr;

		public Values(FunctionValues[] valsArr) {
			mValsArr = valsArr;
		}

		@Override
		public String toString(int doc) {
			return MultiFunction.toString(getName(), mValsArr, doc);
		}

		@Override
		public ValueFiller getValueFiller() {
			// TODO: need ValueSource.type() to determine correct type
			return super.getValueFiller();
		}
	}

	public static String toString(String name, FunctionValues[] valsArr, int doc) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append('(');
		
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

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		for (ValueSource source : mSources) {
			source.createWeight(context, searcher);
		}
	}

	@Override
	public int hashCode() {
		return mSources.hashCode() + getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || this.getClass() != o.getClass()) 
			return false;
		
		MultiFunction other = (MultiFunction)o;
		return this.mSources.equals(other.mSources);
	}
	
}
