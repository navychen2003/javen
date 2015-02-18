package org.javenstudio.cocoka.net.http.download;

public class DownloadSource {

	protected final String mSource; 
	
	public DownloadSource(String source) {
		mSource = source; 
	}
	
	public String getSource() { return mSource; } 
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DownloadSource)) 
			return false; 
		
		DownloadSource other = (DownloadSource)obj; 
		
		if (!mSource.equals(other.mSource)) 
			return false; 

		return true; 
	}
	
}
