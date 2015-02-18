package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.util.CharsRef;

/**
 * An FST {@link Outputs} implementation where each output
 * is a sequence of characters.
 *
 */
public final class CharSequenceOutputs extends Outputs<CharsRef> {

	private final static CharsRef NO_OUTPUT = new CharsRef();
	private final static CharSequenceOutputs sSingleton = new CharSequenceOutputs();

	private CharSequenceOutputs() {}

	public static CharSequenceOutputs getSingleton() {
		return sSingleton;
	}

	@Override
	public CharsRef common(CharsRef output1, CharsRef output2) {
		assert output1 != null;
		assert output2 != null;

		int pos1 = output1.mOffset;
		int pos2 = output2.mOffset;
		int stopAt1 = pos1 + Math.min(output1.mLength, output2.mLength);
		
		while (pos1 < stopAt1) {
			if (output1.mChars[pos1] != output2.mChars[pos2]) 
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
			return new CharsRef(output1.mChars, 
					output1.mOffset, pos1-output1.mOffset);
		}
	}

	@Override
	public CharsRef subtract(CharsRef output, CharsRef inc) {
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
			
			return new CharsRef(output.mChars, output.mOffset + inc.mLength, 
					output.mLength-inc.mLength);
		}
	}

	@Override
	public CharsRef add(CharsRef prefix, CharsRef output) {
		assert prefix != null;
		assert output != null;
		
		if (prefix == NO_OUTPUT) {
			return output;
			
		} else if (output == NO_OUTPUT) {
			return prefix;
			
		} else {
			assert prefix.mLength > 0;
			assert output.mLength > 0;
			
			CharsRef result = new CharsRef(prefix.mLength + output.mLength);
			
			System.arraycopy(prefix.mChars, prefix.mOffset, 
					result.mChars, 0, prefix.mLength);
			System.arraycopy(output.mChars, output.mOffset, 
					result.mChars, prefix.mLength, output.mLength);
			
			result.mLength = prefix.mLength + output.mLength;
			
			return result;
		}
	}

	@Override
	public void write(CharsRef prefix, IDataOutput out) throws IOException {
		assert prefix != null;
		
		out.writeVInt(prefix.mLength);
		
		// TODO: maybe UTF8?
		for (int idx=0; idx < prefix.mLength; idx++) {
			out.writeVInt(prefix.mChars[prefix.mOffset+idx]);
		}
	}

	@Override
	public CharsRef read(IDataInput in) throws IOException {
		final int len = in.readVInt();
		if (len == 0) {
			return NO_OUTPUT;
			
		} else {
			final CharsRef output = new CharsRef(len);
			for (int idx=0; idx < len; idx++) {
				output.mChars[idx] = (char) in.readVInt();
			}
			output.mLength = len;
			
			return output;
		}
	}

	@Override
	public CharsRef getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(CharsRef output) {
		return output.toString();
	}
	
}
