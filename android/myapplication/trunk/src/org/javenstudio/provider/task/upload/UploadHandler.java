package org.javenstudio.provider.task.upload;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.ReceiverCallback;
import org.javenstudio.android.ReceiverHandler;
import org.javenstudio.android.ServiceCallback;
import org.javenstudio.android.ServiceHandlerBase;
import org.javenstudio.android.ServiceHelper;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.ConfirmCallback;
import org.javenstudio.common.util.Logger;

public class UploadHandler extends ServiceHandlerBase implements ReceiverHandler {
	private static final Logger LOG = Logger.getLogger(UploadHandler.class);
	
    private static final String ACTION_RESCHEDULE =
    		UploadHandler.class.getName() + ".RESCHEDULE";
	
    public static void actionReschedule(Context context) {
    	actionReschedule(context, false);
    }
    
    public static void actionReschedule(final Context context, boolean forceStart) {
    	if (context == null) return;
    	if (!NetworkHelper.getInstance().isNetworkAvailable()) return;
    	
    	if (!forceStart && context instanceof IActivity) { 
    		IActivity activity = (IActivity)context;
    		
    		ConfirmCallback callback = new ConfirmCallback() {
					@Override
					public void onYesClick() {
						if (LOG.isDebugEnabled())
		    				LOG.debug("actionReschedule: onYesClick");
						
						actionReschedule(context, true);
					}
					@Override
					public void onNoClick() {
						if (LOG.isDebugEnabled())
		    				LOG.debug("actionReschedule: onNoClick");
					}
	    		};
    		
    		if (!(NetworkHelper.getInstance().isWifiAvailable() || 
    				activity.getActivityHelper().confirmAutoFetch(callback))) {
    			
    			if (LOG.isDebugEnabled())
    				LOG.debug("actionReschedule: confirm start at mobile network");
    			
    			return;
    		}
    	}
    	
    	// only start from activity
    	if (context instanceof IActivity) {
	        Intent intent = new Intent();
	        intent.setAction(ACTION_RESCHEDULE);
	        ServiceHelper.actionService(context, intent);
    	}
    }
    
    private final AccountApp mApp;
    private final UploadNotification mNotifier;
	private final UploadQueue mQueue = new UploadQueue();
	
	private final Map<Long, UploadDataInfo> mUploadInfos = 
			new HashMap<Long, UploadDataInfo>();
	
	private final Map<String, IUploader> mUploaders = 
			new HashMap<String, IUploader>();
	
	public UploadHandler(AccountApp app, UploadNotification notifier) { 
		if (app == null || notifier == null) throw new NullPointerException();
		mApp = app;
		mNotifier = notifier;
	}
	
	public AccountApp getAccountApp() { return mApp; }
	public UploadNotification getNotifier() { return mNotifier; }
    
	public void addUploader(IUploader uploader) { 
		synchronized (this) { 
			if (uploader != null) 
				mUploaders.put(uploader.getPrefix(), uploader);
		}
	}
	
	public IUploader getUploader(String prefix) { 
		synchronized (this) { 
			return mUploaders.get(prefix);
		}
	}
	
	@Override
	public boolean checkServiceEnabled(Context context) {
		return NetworkHelper.getInstance().isNetworkAvailable();
	}
	
	@Override
	public void actionServiceStart(Context context) {
		if (LOG.isDebugEnabled()) LOG.debug("actionServiceStart");
		actionReschedule(context);
	}
	
	@Override
	public boolean handleIntent(ReceiverCallback callback, Intent intent) { 
		final String action = intent.getAction();
		
		if (UploadHelper.ACTION_LIST.equals(action)) { 
			launchUploadList(getAccountApp().getContext());
			return true;
		}
		
		return false;
	}
	
	protected void launchUploadList(Context context) {}
	
	@Override
	public boolean handleCommand(ServiceCallback callback, Intent intent,
			int flags, int startId) {
		final String action = intent.getAction();
		
		if (ACTION_RESCHEDULE.equals(action)) { 
			if (!reschedule(callback)) 
				callback.requestStopSelf(startId);
			
			return true;
		}
		
		return false;
	}
	
    /**
     * Parses data from the content provider into private array
     */
    private boolean reschedule(ServiceCallback callback) {
    	if (LOG.isDebugEnabled())
			LOG.debug("reschedule: start");
    	
    	try {
    		synchronized (this) {
    			return doReschedule(callback);
    		}
    	} finally {
    		if (LOG.isDebugEnabled())
    			LOG.debug("reschedule: done.");
    	}
    }

