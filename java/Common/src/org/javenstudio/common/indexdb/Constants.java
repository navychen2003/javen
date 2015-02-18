package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytePool;

public final class Constants {

	/** Default buffer size set to 1024*/
	public static final int INPUT_BUFFER_SIZE = 1024;
  
	// The normal read buffer size defaults to 1024, but
	// increasing this during merging seems to yield
	// performance gains.  However we don't want to increase
	// it too much because there are quite a few
	// BufferedIndexInputs created during merging.  See
	// LUCENE-888 for details.
	
	/** A buffer size for merges set to 4096 */
	public static final int MERGE_BUFFER_SIZE = 4096;
	
	public static final int OUTPUT_BUFFER_SIZE = 16384;
	
	/**
	 * Constant to identify the start of a codec header.
	 */
	public final static int CODEC_MAGIC = 0x3fd76c17;
	
	public static final byte CODEC_MAGIC_BYTE1 = (byte) (CODEC_MAGIC >>> 24);
	public static final byte CODEC_MAGIC_BYTE2 = (byte) (CODEC_MAGIC >>> 16);
	public static final byte CODEC_MAGIC_BYTE3 = (byte) (CODEC_MAGIC >>> 8);
	public static final byte CODEC_MAGIC_BYTE4 = (byte) (CODEC_MAGIC);
	
	/** 
	 * Initial chunks size of the shared byte[] blocks used to
     * store postings data 
     */
	public static final int BYTE_BLOCK_NOT_MASK = ~BytePool.BYTE_BLOCK_MASK;

	/** 
	 * if you increase this, you must fix field cache impl for
	 * getTerms/getTermsIndex requires <= 32768 
	 */
	public static final int MAX_TERM_LENGTH_UTF8 = BytePool.BYTE_BLOCK_SIZE-2;
	
	public static final int MAX_CLAUSE_COUNT = 1024;
	
	/**
	 * NOTE: we track per-segment version as a String with the "X.Y" format, e.g.
	 * "4.0", "3.1", "3.0". Therefore when we change this constant, we should keep
	 * the format.
	 *
	 * This is the internal Indexdb version, recorded into each segment.
	 */
	public static final String INDEXDB_MAIN_VERSION = "4.0";
	
	public static boolean isDebug() { 
		return false;
	}
	
}
