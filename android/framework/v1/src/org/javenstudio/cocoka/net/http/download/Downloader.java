package org.javenstudio.cocoka.net.http.download;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.Constants;
import org.javenstudio.cocoka.net.http.HttpClientTask;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.HttpExecutor;
import org.javenstudio.cocoka.net.http.HttpExecutorFactory;
import org.javenstudio.cocoka.net.http.HttpTask;
import org.javenstudio.cocoka.net.http.ResponseChecker;
import org.javenstudio.cocoka.net.http.SimpleCallbacks;
import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.net.http.SimpleHttpExecutor;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.TempFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.common.util.Logger;

public final class Downloader {
	private static Logger LOG = Logger.getLogger(Downloader.class);

	private static class CacheFileResult implements HttpTask.Result {
		private final StorageFile mFile; 
		public CacheFileResult(StorageFile file) {
			mFile = file; 
		}
		public Object getData() {
			return mFile; 
		}
	}
	
	private static class WriterResult implements HttpTask.Result {
		private final Writer mWriter; 
		public WriterResult(Writer writer) {
			mWriter = writer; 
		}
		public Object getData() {
			return mWriter; 
		}
	}
	
	private static class OutputStreamResult implements HttpTask.Result {
		private final OutputStream mOutput; 
		public OutputStreamResult(OutputStream output) {
			mOutput = output; 
		}
		public Object getData() {
			return mOutput; 
		}
	}
	
	private final DownloadManager mManager; 
	private final DownloadTask mTask; 
	private final WorkerTask.WorkerInfo mWorkerInfo; 
	private final ResponseChecker mResponseChecker; 
	private HttpClientTask mClientTask = null; 
	private StorageFile mCacheFile = null; 
	private volatile DownloadContext mMetrics; 
	private String mUserAgent = null; 
	private long mDownloadedLength = 0; 
	private boolean mAppendMode = false; 
	private boolean mStarted = false; 
	private boolean mClosed = false; 
	
