package org.javenstudio.falcon.datum.data.share;

import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootFile;

final class SqShareFile extends SqRootFile {

	public static SqRootFile create(SqShareRoot root, 
			NameData data) { 
		String contentType = MimeTypes.getContentTypeByExtension(
				data.getAttrs().getExtension().toString());
		
		if (contentType == null) 
			contentType = MimeType.TYPE_APPLICATION.getType();
		
		if (contentType.startsWith("image/")) 
			return new SqShareImage(root, data, contentType);
		
		if (contentType.startsWith("audio/"))
			return new SqShareMedia(root, data, contentType);
		
		if (contentType.startsWith("video/"))
			return new SqShareMedia(root, data, contentType);
		
		return new SqShareFile(root, data, contentType);
	}
	
	public SqShareFile(SqShareRoot root, 
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
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case DELETE: return true;
			case MOVE: return true;
			case COPY: return true;
			case UPLOAD: return false;
			case NEWFOLDER: return false;
			default: return false;
			}
		}
		return false;
	}
	
}
