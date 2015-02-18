package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.codec.ISegmentInfoFormat;

/**
 * Specifies an API for classes that can write out {@link SegmentInfo} data.
 * 
 */
public abstract class SegmentInfoWriter implements ISegmentInfoFormat.Writer {

	/**
	 * Write {@link SegmentInfo} data. 
	 * @throws IOException
	 */
	public abstract void writeSegmentInfo(ISegmentInfo info, IFieldInfos fis) 
			throws IOException;
	
}
