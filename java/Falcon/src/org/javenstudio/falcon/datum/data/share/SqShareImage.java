package org.javenstudio.falcon.datum.data.share;

import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootImage;

final class SqShareImage extends SqRootImage {

	public SqShareImage(SqShareRoot root, 
			NameData data, String contentType) {
		super(root, data, contentType);
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
