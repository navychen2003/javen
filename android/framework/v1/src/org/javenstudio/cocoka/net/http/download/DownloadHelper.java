package org.javenstudio.cocoka.net.http.download;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;

import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.cocoka.net.metrics.IMetricsUpdater;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.worker.LooperThread;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;
import org.javenstudio.cocoka.worker.work.Scheduler;

public class DownloadHelper {
	//private static Logger LOG = Logger.getLogger(DownloadHelper.class);

	private static final int MESSAGE_WHAT = 1000;
	
	private static DownloadManager sManager = null; 
	
	public static synchronized void initDownloadManager(StorageManager manager) { 
		initDownloadManager(manager, (DownloadManager.TaskQueueHandler)null);
	}
	
	public static synchronized void initDownloadManager(
			StorageManager manager, final Scheduler scheduler) { 
		DownloadManager.TaskQueueHandler handler = null;
		if (scheduler != null) { 
			final LooperThread looper = scheduler.getQueue().getLooperThread();
			
			handler = new DownloadManager.TaskQueueHandler() {
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
		
		initDownloadManager(manager, handler);
	}
	
	public static synchronized void initDownloadManager(
			StorageManager manager, final LooperThread looper) { 
		DownloadManager.TaskQueueHandler handler = null;
		if (looper != null) { 
			looper.addMessageListener(new LooperThread.MessageListener() {
					@Override
					public void onHandleMessage(Message msg) {
						//if (msg.what == MESSAGE_WHAT) 
						//	runDownloadTask(); 
					}
				});
		}
		
		initDownloadManager(manager, handler);
	}
	
	public static synchronized void initDownloadManager(
			StorageManager manager, DownloadManager.TaskQueueHandler handler) { 
		if (sManager != null) 
			throw new RuntimeException("DownloadManager already initialized");
		if (manager == null) 
			throw new RuntimeException("StorageManager input null");
		
		sManager = new DownloadManager(manager, handler);
	}
	
	public static synchronized DownloadManager getDownloadManager() { 
		if (sManager == null) 
			throw new RuntimeException("DownloadManager not initialize");
		return sManager; 
	}
	
	public static DownloadCache getDefaultDownloadCache() { 
		return getDownloadManager().getDefaultDownloadCache();
	}
	
	public static void scheduleDownload(DownloadSpans spans) {
		if (spans == null) return; 

		String[] sources = spans.getDownloadSources(); 
		for (int i=0; sources != null && i < sources.length; i++) {
			String source = sources[i]; 
			DownloadSpan[] ss = spans.getDownloadSpans(source); 
			MimeType type = null; 
			if (ss != null && ss.length > 0) 
				type = ss[0].getMimeType(); 
			scheduleDownload(source, type); 
		}
	}
	
	public static void scheduleDownload(DownloadSource ds) {
		if (ds == null) return; 

		scheduleDownload(ds.getSource()); 
	}
	
	public static boolean stopDownload(String source) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		return getDownloadManager().stopTask(source); 
	}
	
	public static StorageFile openOrDownload(String source) { 
		return openOrDownload(source, (MimeType)null); 
	}
	
	public static StorageFile openOrDownload(String source, MimeType type) { 
		return openOrDownload(source, type, true); 
	}
	
	public static StorageFile openOrDownload(String source, MimeType type, boolean download) { 
		return openOrDownload(source, null, type, download); 
	}
	
	public static StorageFile openOrDownload(String source, String downloadSource, 
			MimeType type, boolean download) { 
		return openOrDownload(getDefaultDownloadCache(), source, downloadSource, type, download);
	}
	
	public static StorageFile openOrDownload(DownloadCache cache, String source) { 
		return openOrDownload(cache, source, (MimeType)null); 
	}
	
	public static StorageFile openOrDownload(DownloadCache cache, String source, MimeType type) { 
		return openOrDownload(cache, source, type, true); 
	}
	
	public static StorageFile openOrDownload(DownloadCache cache, 
			String source, MimeType type, boolean download) { 
		return openOrDownload(cache, source, null, type, download); 
	}
	
	public static StorageFile openOrDownload(DownloadCache cache, 
			String source, String downloadSource, MimeType type, boolean download) { 
		if (source == null || source.length() == 0) 
			return null; 
		
		if (cache == null) 
			cache = getDefaultDownloadCache();
		
		StorageFile file = cache.getCacheFile(source, type); 
		if (file != null) { 
			try { 
				if (file.getStorage().getFileSystem().exists(file.getFile())) 
					return file; 
				
				//if (LOG.isDebugEnabled()) 
				//	LOG.debug("Downloader: file: "+file.getFilePath()+" not found"); 
				
			} catch (IOException e) { 
				return null; 
			}
		}
		
		if (download) 
			scheduleDownloadWithSource(cache, source, downloadSource, type, (DownloadCallback)null); 
		
		return null; 
	}
	
	public static boolean checkDownloaded(String source) { 
		return checkDownloaded(source, (MimeType)null); 
	}
	
	public static boolean checkDownloaded(String source, MimeType type) { 
		return checkDownloaded(null, source, type);
	}
	
	public static boolean checkDownloaded(DownloadCache cache, String source) { 
		return checkDownloaded(cache, source, (MimeType)null); 
	}
	
	public static boolean checkDownloaded(DownloadCache cache, String source, MimeType type) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		StorageFile file = (cache != null) ? 
				cache.getCacheFile(source, type) : getCacheFile(source, type); 
		
		return checkDownloaded(file); 
	}
	
