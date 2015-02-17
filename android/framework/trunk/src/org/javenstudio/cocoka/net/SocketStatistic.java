package org.javenstudio.cocoka.net;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.net.ConnectivityManager;

import org.javenstudio.cocoka.net.metrics.TMetricsHelper;
import org.javenstudio.cocoka.net.metrics.TMetricsRecord;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class SocketStatistic {

	public static interface SocketListener { 
		public void onSocketRead(int type, int bytes);
		public void onSocketWrite(int type, int bytes);
	}
	
	private static final List<WeakReference<SocketListener>> mListeners = 
			new ArrayList<WeakReference<SocketListener>>(); 
	
	public static void addSocketListener(SocketListener listener) { 
    	synchronized (mListeners) { 
    		boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<SocketListener> ref = mListeners.get(i); 
    			SocketListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		
    		if (listener != null) {
    			if (!found) 
    				mListeners.add(new WeakReference<SocketListener>(listener)); 
    		}
    	}
    }
    
    public static void onSocketRead(int type, int bytes) { 
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<SocketListener> ref = mListeners.get(i); 
    			SocketListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				listener.onSocketRead(type, bytes);
    			}
    			i ++; 
    		}
    	}
    }
	
    public static void onSocketWrite(int type, int bytes) { 
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<SocketListener> ref = mListeners.get(i); 
    			SocketListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				listener.onSocketWrite(type, bytes);
    			}
    			i ++; 
    		}
    	}
    }
    
	public static class Statistic { 
		public final long mobileReadBytes, mobileWriteBytes; 
		public final long wifiReadBytes, wifiWriteBytes; 
		public final long firstTime, lastTime;
		
		private Statistic(long mr, long mw, long wr, long ww, long ft, long lt) { 
			mobileReadBytes = mr;
			mobileWriteBytes = mw;
			wifiReadBytes = wr; 
			wifiWriteBytes = ww;
			firstTime = ft;
			lastTime = lt;
		}
	}
	
	public static Statistic queryStatistic() { 
		long mobileReadBytes = 0, mobileWriteBytes = 0; 
		long wifiReadBytes = 0, wifiWriteBytes = 0; 
		long firstTime = SocketMetrics.getStartTime(); 
		long lastTime = 0;
		
		SimpleMemoryDB.TCursor<TMetricsRecord> cursor = TMetricsHelper.queryRecords();
		try { 
			while (cursor.hasNext()) { 
				TMetricsRecord record = cursor.next(); 
				if (record == null) continue;
				
				long updateTime = record.updateTime != null ? record.updateTime.longValue() : 0;
				//if (firstTime == 0 || updateTime < firstTime) firstTime = updateTime;
				if (lastTime == 0 || lastTime < updateTime) lastTime = updateTime;
				
				if (SocketMetrics.READ_ACTION.equals(record.name)) { 
					if (record.longValue != null) { 
						if (record.type != null && record.type == ConnectivityManager.TYPE_WIFI) 
							wifiReadBytes += record.longValue; 
						else 
							mobileReadBytes += record.longValue; 
					}
					
				} else if (SocketMetrics.WRITE_ACTION.equals(record.name)) { 
					if (record.longValue != null) { 
						if (record.type != null && record.type == ConnectivityManager.TYPE_WIFI) 
							wifiWriteBytes += record.longValue; 
						else 
							mobileWriteBytes += record.longValue; 
					}
				}
			}
		} finally { 
			if (cursor != null) 
				cursor.close();
		}
		
		return new Statistic(mobileReadBytes, mobileWriteBytes, 
				wifiReadBytes, wifiWriteBytes, firstTime, lastTime);
	}
	
}