    private boolean doReschedule(ServiceCallback callback) { 
    	AccountUser account = getAccountApp().getAccount();
    	if (account == null) return false;
    	
    	final UploadData[] uploadDatas = ContentHelper.getInstance()
    			.queryUploads(Long.toString(account.getAccountId()));
    	
        // for each update from the database, remember which download is
        // supposed to get restarted soonest in the future
        long wakeUp = 0;
        long now = System.currentTimeMillis();
        boolean keepService = false;
        
        for (int i=0; uploadDatas != null && i < uploadDatas.length; i++) { 
        	UploadData data = uploadDatas[i];
        	if (data == null) continue;
    	
        	UploadDataInfo info;
        	synchronized (mUploadInfos) { 
	    		info = mUploadInfos.get(data.getId());
	    		if (info == null) { 
	    			info = new UploadDataInfo(data);
	    	    	mUploadInfos.put(info.getUploadId(), info);
	    		} else { 
	    			info.setUploadData(data);
	    		}
        	}
    		
    		if (info.isUploadFinished() || info.isUploadAborted()) { 
    			long updateTime = info.getUpdateTime();
    			long finishTime = info.getFinishTime();
    			if (finishTime > updateTime) updateTime = finishTime;
    			
    			if (now - updateTime > 60 * 60 * 1000) { 
	    			long uploadId = info.getUploadId();
	        		info.onRemove();
	        		synchronized (mUploadInfos) { 
	        			mUploadInfos.remove(uploadId);
	        		}
	        		onUploadRemoved(uploadId);
    			}
    			continue;
        	}
    		
    		if (info.isReadyToStart(now) && NetworkHelper.getInstance().isNetworkAvailable()) 
    			enqueueUpload(info);
    		
            long next = info.nextAction(now);
            if (next == 0) 
                keepService = true;
            else if (next > 0 && next < wakeUp) 
                wakeUp = next;
        }
        
        if (wakeUp > 0) 
            scheduleAlarm(callback, wakeUp);
        
        startUploadThread();
        
        return keepService;
    }
    
    public void updateNotification() { 
    	synchronized (mUploadInfos) { 
    		getNotifier().updateNotification(mUploadInfos.values());
    	}
    }
    
    public void cancelAllNotifications() { 
    	synchronized (mUploadInfos) { 
    		getNotifier().cancelAllNotifications();
    	}
    }
    
    private void scheduleAlarm(ServiceCallback callback, long wakeUp) {
        AlarmManager alarms = callback.getAlarmManager();
        if (alarms == null) {
        	if (LOG.isErrorEnabled())
            	LOG.error("scheduleAlarm: couldn't get alarm manager");
            return;
        }

        if (LOG.isDebugEnabled()) 
            LOG.debug("scheduleAlarm: scheduling retry in " + wakeUp + " ms");

        //Intent intent = new Intent(Constants.ACTION_RETRY);
        //intent.setClassName("com.android.providers.downloads",
        //        DownloadReceiver.class.getName());
        //alarms.set(AlarmManager.RTC_WAKEUP,
        //        mSystemFacade.currentTimeMillis() + wakeUp,
        //        PendingIntent.getBroadcast(DownloadService.this, 0, intent,
        //                PendingIntent.FLAG_ONE_SHOT));
    }
    
    private final List<WeakReference<UploadStreamListener>> mListenerRefs = 
    		new ArrayList<WeakReference<UploadStreamListener>>();
    
    public void addUploadStreamListener(UploadStreamListener listener) { 
    	if (listener == null) return;
    	
    	synchronized (mListenerRefs) { 
    		boolean found = false;
    		for (int i=0; i < mListenerRefs.size(); ) { 
    			WeakReference<UploadStreamListener> ref = mListenerRefs.get(i); 
    			UploadStreamListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListenerRefs.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found) 
    			mListenerRefs.add(new WeakReference<UploadStreamListener>(listener));
    	}
    }
    
