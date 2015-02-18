package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.SegmentInfoWriter;

/**
 * Lucene 4.0 implementation of {@link SegmentInfoWriter}.
 * 
 * @see StoredSegmentInfoFormat
 */
final class StoredSegmentInfoWriter extends SegmentInfoWriter {

	private final StoredSegmentInfoFormat mFormat;
	private final IDirectory mDirectory;
	
	public StoredSegmentInfoWriter(StoredSegmentInfoFormat format, IDirectory dir) { 
		mFormat = format;
		mDirectory = dir;
	}
	
	/** Save a single segment's info. */
	@Override
	public void writeSegmentInfo(ISegmentInfo info, IFieldInfos fis) 
			throws IOException { 
	    final String fileName = mFormat.getSegmentInfoFileName(info.getName());
	    info.addFileName(fileName);

	    final IIndexOutput output = mDirectory.createOutput(
	    		mFormat.getContext(), fileName);

	    boolean success = false;
	    try {
	    	CodecUtil.writeHeader(output, mFormat.getCodecName(), 
	    			StoredSegmentInfoFormat.VERSION_CURRENT);
	      
	    	// Write the Lucene version that created this segment, since 3.1
	    	output.writeString(info.getVersion());
	    	output.writeInt(info.getDocCount());

	    	output.writeByte((byte) (info.getUseCompoundFile() ? SegmentInfo.YES : SegmentInfo.NO));
	    	output.writeStringStringMap(info.getDiagnostics());
	    	output.writeStringStringMap(info.getAttributes());
	    	output.writeStringSet(info.getFileNames());

	    	success = true;
	    } finally {
	    	if (!success) {
	    		IOUtils.closeWhileHandlingException(output);
	    		mDirectory.deleteFile(fileName);
	    	} else {
	    		output.close();
	    	}
	    }
	}
	
}
