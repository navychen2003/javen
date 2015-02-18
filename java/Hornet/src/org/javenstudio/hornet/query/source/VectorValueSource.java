package org.javenstudio.hornet.query.source;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Converts individual ValueSource instances to leverage the FunctionValues *Val functions 
 * that work with multiple values,
 * i.e. {@link FunctionValues#doubleVal(int, double[])}
 */
//Not crazy about the name, but...
public class VectorValueSource extends MultiValueSource {
	
	protected final List<ValueSource> mSources;

	public VectorValueSource(List<ValueSource> sources) {
		mSources = sources;
	}

	public String getName() { return "vector"; }
	public List<ValueSource> getSources() { return mSources; }

	@Override
	public int dimension() {
		return mSources.size();
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		int size = mSources.size();

		// special-case x,y and lat,lon since it's so common
		if (size == 2) {
			final FunctionValues x = mSources.get(0).getValues(context, readerContext);
			final FunctionValues y = mSources.get(1).getValues(context, readerContext);
			
			return new FunctionValues() {
				@Override
				public void byteVal(int doc, byte[] vals) {
					vals[0] = x.byteVal(doc);
					vals[1] = y.byteVal(doc);
				}

				@Override
				public void shortVal(int doc, short[] vals) {
					vals[0] = x.shortVal(doc);
					vals[1] = y.shortVal(doc);
				}
				
				@Override
				public void intVal(int doc, int[] vals) {
					vals[0] = x.intVal(doc);
					vals[1] = y.intVal(doc);
				}
				
				@Override
				public void longVal(int doc, long[] vals) {
					vals[0] = x.longVal(doc);
					vals[1] = y.longVal(doc);
				}
				
				@Override
				public void floatVal(int doc, float[] vals) {
					vals[0] = x.floatVal(doc);
					vals[1] = y.floatVal(doc);
				}
				
				@Override
				public void doubleVal(int doc, double[] vals) {
					vals[0] = x.doubleVal(doc);
					vals[1] = y.doubleVal(doc);
				}
				
				@Override
				public void stringVal(int doc, String[] vals) {
					vals[0] = x.stringVal(doc);
					vals[1] = y.stringVal(doc);
				}
				
				@Override
				public String toString(int doc) {
					return getName() + "(" + x.toString(doc) + "," + y.toString(doc) + ")";
				}
			};
		}

		final FunctionValues[] valsArr = new FunctionValues[size];
		for (int i = 0; i < size; i++) {
			valsArr[i] = mSources.get(i).getValues(context, readerContext);
		}

		return new FunctionValues() {
			@Override
			public void byteVal(int doc, byte[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].byteVal(doc);
				}
			}

			@Override
			public void shortVal(int doc, short[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].shortVal(doc);
				}
			}

			@Override
			public void floatVal(int doc, float[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].floatVal(doc);
				}
			}

			@Override
			public void intVal(int doc, int[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].intVal(doc);
				}
			}

			@Override
			public void longVal(int doc, long[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].longVal(doc);
				}
			}

			@Override
			public void doubleVal(int doc, double[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].doubleVal(doc);
				}
			}

			@Override
			public void stringVal(int doc, String[] vals) {
				for (int i = 0; i < valsArr.length; i++) {
					vals[i] = valsArr[i].stringVal(doc);
				}
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
	public void createWeight(ValueSourceContext context, ISearcher searcher) 
			throws IOException {
		for (ValueSource source : mSources) {
			source.createWeight(context, searcher);
		}
	}

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
		
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof VectorValueSource)) 
			return false;

		VectorValueSource that = (VectorValueSource) o;

		return mSources.equals(that.mSources);
	}

	@Override
	public int hashCode() {
		return mSources.hashCode();
	}
	
}
