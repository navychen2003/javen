package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.util.IntsRef;

/**
 * An FST {@link Outputs} implementation where each output
 * is a sequence of ints.
 *
 */
public final class IntSequenceOutputs extends Outputs<IntsRef> {

	private final static IntsRef NO_OUTPUT = new IntsRef();
	private final static IntSequenceOutputs sSingleton = new IntSequenceOutputs();

	private IntSequenceOutputs() {}

	public static IntSequenceOutputs getSingleton() {
		return sSingleton;
	}

	@Override
	public IntsRef common(IntsRef output1, IntsRef output2) {
		assert output1 != null;
		assert output2 != null;

		int pos1 = output1.mOffset;
		int pos2 = output2.mOffset;
		int stopAt1 = pos1 + Math.min(output1.mLength, output2.mLength);
		
		while (pos1 < stopAt1) {
			if (output1.mInts[pos1] != output2.mInts[pos2]) 
				break;
			
			pos1 ++;
			pos2 ++;
		}

		if (pos1 == output1.mOffset) {
			// no common prefix
			return NO_OUTPUT;
			
		} else if (pos1 == output1.mOffset + output1.mLength) {
			// output1 is a prefix of output2
			return output1;
			
		} else if (pos2 == output2.mOffset + output2.mLength) {
			// output2 is a prefix of output1
			return output2;
			
		} else {
			return new IntsRef(output1.mInts, 
					output1.mOffset, pos1-output1.mOffset);
		}
	}

	@Override
	public IntsRef subtract(IntsRef output, IntsRef inc) {
		assert output != null;
		assert inc != null;
		
		if (inc == NO_OUTPUT) {
			// no prefix removed
			return output;
			
		} else if (inc.mLength == output.mLength) {
			// entire output removed
			return NO_OUTPUT;
			
		} else {
			assert inc.mLength < output.mLength: "inc.length=" + inc.mLength + 
					" vs output.length=" + output.mLength;
			assert inc.mLength > 0;
			
			return new IntsRef(output.mInts, output.mOffset + inc.mLength, 
					output.mLength-inc.mLength);
		}
	}

	@Override
	public IntsRef add(IntsRef prefix, IntsRef output) {
		assert prefix != null;
		assert output != null;
		
		if (prefix == NO_OUTPUT) {
			return output;
			
		} else if (output == NO_OUTPUT) {
			return prefix;
			
		} else {
			assert prefix.mLength > 0;
			assert output.mLength > 0;
			
			IntsRef result = new IntsRef(prefix.mLength + output.mLength);
			
			System.arraycopy(prefix.mInts, prefix.mOffset, 
					result.mInts, 0, prefix.mLength);
			System.arraycopy(output.mInts, output.mOffset, result.mInts, 
					prefix.mLength, output.mLength);
			
			result.mLength = prefix.mLength + output.mLength;
			
			return result;
		}
	}

	@Override
	public void write(IntsRef prefix, IDataOutput out) throws IOException {
		assert prefix != null;
		out.writeVInt(prefix.mLength);
		
		for (int idx=0; idx < prefix.mLength; idx++) {
			out.writeVInt(prefix.mInts[prefix.mOffset+idx]);
		}
	}

	@Override
	public IntsRef read(IDataInput in) throws IOException {
		final int len = in.readVInt();
		if (len == 0) {
			return NO_OUTPUT;
			
		} else {
			final IntsRef output = new IntsRef(len);
			
			for (int idx=0; idx < len; idx++) {
				output.mInts[idx] = in.readVInt();
			}
			output.mLength = len;
			
			return output;
		}
	}

	@Override
	public IntsRef getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(IntsRef output) {
		return output.toString();
	}
	
}
