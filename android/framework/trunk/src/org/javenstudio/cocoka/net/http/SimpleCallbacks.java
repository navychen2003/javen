package org.javenstudio.cocoka.net.http;

public class SimpleCallbacks implements HttpCallbacks {

	@Override
	public void onPreExecute() {
		// do nothing
	}
	
	@Override 
	public void onJobStart(HttpTask.Request request) {
		// do nothing
	}
	
	@Override
	public void onProgressUpdate(HttpTask.Progress... progresses) {
		// do nothing
	}
	
	@Override 
	public void onJobFinished(HttpTask.Request request, HttpTask.Result result) {
		// do nothing
	}
	
	@Override
	public void onAllFinished() {
		// do nothing
	}
	
	@Override 
	public void onCancelled() {
		// do nothing
	}
	
}
