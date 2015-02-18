package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;

/**
 * <code>SumFloatFunction</code> returns the sum of it's components.
 */
public class SumFloatFunction extends MultiFloatFunction {
	
	public SumFloatFunction(ValueSource[] sources) {
		super(sources);
	}

	@Override  
	protected String getName() {
		return "sum";
	}

	@Override
	protected float callFunc(int doc, FunctionValues[] valsArr) {
		float val = 0.0f;
		for (FunctionValues vals : valsArr) {
			val += vals.floatVal(doc);
		}
		return val;
	}
	
}
