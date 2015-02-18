package org.javenstudio.hornet.codec.stored;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.store.ChecksumIndexOutput;

final class StoredSegmentInfos extends SegmentInfos {
  
	// Only non-null after prepareCommit has been called and
	// before finishCommit is called
	private ChecksumIndexOutput mPendingOutput = null;
	
	public StoredSegmentInfos(IDirectory directory) { 
		super(directory);
	}
  
	final ChecksumIndexOutput getPendingOutput() { 
		return mPendingOutput;
	}
	
	final void setPendingOutput(ChecksumIndexOutput output) { 
		mPendingOutput = output;
	}
	
	@Override
	public StoredSegmentInfos clone() { 
		StoredSegmentInfos sis = (StoredSegmentInfos)super.clone();
		sis.mPendingOutput = null;
		
		return sis;
	}
	
}
