package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;

/**
 * An FST {@link Outputs} implementation where each output
 * is one or two non-negative long values.  If it's a
 * single output, Long is returned; else, TwoLongs.  Order
 * is preserved in the TwoLongs case, ie .first is the first
 * input/output added to Builder, and .second is the
 * second.  You cannot store 0 output with this (that's
 * reserved to mean "no output")!
 *
 * NOTE: the resulting FST is not guaranteed to be minimal!
 * See {@link Builder}.
 *
 */
public final class UpToTwoPositiveIntOutputs extends Outputs<Object> {

	/** Holds two long outputs. */
	public final static class TwoLongs {
		public final long mFirst;
		public final long mSecond;

		public TwoLongs(long first, long second) {
			mFirst = first;
			mSecond = second;
			assert first >= 0;
			assert second >= 0;
		}

		@Override
		public String toString() {
			return "TwoLongs:" + mFirst + "," + mSecond;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TwoLongs) {
				final TwoLongs other = (TwoLongs) obj;
				return mFirst == other.mFirst && mSecond == other.mSecond;
			} else 
				return false;
		}

		@Override
		public int hashCode() {
			return (int) ((mFirst^(mFirst>>>32)) ^ (mSecond^(mSecond>>32)));
		}
	}
  
	private final static Long NO_OUTPUT = new Long(0);

	private final static UpToTwoPositiveIntOutputs sSingletonShare = 
			new UpToTwoPositiveIntOutputs(true);
	private final static UpToTwoPositiveIntOutputs sSingletonNoShare = 
			new UpToTwoPositiveIntOutputs(false);

	private final boolean mDoShare;
	
	private UpToTwoPositiveIntOutputs(boolean doShare) {
		mDoShare = doShare;
	}

	public static UpToTwoPositiveIntOutputs getSingleton(boolean doShare) {
		return doShare ? sSingletonShare : sSingletonNoShare;
	}

	public Long get(long v) {
		if (v == 0) 
			return NO_OUTPUT;
		else 
			return Long.valueOf(v);
	}

	public TwoLongs get(long first, long second) {
		return new TwoLongs(first, second);
	}

	@Override
	public Long common(Object _output1, Object _output2) {
		assert valid(_output1, false);
		assert valid(_output2, false);
		
		final Long output1 = (Long) _output1;
		final Long output2 = (Long) _output2;
		
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
	public Long subtract(Object _output, Object _inc) {
		assert valid(_output, false);
		assert valid(_inc, false);
		
		final Long output = (Long) _output;
		final Long inc = (Long) _inc;
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
	public Object add(Object _prefix, Object _output) {
		assert valid(_prefix, false);
		assert valid(_output, true);
		
		final Long prefix = (Long) _prefix;
		if (_output instanceof Long) {
			final Long output = (Long) _output;
			if (prefix == NO_OUTPUT) 
				return output;
			else if (output == NO_OUTPUT) 
				return prefix;
			else 
				return prefix + output;
			
		} else {
			final TwoLongs output = (TwoLongs) _output;
			final long v = prefix;
			
			return new TwoLongs(output.mFirst + v, output.mSecond + v);
		}
	}

	@Override
	public void write(Object _output, IDataOutput out) throws IOException {
		assert valid(_output, true);
		if (_output instanceof Long) {
			final Long output = (Long) _output;
			out.writeVLong(output<<1);
			
		} else {
			final TwoLongs output = (TwoLongs) _output;
			out.writeVLong((output.mFirst<<1) | 1);
			out.writeVLong(output.mSecond);
		}
	}

	@Override
	public Object read(IDataInput in) throws IOException {
		final long code = in.readVLong();
		if ((code & 1) == 0) {
			// single long
			final long v = code >>> 1;
			if (v == 0) 
				return NO_OUTPUT;
			else 
				return Long.valueOf(v);
			
		} else {
			// two longs
			final long first = code >>> 1;
			final long second = in.readVLong();
			
			return new TwoLongs(first, second);
		}
	}

	private boolean valid(Long o) {
		assert o != null;
		assert o instanceof Long;
		assert o == NO_OUTPUT || o > 0;
		return true;
	}

	// Used only by assert
	private boolean valid(Object _o, boolean allowDouble) {
		if (!allowDouble) {
			assert _o instanceof Long;
			return valid((Long) _o);
			
		} else if (_o instanceof TwoLongs) {
			return true;
			
		} else 
			return valid((Long) _o);
	}

	@Override
	public Object getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(Object output) {
		return output.toString();
	}

	@Override
	public Object merge(Object first, Object second) {
		assert valid(first, false);
		assert valid(second, false);
		return new TwoLongs((Long) first, (Long) second);
	}
	
}
