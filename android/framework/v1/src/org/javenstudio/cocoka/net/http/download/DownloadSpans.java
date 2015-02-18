package org.javenstudio.cocoka.net.http.download;

public interface DownloadSpans {

	public void clearDownloadSpans(); 
	public void addDownloadSpan(DownloadSpan span); 
	public DownloadSpan[] getDownloadSpans(String source); 
	public String[] getDownloadSources(); 
	
}
