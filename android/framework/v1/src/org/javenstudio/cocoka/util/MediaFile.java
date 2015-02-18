package org.javenstudio.cocoka.util;

public interface MediaFile extends ContentFile {

	public MimeType getMimeType(); 
	public MediaInfo getMediaInfo(); 
	
	public void recycle(); 
	public boolean isRecycled(); 
	
}
