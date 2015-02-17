package org.javenstudio.cocoka.net.http.fetch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.cocoka.net.http.AsyncHttpClientTask;
import org.javenstudio.cocoka.net.http.HttpClientTask;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.HttpExecutor;
import org.javenstudio.cocoka.net.http.HttpExecutorFactory;
import org.javenstudio.cocoka.net.http.HttpResult;
import org.javenstudio.cocoka.net.http.HttpTask;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.cocoka.net.http.SimpleCallbacks;
import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.net.http.SimpleHttpClientTask;
import org.javenstudio.cocoka.net.http.SimpleHttpExecutor;
import org.javenstudio.cocoka.net.http.SimpleHttpResult;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.TempStorageFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.common.util.Logger;

public final class Fetcher {
	private static Logger LOG = Logger.getLogger(Fetcher.class);

	public static final String DEFAULT_USER_AGENT = "Fetcher";
	
	private static class CacheFileResult implements HttpTask.Result {
		private final StorageFile mFile; 
		private final HttpResult mResult;
		
		public CacheFileResult(HttpResult result, StorageFile file) {
			mFile = file; 
			mResult = result;
		}
		
		public Object getData() { return mFile; }
		public HttpResult getHttpResult() { return mResult; }
	}
	
	private static class WriterResult implements HttpTask.Result {
		private final Writer mWriter; 
		private final HttpResult mResult;
		
		public WriterResult(HttpResult result, Writer writer) {
			mWriter = writer; 
			mResult = result;
		}
		
		public Object getData() { return mWriter; }
		public HttpResult getHttpResult() { return mResult; }
	}
	
	private static class OutputStreamResult implements HttpTask.Result {
		private final OutputStream mOutput; 
		private final HttpResult mResult;
		
		public OutputStreamResult(HttpResult result, OutputStream output) {
			mOutput = output; 
			mResult = result;
		}
		
		public Object getData() { return mOutput; }
		public HttpResult getHttpResult() { return mResult; }
	}
	
	private final FetchManager mManager; 
	private final FetchTask mTask; 
	private final WorkerTask.WorkerInfo mWorkerInfo; 
	private final ResponseChecker mResponseChecker; 
	private HttpClientTask mClientTask = null; 
	private StorageFile mCacheFile = null; 
	private volatile FetchContext mMetrics; 
	private String mUserAgent = null; 
	private long mFetchedLength = 0; 
	private boolean mAppendMode = false; 
	private boolean mStarted = false; 
	private boolean mClosed = false; 
	
	Fetcher(FetchManager manager, FetchTask task) {
		mManager = manager; 
		mTask = task; 
		mUserAgent = DEFAULT_USER_AGENT; 
		
		String name = null; 
		FetchCallback callback = task.getCallback(); 
		if (callback != null) { 
			mResponseChecker = callback.getResponseChecker(); 
			name = callback.getFetchName(); 
			
			String agent = callback.getFetchAgent(); 
			if (agent != null && agent.length() > 0) 
				mUserAgent = agent; 
			
		} else { 
			mResponseChecker = new FetchResponseChecker(); 
		}
		
		if (name == null || name.length() == 0) 
			name = getClass().getSimpleName(); 
		
		final String workerName = name; 
		mWorkerInfo = new WorkerTask.WorkerInfo() { 
				public String getName() { 
					return workerName; // + ":" + mTask.getSource(); 
				}
				public Object getData() { 
					return Fetcher.this; 
				}
			};
	}
	
	public String getUserAgent() {
		return mUserAgent; 
	}
	
	private class SimpleBytesHttpExecutor extends SimpleHttpExecutor.BytesHttpExecutor {
		public SimpleBytesHttpExecutor(ResponseChecker checker) { 
			super(checker); 
		}
		
		@Override 
		protected HttpTask.Result readEntity(HttpResponse response, 
			final HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
			return readBytesHttpEntity(this, response, entity, publisher); 
		}
		
