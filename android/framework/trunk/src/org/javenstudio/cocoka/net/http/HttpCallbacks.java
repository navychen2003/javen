package org.javenstudio.cocoka.net.http;

public interface HttpCallbacks {

	public void onPreExecute(); 
	public void onJobStart(HttpTask.Request request); 
	public void onProgressUpdate(HttpTask.Progress... progresses); 
	public void onJobFinished(HttpTask.Request request, HttpTask.Result result); 
	public void onAllFinished(); 
	public void onCancelled(); 
	
}
