package org.javenstudio.android;

import java.util.List;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class ServiceHelper {
	private static final Logger LOG = Logger.getLogger(ServiceHelper.class);

	public static PendingIntent getServicePendingIntent(int requestCode, Intent intent, int flags) { 
		Context context = ResourceHelper.getContext(); 
		intent.setClass(context, AndroidService.class);
        return PendingIntent.getService(context, requestCode, intent, flags);
	}
	
	public static PendingIntent getBroadcastPendingIntent(int requestCode, Intent intent, int flags) { 
		Context context = ResourceHelper.getContext(); 
		intent.setClass(context, AndroidReceiver.class);
        return PendingIntent.getService(context, requestCode, intent, flags);
	}
	
	public static PendingIntent getActivityPendingIntent(int requestCode, Intent intent, int flags) { 
		Context context = ResourceHelper.getContext(); 
		return PendingIntent.getActivity(context, requestCode, intent, flags); 
	}
	
	public static void actionService(Context context, Intent intent) { 
		if (intent != null) { 
			if (checkServicesEnabled(context)) { 
				intent.setClass(context, AndroidService.class);
		        context.startService(intent);
		        
			} else { 
				if (LOG.isDebugEnabled()) 
					LOG.debug("actionService: service not enabled");
			}
		}
	}
	
	public static void checkServiceStart(Context context) { 
		if (checkServicesEnabled(context)) { 
			AndroidService.actionServiceStart(context);
			
		} else { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("checkServiceStart: service not enabled");
		}
	}
	
	public static boolean checkServicesEnabled(Context context) { 
		if (isServicesEnabled(context)) return true;
		return setServicesEnabled(AndroidService.checkServicesEnabled(context)); 
	}
	
	public static boolean isServicesEnabled(Context context) { 
		PackageManager pm = context.getPackageManager();
		
		return pm.getComponentEnabledSetting(new ComponentName(context, AndroidService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED; 
	}
	
	private static boolean setServicesEnabled(boolean enabled) { 
		Context context = ResourceHelper.getContext(); 
        PackageManager pm = context.getPackageManager();
        
        //pm.setComponentEnabledSetting(
        //        new ComponentName(context, MessageCompose.class),
        //        enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
        //            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        //        PackageManager.DONT_KILL_APP);
        
        //pm.setComponentEnabledSetting(
        //        new ComponentName(context, AccountShortcutPicker.class),
        //        enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
        //            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        //        PackageManager.DONT_KILL_APP);
        
        //pm.setComponentEnabledSetting(
        //        new ComponentName(context, AndroidReceiver.class),
        //        enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
        //            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        //        PackageManager.DONT_KILL_APP);
        
        pm.setComponentEnabledSetting(
                new ComponentName(context, AndroidService.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        
        return enabled; 
    }
	
	public static boolean isForeground() {
		return isForeground(ResourceHelper.getContext());
	}
	
	public static boolean isForeground(Context context) {
		ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		if (activityManager == null) 
			return false;
		
		List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo process : processes) {
			if (process == null || process.processName == null) 
				continue;
			
			if (process.processName.equals(context.getPackageName())) 
				return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND; 
		}
		
		return false;
	}
	
}
