package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;

/**
 * <code>MaxFloatFunction</code> returns the max of it's components.
 */
public class MaxFloatFunction extends MultiFloatFunction {
	
	public MaxFloatFunction(ValueSource[] sources) {
		super(sources);
	}

	@Override  
	protected String getName() {
		return "max";
	}

	@Override
	protected float callFunc(int doc, FunctionValues[] valsArr) {
		boolean first = true;
		float val = 0.0f;
		
		for (FunctionValues vals : valsArr) {
			if (first) {
				first = false;
				val = vals.floatVal(doc);
			} else {
				val = Math.max(vals.floatVal(doc),val);
			}
		}
		
		return val;
	}
	
}
