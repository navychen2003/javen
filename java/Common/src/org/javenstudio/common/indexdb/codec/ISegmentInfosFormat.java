package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentInfos;

public interface ISegmentInfosFormat {

	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getSegmentInfosFileName(long generation);
	public String getSegmentsGenFileName();
	
	public ISegmentInfos readSegmentInfos(IDirectory dir) 
			throws CorruptIndexException, IOException;
	
	public ISegmentInfos readSegmentInfos(IDirectory dir, String segment) 
			throws CorruptIndexException, IOException;
	
	public ISegmentInfos readSegmentInfos(IDirectory dir, ISegmentInfos infos) 
			throws CorruptIndexException, IOException;
	
	public ISegmentInfos readSegmentInfos(IDirectory dir, ISegmentInfos infos, String segment) 
			throws CorruptIndexException, IOException;
	
	public ISegmentInfos newSegmentInfos(IDirectory dir);
	public ISegmentInfos newSegmentInfos(IDirectory dir, ISegmentInfos infos);
	
	/** 
	 * Call this to start a commit.  This writes the new
	 *  segments file, but writes an invalid checksum at the
	 *  end, so that it is not visible to readers.  Once this
	 *  is called you must call {@link #finishCommit} to complete
	 *  the commit or {@link #rollbackCommit} to abort it.
	 *  <p>
	 *  Note: {@link #changed()} should be called prior to this
	 *  method if changes have been made to this {@link SegmentInfos} instance
	 *  </p>  
	 */
	public void prepareCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
	public void rollbackCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
	public void finishCommit(IDirectory dir, ISegmentInfos infos) 
			throws IOException;
	
}