		@Override 
		protected HttpTask.Progress createProgress(int contentLength, 
				int fetchedLength, boolean aborted) {
			return createProgressContext(contentLength, fetchedLength, aborted); 
		}
	}
	
	private class SimpleStringHttpExecutor extends SimpleHttpExecutor.StringHttpExecutor {
		public SimpleStringHttpExecutor(ResponseChecker checker) { 
			super(checker); 
		}
		
		@Override 
		protected HttpTask.Result readEntity(HttpResponse response, 
			final HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
			return readStringHttpEntity(this, response, entity, publisher); 
		}
		
		@Override 
		protected HttpTask.Progress createProgress(int contentLength, 
				int fetchedLength, boolean aborted) {
			return createProgressContext(contentLength, fetchedLength, aborted); 
		}
	}
	
	private FetchContext createProgressContext(int contentLength, 
			int fetchedLength, boolean aborted) { 
		return FetchContext.newFetching(
				(int)(contentLength + mFetchedLength), 
				(int)(fetchedLength + mFetchedLength), aborted); 
	}
	
	public final void start(boolean async) {
		if (!mTask.isScheduled()) return; 
		
		synchronized (this) { 
			startFetch(async); 
		}
	}
	
	public final boolean stop() { 
		synchronized (this) { 
			HttpClientTask task = mClientTask; 
			if (task != null) 
				return task.cancel(true); 
			else
				return false; 
		}
	}
	
	private void startFetch(boolean async) { 
		if (mStarted || mClosed) return; 
		mStarted = true; 
		
		final String source = mTask.getSource(); 
		final FetchCallback callback = mTask.getCallback();  
		long tempFileLength = 0; 
		
		if (callback != null) { 
			try {
				if (!callback.isRefetchContent() || !callback.isFetchContent()) { 
					if (LOG.isDebugEnabled())
						LOG.debug("open saved file for " + source);
					
					boolean finished = onFetchJobFinished(openSaveCacheFile(), 
							callback.getSavedExpireTime());
					
					if (!finished && !callback.isAutoFetch()) { 
						mTask.setFinished(null, null);
						finished = true;
					}
					
					if (finished || !callback.isFetchContent()) {
						onFetchFinished();
						close();
						return;
					}
				}
			} catch (Throwable e) { 
				mTask.setFinished(null, e); 
				onFetchExceptionCached(e); 
				onFetchFinished(); 
				close(); 
				
				return; 
			}
			
		} else {
			// fetched data not return to callback but save to temp cache
			try { 
				StorageFile file = openTempCacheFile(); 
				if (file == null) 
					throw new IOException("cannot open cache file for " + mTask.getSource()); 
				
				final IFileSystem fs = file.getStorage().getFileSystem(); 
				
				if (file instanceof TempStorageFile) { 
					TempStorageFile tempFile = (TempStorageFile)file; 
					if (fs.exists(tempFile.getStorageFile().getFile())) 
						throw new IOException("file: " + tempFile.getStorageFile().getFilePath() + " already fetched"); 
					
					if (fs.exists(tempFile.getFile())) { 
						tempFileLength = tempFile.getFile().length(); 
						mAppendMode = true; 
					}
				} else if (fs.exists(file.getFile())) { 
					throw new IOException("file: " + file.getFilePath() + " already fetched"); 
				}
				
				mCacheFile = file; 
				
			} catch (Throwable e) { 
				mTask.setFinished(null, e); 
				onFetchExceptionCached(e); 
				onFetchFinished(); 
				close(); 
				
				return; 
			}
		}
		
		final SimpleHttpClient client = SimpleHttpClient.newInstance(getUserAgent(), true); 
		mFetchedLength = tempFileLength; 
		
		final HttpExecutorFactory executorFactory = new HttpExecutorFactory() {
				@Override
				public HttpExecutor createExecutor() {
					if (callback != null && mTask.getMimeType() == MimeType.TYPE_TEXT)
						return new SimpleStringHttpExecutor(mResponseChecker); 
					else 
						return new SimpleBytesHttpExecutor(mResponseChecker); 
				}
			};
		
		final SimpleCallbacks taskCallbacks = new SimpleCallbacks() {
				@Override
				public void onJobStart(HttpTask.Request request) {
					onFetchProgressUpdated(FetchContext.newConnecting(source)); 
				}
				@Override 
				public void onProgressUpdate(HttpTask.Progress... progresses) {
					onFetchJobUpdated(progresses); 
				}
				@Override 
				public void onJobFinished(HttpTask.Request request, HttpTask.Result result) {
					onFetchJobFinished(request, result); 
				}
				@Override
				public void onAllFinished() { 
					client.close(); 
					onFetchFinished(); 
					Fetcher.this.close(); 
				}
				@Override 
				public void onCancelled() {
					client.close(); 
					onFetchCanceled(); 
					Fetcher.this.close(); 
				}
			};
		
		if (async) {
			mClientTask = new AsyncHttpClientTask(client, executorFactory, 
					mManager.getAdvancedQueueFactory(), taskCallbacks) {
					@Override 
					protected HttpJobRequest createJobRequest(HttpUriRequest request) { 
						if (mFetchedLength > 0 && request != null) 
							request.addHeader("Range", "bytes="+mFetchedLength+"-"); 
						return super.createJobRequest(request); 
					}
					@Override 
					public WorkerTask.WorkerInfo getWorkerInfo() { 
						return mWorkerInfo; 
					}
				};
		} else { 
			mClientTask = new SimpleHttpClientTask(client, executorFactory, 
					taskCallbacks);
		}
		
		if (mTask.setStarted(this)) { 
			final String fetchSource = mTask.getFetchSource(); 
			final HttpUriRequest request = mTask.getHttpUriRequest(); 
			
			if (callback != null) 
				callback.onStartFetching(fetchSource);
			
			try {
				if (request != null) { 
					if (LOG.isDebugEnabled())
						LOG.debug("fetching " + fetchSource + " with input request"); 
					
					mManager.addFetching(this);
					mClientTask.executeRequest(callback, request); 
					
				} else { 
					if (LOG.isDebugEnabled())
						LOG.debug("fetching " + fetchSource); 
					
					mManager.addFetching(this);
					mClientTask.executeRequest(callback, fetchSource); 
				}
			} catch (Throwable e) { 
				mTask.setFinished(null, e); 
				onFetchExceptionCached(e); 
				onFetchFinished(); 
				close(); 
				
				return;
			}
		}
	}
	
