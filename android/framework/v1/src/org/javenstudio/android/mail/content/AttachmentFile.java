package org.javenstudio.android.mail.content;

import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.ContentFile;

public interface AttachmentFile extends ContentFile {

	public String getContentUri(); 
	
	public StorageFile getFile(); 
	
	public boolean deleteFile(); 
	
}
