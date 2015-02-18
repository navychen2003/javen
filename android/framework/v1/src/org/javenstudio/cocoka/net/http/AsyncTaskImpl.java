package org.javenstudio.cocoka.net.http;

import org.javenstudio.cocoka.worker.AsyncTask;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;

import android.os.Handler;

public class AsyncTaskImpl extends AsyncTask<HttpTask.Request, HttpTask.Progress, Integer> {

	public static interface Callbacks {
		public void onPreExecute(); 
		public void onJobStart(HttpTask.Request request); 
		public void onProgressUpdate(HttpTask.Progress... progresses); 
		public void onJobFinished(HttpTask.Request request, HttpTask.Result result); 
		public void onAllFinished(); 
		public void onCancelled(); 
	}
	
	private static final Handler sHandler = new Handler();
	
	private final HttpTask mRunner; 
	private final Callbacks mCallbacks; 
	
	public AsyncTaskImpl(AdvancedQueueFactory factory, HttpTask runner, Callbacks callbacks) {
		super(factory);
		mRunner = runner; 
		mCallbacks = callbacks; 
	}
	
	@Override
	protected WorkerTask.WorkerInfo getWorkerInfo() { 
		return mRunner != null ? mRunner.getWorkerInfo() : null; 
	}
	
	@Override
	protected Integer doInBackground(HttpTask.Request... params) {
		int count = 0; 
		
		if (params != null && params.length > 0) {
			final HttpTask.Progress[] progresses = new HttpTask.Progress[params.length]; 
			
			for (int i=0; i < params.length; i++) {
				final int index = i; 
				HttpTask.Request request = params[index]; 
				onPostJobStart(request); 
				HttpTask.Result result = mRunner.execute(request, new HttpTask.Publisher() {
					public boolean isAborted() { return isCancelled(); } 
					public void publish(HttpTask.Progress progress) {
						publishProgresses(progresses, index, progress); 
					}
				}); 
				onPostJobResult(request, result); 
				count ++; 
			}
		}
		
		return count; 
	}
	
	private void publishProgresses(HttpTask.Progress[] progresses, int index, HttpTask.Progress progress) {
		progresses[index] = progress; 
		publishProgress(progresses); 
	}
	
	@Override
	protected void onProgressUpdate(HttpTask.Progress... progresses) {
		if (mCallbacks != null) 
			mCallbacks.onProgressUpdate(progresses); 
	}
	
	@Override
	protected void onPreExecute() {
		if (mCallbacks != null) { 
			sHandler.post(new Runnable() {
				public void run() {
					mCallbacks.onPreExecute(); 
				}
			}); 
		}
	}
	
	protected void onPostJobStart(final HttpTask.Request request) {
		if (mCallbacks != null) {
			sHandler.post(new Runnable() {
				public void run() {
					mCallbacks.onJobStart(request); 
				}
			}); 
		}
	}
	
	protected void onPostJobResult(final HttpTask.Request request, final HttpTask.Result result) {
		if (mCallbacks != null) { 
			sHandler.post(new Runnable() {
				public void run() {
					mCallbacks.onJobFinished(request, result); 
				}
			}); 
		}
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (mCallbacks != null) { 
			sHandler.post(new Runnable() {
				public void run() {
					mCallbacks.onAllFinished(); 
				}
			}); 
		}
	}
	
	@Override
	protected void onCancelled() {
		if (mCallbacks != null) 
			mCallbacks.onCancelled(); 
	}
	
}
