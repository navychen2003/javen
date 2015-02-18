package org.javenstudio.falcon.datum.data.recycle;

import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootMedia;

final class SqRecycleMedia extends SqRootMedia {

	public SqRecycleMedia(SqRecycleRoot root, 
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
		return false;
	}
	
	@Override
	public boolean canCopy() { 
		return false;
	}
	
}
