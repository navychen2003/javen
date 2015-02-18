package org.javenstudio.hornet.query.source;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * {@link ValueSource} implementation which only returns the values from the provided
 * ValueSources which are available for a particular docId.  Consequently, when combined
 * with a {@link ConstValueSource}, this function serves as a way to return a default
 * value when the values for a field are unavailable.
 */
public class DefFunction extends MultiFunction {
	
	public DefFunction(List<ValueSource> sources) {
		super(sources);
	}

	@Override
	protected String getName() {
		return "def";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext fcontext, 
			IAtomicReaderRef readerContext) throws IOException {
		return new Values(valsArr(mSources, fcontext, readerContext)) {
			final int mUpto = mValsArr.length - 1;

			private FunctionValues get(int doc) {
				for (int i=0; i < mUpto; i++) {
					FunctionValues vals = mValsArr[i];
					if (vals.exists(doc)) 
						return vals;
				}
				return mValsArr[mUpto];
			}

			@Override
			public byte byteVal(int doc) {
				return get(doc).byteVal(doc);
			}

			@Override
			public short shortVal(int doc) {
				return get(doc).shortVal(doc);
			}

			@Override
			public float floatVal(int doc) {
				return get(doc).floatVal(doc);
			}

			@Override
			public int intVal(int doc) {
				return get(doc).intVal(doc);
			}

			@Override
			public long longVal(int doc) {
				return get(doc).longVal(doc);
			}

			@Override
			public double doubleVal(int doc) {
				return get(doc).doubleVal(doc);
			}

			@Override
			public String stringVal(int doc) {
				return get(doc).stringVal(doc);
			}

			@Override
			public boolean boolVal(int doc) {
				return get(doc).boolVal(doc);
			}

			@Override
			public boolean bytesVal(int doc, BytesRef target) {
				return get(doc).bytesVal(doc, target);
			}

			@Override
			public Object objectVal(int doc) {
				return get(doc).objectVal(doc);
			}

			@Override
			public boolean exists(int doc) {
				// return true if any source is exists?
				for (FunctionValues vals : mValsArr) {
					if (vals.exists(doc)) 
						return true;
				}
				return false;
			}

			@Override
			public ValueFiller getValueFiller() {
				// TODO: need ValueSource.type() to determine correct type
				return super.getValueFiller();
			}
		};
	}
	
}
