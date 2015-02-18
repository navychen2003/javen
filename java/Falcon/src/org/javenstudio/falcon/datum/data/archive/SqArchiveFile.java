package org.javenstudio.falcon.datum.data.archive;

import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.NameData;
import org.javenstudio.falcon.datum.data.SqRootFile;

final class SqArchiveFile extends SqRootFile {

	public static SqRootFile create(SqArchiveRoot root, 
			NameData nameData) { 
		String contentType = MimeTypes.getContentTypeByExtension(
				nameData.getAttrs().getExtension().toString());
		
		if (contentType == null) 
			contentType = MimeType.TYPE_APPLICATION.getType();
		
		if (contentType.startsWith("image/")) 
			return new SqArchiveImage(root, nameData, contentType);
		
		if (contentType.startsWith("audio/"))
			return new SqArchiveMedia(root, nameData, contentType);
		
		if (contentType.startsWith("video/"))
			return new SqArchiveMedia(root, nameData, contentType);
		
		return new SqArchiveFile(root, nameData, contentType);
	}
	
	public SqArchiveFile(SqArchiveRoot root, 
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
