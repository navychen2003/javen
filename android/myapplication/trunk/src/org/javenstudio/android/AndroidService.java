package org.javenstudio.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

import org.javenstudio.common.util.Logger;

public final class AndroidService extends Service implements ServiceCallback {
	private static final Logger LOG = Logger.getLogger(AndroidService.class);
	
	@Override
	public AlarmManager getAlarmManager() { 
		return (AlarmManager)getSystemService(Context.ALARM_SERVICE);
	}
	
	@Override
	public AudioManager getAudioManager() { 
		return (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	@Override
	public NotificationManager getNotificationManager() { 
		return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public void requestUpdateWidget() { 
		Intent widgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        sendBroadcast(widgetIntent);
	}
	
	@Override
    public IBinder onBind(Intent intent) {
		if (LOG.isDebugEnabled())
			LOG.debug("onBind: intent=" + intent);
		
        return null;
    }
	
	@Override
	public void onCreate() {
		if (LOG.isDebugEnabled())
			LOG.debug("onCreate");
		
		super.onCreate();
		onServiceCreate(this);
    }
	
	@Override
	public void onLowMemory() {
		if (LOG.isDebugEnabled())
			LOG.debug("onLowMemory");
		
		onServiceLowMemory(this);
		super.onLowMemory();
    }
	
	@Override
	public void onDestroy() {
		if (LOG.isDebugEnabled())
			LOG.debug("onDestroy");
		
		onServiceDestroy(this);
		super.onDestroy();
    }
	
	@Override 
	public void requestStopSelf(int startId) { 
		if (canStopSelf()) 
			stopSelf(startId);
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onStartCommand: intent=" + intent 
					+ " flags=" + flags + " startId=" + startId);
		}
		
        super.onStartCommand(intent, flags, startId);
        
        handleCommand(this, intent, flags, startId);
        
        // Returning START_NOT_STICKY means that if a mail check is killed (e.g. due to memory
        // pressure, there will be no explicit restart.  This is OK;  Note that we set a watchdog
        // alarm before each mailbox check.  If the mailbox check never completes, the watchdog
        // will fire and get things running again.
        return START_NOT_STICKY;
	}
	
	private static List<WeakReference<ServiceHandler>> mHandlerRefs = 
			new ArrayList<WeakReference<ServiceHandler>>(); 
	
	public static void registerHandler(ServiceHandler handler) { 
		if (handler == null) return; 
		
		synchronized (mHandlerRefs) { 
			boolean found = false; 
			
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				} else if (sh == handler) 
					found = true; 
				i ++; 
			}
			
			if (!found) 
				mHandlerRefs.add(new WeakReference<ServiceHandler>(handler)); 
		}
	}
	
	public static void unregisterHandler(ServiceHandler handler) { 
		if (handler == null) return; 
		
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null || sh == handler) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				i ++; 
			}
		}
	}
	
	static void onServiceCreate(ServiceCallback callback) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.onServiceCreate(callback); 
				i ++; 
			}
		}
	}
	
	static void onServiceDestroy(ServiceCallback callback) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.onServiceDestroy(callback); 
				i ++; 
			}
		}
	}
	
	static void onServiceLowMemory(ServiceCallback callback) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.onServiceLowMemory(callback); 
				i ++; 
			}
		}
	}
	
	static void handleCommand(ServiceCallback callback, Intent intent, int flags, int startId) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.handleCommand(callback, intent, flags, startId); 
				i ++; 
			}
		}
	}
	
	static boolean checkServicesEnabled(Context context) { 
		boolean enabled = false; 
		
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				if (sh.checkServiceEnabled(context)) 
					enabled = true; 
				i ++; 
			}
		}
		
		return enabled; 
	}
	
	static boolean canStopSelf() { 
		boolean result = true; 
		
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				if (!sh.canStopSelf()) 
					result = false; 
				i ++; 
			}
		}
		
		return result; 
	}
	
	static void actionServiceStart(Context context) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ServiceHandler> ref = mHandlerRefs.get(i); 
				ServiceHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.actionServiceStart(context); 
				i ++; 
			}
		}
	}
	
}