	private HttpTask.Result readStringHttpEntity(SimpleStringHttpExecutor executor, 
			HttpResponse response, HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
		FetchCallback callback = mTask.getCallback(); 
		
		if (callback != null) { 
			Writer writer = callback.getWriter(entity); 
			if (writer != null) {
				try { 
					SimpleHttpExecutor.saveStringEntity(executor, entity, publisher, writer, 
							callback.getDefaultContentCharset()); 
				} finally { 
					writer.flush(); 
				}
				return new WriterResult(new SimpleHttpResult(response, entity), writer); 
			}
			
			SimpleHttpExecutor.StringResult result = SimpleHttpExecutor.readString(
					executor, response, entity, publisher, callback.getDefaultContentCharset()); 
			
			if (result != null && callback.isSaveContent()) { 
				StorageFile file = openSaveCacheFile();
				String content = result.getData();
				//String charset = result.mContentCharset;
				
				if (file != null && content != null) { 
					OutputStream output = file.createFile();
					Writer wr = new OutputStreamWriter(output); //, charset);
					try { 
						wr.write(content); 
						callback.onContentSaved(file);
						
						if (LOG.isDebugEnabled())
							LOG.debug("saved " + content.length() + " length to " + file.getFile()); 
					} finally { 
						wr.flush();
						wr.close();
						output.close();
					}
				}
			}
			
			return result;
		}
		
		StorageFile file = mCacheFile; 
		if (file != null) { 
			OutputStream output = mAppendMode ? file.appendFile() : file.createFile(); 
			Writer writer = new OutputStreamWriter(output); 
			try { 
				int size = SimpleHttpExecutor.saveStringEntity(executor, entity, publisher, writer); 
				
				if (LOG.isDebugEnabled())
					LOG.debug((mAppendMode?"append":"saved") + " " + size + " length to " + file.getFile()); 
			} finally { 
				writer.flush(); 
				output.flush(); 
				output.close(); 
			}
			return new CacheFileResult(new SimpleHttpResult(response, entity), file); 
		}
		
		throw new IOException("cache file not opened for " + mTask.getSource()); 
	}
	
