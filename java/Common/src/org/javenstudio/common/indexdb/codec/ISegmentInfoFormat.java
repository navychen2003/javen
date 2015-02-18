package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.ISegmentInfo;

/**
 * Expert: Controls the format of the 
 * {@link ISegmentInfo} (segment metadata file).
 * <p>
 * 
 * @see ISegmentInfo
 */
public interface ISegmentInfoFormat {

	public static interface Reader {

		/**
		 * Read {@link SegmentInfo} data from a directory.
		 * @param segmentName name of the segment to read
		 * @return infos instance to be populated with data
		 * @throws IOException
		 */
		public ISegmentInfo readSegmentInfo(String segmentName) 
				throws IOException;
		
	}
	
	public static interface Writer {

		/**
		 * Write {@link SegmentInfo} data. 
		 * @throws IOException
		 */
		public void writeSegmentInfo(ISegmentInfo info, IFieldInfos fis) 
				throws IOException;
		
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getCodecName();
	public String getSegmentInfoFileName(String segmentName);
	
	public ISegmentInfoFormat.Reader createReader(IDirectory dir) 
			throws IOException;
	
	public ISegmentInfoFormat.Writer createWriter(IDirectory dir) 
			throws IOException;
	
}
