package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;

/**
 * A null FST {@link Outputs} implementation; use this if
 * you just want to build an FSA.
 *
 */
public final class NoOutputs extends Outputs<Object> {

	static final Object NO_OUTPUT = new Object() {
			// NodeHash calls hashCode for this output; we fix this
			// so we get deterministic hashing.
			@Override
			public int hashCode() {
				return 42;
			}
	
			@Override
			public boolean equals(Object other) {
				return other == this;
			}
		};

	private static final NoOutputs sSingleton = new NoOutputs();

	private NoOutputs() {}

	public static NoOutputs getSingleton() {
		return sSingleton;
	}

	@Override
	public Object common(Object output1, Object output2) {
		assert output1 == NO_OUTPUT;
		assert output2 == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public Object subtract(Object output, Object inc) {
		assert output == NO_OUTPUT;
		assert inc == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public Object add(Object prefix, Object output) {
		assert prefix == NO_OUTPUT: "got " + prefix;
		assert output == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public void write(Object prefix, IDataOutput out) {
		//assert false;
	}

	@Override
	public Object read(IDataInput in) {
		//assert false;
		//return null;
		return NO_OUTPUT;
	}

	@Override
	public Object getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(Object output) {
		return "";
	}
	
}
