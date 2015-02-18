package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;

/** 
 * Function to divide "a" by "b"
 */
public class DivFloatFunction extends DualFloatFunction {
	
	/**
	 * @param   a  the numerator.
	 * @param   b  the denominator.
	 */
	public DivFloatFunction(ValueSource a, ValueSource b) {
		super(a,b);
	}

	@Override
	protected String getName() {
		return "div";
	}

	@Override
	protected float callFunc(int doc, FunctionValues aVals, FunctionValues bVals) {
		return aVals.floatVal(doc) / bVals.floatVal(doc);
	}
	
}
