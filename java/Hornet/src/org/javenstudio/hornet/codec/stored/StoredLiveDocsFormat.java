package org.javenstudio.hornet.codec.stored;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.store.BitVector;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.MutableBits;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.LiveDocsFormat;

/**
 * Lucene 4.0 Live Documents Format.
 * <p>
 * <p>The .del file is optional, and only exists when a segment contains
 * deletions.</p>
 * <p>Although per-segment, this file is maintained exterior to compound segment
 * files.</p>
 * <p>Deletions (.del) --&gt; Format,Header,ByteCount,BitCount, Bits | DGaps (depending
 * on Format)</p>
 * <ul>
 *   <li>Format,ByteSize,BitCount --&gt; {@link DataOutput#writeInt Uint32}</li>
 *   <li>Bits --&gt; &lt;{@link DataOutput#writeByte Byte}&gt; <sup>ByteCount</sup></li>
 *   <li>DGaps --&gt; &lt;DGap,NonOnesByte&gt; <sup>NonzeroBytesCount</sup></li>
 *   <li>DGap --&gt; {@link DataOutput#writeVInt VInt}</li>
 *   <li>NonOnesByte --&gt; {@link DataOutput#writeByte Byte}</li>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 * </ul>
 * <p>Format is 1: indicates cleared DGaps.</p>
 * <p>ByteCount indicates the number of bytes in Bits. It is typically
 * (SegSize/8)+1.</p>
 * <p>BitCount indicates the number of bits that are currently set in Bits.</p>
 * <p>Bits contains one bit for each document indexed. When the bit corresponding
 * to a document number is cleared, that document is marked as deleted. Bit ordering
 * is from least to most significant. Thus, if Bits contains two bytes, 0x00 and
 * 0x02, then document 9 is marked as alive (not deleted).</p>
 * <p>DGaps represents sparse bit-vectors more efficiently than Bits. It is made
 * of DGaps on indexes of nonOnes bytes in Bits, and the nonOnes bytes themselves.
 * The number of nonOnes bytes in Bits (NonOnesBytesCount) is not stored.</p>
 * <p>For example, if there are 8000 bits and only bits 10,12,32 are cleared, DGaps
 * would be used:</p>
 * <p>(VInt) 1 , (byte) 20 , (VInt) 3 , (Byte) 1</p>
 */
final class StoredLiveDocsFormat extends LiveDocsFormat {
	static final Logger LOG = Logger.getLogger(StoredLiveDocsFormat.class);

	public StoredLiveDocsFormat(IIndexFormat format) { 
		super(format);
	}
	
	@Override
	public MutableBits newLiveDocs(int size) throws IOException {
		BitVector bitVector = new BitVector(size);
		bitVector.invertAll();
		return bitVector;
	}

	@Override
	public MutableBits newLiveDocs(Bits existing) throws IOException {
		final BitVector liveDocs = (BitVector) existing;
		return liveDocs.clone();
	}

	@Override
	public Bits readLiveDocs(IDirectory dir, ISegmentCommitInfo info) throws IOException {
		final String filename = getDeletesFileName(info.getSegmentInfo().getName(), info.getDelGen());
		final BitVector liveDocs = new BitVector(getContext(), dir, filename);
		
		assert liveDocs.count() == info.getSegmentInfo().getDocCount() - info.getDelCount():
			"liveDocs.count()=" + liveDocs.count() + " info.docCount=" + info.getSegmentInfo().getDocCount() + 
			" info.getDelCount()=" + info.getDelCount();
		assert liveDocs.length() == info.getSegmentInfo().getDocCount();
		
		return liveDocs;
	}

	@Override
	public void writeLiveDocs(IDirectory dir, MutableBits bits, ISegmentCommitInfo info, 
			int newDelCount) throws IOException {
		final String filename = getDeletesFileName(info.getSegmentInfo().getName(), info.getNextDelGen());
		final BitVector liveDocs = (BitVector) bits;
		
		assert liveDocs.count() == info.getSegmentInfo().getDocCount() - info.getDelCount() - newDelCount;
		assert liveDocs.length() == info.getSegmentInfo().getDocCount();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("writeLiveDocs: length=" + liveDocs.length() + " count=" + liveDocs.count() 
					+ " to file: " + filename);
		}
		
		liveDocs.write(getContext(), dir, filename);
	}

	@Override
	public void recordFiles(ISegmentCommitInfo info, Collection<String> files) throws IOException {
		if (info.hasDeletions()) 
			files.add(getDeletesFileName(info.getSegmentInfo().getName(), info.getDelGen()));
	}
	
}
