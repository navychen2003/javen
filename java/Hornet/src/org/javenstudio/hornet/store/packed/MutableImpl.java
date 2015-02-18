package org.javenstudio.hornet.store.packed;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IIntsMutable;

abstract class MutableImpl extends ReaderImpl implements IIntsMutable {

    protected MutableImpl(int valueCount, int bitsPerValue) {
    	super(valueCount, bitsPerValue);
    }

    public int set(int index, long[] arr, int off, int len) {
    	assert len > 0 : "len must be > 0 (got " + len + ")";
    	assert index >= 0 && index < mValueCount;
    	
    	len = Math.min(len, mValueCount - index);
    	assert off + len <= arr.length;

    	for (int i = index, o = off, end = index + len; i < end; ++i, ++o) {
    		set(i, arr[o]);
    	}
    	return len;
    }

    public void fill(int fromIndex, int toIndex, long val) {
    	assert val <= PackedInts.maxValue(mBitsPerValue);
    	assert fromIndex <= toIndex;
    	
    	for (int i = fromIndex; i < toIndex; ++i) {
    		set(i, val);
    	}
    }

    protected Format getFormat() {
    	return Format.PACKED;
    }

    @Override
    public void save(IDataOutput out) throws IOException {
    	PackedWriter writer = PackedInts.getWriterNoHeader(out, getFormat(),
    			mValueCount, mBitsPerValue, PackedInts.DEFAULT_BUFFER_SIZE);
    	
    	writer.writeHeader();
    	for (int i = 0; i < mValueCount; ++i) {
    		writer.add(get(i));
    	}
    	writer.finish();
    }
    
}
