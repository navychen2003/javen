package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.hornet.codec.CodecUtil;

/** 
 * A write-once Writer.
 */
public abstract class PackedWriter {
	
    protected final IDataOutput mOutput;
    protected final int mValueCount;
    protected final int mBitsPerValue;

    protected PackedWriter(IDataOutput out, int valueCount, int bitsPerValue) 
    		throws IOException {
    	assert bitsPerValue <= 64;
    	assert valueCount >= 0 || valueCount == -1;
    	
    	mOutput = out;
    	mValueCount = valueCount;
    	mBitsPerValue = bitsPerValue;
    }

    protected void writeHeader() throws IOException {
    	assert mValueCount != -1;
    	CodecUtil.writeHeader(mOutput, PackedInts.CODEC_NAME, PackedInts.VERSION_CURRENT);
    	mOutput.writeVInt(mBitsPerValue);
    	mOutput.writeVInt(mValueCount);
    	mOutput.writeVInt(getFormat().getId());
    }

    /** The format used to serialize values. */
    protected abstract Format getFormat();

    /** Add a value to the stream. */
    public abstract void add(long v) throws IOException;

    /** The number of bits per value. */
    public final int bitsPerValue() {
    	return mBitsPerValue;
    }

    /** Perform end-of-stream operations. */
    public abstract void finish() throws IOException;

    /**
     * Returns the current ord in the stream (number of values that have been
     * written so far minus one).
     */
    public abstract int ord();
    
}