	public static boolean checkDownloaded(StorageFile file) { 
		if (file != null) { 
			try { 
				if (file.getStorage().getFileSystem().exists(file.getFile())) 
					return true; 
				
				//if (LOG.isDebugEnabled()) 
				//	LOG.debug("Downloader: file: "+file.getFilePath()+" not found"); 
				
			} catch (IOException e) { 
				return false; 
			}
		}
		return false; 
	}
	
	public static boolean scheduleDownload(String source) {
		return scheduleDownload(source, null); 
	}
	
	public static boolean scheduleDownload(String source, MimeType type) {
		return scheduleDownload(source, type, (DownloadCallback)null); 
	}
	
	public static boolean scheduleDownload(String source, MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, null, null, type, callback); 
	}
	
	public static boolean scheduleDownloadWithSource(String source, String downloadSource, 
			MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, downloadSource, null, type, callback); 
	}
	
	public static boolean scheduleDownloadWithRequest(String source, HttpUriRequest request, 
			MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, null, request, type, callback); 
	}
	
	private static boolean doScheduleDownload(String source, String downloadSource, HttpUriRequest request, 
			MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, downloadSource, request, type, callback, 
				getDefaultDownloadCache());
	}
	
	public static boolean scheduleDownload(DownloadCache cache, String source) {
		return scheduleDownload(cache, source, null); 
	}
	
	public static boolean scheduleDownload(DownloadCache cache, String source, MimeType type) {
		return scheduleDownload(cache, source, type, (DownloadCallback)null); 
	}
	
	public static boolean scheduleDownload(DownloadCache cache, 
			String source, MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, null, null, type, callback, cache); 
	}
	
	public static boolean scheduleDownloadWithSource(DownloadCache cache, 
			String source, String downloadSource, MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, downloadSource, null, type, callback, cache); 
	}
	
	public static boolean scheduleDownloadWithRequest(DownloadCache cache, 
			String source, HttpUriRequest request, MimeType type, DownloadCallback callback) {
		return doScheduleDownload(source, null, request, type, callback, cache); 
	}
	
	private static boolean doScheduleDownload(String source, String downloadSource, HttpUriRequest request, 
			MimeType type, DownloadCallback callback, final DownloadCache cache) {
		if (source == null || source.length() == 0) 
			return false; 
		
		DownloadManager manager = getDownloadManager(); 
		DownloadTask task = manager.offerTask(source, cache);
		
		if (task != null) { 
			task.setDownloadCache(cache);
			
			if (callback != null && callback instanceof AbstractCallback) { 
				DownloadCache callbackCache = ((AbstractCallback)callback).getDowndloadCache();
				if (callbackCache != null && callbackCache != cache)
					task.setDownloadCache(callbackCache);
			}
			
			if (task.getDownloadCache() == null)
				task.setDownloadCache(manager.getDefaultDownloadCache());
			
			if (downloadSource != null && downloadSource.length() > 0) 
				task.setDownloadSource(downloadSource); 
			
			if (request != null) 
				task.setHttpUriRequest(request); 
			
			if (task.setScheduled(type, callback)) { 
				manager.notifyStart(); 
				return true; 
			}
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("Downloader: " + source + " already scheduled or downloaded"); 
		}
		
		return false; 
	}
	
	public static boolean scheduleFetchHtml(String source, HtmlCallback callback) {
		return scheduleDownload(source, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean scheduleFetchHtmlWithRequest(String source, HttpUriRequest request, HtmlCallback callback) {
		return scheduleDownloadWithRequest(source, request, MimeType.TYPE_TEXT, callback); 
	}
	
	public static boolean scheduleFetchFile(String source, FileCallback callback) {
		return scheduleDownload(source, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static boolean scheduleFetchFileWithRequest(String source, HttpUriRequest request, FileCallback callback) {
		return scheduleDownloadWithRequest(source, request, MimeType.TYPE_APPLICATION, callback); 
	}
	
	public static void registerUpdater(String source, IMetricsUpdater updater) {
		if (source == null || updater == null) 
			return; 
		
		DownloadTask task = getDownloadManager().offerTask(source); 
		if (task != null) 
			task.addUpdater(updater); 
	}
	
	public static void setDownloadListener(String source, DownloadListener listener) { 
		if (source == null || listener == null) 
			return;
		
		DownloadTask task = getDownloadManager().offerTask(source); 
		if (task != null) 
			task.setListener(listener);
	}
	
	public static DownloadContext getMetricsContext(String source) {
		if (source == null || source.length() == 0) 
			return null; 
		
		DownloadTask task = getDownloadManager().getTask(source); 
		if (task != null) {
			Downloader downloader = task.getDownloader(); 
			if (downloader != null) 
				return downloader.getMetricsContext(); 
		}
		
		return null; 
	}
	
	public static StorageFile getCacheFile(String source) {
		return getCacheFile(source, (MimeType)null); 
	}
	
	public static StorageFile getCacheFile(String source, MimeType type) {
		if (source == null) 
			return null; 
		
		DownloadTask task = DownloadHelper.getDownloadManager().getTask(source); 
		if (task != null && task.isStarted()) 
			return null; 
		
		try { 
			DownloadCache cache = task.getDownloadCache();
			if (cache != null)
				return cache.openCacheFile(source, type); 
		} catch (Exception e) { 
			// ignore
		}
		
		return null; 
	}
	
}
