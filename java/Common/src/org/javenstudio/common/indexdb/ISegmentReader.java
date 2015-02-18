package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;

public interface ISegmentReader extends IAtomicReader {
	
	public IFieldInfos getFieldInfos();
	public IFieldsFormat.Reader getFieldsReader();
	public ITermVectorsFormat.Reader getTermVectorsReader();

	/** Expert: adds a CoreClosedListener to this reader's shared core */
	public void addSegmentClosedListener(ISegmentClosedListener listener);
	
	/** Expert: removes a CoreClosedListener from this reader's shared core */
	public void removeSegmentClosedListener(ISegmentClosedListener listener);
	
	public void increaseRef();
	public void decreaseRef() throws IOException;
	
	/**
	 * Return the SegmentInfoPerCommit of the segment this reader is reading.
	 */
	public ISegmentCommitInfo getCommitInfo();
	
}
