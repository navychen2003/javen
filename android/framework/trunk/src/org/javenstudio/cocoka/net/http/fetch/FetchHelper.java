package org.javenstudio.cocoka.net.http.fetch;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;

import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.http.HttpHelper;
import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.worker.LooperThread;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;
import org.javenstudio.cocoka.worker.work.Scheduler;
import org.javenstudio.common.util.Logger;

public class FetchHelper {
	private static Logger LOG = Logger.getLogger(FetchHelper.class);

	private static final int MESSAGE_WHAT = 1000;
	private static FetchManager sManager = null; 
	
	public static synchronized void initFetchManager(StorageManager manager) { 
		initFetchManager(manager, (FetchManager.TaskQueueHandler)null);
	}
	
	public static synchronized void initFetchManager(
			StorageManager manager, final Scheduler scheduler) { 
		FetchManager.TaskQueueHandler handler = null;
		if (scheduler != null) { 
			final LooperThread looper = scheduler.getQueue().getLooperThread();
			
			handler = new FetchManager.TaskQueueHandler() {
					private Handler mHandler;
					@Override
					public void initCommand(final Runnable command) throws Exception {
						mHandler = looper.getHandler();
						looper.addMessageListener(new LooperThread.MessageListener() {
							@Override
							public void onHandleMessage(Message msg) {
								if (msg.what == MESSAGE_WHAT) 
									command.run();
							}
						});
					}
					@Override
					public boolean handleNotifyStart() { 
						return mHandler.sendEmptyMessage(MESSAGE_WHAT); 
					}
					@Override
					public AdvancedQueueFactory getQueueFactory() {
						return scheduler.getQueueFactory();
					}
				};
		}
		
		initFetchManager(manager, handler);
	}
	
	public static synchronized void initFetchManager(
			StorageManager manager, final LooperThread looper) { 
		FetchManager.TaskQueueHandler handler = null;
		if (looper != null) { 
			looper.addMessageListener(new LooperThread.MessageListener() {
					@Override
					public void onHandleMessage(Message msg) {
						//if (msg.what == MESSAGE_WHAT) 
						//	runFetchTask(); 
					}
				});
		}
		
		initFetchManager(manager, handler);
	}
	
	public static synchronized void initFetchManager(
			StorageManager manager, FetchManager.TaskQueueHandler handler) { 
		if (sManager != null) 
			throw new RuntimeException("FetchManager already initialized");
		if (manager == null) 
			throw new RuntimeException("StorageManager input null");
		
		sManager = new FetchManager(manager, handler);
	}
	
	public static synchronized FetchManager getFetchManager() { 
		if (sManager == null) 
			throw new RuntimeException("FetchManager not initialize");
		return sManager; 
	}
	
	//public static FetchCache getDefaultFetchCache() { 
	//	return getFetchManager().getDefaultFetchCache();
	//}
	
	public static void scheduleFetch(FetchSpans spans) {
		if (spans == null) return; 

		String[] sources = spans.getFetchSources(); 
		for (int i=0; sources != null && i < sources.length; i++) {
			String source = sources[i]; 
			FetchSpan[] ss = spans.getFetchSpans(source); 
			MimeType type = null; 
			if (ss != null && ss.length > 0) 
				type = ss[0].getMimeType(); 
			scheduleFetch(source, type); 
		}
	}
	
	public static void scheduleFetch(FetchSource ds) {
		if (ds == null) return; 

		scheduleFetch(ds.getSource()); 
	}
	
