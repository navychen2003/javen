package org.javenstudio.hornet.query;

import org.javenstudio.hornet.util.MutableValue;

/**
 * Abstraction of the logic required to fill the value of a specified doc into
 * a reusable {@link MutableValue}.  Implementations of {@link FunctionValues}
 * are encouraged to define their own implementations of ValueFiller if their
 * value is not a float.
 *
 */
public abstract class ValueFiller {

	/** MutableValue will be reused across calls */
    public abstract MutableValue getValue();

    /** MutableValue will be reused across calls.  Returns true if the value exists. */
    public abstract void fillValue(int doc);
	
}
