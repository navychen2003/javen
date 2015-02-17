package org.javenstudio.cocoka.net.http.fetch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;
import org.javenstudio.common.util.Logger;

public final class FetchManager {
	private static final Logger LOG = Logger.getLogger(FetchManager.class);
	
	public interface FetcherListener { 
		public void onStarted(Fetcher fetcher); 
		public void onFinished(Fetcher fetcher); 
		public void onCanceled(Fetcher fetcher); 
		public void onClosed(Fetcher fetcher); 
	}
	
	public interface TaskQueueHandler { 
		public void initCommand(Runnable command) throws Exception;
		public boolean handleNotifyStart();
		public AdvancedQueueFactory getQueueFactory();
	}
	
	private final Map<String, FetchTask> mTasksMap = new HashMap<String, FetchTask>(); 
	private final List<FetchTask> mTasksQueue = new ArrayList<FetchTask>(); 
	private final List<WeakReference<FetcherListener>> mListeners = 
			new ArrayList<WeakReference<FetcherListener>>(); 
	
	private final TaskQueueHandler mHandler; 
	private final List<Fetcher> mFetchers; 
	private final List<Fetcher> mFetchings; 
	//private final FetchCache mFetchCache; 
	private final StorageManager mStorageManager;
	private AdvancedQueueFactory mQueueFactory = null;
	
	FetchManager(StorageManager manager, TaskQueueHandler handler) {
		try { 
			if (handler == null) { 
				handler = new TaskQueueHandler() { 
						public void initCommand(Runnable command) { 
						}
						public boolean handleNotifyStart() { 
							runFetchTask(); return true;
						}
						public AdvancedQueueFactory getQueueFactory() { 
							return null;
						}
					}; 
			}
			
			mStorageManager = manager;
			//mFetchCache = new FetchCache(manager.getCacheStorage("fetch")); 
			mFetchers = new ArrayList<Fetcher>(); 
			mFetchings = new ArrayList<Fetcher>(); 
			mHandler = handler;
			
			handler.initCommand(new Runnable() { 
					public void run() { 
						runFetchTask();
					}
				});
		} catch (Throwable e) {
			throw new RuntimeException("FetchManager init error: "+e, e); 
		}
	} 
	
	public final StorageManager getStorageManager() { 
		return mStorageManager;
	}
	
	//public final FetchCache getDefaultFetchCache() { 
	//	return mFetchCache; 
	//}
	
	public void setAdvancedQueueFactory(AdvancedQueueFactory factory) { 
		mQueueFactory = factory;
	}
	
	AdvancedQueueFactory getAdvancedQueueFactory() { 
		return mQueueFactory;
	}
	
	private void runFetchTask() {
		while (true) {
			FetchTask task = null; 
			synchronized (mTasksQueue) {
				for (int i=0; i < mTasksQueue.size(); i++) {
					FetchTask dt = mTasksQueue.get(i); 
					if (dt != null && dt.isScheduled()) {
						task = dt; 
						mTasksQueue.remove(i); 
						break; 
					}
				}
			}
			
			if (task == null) 
				break; 
			
			startTask(task, true);
		}
	}
	
	public void startTask(FetchTask task) { 
		startTask(task, false);
	}
	
	private void startTask(FetchTask task, boolean async) { 
		Fetcher fetcher = new Fetcher(this, task); 
		synchronized (this) {
			mFetchers.add(fetcher); 
		}
		onFetcherStarted(fetcher); 
		fetcher.start(async); 
	}
	
	private void nextFetcher() { 
		Fetcher fetcher = null; 
		
		synchronized (this) {
			for (int i=0; i < mFetchers.size(); i++) { 
				Fetcher d = mFetchers.get(i); 
				if (d == null) continue; 
				if (d.isStarted()) return; // only one start
				if (!d.isClosed()) { 
					fetcher = d; 
					break; 
				}
			}
		}
		
		if (fetcher != null) { 
			onFetcherStarted(fetcher); 
			fetcher.start(true); 
		}
	}
	
	public void notifyStart() {
		synchronized (this) {
			mHandler.handleNotifyStart();
		}
	}
	
	void addFetching(Fetcher fetcher) { 
		if (fetcher == null) return;
		synchronized (this) { 
			for (Fetcher f : mFetchings) { 
				if (f == fetcher) return;
			}
			mFetchings.add(fetcher);
			
			if (LOG.isDebugEnabled())
				LOG.debug("addFetching, fetchings=" + mFetchings.size());
		}
	}
	