	private HttpTask.Result readBytesHttpEntity(SimpleBytesHttpExecutor executor, 
			HttpResponse response, HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
		FetchCallback callback = mTask.getCallback(); 
		
		if (callback != null) { 
			OutputStream writer = callback.getOutputStream(entity); 
			if (writer != null) {
				try {
					SimpleHttpExecutor.saveByteArrayEntity(executor, entity, publisher, writer); 
				} finally { 
					writer.flush(); 
				}
				return new OutputStreamResult(new SimpleHttpResult(response, entity), writer); 
			}
			
			SimpleHttpExecutor.BytesResult result = 
					SimpleHttpExecutor.readByteArray(executor, response, entity, publisher); 
			
			if (result != null && callback.isSaveContent()) { 
				StorageFile file = openSaveCacheFile();
				byte[] content = result.getData();
				
				if (file != null && content != null) { 
					OutputStream output = file.createFile();
					try {
						output.write(content, 0, content.length);
						callback.onContentSaved(file);
						
						if (LOG.isDebugEnabled())
							LOG.debug("saved " + content.length + " bytes to " + file.getFile()); 
					} finally { 
						output.flush();
						output.close();
					}
				}
			}
			
			return result;
		} 
		
		StorageFile file = mCacheFile; 
		if (file != null) { 
			OutputStream output = mAppendMode ? file.appendFile() : file.createFile(); 
			try { 
				int size = 0; 
				if (output != null) 
					size = SimpleHttpExecutor.saveByteArrayEntity(executor, entity, publisher, output); 
				
				if (LOG.isDebugEnabled())
					LOG.debug((mAppendMode?"append":"saved") + " " + size + " bytes to " + file.getFile()); 
			} finally { 
				if (output != null) { 
					output.flush(); 
					output.close(); 
				}
			}
			return new CacheFileResult(new SimpleHttpResult(response, entity), file); 
		}
		
		throw new IOException("cache file not opened for " + mTask.getSource()); 
	}
	
	private void onFetchJobUpdated(HttpTask.Progress... progresses) {
		if (progresses != null) {
			for (int i=0; i < progresses.length; i++) { 
				HttpTask.Progress p = progresses[i]; 
				if (p != null) 
					onFetchProgressUpdated(p); 
			}
		}
	}
	
	private boolean onFetchJobFinished(HttpTask.Request request, HttpTask.Result result) {
		if (result != null) { 
			FetchCallback callback = mTask.getCallback(); 
			StorageFile contentFile = null;
			if (callback != null)
				contentFile = (StorageFile)callback.getSavedContent();
			
			if (result instanceof HttpException) {
				HttpException e = (HttpException)result; 
				e.setLocation(mTask.getFetchSource()); 
				mTask.setFinished(contentFile, e); 
				
			} else if (result instanceof CacheFileResult) { 
				mTask.setFinished((StorageFile)result.getData(), null); 
				
			} else 
				mTask.setFinished(contentFile, null); 
			
			if (callback != null) { 
				if (result instanceof HttpException) 
					callback.onContentFetched(null, (HttpException)result); 
				else 
					callback.onContentFetched(result.getData(), null); 
			}
			
			return true;
		}
		
		return false;
	}
	
