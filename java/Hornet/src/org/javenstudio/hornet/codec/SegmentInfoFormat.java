package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentInfoFormat;

/**
 * Expert: Controls the format of the 
 * {@link SegmentInfo} (segment metadata file).
 * <p>
 * 
 * @see SegmentInfo
 */
public abstract class SegmentInfoFormat implements ISegmentInfoFormat {

	public final static String SI_EXTENSION = "si";
	
	private final IIndexFormat mFormat;
	
	protected SegmentInfoFormat(IIndexFormat format) { 
		mFormat = format;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext(); 
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		return mFormat; 
	}
	
	@Override
	public String getSegmentInfoFileName(String segmentName) { 
		return getContext().getSegmentFileName(segmentName, SI_EXTENSION);
	}
	
	public abstract String getCodecName();
	
	/** 
	 * Returns a {@link SegmentInfoReader} to read segment info file
	 *  from the index 
	 */
	public abstract ISegmentInfoFormat.Reader createReader(IDirectory dir) 
			throws IOException;
	
	/** 
	 * Returns a {@link SegmentInfoWriter} to write segment info file
	 *  to the index 
	 */
	public abstract ISegmentInfoFormat.Writer createWriter(IDirectory dir) 
			throws IOException;
	
}
