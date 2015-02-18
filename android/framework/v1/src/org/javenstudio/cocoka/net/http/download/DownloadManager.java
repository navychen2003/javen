package org.javenstudio.cocoka.net.http.download;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;

public final class DownloadManager {
	
	private static final int DOWNLOADCACHE_VERSION = 1; 
	
	public interface DownloaderListener { 
		public void onStarted(Downloader downloader); 
		public void onFinished(Downloader downloader); 
		public void onCanceled(Downloader downloader); 
		public void onClosed(Downloader downloader); 
	}
	
	public interface TaskQueueHandler { 
		public void initCommand(Runnable command) throws Exception;
		public boolean handleNotifyStart();
		public AdvancedQueueFactory getQueueFactory();
	}
	
	private final Map<String, DownloadTask> mTasksMap = new HashMap<String, DownloadTask>(); 
	private final List<DownloadTask> mTasksQueue = new ArrayList<DownloadTask>(); 
	private final List<WeakReference<DownloaderListener>> mListeners = 
			new ArrayList<WeakReference<DownloaderListener>>(); 
	
	private final TaskQueueHandler mHandler; 
	private final List<Downloader> mDownloaders; 
	private final DownloadCache mDownloadCache; 
	private final StorageManager mStorageManager;
	private AdvancedQueueFactory mQueueFactory = null;
	
	DownloadManager(StorageManager manager, TaskQueueHandler handler) {
		try { 
			if (handler == null) { 
				handler = new TaskQueueHandler() { 
						public void initCommand(Runnable command) { 
						}
						public boolean handleNotifyStart() { 
							runDownloadTask(); return true;
						}
						public AdvancedQueueFactory getQueueFactory() { 
							return null;
						}
					}; 
			}
			
			mStorageManager = manager;
			mDownloadCache = new DownloadCache(manager.getCacheStorage("download", DOWNLOADCACHE_VERSION)); 
			mDownloaders = new ArrayList<Downloader>(); 
			mHandler = handler;
			
			handler.initCommand(new Runnable() { 
					public void run() { 
						runDownloadTask();
					}
				});
		} catch (Exception e) {
			throw new RuntimeException("DownloadManager init error: "+e); 
		}
	} 
	
	public final StorageManager getStorageManager() { 
		return mStorageManager;
	}
	
	public final DownloadCache getDefaultDownloadCache() { 
		return mDownloadCache; 
	}
	
	public void setAdvancedQueueFactory(AdvancedQueueFactory factory) { 
		mQueueFactory = factory;
	}
	
	AdvancedQueueFactory getAdvancedQueueFactory() { 
		return mQueueFactory;
	}
	
	private void runDownloadTask() {
		while (true) {
			DownloadTask task = null; 
			synchronized (mTasksQueue) {
				for (int i=0; i < mTasksQueue.size(); i++) {
					DownloadTask dt = mTasksQueue.get(i); 
					if (dt != null && dt.isScheduled()) {
						task = dt; 
						mTasksQueue.remove(i); 
						break; 
					}
				}
			}
			
			if (task == null) 
				break; 
			
			Downloader downloader = new Downloader(this, task); 
			synchronized (this) {
				mDownloaders.add(downloader); 
			}
			downloader.start(); 
		}
	}
	
	private void startDownloader() { 
		Downloader downloader = null; 
		
		synchronized (this) {
			for (int i=0; i < mDownloaders.size(); i++) { 
				Downloader d = mDownloaders.get(i); 
				if (d == null) continue; 
				if (d.isStarted()) return; // only one start
				if (!d.isClosed()) { 
					downloader = d; 
					break; 
				}
			}
		}
		
		if (downloader != null) { 
			downloader.start(); 
			onDownloaderStarted(downloader); 
		}
	}
	
	public void notifyStart() {
		synchronized (this) {
			mHandler.handleNotifyStart();
		}
	}
	
	public void addListener(DownloaderListener listener) { 
		synchronized (mListeners) { 
			boolean found = false; 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<DownloaderListener> ref = mListeners.get(i); 
				DownloaderListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else if (lsr == listener) 
					found = true; 
				i ++; 
			}
			if (!found) 
				mListeners.add(new WeakReference<DownloaderListener>(listener)); 
		}
	}
	
	private void onDownloaderStarted(Downloader downloader) { 
		if (downloader == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<DownloaderListener> ref = mListeners.get(i); 
				DownloaderListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onStarted(downloader); 
				i ++; 
			}
		}
	}
	
	void onDownloaderFinished(Downloader downloader) { 
		if (downloader == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<DownloaderListener> ref = mListeners.get(i); 
				DownloaderListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onFinished(downloader); 
				i ++; 
			}
		}
	}
	
	void onDownloaderCanceled(Downloader downloader) { 
		if (downloader == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<DownloaderListener> ref = mListeners.get(i); 
				DownloaderListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onCanceled(downloader); 
				i ++; 
			}
		}
	}
	
	private void onDownloaderClosed(Downloader downloader) { 
		if (downloader == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<DownloaderListener> ref = mListeners.get(i); 
				DownloaderListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onClosed(downloader); 
				i ++; 
			}
		}
	}
	
	void closeDownloader(Downloader downloader) { 
		closeTask(downloader.getTask()); 
		
		synchronized (this) {
			for (int i=0; i < mDownloaders.size(); ) { 
				if (downloader == mDownloaders.get(i)) { 
					mDownloaders.remove(i); 
					continue; 
				}
				i ++; 
			}
			
			onDownloaderClosed(downloader); 
		}
	}
	
	void closeTask(DownloadTask task) {
		if (task == null || task.isScheduled() || task.isStarted()) 
			return; 
		
		synchronized (mTasksQueue) {
			String source = task.getSource(); 
			
			for (int i=0; i < mTasksQueue.size(); i++) {
				DownloadTask dt = mTasksQueue.get(i); 
				if (dt == null) continue; 
				if (task == dt || source.equals(dt.getSource())) 
					return; 
			}
			
			if (task.setClosed()) 
				mTasksMap.remove(source); 
		}
		
		startDownloader(); // start next
	}
	
	public DownloadTask offerTask(String source) { 
		return offerTask(source, null);
	}
	
	public DownloadTask offerTask(String source, DownloadCache cache) {
		if (source == null || source.length() == 0) 
			return null; 
		
		synchronized (mTasksQueue) {
			DownloadTask task = mTasksMap.get(source); 
			if (task == null) {
				task = new DownloadTask(this, source, cache); 
				mTasksQueue.add(task); 
				mTasksMap.put(source, task); 
			}
			
			return task; 
		}
	}
	
	public DownloadTask getTask(String source) {
		if (source == null || source.length() == 0) 
			return null; 
		
		synchronized (mTasksQueue) {
			return mTasksMap.get(source); 
		}
	}
	
	public boolean stopTask(String source) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		Downloader downloader = null; 
		synchronized (this) {
			for (Downloader d : mDownloaders) { 
				if (source.equals(d.getTask().getSource())) { 
					downloader = d; 
					break; 
				}
			}
		}
		
		if (downloader != null) { 
			if (source.equals(downloader.getTask().getSource())) 
				return downloader.stop(); 
		}
		
		return false; 
	}
	
}