	private boolean onFetchJobFinished(StorageFile file, long expireMillis) {
		if (file != null) { 
			Object content = loadSaveCacheFile(file, expireMillis);
			if (content == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("empty content for " + file.getFile());
				
				return false;
			}
			
			FetchCallback callback = mTask.getCallback(); 
			
			mTask.setFinished(null, null); 
			if (callback != null) 
				callback.onContentFetched(content, null);
			
			return true;
		}
		
		return false;
	}
	
	protected void onFetchProgressUpdated(HttpTask.Progress progress) {
		if (progress == null) return; 
		
		if (progress instanceof FetchContext) { 
			FetchContext context = (FetchContext)progress; 
			mMetrics = context; 
			mTask.updateMetrics(context); 
		}
	}
	
	protected void onFetchFinished() {
		Throwable exception = mTask.getException(); 
		FetchContext context = exception != null ? 
				FetchContext.newFailed(mTask.getSource(), exception) : 
				FetchContext.newFinished(mTask.getSource(), mTask.getCacheFile()); 
		
		mMetrics = context; 
		mTask.updateMetrics(context); 
		
		FetchCallback callback = mTask.getCallback(); 
		if (callback != null) 
			callback.onFinished(); 
		
		mManager.onFetcherFinished(this); 
	}
	
	protected void onFetchExceptionCached(Throwable exception) { 
		FetchContext context = FetchContext.newFailed(mTask.getSource(), exception); 
		mMetrics = context; 
		mTask.updateMetrics(context); 
		
		FetchCallback callback = mTask.getCallback(); 
		if (callback != null) 
			callback.onFetchException(exception);
	}
	
	protected void onFetchCanceled() { 
		mTask.setCanceled(); 
		mManager.onFetcherCanceled(this); 
	}
	
	public FetchContext getMetricsContext() {
		return mMetrics; 
	}
	
	public FetchTask getTask() { 
		return mTask; 
	}
	
	public void close() { 
		mStarted = false; 
		mClosed = true; 
		mManager.closeFetcher(this); 
	}
	
	public boolean isClosed() { 
		return mClosed; 
	}
	
	public boolean isStarted() { 
		return mStarted; 
	}
	
	private StorageFile openTempCacheFile() throws IOException {
		final FetchCache cache = mTask.getFetchCache();
		final String source = mTask.getSource(); 
		final MimeType type = mTask.getMimeType(); 
		
		if (cache == null) 
			return null;
		
		StorageFile file = cache.openTempFile(source, type); 
		
		if (LOG.isDebugEnabled())
			LOG.debug("open temp file: " + file);
		
		return file;
	}
	
	private StorageFile openSaveCacheFile() throws IOException {
		final FetchCache cache = mTask.getFetchCache();
		final String source = mTask.getSource(); 
		final MimeType type = mTask.getMimeType(); 
		
		if (cache == null) 
			return null;
		
		StorageFile file = cache.openSaveFile(source, type); 
		
		if (LOG.isDebugEnabled())
			LOG.debug("open cache file: " + file);
		
		return file;
	}
	
	private Object loadSaveCacheFile(StorageFile file, long expireMillis) { 
		try {
			if (file == null) { 
				if (LOG.isDebugEnabled())
					LOG.debug("saved cache file is null");
				
				return null;
			}
			
			if (expireMillis > 0 && file.getFile().exists()) { 
				long time = System.currentTimeMillis() - file.getFile().lastModified();
				if (expireMillis < time) { 
					if (LOG.isDebugEnabled())
						LOG.debug("saved cache file expired " + time + "ms > " + expireMillis + "ms");
					
					return null;
				}
			}
			
			final FetchCache cache = mTask.getFetchCache();
			if (cache != null)
				return cache.loadSaveFile(file);
			
			if (LOG.isDebugEnabled())
				LOG.debug("no FetchCache for Task: " + mTask.getSource());
			
		} catch (IOException e) { 
			if (LOG.isDebugEnabled())
				LOG.debug("load saved cachefile error", e);
		}
		
		return null;
	}
	
}
