package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.hornet.store.fst.Builder;
import org.javenstudio.hornet.store.fst.ByteSequenceOutputs;
import org.javenstudio.hornet.store.fst.BytesRefFSTEnum;
import org.javenstudio.hornet.store.fst.FST;
import org.javenstudio.hornet.store.fst.FSTUtil;

final class PendingBlock extends PendingEntry {

	private final IntsRef mScratchIntsRef = new IntsRef();
	private final BytesRef mPrefix;
	private final long mFp;
	private FST<BytesRef> mIndex;
	private List<FST<BytesRef>> mSubIndices;
	private final boolean mHasTerms;
	private final boolean mIsFloor;
	private final int mFloorLeadByte;
    
    public PendingBlock(BytesRef prefix, long fp, boolean hasTerms, boolean isFloor, 
    		int floorLeadByte, List<FST<BytesRef>> subIndices) {
    	super(false);
    	mPrefix = prefix;
    	mFp = fp;
    	mHasTerms = hasTerms;
    	mIsFloor = isFloor;
    	mFloorLeadByte = floorLeadByte;
    	mSubIndices = subIndices;
    }
    
    public final FST<BytesRef> getIndex() { return mIndex; }
    public final BytesRef getPrefix() { return mPrefix; }
    public final long getFilePointer() { return mFp; }
    
    public void compileIndex(List<PendingBlock> floorBlocks, RAMOutputStream scratchBytes) 
    		throws IOException {
    	assert (mIsFloor && floorBlocks != null && floorBlocks.size() != 0) || 
    		(!mIsFloor && floorBlocks == null): "isFloor=" + mIsFloor + " floorBlocks=" + floorBlocks;
    	assert scratchBytes.getFilePointer() == 0;

    	// TODO: try writing the leading vLong in MSB order
    	// (opposite of what Lucene does today), for better
    	// outputs sharing in the FST
    	scratchBytes.writeVLong(encodeOutput(mFp, mHasTerms, mIsFloor));
    	
    	if (mIsFloor) {
    		scratchBytes.writeVInt(floorBlocks.size());
    		for (PendingBlock sub : floorBlocks) {
    			assert sub.mFloorLeadByte != -1;
    			scratchBytes.writeByte((byte) sub.mFloorLeadByte);
    			assert sub.mFp > mFp;
    			scratchBytes.writeVLong((sub.mFp - mFp) << 1 | (sub.mHasTerms ? 1 : 0));
    		}
    	}

    	final ByteSequenceOutputs outputs = ByteSequenceOutputs.getSingleton();
    	final Builder<BytesRef> indexBuilder = new Builder<BytesRef>(
    			FST.INPUT_TYPE.BYTE1, 0, 0, true, false, Integer.MAX_VALUE,
    			outputs, null, false);

    	final byte[] bytes = new byte[(int) scratchBytes.getFilePointer()];
    	assert bytes.length > 0;
    	scratchBytes.writeTo(bytes, 0);
    	indexBuilder.add(FSTUtil.toIntsRef(mPrefix, mScratchIntsRef), 
    			new BytesRef(bytes, 0, bytes.length));
    	scratchBytes.reset();

    	// Copy over index for all sub-blocks
    	if (mSubIndices != null) {
    		for (FST<BytesRef> subIndex : mSubIndices) {
    			append(indexBuilder, subIndex);
    		}
    	}

    	if (floorBlocks != null) {
    		for (PendingBlock sub : floorBlocks) {
    			if (sub.mSubIndices != null) {
    				for (FST<BytesRef> subIndex : sub.mSubIndices) {
    					append(indexBuilder, subIndex);
    				}
    			}
    			sub.mSubIndices = null;
    		}
    	}

    	mIndex = indexBuilder.finish();
    	mSubIndices = null;

    	/*
      	Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"));
      	Util.toDot(index, w, false, false);
      	w.close();
    	 */
    }

    // TODO: maybe we could add bulk-add method to
    // Builder?  Takes FST and unions it w/ current
    // FST.
    private void append(Builder<BytesRef> builder, FST<BytesRef> subIndex) throws IOException {
    	final BytesRefFSTEnum<BytesRef> subIndexEnum = new BytesRefFSTEnum<BytesRef>(subIndex);
    	BytesRefFSTEnum.InputOutput<BytesRef> indexEnt;
    	while ((indexEnt = subIndexEnum.next()) != null) {
    		builder.add(FSTUtil.toIntsRef(indexEnt.getInput(), mScratchIntsRef), indexEnt.getOutput());
    	}
    }

    static long encodeOutput(long fp, boolean hasTerms, boolean isFloor) {
    	assert fp < (1L << 62);
        return (fp << 2) | 
        		(hasTerms ? BlockTreeTermsWriter.OUTPUT_FLAG_HAS_TERMS : 0) | 
        		(isFloor ? BlockTreeTermsWriter.OUTPUT_FLAG_IS_FLOOR : 0);
	}
    
    @Override
    public String toString() {
    	return "PendingBlock: " + mPrefix.utf8ToString();
    }
    
}
