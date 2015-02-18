package org.javenstudio.falcon.datum.data.upload;

import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootImage;

final class SqUploadImage extends SqRootImage {

	public SqUploadImage(SqUploadRoot root, 
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
