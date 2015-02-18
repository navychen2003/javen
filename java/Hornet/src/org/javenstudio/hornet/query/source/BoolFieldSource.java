package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.query.BoolDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueBool;

public class BoolFieldSource extends FieldCacheSource {
	
	public BoolFieldSource(String field) {
		super(field);
	}

	@Override
	public String getDescription() {
		return "bool(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final IDocTermsIndex sindex = mCache.getTermsIndex(readerContext.getReader(), mField);

		// figure out what ord maps to true
		int nord = sindex.getNumOrd();
		BytesRef br = new BytesRef();
		int tord = -1;
		
		for (int i=1; i < nord; i++) {
			sindex.lookup(i, br);
			if (br.getLength() == 1 && br.getBytes()[br.getOffset()] == 'T') {
				tord = i;
				break;
			}
		}

		final int trueOrd = tord;

		return new BoolDocValues(this) {
			@Override
			public boolean boolVal(int doc) {
				return sindex.getOrd(doc) == trueOrd;
			}

			@Override
			public boolean exists(int doc) {
				return sindex.getOrd(doc) != 0;
			}

			@Override
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final MutableValueBool mVal = new MutableValueBool();

					@Override
					public MutableValue getValue() {
						return mVal;
					}

					@Override
					public void fillValue(int doc) {
						int ord = sindex.getOrd(doc);
						mVal.set(ord == trueOrd);
						mVal.setExists(ord != 0);
					}
				};
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	};

}