	Downloader(DownloadManager manager, DownloadTask task) {
		mManager = manager; 
		mTask = task; 
		mUserAgent = Constants.DEFAULT_USER_AGENT; 
		
		String name = null; 
		DownloadCallback callback = task.getCallback(); 
		if (callback != null) { 
			mResponseChecker = callback.getResponseChecker(); 
			name = callback.getDownloadName(); 
			
			String agent = callback.getDownloadAgent(); 
			if (agent != null && agent.length() > 0) 
				mUserAgent = agent; 
			
		} else { 
			mResponseChecker = new DownloadResponseChecker(); 
		}
		
		if (name == null || name.length() == 0) 
			name = getClass().getName(); 
		
		final String workerName = name; 
		mWorkerInfo = new WorkerTask.WorkerInfo() { 
				public String getName() { 
					return workerName + ":" + mTask.getSource(); 
				}
				public Object getData() { 
					return Downloader.this; 
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
		protected HttpTask.Result readEntity(final HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
			return readBytesHttpEntity(this, entity, publisher); 
		}
		
		@Override 
		protected HttpTask.Progress createProgress(int contentLength, int fetchedLength, boolean aborted) {
			return createProgressContext(contentLength, fetchedLength, aborted); 
		}
	}
	
	private class SimpleStringHttpExecutor extends SimpleHttpExecutor.StringHttpExecutor {
		public SimpleStringHttpExecutor(ResponseChecker checker) { 
			super(checker); 
		}
		
		@Override 
		protected HttpTask.Result readEntity(final HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
			return readStringHttpEntity(this, entity, publisher); 
		}
		
		@Override 
		protected HttpTask.Progress createProgress(int contentLength, int fetchedLength, boolean aborted) {
			return createProgressContext(contentLength, fetchedLength, aborted); 
		}
	}
	
	private DownloadContext createProgressContext(int contentLength, int fetchedLength, boolean aborted) { 
		return DownloadContext.newDownloading(
				(int)(contentLength + mDownloadedLength), 
				(int)(fetchedLength + mDownloadedLength), aborted); 
	}
	
	public final void start() {
		if (!mTask.isScheduled()) return; 
		
		synchronized (this) { 
			startDownload(); 
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
	
	private void startDownload() { 
		if (mStarted || mClosed) return; 
		mStarted = true; 
		
		final String source = mTask.getSource(); 
		final DownloadCallback callback = mTask.getCallback();  
		long tempFileLength = 0; 
		
		if (callback != null) { 
			try {
				if (!callback.isRefetchContent()) { 
					if (LOG.isDebugEnabled())
						LOG.debug("Downloader: open saved file for "+source);
					
					boolean finished = onDownloadJobFinished(openSaveCacheFile());
					if (!finished && !callback.isAutoFetch()) { 
						mTask.setFinished(null, null);
						finished = true;
					}
					
					if (finished) {
						onDownloadFinished();
						close();
						return;
					}
				}
			} catch (IOException e) { 
				mTask.setFinished(null, e); 
				onDownloadExceptionCached(e); 
				onDownloadFinished(); 
				close(); 
				
				return; 
			}
			
		} else {
			// downloaded data not return to callback but save to temp cache
			try { 
				StorageFile file = openTempCacheFile(); 
				if (file == null) 
					throw new IOException("cannot open cache file for "+mTask.getSource()); 
				
				final IFileSystem fs = file.getStorage().getFileSystem(); 
				
				if (file instanceof TempFile) { 
					TempFile tempFile = (TempFile)file; 
					if (fs.exists(tempFile.getStorageFile().getFile())) 
						throw new IOException("file: "+tempFile.getStorageFile().getFilePath()+" already downloaded"); 
					
					if (fs.exists(tempFile.getFile())) { 
						tempFileLength = tempFile.getFile().length(); 
						mAppendMode = true; 
					}
				} else if (fs.exists(file.getFile())) { 
					throw new IOException("file: "+file.getFilePath()+" already downloaded"); 
				}
				
				mCacheFile = file; 
				
			} catch (IOException e) { 
				mTask.setFinished(null, e); 
				onDownloadExceptionCached(e); 
				onDownloadFinished(); 
				close(); 
				
				return; 
			}
		}
		
		final SimpleHttpClient client = SimpleHttpClient.newInstance(getUserAgent()); 
		mDownloadedLength = tempFileLength; 
		
		final HttpExecutorFactory executorFactory = new HttpExecutorFactory() {
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
					onDownloadProgressUpdated(DownloadContext.newConnecting(source)); 
				}
				@Override 
				public void onProgressUpdate(HttpTask.Progress... progresses) {
					onDownloadJobUpdated(progresses); 
				}
				@Override 
				public void onJobFinished(HttpTask.Request request, HttpTask.Result result) {
					onDownloadJobFinished(request, result); 
				}
				@Override
				public void onAllFinished() { 
					client.close(); 
					onDownloadFinished(); 
					Downloader.this.close(); 
				}
				@Override 
				public void onCancelled() {
					client.close(); 
					onDownloadCanceled(); 
					Downloader.this.close(); 
				}
			};
		
		mClientTask = new HttpClientTask(client, executorFactory, 
				mManager.getAdvancedQueueFactory(), taskCallbacks) {
				@Override 
				protected HttpJobRequest createJobRequest(HttpUriRequest request) { 
					if (mDownloadedLength > 0 && request != null) 
						request.addHeader("Range", "bytes="+mDownloadedLength+"-"); 
					return super.createJobRequest(request); 
				}
				@Override 
				public WorkerTask.WorkerInfo getWorkerInfo() { 
					return mWorkerInfo; 
				}
			}; 
		
		if (mTask.setStarted(this)) { 
			final String downloadSource = mTask.getDownloadSource(); 
			final HttpUriRequest request = mTask.getHttpUriRequest(); 
			
			if (callback != null) 
				callback.onStartFetching(downloadSource);
			
			if (request != null) { 
				LOG.info("Downloader: fetching "+downloadSource+" with input request"); 
				mClientTask.executeRequest(request); 
				
			} else { 
				LOG.info("Downloader: fetching "+downloadSource); 
				mClientTask.executeRequest(downloadSource); 
			}
		}
	}
	
	private HttpTask.Result readStringHttpEntity(SimpleStringHttpExecutor executor, HttpEntity entity, 
			HttpTask.Publisher publisher) throws IOException, ParseException {
		DownloadCallback callback = mTask.getCallback(); 
		
		if (callback != null) { 
			Writer writer = callback.getWriter(entity); 
			if (writer != null) {
				try { 
					SimpleHttpExecutor.saveStringEntity(executor, entity, publisher, writer, 
							callback.getDefaultContentCharset()); 
				} finally { 
					writer.flush(); 
				}
				return new WriterResult(writer); 
			}
			
			SimpleHttpExecutor.StringResult result = SimpleHttpExecutor.readString(
					executor, entity, publisher, callback.getDefaultContentCharset()); 
			
			if (result != null && callback.isSaveContent()) { 
				StorageFile file = openSaveCacheFile();
				String content = result.mData;
				//String charset = result.mContentCharset;
				
				if (file != null && content != null) { 
					OutputStream output = file.createFile();
					Writer wr = new OutputStreamWriter(output); //, charset);
					try { 
						wr.write(content); 
						callback.onContentSaved(file);
						LOG.info("Downloader: saved "+content.length()+" length to "+file.getFile()); 
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
				LOG.info("Downloader: "+(mAppendMode?"append":"saved")+" "+size+" length to "+file.getFile()); 
			} finally { 
				writer.flush(); 
				output.flush(); 
				output.close(); 
			}
			return new CacheFileResult(file); 
		}
		
		throw new IOException("cache file not opened for "+mTask.getSource()); 
	}
	
	private HttpTask.Result readBytesHttpEntity(SimpleBytesHttpExecutor executor, HttpEntity entity, 
			HttpTask.Publisher publisher) throws IOException, ParseException {
		DownloadCallback callback = mTask.getCallback(); 
		
		if (callback != null) { 
			OutputStream writer = callback.getOutputStream(entity); 
			if (writer != null) {
				try {
					SimpleHttpExecutor.saveByteArrayEntity(executor, entity, publisher, writer); 
				} finally { 
					writer.flush(); 
				}
				return new OutputStreamResult(writer); 
			}
			
			SimpleHttpExecutor.BytesResult result = 
					SimpleHttpExecutor.readByteArray(executor, entity, publisher); 
			
			if (result != null && callback.isSaveContent()) { 
				StorageFile file = openSaveCacheFile();
				byte[] content = result.mData;
				
				if (file != null && content != null) { 
					OutputStream output = file.createFile();
					try {
						output.write(content, 0, content.length);
						callback.onContentSaved(file);
						LOG.info("Downloader: saved "+content.length+" bytes to "+file.getFile()); 
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
				LOG.info("Downloader: "+(mAppendMode?"append":"saved")+" "+size+" bytes to "+file.getFile()); 
			} finally { 
				if (output != null) { 
					output.flush(); 
					output.close(); 
				}
			}
			return new CacheFileResult(file); 
		}
		
		throw new IOException("cache file not opened for "+mTask.getSource()); 
	}
	
	private void onDownloadJobUpdated(HttpTask.Progress... progresses) {
		if (progresses != null) {
			for (int i=0; i < progresses.length; i++) { 
				HttpTask.Progress p = progresses[i]; 
				if (p != null) 
					onDownloadProgressUpdated(p); 
			}
		}
	}
	
	private boolean onDownloadJobFinished(HttpTask.Request request, HttpTask.Result result) {
		if (result != null) { 
			DownloadCallback callback = mTask.getCallback(); 
			StorageFile contentFile = null;
			if (callback != null)
				contentFile = (StorageFile)callback.getSavedContent();
			
			if (result instanceof HttpException) {
				HttpException e = (HttpException)result; 
				e.setLocation(mTask.getDownloadSource()); 
				mTask.setFinished(contentFile, e); 
				
			} else if (result instanceof CacheFileResult) { 
				mTask.setFinished((StorageFile)result.getData(), null); 
				
			} else 
				mTask.setFinished(contentFile, null); 
			
			if (callback != null) { 
				if (result instanceof HttpException) { 
					callback.onContentDownloaded(null); 
					callback.onHttpException((HttpException)result); 
					
				} else 
					callback.onContentDownloaded(result.getData()); 
			}
			
			return true;
		}
		
		return false;
	}
	
	private boolean onDownloadJobFinished(StorageFile file) {
		if (file != null) { 
			Object content = loadSaveCacheFile(file);
			if (content == null)
				return false;
			
			DownloadCallback callback = mTask.getCallback(); 
			
			mTask.setFinished(null, null); 
			if (callback != null) 
				callback.onContentDownloaded(content);
			
			return true;
		}
		
		return false;
	}
	
	protected void onDownloadProgressUpdated(HttpTask.Progress progress) {
		if (progress == null) return; 
		
		if (progress instanceof DownloadContext) { 
			DownloadContext context = (DownloadContext)progress; 
			mMetrics = context; 
			mTask.updateMetrics(context); 
		}
	}
	
	protected void onDownloadFinished() {
		Throwable exception = mTask.getException(); 
		DownloadContext context = exception != null ? 
				DownloadContext.newFailed(mTask.getSource(), exception) : 
				DownloadContext.newFinished(mTask.getSource(), mTask.getCacheFile()); 
		
		mMetrics = context; 
		mTask.updateMetrics(context); 
		
		DownloadCallback callback = mTask.getCallback(); 
		if (callback != null) 
			callback.onFinished(); 
		
		mManager.onDownloaderFinished(this); 
	}
	
	protected void onDownloadExceptionCached(Throwable exception) { 
		DownloadContext context = DownloadContext.newFailed(mTask.getSource(), exception); 
		mMetrics = context; 
		mTask.updateMetrics(context); 
		
		DownloadCallback callback = mTask.getCallback(); 
		if (callback != null) 
			callback.onFinished(); 
	}
	
	protected void onDownloadCanceled() { 
		mTask.setCanceled(); 
		mManager.onDownloaderCanceled(this); 
	}
	
	public DownloadContext getMetricsContext() {
		return mMetrics; 
	}
	
	public DownloadTask getTask() { 
		return mTask; 
	}
	
	public void close() { 
		mStarted = false; 
		mClosed = true; 
		mManager.closeDownloader(this); 
	}
	
	public boolean isClosed() { 
		return mClosed; 
	}
	
	public boolean isStarted() { 
		return mStarted; 
	}
	
	private StorageFile openTempCacheFile() throws IOException {
		final DownloadCache cache = mTask.getDownloadCache();
		final String source = mTask.getSource(); 
		final MimeType type = mTask.getMimeType(); 
		
		return cache != null ? cache.openTempFile(source, type) : null; 
	}
	
	private StorageFile openSaveCacheFile() throws IOException {
		final DownloadCache cache = mTask.getDownloadCache();
		final String source = mTask.getSource(); 
		final MimeType type = mTask.getMimeType(); 
		
		return cache != null ? cache.openSaveFile(source, type) : null; 
	}
	
	private Object loadSaveCacheFile(StorageFile file) { 
		try {
			final DownloadCache cache = mTask.getDownloadCache();
			if (cache != null)
				return cache.loadSaveFile(file);
		} catch (IOException e) { 
			if (LOG.isDebugEnabled())
				LOG.debug("Downloader: load saved cachefile error", e);
		}
		return null;
	}
	
}