	private void removeFetching(Fetcher fetcher) { 
		if (fetcher == null) return;
		synchronized (this) { 
			for (int i=0; i < mFetchings.size(); ) { 
				if (fetcher == mFetchings.get(i)) { 
					mFetchings.remove(i);
					continue;
				}
				i++;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("removeFetching, fetchings=" + mFetchings.size());
		}
	}
	
	//private int getFetchingCount() { 
	//	synchronized (this) { 
	//		return mFetchings.size();
	//	}
	//}
	
	public void addListener(FetcherListener listener) { 
		synchronized (mListeners) { 
			boolean found = false; 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<FetcherListener> ref = mListeners.get(i); 
				FetcherListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else if (lsr == listener) 
					found = true; 
				i ++; 
			}
			if (!found) 
				mListeners.add(new WeakReference<FetcherListener>(listener)); 
		}
	}
	
	private void onFetcherStarted(Fetcher fetcher) { 
		if (fetcher == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<FetcherListener> ref = mListeners.get(i); 
				FetcherListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onStarted(fetcher); 
				i ++; 
			}
		}
	}
	
	void onFetcherFinished(Fetcher fetcher) { 
		if (fetcher == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<FetcherListener> ref = mListeners.get(i); 
				FetcherListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onFinished(fetcher); 
				i ++; 
			}
		}
	}
	
	void onFetcherCanceled(Fetcher fetcher) { 
		if (fetcher == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<FetcherListener> ref = mListeners.get(i); 
				FetcherListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onCanceled(fetcher); 
				i ++; 
			}
		}
	}
	
	private void onFetcherClosed(Fetcher fetcher) { 
		if (fetcher == null) return; 
		
		synchronized (mListeners) { 
			for (int i=0; i < mListeners.size(); ) { 
				WeakReference<FetcherListener> ref = mListeners.get(i); 
				FetcherListener lsr = ref != null ? ref.get() : null; 
				if (lsr == null) { 
					mListeners.remove(i); continue; 
				} else
					lsr.onClosed(fetcher); 
				i ++; 
			}
		}
	}
	
	void closeFetcher(Fetcher fetcher) { 
		closeTask(fetcher.getTask()); 
		
		synchronized (this) {
			for (int i=0; i < mFetchers.size(); ) { 
				if (fetcher == mFetchers.get(i)) { 
					mFetchers.remove(i); 
					continue; 
				}
				i ++; 
			}
			
			onFetcherClosed(fetcher); 
			removeFetching(fetcher);
		}
	}
	
	void closeTask(FetchTask task) {
		if (task == null || task.isScheduled() || task.isStarted()) 
			return; 
		
		synchronized (mTasksQueue) {
			String source = task.getSource(); 
			
			for (int i=0; i < mTasksQueue.size(); i++) {
				FetchTask dt = mTasksQueue.get(i); 
				if (dt == null) continue; 
				if (task == dt || source.equals(dt.getSource())) 
					return; 
			}
			
			if (task.setClosed()) 
				mTasksMap.remove(source); 
		}
		
		nextFetcher(); // start next
	}
	
	public FetchTask offerTask(String source) { 
		return offerTask(source, null);
	}
	
	public FetchTask offerTask(String source, FetchCache cache) {
		if (source == null || source.length() == 0) 
			return null; 
		
		synchronized (mTasksQueue) {
			FetchTask task = mTasksMap.get(source); 
			if (task == null) {
				task = newTask(source, cache); 
				mTasksQueue.add(task); 
				mTasksMap.put(source, task); 
			}
			
			return task; 
		}
	}
	
	public FetchTask newTask(String source) { 
		return newTask(source, null);
	}
	
	public FetchTask newTask(String source, FetchCache cache) { 
		if (LOG.isDebugEnabled())
			LOG.debug("new task: " + source);
		
		return new FetchTask(this, source, cache); 
	}
	
	public FetchTask getTask(String source) {
		if (source == null || source.length() == 0) 
			return null; 
		
		synchronized (mTasksQueue) {
			return mTasksMap.get(source); 
		}
	}
	
	public boolean stopTask(String source) { 
		if (source == null || source.length() == 0) 
			return false; 
		
		Fetcher fetcher = null; 
		synchronized (this) {
			for (Fetcher d : mFetchers) { 
				if (source.equals(d.getTask().getSource())) { 
					fetcher = d; 
					break; 
				}
			}
		}
		
		if (fetcher != null) { 
			if (source.equals(fetcher.getTask().getSource())) 
				return fetcher.stop(); 
		}
		
		return false; 
	}
	
}
