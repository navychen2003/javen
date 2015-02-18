package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.TermPostingsWriter;
import org.javenstudio.hornet.codec.SegmentWriteState;
import org.javenstudio.hornet.codec.TermsConsumer;
import org.javenstudio.hornet.codec.TermsFormat;
import org.javenstudio.hornet.codec.TermsWriter;

/**
 * TODO:
 *
 *  - Currently there is a one-to-one mapping of indexed
 *    term to term block, but we could decouple the two, ie,
 *    put more terms into the index than there are blocks.
 *    The index would take up more RAM but then it'd be able
 *    to avoid seeking more often and could make PK/FuzzyQ
 *    faster if the additional indexed terms could store
 *    the offset into the terms block.
 *
 *  - The blocks are not written in true depth-first
 *    order, meaning if you just next() the file pointer will
 *    sometimes jump backwards.  For example, block foo* will
 *    be written before block f* because it finished before.
 *    This could possibly hurt performance if the terms dict is
 *    not hot, since OSs anticipate sequential file access.  We
 *    could fix the writer to re-order the blocks as a 2nd
 *    pass.
 *
 *  - Each block encodes the term suffixes packed
 *    sequentially using a separate vInt per term, which is
 *    1) wasteful and 2) slow (must linear scan to find a
 *    particular suffix).  We should instead 1) make
 *    random-access array so we can directly access the Nth
 *    suffix, and 2) bulk-encode this array using bulk int[]
 *    codecs; then at search time we can binary search when
 *    we seek a particular term.
 *
 * block-based terms index and dictionary writer.
 * <p>
 * Writes terms dict and index, block-encoding (column
 * stride) each term's metadata for each set of terms
 * between two index terms.
 *
 * @see BlockTreeTermsReader
 */
public class BlockTreeTermsWriter extends TermsWriter {

	public final static int DEFAULT_MIN_BLOCK_SIZE = 25;
	public final static int DEFAULT_MAX_BLOCK_SIZE = 48;

	static final int OUTPUT_FLAGS_NUM_BITS = 2;
	static final int OUTPUT_FLAGS_MASK = 0x3;
	static final int OUTPUT_FLAG_IS_FLOOR = 0x1;
	static final int OUTPUT_FLAG_HAS_TERMS = 0x2;

	final static String TERMS_CODEC_NAME = "BLOCK_TREE_TERMS_DICT";
	final static String TERMS_INDEX_CODEC_NAME = "BLOCK_TREE_TERMS_INDEX";
	
	// Initial format
	public static final int TERMS_VERSION_START = 0;
	public static final int TERMS_VERSION_CURRENT = TERMS_VERSION_START;

	// Initial format
	public static final int TERMS_INDEX_VERSION_START = 0;
	public static final int TERMS_INDEX_VERSION_CURRENT = TERMS_INDEX_VERSION_START;

	private final List<TermsWriterImpl> mFields = new ArrayList<TermsWriterImpl>();
	private final RAMOutputStream mScratchBytes;
	
	private final IIndexOutput mTermsOut;
	private final IIndexOutput mIndexOut;
	private final int mMinItemsInBlock;
	private final int mMaxItemsInBlock;

	private final TermPostingsWriter mPostingsWriter;
	private final IFieldInfos mFieldInfos;
	private IFieldInfo mCurrentField;
	
	final RAMOutputStream getScratchBytes() { return mScratchBytes; }
	final TermPostingsWriter getPostingsWriter() { return mPostingsWriter; }
	final IFieldInfos getFieldInfos() { return mFieldInfos; }
	final IIndexOutput getTermsOutput() { return mTermsOut; }
	final IIndexOutput getIndexOutput() { return mIndexOut; }
	final int getMinItemsInBlock() { return mMinItemsInBlock; }
	final int getMaxItemsInBlock() { return mMaxItemsInBlock; }
	
	public BlockTreeTermsWriter(TermsFormat file, SegmentWriteState state, 
			TermPostingsWriter postingsWriter) throws IOException { 
		this(file, state, postingsWriter, DEFAULT_MIN_BLOCK_SIZE, DEFAULT_MAX_BLOCK_SIZE);
	}
  
