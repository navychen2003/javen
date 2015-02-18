package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IBytesReader {

    /**
     * Gets a slice out of {@link PagedBytes} starting at <i>start</i> with a
     * given length. If the slice spans across a block border this method will
     * allocate sufficient resources and copy the paged data.
     * <p>
     * Slices spanning more than one block are not supported.
     * </p>
     */
    public BytesRef fillSlice(BytesRef b, long start, int length);
	
    /**
     * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i>.
     * <p>
     * <b>Note:</b> this method does not support slices spanning across block
     * borders.
     * </p>
     * 
     * @return the given {@link BytesRef}
     */
    public BytesRef fill(BytesRef b, long start);
    
    /**
     * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i>. *
     * <p>
     * <b>Note:</b> this method does not support slices spanning across block
     * borders.
     * </p>
     * 
     * @return the internal block number of the slice.
     */
    public int fillAndGetIndex(BytesRef b, long start);
    
    /**
     * Reads length as 1 or 2 byte vInt prefix, starting at <i>start</i> and
     * returns the start offset of the next part, suitable as start parameter on
     * next call to sequentially read all {@link BytesRef}.
     * 
     * <p>
     * <b>Note:</b> this method does not support slices spanning across block
     * borders.
     * </p>
     * 
     * @return the start offset of the next part, suitable as start parameter on
     *         next call to sequentially read all {@link BytesRef}.
     */
    public long fillAndGetStart(BytesRef b, long start);
    
    /**
     * Gets a slice out of {@link PagedBytes} starting at <i>start</i>, the
     * length is read as 1 or 2 byte vInt prefix. Iff the slice spans across a
     * block border this method will allocate sufficient resources and copy the
     * paged data.
     * <p>
     * Slices spanning more than one block are not supported.
     * </p>
     */
    public BytesRef fillSliceWithPrefix(BytesRef b, long start);
    
    public byte[][] getBlocks();
    public int[] getBlockEnds();
    
}
