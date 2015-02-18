package org.javenstudio.falcon.datum.data.archive;

import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootMedia;

final class SqArchiveMedia extends SqRootMedia {

	public SqArchiveMedia(SqArchiveRoot root, 
			NameData nameData, String contentType) {
		super(root, nameData, contentType);
	}
	
	@Override
	public boolean canMove() { 
		return true;
	}
	
	@Override
	public boolean canDelete() { 
		return true;
	}
	
	@Override
	public boolean canWrite() { 
		return true;
	}
	
	@Override
	public boolean canCopy() { 
		return true;
	}
	
}
