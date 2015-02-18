package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;

/**
 * An FST {@link Outputs} implementation, holding two other outputs.
 *
 */
public class PairOutputs<A,B> extends Outputs<PairOutputs.Pair<A,B>> {

	private final Pair<A,B> NO_OUTPUT;
	private final Outputs<A> mOutputs1;
	private final Outputs<B> mOutputs2;

	/** Holds a single pair of two outputs. */
	public static class Pair<A,B> {
		public final A mOutput1;
		public final B mOutput2;

		// use newPair
		private Pair(A output1, B output2) {
			mOutput1 = output1;
			mOutput2 = output2;
		}

		@Override @SuppressWarnings("rawtypes")
		public boolean equals(Object other) {
			if (other == this) {
				return true;
				
			} else if (other instanceof Pair) {
				
				Pair pair = (Pair) other;
				return mOutput1.equals(pair.mOutput1) && 
						mOutput2.equals(pair.mOutput2);
				
			} else 
				return false;
		}

		@Override
		public int hashCode() {
			return mOutput1.hashCode() + mOutput2.hashCode();
		}
	};

	public PairOutputs(Outputs<A> outputs1, Outputs<B> outputs2) {
		mOutputs1 = outputs1;
		mOutputs2 = outputs2;
		NO_OUTPUT = new Pair<A,B>(outputs1.getNoOutput(), outputs2.getNoOutput());
	}

	/** Create a new Pair */
	public Pair<A,B> newPair(A a, B b) {
		if (a.equals(mOutputs1.getNoOutput())) 
			a = mOutputs1.getNoOutput();
		
		if (b.equals(mOutputs2.getNoOutput())) 
			b = mOutputs2.getNoOutput();
		
		if (a == mOutputs1.getNoOutput() && b == mOutputs2.getNoOutput()) {
			return NO_OUTPUT;
			
		} else {
			final Pair<A,B> p = new Pair<A,B>(a, b);
			assert valid(p);
			return p;
		}
	}

	// for assert
	private boolean valid(Pair<A,B> pair) {
		final boolean noOutput1 = pair.mOutput1.equals(mOutputs1.getNoOutput());
		final boolean noOutput2 = pair.mOutput2.equals(mOutputs2.getNoOutput());

		if (noOutput1 && pair.mOutput1 != mOutputs1.getNoOutput()) 
			return false;

		if (noOutput2 && pair.mOutput2 != mOutputs2.getNoOutput()) 
			return false;

		if (noOutput1 && noOutput2) {
			if (pair != NO_OUTPUT) 
				return false;
			else 
				return true;
			
		} else 
			return true;
	}
  
	@Override
	public Pair<A,B> common(Pair<A,B> pair1, Pair<A,B> pair2) {
		assert valid(pair1);
		assert valid(pair2);
		
		return newPair(mOutputs1.common(pair1.mOutput1, pair2.mOutput1),
                   	   mOutputs2.common(pair1.mOutput2, pair2.mOutput2));
	}

	@Override
	public Pair<A,B> subtract(Pair<A,B> output, Pair<A,B> inc) {
		assert valid(output);
		assert valid(inc);
		
		return newPair(mOutputs1.subtract(output.mOutput1, inc.mOutput1),
                       mOutputs2.subtract(output.mOutput2, inc.mOutput2));
	}

	@Override
	public Pair<A,B> add(Pair<A,B> prefix, Pair<A,B> output) {
		assert valid(prefix);
		assert valid(output);
		
		return newPair(mOutputs1.add(prefix.mOutput1, output.mOutput1),
                       mOutputs2.add(prefix.mOutput2, output.mOutput2));
	}

	@Override
	public void write(Pair<A,B> output, IDataOutput writer) throws IOException {
		assert valid(output);
		mOutputs1.write(output.mOutput1, writer);
		mOutputs2.write(output.mOutput2, writer);
	}

	@Override
	public Pair<A,B> read(IDataInput in) throws IOException {
		A output1 = mOutputs1.read(in);
		B output2 = mOutputs2.read(in);
		return newPair(output1, output2);
	}

	@Override
	public Pair<A,B> getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(Pair<A,B> output) {
		assert valid(output);
		return "<pair:" + mOutputs1.outputToString(output.mOutput1) + "," + 
				mOutputs2.outputToString(output.mOutput2) + ">";
	}

	@Override
	public String toString() {
		return "PairOutputs<" + mOutputs1 + "," + mOutputs2 + ">";
	}
	
}
