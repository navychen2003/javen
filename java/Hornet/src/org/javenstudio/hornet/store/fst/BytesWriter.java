package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.store.DataOutput;
import org.javenstudio.common.indexdb.util.ArrayUtil;

//Non-static: writes to FST's byte[]
final class BytesWriter extends DataOutput {
	
	private final FST<?> mFST;
	protected int mPosWrite;

    public BytesWriter(FST<?> fst) {
    	mFST = fst;
    	// pad: ensure no node gets address 0 which is reserved to mean
    	// the stop state w/ no arcs
    	mPosWrite = 1;
    }

    public final int getPosWrite() { return mPosWrite; }
    
    @Override
    public void writeByte(byte b) {
    	assert mPosWrite <= mFST.mBytes.length;
    	if (mFST.mBytes.length == mPosWrite) 
    		mFST.mBytes = ArrayUtil.grow(mFST.getBytes());
    	
    	assert mPosWrite < mFST.mBytes.length: "posWrite=" + mPosWrite + 
    			" bytes.length=" + mFST.mBytes.length;
    	mFST.mBytes[mPosWrite++] = b;
    }

    public void setPosWrite(int posWrite) {
    	mPosWrite = posWrite;
    	if (mFST.mBytes.length < posWrite) 
    		mFST.mBytes = ArrayUtil.grow(mFST.mBytes, posWrite);
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) {
    	final int size = mPosWrite + length;
    	mFST.mBytes = ArrayUtil.grow(mFST.mBytes, size);
    	System.arraycopy(b, offset, mFST.mBytes, mPosWrite, length);
    	mPosWrite += length;
    }
    
}
