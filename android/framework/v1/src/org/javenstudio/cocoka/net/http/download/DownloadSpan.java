package org.javenstudio.cocoka.net.http.download;

import org.javenstudio.cocoka.util.MimeType;

public abstract class DownloadSpan extends DownloadSource {

	public DownloadSpan(String source) {
		super(source); 
	}
	
	public MimeType getMimeType() {
		return MimeType.TYPE_APPLICATION; 
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DownloadSpan)) 
			return false; 
		
		DownloadSpan other = (DownloadSpan)obj; 
		
		if (!super.equals(other)) 
			return false; 

		return true; 
	}
	
}
