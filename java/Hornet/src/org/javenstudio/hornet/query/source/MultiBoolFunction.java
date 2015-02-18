package org.javenstudio.hornet.query.source;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.BoolDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Abstract {@link ValueSource} implementation which wraps multiple ValueSources
 * and applies an extendible boolean function to their values.
 **/
public abstract class MultiBoolFunction extends BoolFunction {
	
	protected final List<ValueSource> mSources;

	public MultiBoolFunction(List<ValueSource> sources) {
		mSources = sources;
	}

	protected abstract String getName();
	protected abstract boolean callFunc(int doc, FunctionValues[] vals);

	@Override
	public BoolDocValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues[] vals =  new FunctionValues[mSources.size()];
		
		int i=0;
		for (ValueSource source : mSources) {
			vals[i++] = source.getValues(context, readerContext);
		}

		return new BoolDocValues(this) {
			@Override
			public boolean boolVal(int doc) {
				return callFunc(doc, vals);
			}

			@Override
			public String toString(int doc) {
				StringBuilder sb = new StringBuilder(getName());
				sb.append('(');
				
				boolean first = true;
				for (FunctionValues dv : vals) {
					if (first) 
						first = false;
					else 
						sb.append(',');
					
					sb.append(dv.toString(doc));
				}
				return sb.toString();
			}
		};
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder(getName());
		sb.append('(');
		
		boolean first = true;
		for (ValueSource source : mSources) {
			if (first) 
				first = false;
			else 
				sb.append(',');
			
			sb.append(source.getDescription());
		}
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return mSources.hashCode() + getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) 
			return false;
		
		MultiBoolFunction other = (MultiBoolFunction)o;
		return this.mSources.equals(other.mSources);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		for (ValueSource source : mSources) {
			source.createWeight(context, searcher);
		}
	}
	
}