	public static boolean stopFetch(String source) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		return getFetchManager().stopTask(source); 
	}
	
	public static StorageFile openOrSchedule(String source, boolean schedule) { 
		return openOrSchedule(source, (MimeType)null, schedule); 
	}
	
	public static StorageFile openOrSchedule(String source, MimeType type, boolean schedule) { 
		return openOrSchedule(source, type, true, schedule); 
	}
	
	public static StorageFile openOrSchedule(String source, MimeType type, 
			boolean fetch, boolean schedule) { 
		return openOrSchedule(source, null, type, fetch, schedule); 
	}
	
	public static StorageFile openOrSchedule(String source, String fetchSource, 
			MimeType type, boolean fetch, boolean schedule) { 
		return openOrSchedule(null, source, fetchSource, type, fetch, schedule);
	}
	
	public static StorageFile openOrSchedule(FetchCache cache, String source, 
			boolean schedule) { 
		return openOrSchedule(cache, source, (MimeType)null, schedule); 
	}
	
	public static StorageFile openOrSchedule(FetchCache cache, String source, 
			MimeType type, boolean schedule) { 
		return openOrSchedule(cache, source, type, true, schedule); 
	}
	
	public static StorageFile openOrSchedule(FetchCache cache, 
			String source, MimeType type, boolean fetch, boolean schedule) { 
		return openOrSchedule(cache, source, null, type, fetch, schedule); 
	}
	
	public static StorageFile openOrSchedule(FetchCache cache, 
			String source, String fetchSource, MimeType type, 
			boolean fetch, boolean schedule) { 
		if (source == null || source.length() == 0) 
			return null; 
		
		//if (cache == null) 
		//	cache = getDefaultFetchCache();
		
		StorageFile file = cache != null ? cache.getCacheFile(source, type) : null; 
		if (file != null) { 
			try { 
				if (file.getStorage().getFileSystem().exists(file.getFile())) 
					return file; 
				
				//if (LOG.isDebugEnabled()) 
				//	LOG.debug("file: "+file.getFilePath()+" not found"); 
				
			} catch (IOException e) { 
				return null; 
			}
		}
		
		if (fetch) {
			if (schedule) {
				scheduleFetchWithSource(cache, source, fetchSource, type, 
						(FetchCallback)null); 
			} else {
				fetchWithSource(cache, source, fetchSource, type, 
						(FetchCallback)null); 
			}
		}
		
		return null; 
	}
	
	public static boolean checkFetched(String source) { 
		return checkFetched(source, (MimeType)null); 
	}
	
	public static boolean checkFetched(String source, MimeType type) { 
		return checkFetched(null, source, type);
	}
	
	public static boolean checkFetched(FetchCache cache, String source) { 
		return checkFetched(cache, source, (MimeType)null); 
	}
	
	public static boolean checkFetched(FetchCache cache, String source, MimeType type) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		StorageFile file = (cache != null) ? 
				cache.getCacheFile(source, type) : getCacheFile(source, type); 
		
		return checkFetched(file); 
	}
	
	public static boolean checkFetched(StorageFile file) { 
		if (file != null) { 
			try { 
				if (file.getStorage().getFileSystem().exists(file.getFile())) 
					return true; 
				
				//if (LOG.isDebugEnabled()) 
				//	LOG.debug("file: "+file.getFilePath()+" not found"); 
				
			} catch (IOException e) { 
				return false; 
			}
		}
		return false; 
	}
	
	public static boolean scheduleFetch(String source) {
		return scheduleFetch(source, null); 
	}
	
	public static boolean scheduleFetch(String source, MimeType type) {
		return scheduleFetch(source, type, (FetchCallback)null); 
	}
	
	public static boolean scheduleFetch(String source, MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, null, null, type, callback); 
	}
	
	public static boolean scheduleFetchWithSource(String source, String fetchSource, 
			MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, fetchSource, null, type, callback); 
	}
	
	public static boolean scheduleFetchWithRequest(String source, HttpUriRequest request, 
			MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, null, request, type, callback); 
	}
	
	private static boolean doScheduleFetch(String source, String fetchSource, HttpUriRequest request, 
			MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, fetchSource, request, type, callback, null);
	}
	
	public static boolean scheduleFetch(FetchCache cache, String source) {
		return scheduleFetch(cache, source, null); 
	}
	
	public static boolean scheduleFetch(FetchCache cache, String source, MimeType type) {
		return scheduleFetch(cache, source, type, (FetchCallback)null); 
	}
	
	public static boolean scheduleFetch(FetchCache cache, 
			String source, MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, null, null, type, callback, cache); 
	}
	
	public static boolean scheduleFetchWithSource(FetchCache cache, 
			String source, String fetchSource, MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, fetchSource, null, type, callback, cache); 
	}
	
	public static boolean scheduleFetchWithRequest(FetchCache cache, 
			String source, HttpUriRequest request, MimeType type, FetchCallback callback) {
		return doScheduleFetch(source, null, request, type, callback, cache); 
	}
	
	public static void removeFailed(String source) { 
		HttpHelper.removeFailed(source);
	}
	
	public static boolean fetch(String source) {
		return fetch(source, null); 
	}
	
	public static boolean fetch(String source, MimeType type) {
		return fetch(source, type, (FetchCallback)null); 
	}
	
	public static boolean fetch(String source, MimeType type, FetchCallback callback) {
		return doFetch(source, null, null, type, callback); 
	}
	
	public static boolean fetchWithSource(String source, String fetchSource, 
			MimeType type, FetchCallback callback) {
		return doFetch(source, fetchSource, null, type, callback); 
	}
	
	public static boolean fetchWithRequest(String source, HttpUriRequest request, 
			MimeType type, FetchCallback callback) {
		return doFetch(source, null, request, type, callback); 
	}
	
	private static boolean doFetch(String source, String fetchSource, HttpUriRequest request, 
			MimeType type, FetchCallback callback) {
		return doFetch(source, fetchSource, request, type, callback, null, false);
	}
	
	public static boolean fetch(FetchCache cache, String source) {
		return fetch(cache, source, null); 
	}
	
	public static boolean fetch(FetchCache cache, String source, MimeType type) {
		return fetch(cache, source, type, (FetchCallback)null); 
	}
	
	public static boolean fetch(FetchCache cache, 
			String source, MimeType type, FetchCallback callback) {
		return doFetch(source, null, null, type, callback, cache, false); 
	}
	
	public static boolean fetchWithSource(FetchCache cache, 
			String source, String fetchSource, MimeType type, FetchCallback callback) {
		return doFetch(source, fetchSource, null, type, callback, cache, false); 
	}
	
	public static boolean fetchWithRequest(FetchCache cache, 
			String source, HttpUriRequest request, MimeType type, FetchCallback callback) {
		return doFetch(source, null, request, type, callback, cache, false); 
	}
	
	private static boolean doScheduleFetch(String source, String fetchSource, HttpUriRequest request, 
			MimeType type, FetchCallback callback, final FetchCache cache) {
		return doFetch(source, fetchSource, request, type, callback, cache, true);
	}
	
	private static boolean doFetch(String source, String fetchSource, HttpUriRequest request, 
			MimeType type, FetchCallback callback, final FetchCache cache, final boolean schedule) {
		if (source == null || source.length() == 0) 
			return false; 
		
		HttpHelper.FailedHistory failed = HttpHelper.getFailed(source);
		if (failed != null) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("Source: " + source + " cannot fetch for failed: " 
						+ failed.getException());
			}
			
			return false;
		}
		
		FetchManager manager = getFetchManager(); 
		FetchTask task = schedule ? manager.offerTask(source, cache) : 
				manager.newTask(source, cache);
		
		if (task != null) { 
			task.setFetchCache(cache);
			
			if (callback != null && callback instanceof AbstractCallback) { 
				FetchCache callbackCache = ((AbstractCallback)callback).getFetchCache();
				if (callbackCache != null && callbackCache != cache)
					task.setFetchCache(callbackCache);
			}
			
			//if (task.getFetchCache() == null)
			//	task.setFetchCache(manager.getDefaultFetchCache());
			
			if (fetchSource != null && fetchSource.length() > 0) 
				task.setFetchSource(fetchSource); 
			
			if (request != null) 
				task.setHttpUriRequest(request); 
			
			if (schedule) {
				if (task.setScheduled(type, callback)) { 
					manager.notifyStart(); 
					return true; 
				}
			} else { 
				if (task.setScheduled(type, callback)) { 
					manager.startTask(task);
					return true;
				}
			}
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("Source: " + source + " already scheduled or fetched"); 
		}
		
		return false; 
	}
	
	public static boolean scheduleFetchHtml(String source, HtmlCallback callback) {
		return scheduleFetch(source, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean scheduleFetchHtmlWithRequest(String source, HttpUriRequest request, HtmlCallback callback) {
		return scheduleFetchWithRequest(source, request, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean scheduleFetchFile(String source, FileCallback callback) {
		return scheduleFetch(source, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static boolean scheduleFetchFileWithRequest(String source, HttpUriRequest request, FileCallback callback) {
		return scheduleFetchWithRequest(source, request, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static boolean fetchHtml(String source, HtmlCallback callback) {
		return fetch(source, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean fetchHtmlWithRequest(String source, HttpUriRequest request, HtmlCallback callback) {
		return fetchWithRequest(source, request, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean fetchFile(String source, FileCallback callback) {
		return fetch(source, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static boolean fetchFileWithRequest(String source, HttpUriRequest request, FileCallback callback) {
		return fetchWithRequest(source, request, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static void registerUpdater(String source, IMetricsUpdater updater) {
		if (source == null || updater == null) 
			return; 
		
		FetchTask task = getFetchManager().offerTask(source); 
		if (task != null) 
			task.addUpdater(updater); 
	}
	
	public static void setFetchListener(String source, FetchListener listener) { 
		if (source == null || listener == null) 
			return;
		
		FetchTask task = getFetchManager().offerTask(source); 
		if (task != null) 
			task.setListener(listener);
	}
	
	public static FetchContext getMetricsContext(String source) {
		if (source == null || source.length() == 0) 
			return null; 
		
		FetchTask task = getFetchManager().getTask(source); 
		if (task != null) {
			Fetcher fetcher = task.getFetcher(); 
			if (fetcher != null) 
				return fetcher.getMetricsContext(); 
		}
		
		return null; 
	}
	
	public static StorageFile getCacheFile(String source) {
		return getCacheFile(source, (MimeType)null); 
	}
	
	public static StorageFile getCacheFile(String source, MimeType type) {
		if (source == null) 
			return null; 
		
		FetchTask task = FetchHelper.getFetchManager().getTask(source); 
		if (task != null && task.isStarted()) 
			return null; 
		
		try { 
			FetchCache cache = task.getFetchCache();
			if (cache != null)
				return cache.openCacheFile(source, type); 
		} catch (Exception e) { 
			// ignore
		}
		
		return null; 
	}
	
}
