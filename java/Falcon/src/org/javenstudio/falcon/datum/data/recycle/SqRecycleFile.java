package org.javenstudio.falcon.datum.data.recycle;

import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootFile;

final class SqRecycleFile extends SqRootFile {

	public static SqRootFile create(SqRecycleRoot root, 
			NameData nameData) { 
		String contentType = MimeTypes.getContentTypeByExtension(
				nameData.getAttrs().getExtension().toString());
		
		if (contentType == null) 
			contentType = MimeType.TYPE_APPLICATION.getType();
		
		if (contentType.startsWith("image/")) 
			return new SqRecycleImage(root, nameData, contentType);
		
		if (contentType.startsWith("audio/"))
			return new SqRecycleMedia(root, nameData, contentType);
		
		if (contentType.startsWith("video/"))
			return new SqRecycleMedia(root, nameData, contentType);
		
		return new SqRecycleFile(root, nameData, contentType);
	}
	
	public SqRecycleFile(SqRecycleRoot root, 
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
