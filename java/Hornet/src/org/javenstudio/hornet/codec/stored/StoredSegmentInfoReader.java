package org.javenstudio.hornet.codec.stored;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.SegmentInfoReader;

final class StoredSegmentInfoReader extends SegmentInfoReader {

	private final StoredSegmentInfoFormat mFormat;
	private final IDirectory mDirectory;
	
	public StoredSegmentInfoReader(StoredSegmentInfoFormat format, IDirectory dir) { 
		mFormat = format;
		mDirectory = dir;
	}
	
	@Override
	public SegmentInfo readSegmentInfo(String segmentName) throws IOException { 
	    final String fileName = mFormat.getSegmentInfoFileName(segmentName);
	    final IIndexInput input = mDirectory.openInput(mFormat.getContext(), fileName);
	    
	    boolean success = false;
	    try {
	    	CodecUtil.checkHeader(input, mFormat.getCodecName(), 
	    			StoredSegmentInfoFormat.VERSION_START,
	    			StoredSegmentInfoFormat.VERSION_CURRENT);
	    	
	    	final String version = input.readString();
	    	final int docCount = input.readInt();
	    	final boolean isCompoundFile = input.readByte() == SegmentInfo.YES;
	    	final Map<String,String> diagnostics = input.readStringStringMap();
	    	final Map<String,String> attributes = input.readStringStringMap();
	    	final Set<String> files = input.readStringSet();

	    	final SegmentInfo si = new SegmentInfo(mDirectory, 
	    			version, segmentName, docCount, isCompoundFile,
	    			diagnostics, Collections.unmodifiableMap(attributes));
	    	si.setFileNames(files);

	    	success = true;
	    	return si;
	    } finally {
	    	if (!success) 
	    		IOUtils.closeWhileHandlingException(input);
	    	else 
	    		input.close();
	    }
	}
	
}
