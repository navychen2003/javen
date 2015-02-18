package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * An FST {@link Outputs} implementation where each output
 * is a sequence of bytes.
 *
 */
public final class ByteSequenceOutputs extends Outputs<BytesRef> {

	private final static BytesRef NO_OUTPUT = new BytesRef();
	private final static ByteSequenceOutputs sSingleton = new ByteSequenceOutputs();

	private ByteSequenceOutputs() {}

	public static ByteSequenceOutputs getSingleton() {
		return sSingleton;
	}

	@Override
	public BytesRef common(BytesRef output1, BytesRef output2) {
		assert output1 != null;
		assert output2 != null;

		int pos1 = output1.mOffset;
		int pos2 = output2.mOffset;
		int stopAt1 = pos1 + Math.min(output1.mLength, output2.mLength);
		
		while (pos1 < stopAt1) {
			if (output1.mBytes[pos1] != output2.mBytes[pos2]) 
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
			return new BytesRef(output1.mBytes, 
					output1.mOffset, pos1-output1.mOffset);
		}
	}

	@Override
	public BytesRef subtract(BytesRef output, BytesRef inc) {
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
			
			return new BytesRef(output.mBytes, output.mOffset + inc.mLength, 
					output.mLength-inc.mLength);
		}
	}

	@Override
	public BytesRef add(BytesRef prefix, BytesRef output) {
		assert prefix != null;
		assert output != null;
		
		if (prefix == NO_OUTPUT) {
			return output;
			
		} else if (output == NO_OUTPUT) {
			return prefix;
			
		} else {
			assert prefix.mLength > 0;
			assert output.mLength > 0;
			
			BytesRef result = new BytesRef(prefix.mLength + output.mLength);
			
			System.arraycopy(prefix.mBytes, prefix.mOffset, 
					result.mBytes, 0, prefix.mLength);
			System.arraycopy(output.mBytes, output.mOffset, 
					result.mBytes, prefix.mLength, output.mLength);
			
			result.mLength = prefix.mLength + output.mLength;
			
			return result;
		}
	}

	@Override
	public void write(BytesRef prefix, IDataOutput out) throws IOException {
		assert prefix != null;
		
		out.writeVInt(prefix.mLength);
		out.writeBytes(prefix.mBytes, prefix.mOffset, prefix.mLength);
	}

	@Override
	public BytesRef read(IDataInput in) throws IOException {
		final int len = in.readVInt();
		if (len == 0) {
			return NO_OUTPUT;
			
		} else {
			final BytesRef output = new BytesRef(len);
			in.readBytes(output.mBytes, 0, len);
			output.mLength = len;
			
			return output;
		}
	}

	@Override
	public BytesRef getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(BytesRef output) {
		return output.toString();
	}
	
}
