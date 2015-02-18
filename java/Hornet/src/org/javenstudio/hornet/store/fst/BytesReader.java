package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.store.DataInput;

/** 
 * Reads the bytes from this FST.  Use {@link
 *  #getBytesReader(int)} to obtain an instance for this
 *  FST; re-use across calls (but only within a single
 *  thread) for better performance. 
 */
public abstract class BytesReader extends DataInput {
	
    protected final byte[] mBytes;
    protected int mPos;
    
    protected BytesReader(byte[] bytes, int pos) {
      	mBytes = bytes;
      	mPos = pos;
    }
    
    protected abstract void skip(int byteCount);
    protected abstract void skip(int base, int byteCount);
    
    public final int getPosition() { return mPos; }
    
}
