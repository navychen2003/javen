package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;

public abstract class Double2SourceParser extends NamedSourceParser {
	
	public Double2SourceParser(String name) {
		super(name);
	}

	public abstract double callFunc(int doc, FunctionValues a, FunctionValues b);

	@Override
	public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
		return new Function(fp.parseValueSource(), fp.parseValueSource());
	}

	class Function extends ValueSource {
		private final ValueSource mSourceA;
		private final ValueSource mSourceB;

		/**
		 * @param   a  the base.
		 * @param   b  the exponent.
		 */
		public Function(ValueSource a, ValueSource b) {
			mSourceA = a;
			mSourceB = b;
		}

		@Override
		public String getDescription() {
			return getName() + "(" + mSourceA.getDescription() + "," 
					+ mSourceB.getDescription() + ")";
		}

		@Override
		public FunctionValues getValues(ValueSourceContext context, 
				IAtomicReaderRef readerContext) throws IOException {
			final FunctionValues aVals =  mSourceA.getValues(context, readerContext);
			final FunctionValues bVals =  mSourceB.getValues(context, readerContext);
			
			return new DoubleDocValues(this) {
					@Override
					public double doubleVal(int doc) {
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
			// do nothing
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
			
			Function other = (Function)o;
			return this.mSourceA.equals(other.mSourceA)
					&& this.mSourceB.equals(other.mSourceB);
		}
	}
	
}
