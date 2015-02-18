package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.hornet.codec.SegmentInfoFormat;
import org.javenstudio.hornet.codec.SegmentInfoReader;
import org.javenstudio.hornet.codec.SegmentInfoWriter;

/**
 * Lucene 4.0 Segment info format.
 * <p>
 * Files:
 * <ul>
 *   <li><tt>.si</tt>: Header, SegVersion, SegSize, IsCompoundFile, Diagnostics, Attributes, Files
 * </ul>
 * </p>
 * Data types:
 * <p>
 * <ul>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 *   <li>SegSize --&gt; {@link DataOutput#writeInt Int32}</li>
 *   <li>SegVersion --&gt; {@link DataOutput#writeString String}</li>
 *   <li>Files --&gt; {@link DataOutput#writeStringSet Set&lt;String&gt;}</li>
 *   <li>Diagnostics, Attributes --&gt; {@link DataOutput#writeStringStringMap Map&lt;String,String&gt;}</li>
 *   <li>IsCompoundFile --&gt; {@link DataOutput#writeByte Int8}</li>
 * </ul>
 * </p>
 * Field Descriptions:
 * <p>
 * <ul>
 *   <li>SegVersion is the code version that created the segment.</li>
 *   <li>SegSize is the number of documents contained in the segment index.</li>
 *   <li>IsCompoundFile records whether the segment is written as a compound file or
 *       not. If this is -1, the segment is not a compound file. If it is 1, the segment
 *       is a compound file.</li>
 *   <li>Checksum contains the CRC32 checksum of all bytes in the segments_N file up
 *       until the checksum. This is used to verify integrity of the file on opening the
 *       index.</li>
 *   <li>The Diagnostics Map is privately written by {@link IndexWriter}, as a debugging aid,
 *       for each segment it creates. It includes metadata like the current Lucene
 *       version, OS, Java version, why the segment was created (merge, flush,
 *       addIndexes), etc.</li>
 *   <li>Attributes: a key-value map of codec-private attributes.</li>
 *   <li>Files is a list of files referred to by this segment.</li>
 * </ul>
 * </p>
 * 
 * @see SegmentInfos
 */
final class StoredSegmentInfoFormat extends SegmentInfoFormat {

	private static final String CODEC_NAME = "Lucene40SegmentInfo";
	
	static final int VERSION_START = 0;
	static final int VERSION_CURRENT = VERSION_START;
	
	public StoredSegmentInfoFormat(IIndexFormat format) { 
		super(format);
	}
	
	@Override
	public String getCodecName() { return CODEC_NAME; }
	
	@Override
	public SegmentInfoReader createReader(IDirectory dir) throws IOException { 
		return new StoredSegmentInfoReader(this, dir);
	}
	
	@Override
	public SegmentInfoWriter createWriter(IDirectory dir) throws IOException { 
		return new StoredSegmentInfoWriter(this, dir);
	}
	
}
