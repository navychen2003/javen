package org.javenstudio.falcon.datum.data.recycle;

import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootDir;

final class SqRecycleDir extends SqRootDir {

	public SqRecycleDir(SqRecycleRoot root, NameData nameData) {
		super(root, nameData);
	}
	
	@Override
	public String getContentType() { 
		return "application/x-recycle-dir";
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
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case DELETE: return true;
			case MOVE: return true;
			case COPY: return false;
			case UPLOAD: return false;
			case NEWFOLDER: return false;
			default: return false;
			}
		}
		return false;
	}
	
}
