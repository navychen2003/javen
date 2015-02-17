package org.javenstudio.cocoka.storage;

import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.MediaFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;

public abstract class BaseMediaFile extends StorageFile implements MediaFile {

	//private MediaInfo mMediaInfo = null; 
	private MimeTypes.FileTypeInfo mFileTypeInfo = null;
	
	public BaseMediaFile(Storage store, IFile file) {
		super(store, file); 
	}
	
	@Override 
	public abstract MimeType getMimeType(); 
	
	@Override 
	public String getContentType() { 
		MimeTypes.FileTypeInfo info = mFileTypeInfo;
		if (info == null) {
			info = MimeTypes.lookupFileType(getFileName());
			mFileTypeInfo = info;
		}
		if (info != null) 
			return info.getContentType();
		
		return getMimeType().getType(); 
	}
	
	//@Override 
	//public void recycle() {
	//	// do nothing
	//}
	
	//@Override 
	//public boolean isRecycled() {
	//	return true; 
	//}
	
}
