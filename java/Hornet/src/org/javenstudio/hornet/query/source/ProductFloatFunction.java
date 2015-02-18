package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;

/**
 * <code>ProductFloatFunction</code> returns the product of it's components.
 */
public class ProductFloatFunction extends MultiFloatFunction {
	
	public ProductFloatFunction(ValueSource[] sources) {
		super(sources);
	}

	@Override
	protected String getName() {
		return "product";
	}

	@Override
	protected float callFunc(int doc, FunctionValues[] valsArr) {
		float val = 1.0f;
		for (FunctionValues vals : valsArr) {
			val *= vals.floatVal(doc);
		}
		return val;
	}
	
}
