package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.ISegmentInfoFormat;

/**
 * Specifies an API for classes that can read {@link SegmentInfo} information.
 * 
 */
public abstract class SegmentInfoReader implements ISegmentInfoFormat.Reader {

	/**
	 * Read {@link SegmentInfo} data from a directory.
	 * @param segmentName name of the segment to read
	 * @return infos instance to be populated with data
	 * @throws IOException
	 */
	public abstract ISegmentInfo readSegmentInfo(String segmentName) 
			throws IOException;
	
}