	/** 
	 * Create a new writer.  The number of items (terms or
	 *  sub-blocks) per block will aim to be between
	 *  minItemsPerBlock and maxItemsPerBlock, though in some
	 *  cases the blocks may be smaller than the min. 
	 */
	public BlockTreeTermsWriter(TermsFormat format,
			SegmentWriteState state, TermPostingsWriter postingsWriter,
			int minItemsInBlock, int maxItemsInBlock) throws IOException {
		super(format, postingsWriter.getDirectory(), state.getSegmentInfo().getName());
		mScratchBytes = new RAMOutputStream(format.getContext());
	
		if (minItemsInBlock <= 1) 
			throw new IllegalArgumentException("minItemsInBlock must be >= 2; got " + minItemsInBlock);
		
		if (maxItemsInBlock <= 0) 
			throw new IllegalArgumentException("maxItemsInBlock must be >= 1; got " + maxItemsInBlock);
		
		if (minItemsInBlock > maxItemsInBlock) {
			throw new IllegalArgumentException("maxItemsInBlock must be >= minItemsInBlock; " + 
					"got maxItemsInBlock=" + maxItemsInBlock + " minItemsInBlock=" + minItemsInBlock);
		}
		
		if (2*(minItemsInBlock-1) > maxItemsInBlock) {
			throw new IllegalArgumentException("maxItemsInBlock must be at least 2*(minItemsInBlock-1); " + 
					"got maxItemsInBlock=" + maxItemsInBlock + " minItemsInBlock=" + minItemsInBlock);
		}

		mTermsOut = createTermsOutput();
		
		boolean success = false;
		IIndexOutput indexOut = null;
		try {
			mFieldInfos = state.getFieldInfos();
			mMinItemsInBlock = minItemsInBlock;
			mMaxItemsInBlock = maxItemsInBlock;
			writeHeader(mTermsOut);

			indexOut = createTermsIndexOutput();
			writeIndexHeader(indexOut);

			mCurrentField = null;
			mPostingsWriter = postingsWriter;
      
			postingsWriter.start(mTermsOut); // have consumer write its format/header
			success = true;
		} finally {
			if (!success) 
				IOUtils.closeWhileHandlingException(mTermsOut, indexOut);
		}
		
		mIndexOut = indexOut;
	}
  
	protected void writeHeader(IIndexOutput out) throws IOException {
		CodecUtil.writeHeader(out, TERMS_CODEC_NAME, TERMS_VERSION_CURRENT); 
		out.writeLong(0); 	// leave space for end index pointer    
	}

	protected void writeIndexHeader(IIndexOutput out) throws IOException {
		CodecUtil.writeHeader(out, TERMS_INDEX_CODEC_NAME, TERMS_INDEX_VERSION_CURRENT); 
		out.writeLong(0); 	// leave space for end index pointer    
	}

	protected void writeTrailer(IIndexOutput out, long dirStart) throws IOException {
		out.seek(CodecUtil.headerLength(TERMS_CODEC_NAME));
		out.writeLong(dirStart);    
	}

	protected void writeIndexTrailer(IIndexOutput indexOut, long dirStart) throws IOException {
		indexOut.seek(CodecUtil.headerLength(TERMS_INDEX_CODEC_NAME));
		indexOut.writeLong(dirStart);    
	}
  
	@Override
	public TermsConsumer addField(IFieldInfo field) throws IOException {
		assert mCurrentField == null || mCurrentField.getName().compareTo(field.getName()) < 0;
		
		mCurrentField = field;
		final TermsWriterImpl terms = new TermsWriterImpl(this, getTermsFormat().getContext(), field);
		mFields.add(terms);
		
		return terms;
	}

	@Override
	public void close() throws IOException {
		IOException ioe = null;
		try {
			int nonZeroCount = 0;
			for (TermsWriterImpl field : mFields) {
				if (field.getNumTerms() > 0) 
					nonZeroCount ++;
			}

			final long dirStart = mTermsOut.getFilePointer();
			final long indexDirStart = mIndexOut.getFilePointer();

			mTermsOut.writeVInt(nonZeroCount);
      
			for (TermsWriterImpl field : mFields) {
				if (field.getNumTerms() > 0) {
					mTermsOut.writeVInt(field.getFieldInfo().getNumber());
					mTermsOut.writeVLong(field.getNumTerms());
					
					final BytesRef rootCode = ((PendingBlock) field.getPendingEntryList().get(0))
							.getIndex().getEmptyOutput();
					assert rootCode != null: "field=" + field.getFieldInfo().getName() + 
							" numTerms=" + field.getNumTerms();
					
					mTermsOut.writeVInt(rootCode.getLength());
					mTermsOut.writeBytes(rootCode.getBytes(), rootCode.getOffset(), rootCode.getLength());
					if (field.getFieldInfo().getIndexOptions() != IndexOptions.DOCS_ONLY) 
						mTermsOut.writeVLong(field.getSumTotalTermFreq());
					
					mTermsOut.writeVLong(field.getSumDocFreq());
					mTermsOut.writeVInt(field.getDocCount());
					mIndexOut.writeVLong(field.getIndexStartPointer());
				}
			}
			
			writeTrailer(mTermsOut, dirStart);
			writeIndexTrailer(mIndexOut, indexDirStart);
			
		} catch (IOException ioe2) {
			ioe = ioe2;
		} finally {
			IOUtils.closeWhileHandlingException(ioe, mTermsOut, mIndexOut, mPostingsWriter);
		}
	}
	
}
