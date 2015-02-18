package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;

/**
 * <code>MinFloatFunction</code> returns the min of it's components.
 */
public class MinFloatFunction extends MultiFloatFunction {
	
	public MinFloatFunction(ValueSource[] sources) {
		super(sources);
	}

	@Override  
	protected String getName() {
		return "min";
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
				val = Math.min(vals.floatVal(doc),val);
			}
		}
		
		return val;
	}
	
}