    public void removeUploadStreamListener(UploadStreamListener listener) { 
    	if (listener == null) return;
    	
    	synchronized (mListenerRefs) { 
    		for (int i=0; i < mListenerRefs.size(); ) { 
    			WeakReference<UploadStreamListener> ref = mListenerRefs.get(i); 
    			UploadStreamListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null || lsr == listener) { 
    				mListenerRefs.remove(i); continue; 
    			}
    			i ++; 
    		}
    	}
    }
    
    /* package */void onUploadFileRead(long uploadId, long readSize, long totalSize) { 
    	synchronized (mUploadInfos) { 
    		UploadDataInfo info = mUploadInfos.get(uploadId);
    		if (info != null) {
    			info.setTotalBytes(totalSize);
    			info.setCurrentBytes(readSize);
    		}
    	}
    	
    	synchronized (mListenerRefs) { 
    		for (int i=0; i < mListenerRefs.size(); ) { 
    			WeakReference<UploadStreamListener> ref = mListenerRefs.get(i); 
    			UploadStreamListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListenerRefs.remove(i); continue; 
    			} else { 
    				listener.onUploadRead(uploadId, readSize, totalSize);
    			}
    			i ++; 
    		}
    	}
    	
    	updateNotification();
    }
    
    /* package */void onUploadPending(long uploadId) { 
    	synchronized (mListenerRefs) { 
    		for (int i=0; i < mListenerRefs.size(); ) { 
    			WeakReference<UploadStreamListener> ref = mListenerRefs.get(i); 
    			UploadStreamListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListenerRefs.remove(i); continue; 
    			} else { 
    				listener.onUploadPending(uploadId);
    			}
    			i ++; 
    		}
    	}
    }
    
    /* package */void onUploadRemoved(long uploadId) { 
    	synchronized (mListenerRefs) { 
    		for (int i=0; i < mListenerRefs.size(); ) { 
    			WeakReference<UploadStreamListener> ref = mListenerRefs.get(i); 
    			UploadStreamListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListenerRefs.remove(i); continue; 
    			} else { 
    				listener.onUploadRemoved(uploadId);
    			}
    			i ++; 
    		}
    	}
    }
    
    public synchronized boolean cancelUpload(long id) { 
    	synchronized (mUploadInfos) { 
    		if (hasUploadInQueueRunning(id)) { 
    			UploadDataInfo info = mUploadInfos.get(id);
    			if (info != null && info.getUploadId() == id) { 
    				info.postAbortUpload();
    				return true;
    			}
    		} else {
	    		UploadDataInfo info = mUploadInfos.get(id);
	    		if (info != null && info.getUploadId() == id && !info.isUploadFinished()) { 
	    			mUploadInfos.remove(id);
	    			mQueue.cancelUploadInQueue(id);
	    			
	    			info.onRemove();
	    			onUploadRemoved(id);
	    			
	    			return true;
	    		}
    		}
    		
    		return false;
    	}
    }
    
    private void enqueueUpload(UploadDataInfo info) {
    	if (info != null) mQueue.enqueueUpload(info);
    }
    
    public boolean hasUploadInQueue(long id) {
    	return mQueue.hasUploadInQueue(id);
    }
    
    public boolean hasUploadInQueuePending(long id) {
    	return mQueue.hasUploadInQueuePending(id);
    }
    
    public boolean hasUploadInQueueRunning(long id) {
    	return mQueue.hasUploadInQueueRunning(id);
    }
    
    public void dequeueUpload(long id) { mQueue.dequeueUpload(id); }
    private void startUploadThread() { mQueue.startUploadThread(); }
    
    private final class UploadQueue {
    	private static final int MAX_CONCURRENT_UPLOADS = 1;
    	
    	private final Map<Long, UploadDataInfo> mUploadsInProgress =
                new HashMap<Long, UploadDataInfo>();
    	
        private final Map<Long, UploadDataInfo> mUploadsQueue =
                new TreeMap<Long, UploadDataInfo>(new Comparator<Long>() {
					@Override
					public int compare(Long lhs, Long rhs) {
						long lnum = lhs != null ? lhs.longValue() : 0;
						long rnum = rhs != null ? rhs.longValue() : 0;
						return lnum > rnum ? 1 : (lnum < rnum ? -1 : 0);
					}
                });
        
        synchronized void enqueueUpload(UploadDataInfo info) {
        	if (info == null) return;
        	final long uploadId = info.getUploadId();
        	
            if (!hasUploadInQueue(uploadId)) {
                if (LOG.isDebugEnabled()) 
                    LOG.debug("enqueueUpload: id=" + uploadId + " uri=" + info.getContentUri());
                
                mUploadsQueue.put(uploadId, info);
                
                info.onPending();
                info.setQueueTime(System.currentTimeMillis());
                
                onUploadPending(uploadId);
                startUploadThread();
            }
        }

        synchronized void startUploadThread() {
            final long current = System.currentTimeMillis();
            final Long[] inkeys = mUploadsInProgress.keySet().toArray(new Long[0]);
            
            for (int i=0; inkeys != null && i < inkeys.length; i++) { 
            	Long id = inkeys[i];
            	final UploadDataInfo info = id != null ? mUploadsInProgress.get(id) : null;
            	if (id == null || info == null) { 
            		mUploadsInProgress.remove(id);
            		continue;
            	}
            	
            	if (!info.isUploadRunning()) { 
            		if (info.isUploadDone()) { 
            			if (LOG.isDebugEnabled()) {
            				LOG.debug("startUploadThread: remove finished upload: " 
            						+ id + " from inProgress");
            			}
            			
            			mUploadsInProgress.remove(id);
            			continue;
            		}
            		
            		if (info.isUploadPending() && current - info.getQueueTime() > 10000) { 
            			if (LOG.isDebugEnabled()) {
            				LOG.debug("startUploadThread: remove not-run upload: " 
            						+ id + " from inProgress");
            			}
            			
            			mUploadsInProgress.remove(id);
            			continue;
            		}
            	}
            }
            
            Iterator<Long> keys = mUploadsQueue.keySet().iterator();
            ArrayList<Long> ids = new ArrayList<Long>();
            
            while (mUploadsInProgress.size() < MAX_CONCURRENT_UPLOADS && keys.hasNext()) {
                Long id = keys.next();
                final UploadDataInfo info = mUploadsQueue.get(id);
                ids.add(id);
                
                if (info == null) continue;
                
                IUploader uploader = getUploader(info.getPrefix());
                if (uploader == null) { 
                	if (LOG.isWarnEnabled()) {
                		LOG.warn("startUploadThread: no uploader: " + info.getPrefix() 
                				+ " for " + id);
                	}
                	
                	ResourceHelper.getHandler().post(new Runnable() {
	        				@Override
	        				public void run() {
	        					info.onFailed(UploadData.CODE_NOUPLOADER, null);
	        					dequeueUpload(info.getUploadId());
	        				}
	        			});
                	continue;
                }
                
                try {
	                if (!uploader.startUploadThread(info)) { 
	                	if (LOG.isWarnEnabled()) {
	                		LOG.warn("startUploadThread: uploader: " + uploader 
	                				+ " start failed for " + id);
	                	}
	                	continue;
	                }
	                
	                mUploadsInProgress.put(id, mUploadsQueue.get(id));
	                
	                if (LOG.isDebugEnabled()) 
	                    LOG.debug("startUploadThread: started upload for " + id);
	                
                } catch (Throwable e) { 
                	if (LOG.isWarnEnabled()) {
                		LOG.warn("startUploadThread: uploader: " + uploader 
                				+ " start failed for " + id + " cause " 
                				+ e.toString(), e);
                	}
                }
            }
            
            for (Long id : ids) {
                mUploadsQueue.remove(id);
            }
            
            updateNotification();
            
            if (LOG.isDebugEnabled()) {
            	LOG.debug("startUploadThread: " + ids.size() + " upload started," 
            			+ " queueCount=" + mUploadsQueue.size() 
            			+ " runningCount=" + mUploadsInProgress.size());
            }
        }

        synchronized boolean hasUploadInQueue(long id) {
            return mUploadsQueue.containsKey(id) || mUploadsInProgress.containsKey(id);
        }

        synchronized boolean hasUploadInQueuePending(long id) {
            return mUploadsQueue.containsKey(id);
        }
        
        synchronized boolean hasUploadInQueueRunning(long id) {
            return mUploadsInProgress.containsKey(id);
        }
        
        synchronized boolean cancelUploadInQueue(long id) { 
        	if (LOG.isDebugEnabled()) 
                LOG.debug("cancelUploadInQueue: id=" + id);
        	
        	return mUploadsQueue.remove(id) != null;
        }
        
        synchronized void dequeueUpload(long id) {
        	if (LOG.isDebugEnabled()) 
                LOG.debug("dequeueUpload: id=" + id);
        	
            mUploadsInProgress.remove(id);
            startUploadThread();
            
            if (mUploadsInProgress.size() == 0 && mUploadsQueue.size() == 0) {
                notifyAll();
                cancelAllNotifications();
            }
        }
    }
    
}
