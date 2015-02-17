package org.javenstudio.cocoka.net.http;

import org.apache.http.client.HttpClient;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;

public abstract class AsyncHttpClientTask extends HttpClientTask {

	private final AsyncTaskImpl mTask; 
	
	public AsyncHttpClientTask(HttpClient client, HttpExecutorFactory factory) {
		this(client, factory, null, null); 
	}
	
	public AsyncHttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			AdvancedQueueFactory queueFactory) {
		this(client, factory, queueFactory, null); 
	}
	
	public AsyncHttpClientTask(HttpClient client, HttpExecutorFactory factory, 
			AdvancedQueueFactory queueFactory, HttpCallbacks callbacks) {
		super(client, factory);
		mTask = new AsyncTaskImpl(
			queueFactory, 
			new HttpTask() {
				@Override
				public HttpTask.Result execute(HttpTask.Request request, HttpTask.Publisher publisher) {
					return doExecuteJob((HttpJobRequest)request, publisher); 
				}
				@Override
				public WorkerTask.WorkerInfo getWorkerInfo() { 
					return AsyncHttpClientTask.this.getWorkerInfo(); 
				}
			}, 
			(callbacks != null ? callbacks : this));
	}
	
	public abstract WorkerTask.WorkerInfo getWorkerInfo(); 
	
	@Override
	public final boolean cancel(boolean mayInterruptIfRunning) {
		return mTask.cancel(mayInterruptIfRunning); 
	}
	
	@Override
	protected void doExecuteRequest(HttpJobRequest[] jobs) { 
		mTask.execute(jobs); 
	}
	
}
