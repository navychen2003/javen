package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.SingleFunction;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;

public abstract class DoubleSourceParser extends NamedSourceParser {
	
	public DoubleSourceParser(String name) {
		super(name);
	}

	public abstract double callFunc(int doc, FunctionValues vals);

	@Override
	public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
		return new Function(fp.parseValueSource());
	}

	class Function extends SingleFunction {
		public Function(ValueSource source) {
			super(source);
	    }

	    @Override
	    public String getName() {
	    	return DoubleSourceParser.this.getName();
	    }

	    @Override
	    public FunctionValues getValues(ValueSourceContext context, 
	    		IAtomicReaderRef readerContext) throws IOException {
	    	final FunctionValues vals =  mSource.getValues(context, readerContext);
	    	return new DoubleDocValues(this) {
		    		@Override
		    		public double doubleVal(int doc) {
		    			return callFunc(doc, vals);
		    		}
		    		
		    		@Override
		    		public String toString(int doc) {
		    			return getName() + '{' + vals.toString(doc) + '}';
		    		}
		    	};
	    }
	}
	
}
