package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;

/**
 * An FST {@link Outputs} implementation where each output
 * is a non-negative long value.
 *
 */
public final class PositiveIntOutputs extends Outputs<Long> {
  
	private final static Long NO_OUTPUT = new Long(0);

	private final static PositiveIntOutputs sSingletonShare = new PositiveIntOutputs(true);
	private final static PositiveIntOutputs sSingletonNoShare = new PositiveIntOutputs(false);

	private final boolean mDoShare;
	
	private PositiveIntOutputs(boolean doShare) {
		mDoShare = doShare;
	}

	public static PositiveIntOutputs getSingleton(boolean doShare) {
		return doShare ? sSingletonShare : sSingletonNoShare;
	}

	@Override
	public Long common(Long output1, Long output2) {
		assert valid(output1);
		assert valid(output2);
		
		if (output1 == NO_OUTPUT || output2 == NO_OUTPUT) {
			return NO_OUTPUT;
			
		} else if (mDoShare) {
			assert output1 > 0;
			assert output2 > 0;
			return Math.min(output1, output2);
			
		} else if (output1.equals(output2)) {
			return output1;
			
		} else {
			return NO_OUTPUT;
		}
	}

	@Override
	public Long subtract(Long output, Long inc) {
		assert valid(output);
		assert valid(inc);
		assert output >= inc;

		if (inc == NO_OUTPUT) {
			return output;
		} else if (output.equals(inc)) {
			return NO_OUTPUT;
		} else {
			return output - inc;
		}
	}

	@Override
	public Long add(Long prefix, Long output) {
		assert valid(prefix);
		assert valid(output);
		
		if (prefix == NO_OUTPUT) {
			return output;
		} else if (output == NO_OUTPUT) {
			return prefix;
		} else {
			return prefix + output;
		}
	}

	@Override
	public void write(Long output, IDataOutput out) throws IOException {
		assert valid(output);
		out.writeVLong(output);
	}

	@Override
	public Long read(IDataInput in) throws IOException {
		long v = in.readVLong();
		if (v == 0) 
			return NO_OUTPUT;
		else 
			return v;
	}

	private boolean valid(Long o) {
		assert o != null;
		assert o == NO_OUTPUT || o > 0;
		return true;
	}

	@Override
	public Long getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(Long output) {
		return output.toString();
	}

	@Override
	public String toString() {
		return "PositiveIntOutputs: doShare=" + mDoShare;
	}
	
}
