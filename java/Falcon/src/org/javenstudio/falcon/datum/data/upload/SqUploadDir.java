package org.javenstudio.falcon.datum.data.upload;

import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootDir;

final class SqUploadDir extends SqRootDir {

	public SqUploadDir(SqUploadRoot root, NameData data) {
		super(root, data);
	}
	
	@Override
	public String getContentType() { 
		return "application/x-upload-dir";
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
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case DELETE: return true;
			case UPLOAD: return true;
			case NEWFOLDER: return true;
			case MOVE: return true;
			case COPY: return true;
			default: return false;
			}
		}
		return false;
	}
	
}
