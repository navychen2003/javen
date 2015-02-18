package org.javenstudio.cocoka.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.widget.activity.BaseActivity;
import org.javenstudio.cocoka.widget.activity.BaseActivityGroup;
import org.javenstudio.common.util.Logger;

public class ActivityHelper {
	private static Logger LOG = Logger.getLogger(ActivityHelper.class);

	public static Handler getHandler() { 
		return Implements.getHandler();
	}
	
	public static Context startActivity(Activity fromActivity, 
			String className, Intent intent) { 
		if (className == null && intent == null) 
			return null;
		
		Context context = fromActivity; 
		if (context == null) 
			context = ResourceHelper.getContext(); 
		
		if (intent == null) 
			intent = new Intent();
		
		if (className != null)
			intent.setClassName(context, className);
		
		if (context instanceof BaseActivity) { 
			((BaseActivity)context).startActivitySafely(intent); 
			return context;
		}
		
		context.startActivity(intent);
		
		return context;
	}
	
	public static void setAppMessage(ModuleApp[] apps, 
			String className, String message) { 
		if (apps == null || className == null || className.length() == 0) 
			return;
		
		for (int i=0; apps != null && i < apps.length; i++) { 
			ModuleApp app = apps[i]; 
			if (app == null) continue; 
			
			if (app.getClass().getName().equals(className)) { 
				app.setAppMessage(message);
				continue;
			}
			
			String activityName = app.getActivityClassName();
			if (activityName != null && activityName.equals(className)) { 
				app.setAppMessage(message);
				continue;
			}
		}
	}
	
	public static void finishActivities() { 
		//// disabled after 2.2
		//ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		//am.restartPackage(packageName);
		
		BaseActivity[] activities = BaseActivity.getActivities();
		BaseActivityGroup[] activityGroups = BaseActivityGroup.getActivityGroups();
		
		for (int i=0; activities != null && i < activities.length; i++) { 
			BaseActivity activity = activities[i];
			if (activity != null && !activity.isFinishing()) { 
				if (LOG.isDebugEnabled())
					LOG.debug("finishActivities: finish Activity: " + activity.getClass().getName());
				activity.requestFinish();
			}
		}
		
		for (int i=0; activityGroups != null && i < activityGroups.length; i++) { 
			BaseActivityGroup activity = activityGroups[i];
			if (activity != null && !activity.isFinishing()) {
				if (LOG.isDebugEnabled())
					LOG.debug("finishActivities: finish ActivityGroup: " + activity.getClass().getName());
				activity.requestFinish();
			}
		}
	}
	
}
