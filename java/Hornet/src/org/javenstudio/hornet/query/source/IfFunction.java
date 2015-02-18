package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Depending on the boolean value of the <code>ifSource</code> function,
 * returns the value of the <code>trueSource</code> or <code>falseSource</code> function.
 */
public class IfFunction extends BoolFunction {
	
	private final ValueSource mIfSource;
	private final ValueSource mTrueSource;
	private final ValueSource mFalseSource;

	public IfFunction(ValueSource ifSource, 
			ValueSource trueSource, ValueSource falseSource) {
		mIfSource = ifSource;
		mTrueSource = trueSource;
		mFalseSource = falseSource;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		
		final FunctionValues ifVals = mIfSource.getValues(context, readerContext);
		final FunctionValues trueVals = mTrueSource.getValues(context, readerContext);
		final FunctionValues falseVals = mFalseSource.getValues(context, readerContext);

		return new FunctionValues() {
			@Override
			public byte byteVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.byteVal(doc) 
						: falseVals.byteVal(doc);
			}

			@Override
			public short shortVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.shortVal(doc) 
						: falseVals.shortVal(doc);
			}

			@Override
			public float floatVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.floatVal(doc) 
						: falseVals.floatVal(doc);
			}

			@Override
			public int intVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.intVal(doc) 
						: falseVals.intVal(doc);
			}

			@Override
			public long longVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.longVal(doc) 
						: falseVals.longVal(doc);
			}

			@Override
			public double doubleVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.doubleVal(doc) 
						: falseVals.doubleVal(doc);
			}

			@Override
			public String stringVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.stringVal(doc) 
						: falseVals.stringVal(doc);
			}

			@Override
			public boolean boolVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.boolVal(doc) 
						: falseVals.boolVal(doc);
			}

			@Override
			public boolean bytesVal(int doc, BytesRef target) {
				return ifVals.boolVal(doc) ? trueVals.bytesVal(doc, target) 
						: falseVals.bytesVal(doc, target);
			}

			@Override
			public Object objectVal(int doc) {
				return ifVals.boolVal(doc) ? trueVals.objectVal(doc) 
						: falseVals.objectVal(doc);
			}

			@Override
			public boolean exists(int doc) {
				return true; // TODO: flow through to any sub-sources?
			}

			@Override
			public ValueFiller getValueFiller() {
				// TODO: we need types of trueSource / falseSource to handle this
				// for now, use float.
				return super.getValueFiller();
			}

			@Override
			public String toString(int doc) {
				return "if(" + ifVals.toString(doc) + ',' + trueVals.toString(doc) 
						+ ',' + falseVals.toString(doc) + ')';
			}
		};

	}

	@Override
	public String getDescription() {
		return "if(" + mIfSource.getDescription() + ',' 
				+ mTrueSource.getDescription() + ',' + mFalseSource + ')';
	}

	@Override
	public int hashCode() {
		int h = mIfSource.hashCode();
		h = h * 31 + mTrueSource.hashCode();
		h = h * 31 + mFalseSource.hashCode();
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof IfFunction)) 
			return false;
		
		IfFunction other = (IfFunction)o;
		return this.mIfSource.equals(other.mIfSource)
				&& this.mTrueSource.equals(other.mTrueSource)
				&& this.mFalseSource.equals(other.mFalseSource);
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mIfSource.createWeight(context, searcher);
		mTrueSource.createWeight(context, searcher);
		mFalseSource.createWeight(context, searcher);
	}
	
}
