package org.javenstudio.cocoka.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;

import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.worker.work.Scheduler;
import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.util.Logger;

public class ResourceHelper {
	private static final Logger LOG = Logger.getLogger(ResourceHelper.class);

	private static final AtomicLong sIdentityCounter = new AtomicLong(0);
	
	public static long getIdentity() { 
		return sIdentityCounter.incrementAndGet();
	}
	
	private static final List<WeakReference<Activity>> sActivities = 
			new ArrayList<WeakReference<Activity>>();
	
	public static void addActivity(Activity activity) { 
		synchronized (sActivities) { 
			boolean found = false;
			
			for (int i=0; i < sActivities.size(); ) { 
				WeakReference<Activity> ref= sActivities.get(i);
				Activity a = ref != null ? ref.get() : null;
				if (a != null) { 
					if (a == activity) found = true;
					i ++;
					continue;
				}
				sActivities.remove(i);
			}
			
			if (!found && activity != null) 
				sActivities.add(new WeakReference<Activity>(activity));
		}
	}
	
	public static void exitProcess() { 
		synchronized (sActivities) { 
			try { 
				for (int i=0; i < sActivities.size(); i++) { 
					WeakReference<Activity> ref= sActivities.get(i);
					Activity a = ref != null ? ref.get() : null;
					if (a != null) a.finish();
				}
			} catch (Throwable e) { 
				if (LOG.isWarnEnabled())
					LOG.warn("exitProcess: " + e, e);
			}
			
			System.exit(0);
		}
	}
	
	public static String getLanguage() { 
		return getLanguage(getContext());
	}
	
	public static String getLanguage(Context context) { 
		final Locale locale = context.getResources().getConfiguration().locale;
		return locale.getLanguage();
	}
	
	public static boolean isLanguageZh() { 
		return isLanguageZh(getContext());
	}
	
	public static boolean isLanguageZh(Context context) { 
		String lang = getLanguage(context);
		if (lang != null && lang.equalsIgnoreCase("zh")) 
			return true;
		
		return false;
	}
	
	public static Context getContext() { 
		return Implements.getContext();
	}
	
	public static Resources getResources() { 
		return Implements.getContext().getResources();
	}
	
	public static Application getApplication() { 
		return Implements.getApplication();
	}
	
	public static Handler getHandler() { 
		return Implements.getHandler();
	}
	
	public static EntityObserver.Handler getEntityObserverHandler() { 
		return Implements.getEntityObserverHandler();
	}
	
	public static Scheduler getScheduler() { 
		return Implements.getScheduler();
	}
	
	public static BitmapHolder getBitmapHolder() { 
		return Implements.getInstance().getGlobalBitmapHolder();
	}
	
	public static final NetworkMonitor getNetworkMonitor() { 
		return Implements.getNetworkMonitor();
	}
	
	public static final ModuleManager getModuleManager() { 
		return Implements.getModuleManager();
	}
	
	public static final ModuleApp[] getModuleApps() { 
		return Implements.getModuleApps();
	}
	
	public static ResourceContext getResourceContext() { 
		return Implements.getResourceContext();
	}
	
	public static SharedPreferences getSharedPreferences() { 
		return Implements.getSharedPreferences();
	}
	
	public static int getResourceWithId(int id) { 
		return Implements.getInstance().getResourceWithId(id); 
	}
	
	static PluginManager.PackageInfo[] getResourcePackages() { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.getResourcePackages();
		}
		
		return null;
	}
	
	static PluginManager.PackageInfo getSelectedPackage() { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.getSelectedPackage();
		}
		
		return null;
	}
	
	static boolean setSelectedPackage(String packageName) { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.setSelectedPackage(packageName);
		}
		
		return false;
	}
	
	static void setViewBinder(ResourceManager.ViewBinder binder) { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			manager.setViewBinder(binder);
		}
	}
	
	public static String getPreferenceString(String key, String defValue) { 
		try { 
			String result = ResourceHelper.getSharedPreferences().getString(key, defValue); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getPreferenceString: field=" + key + " value=" + result);
			
			return result;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
		}
		
		return defValue;
	}
	
	public static boolean setPreference(String key, String value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putString(key, value); 
			editor.commit(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("setPreference: field=" + key + " value=" + value);
			
			return true;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
			
			return false;
		}
	}
	
	public static boolean getPreferenceBoolean(String key, boolean defValue) { 
		try { 
			boolean result = ResourceHelper.getSharedPreferences().getBoolean(key, defValue); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getPreferenceBoolean: field=" + key + " value=" + result);
			
			return result;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
		}
		
		return defValue;
	}
	
	public static boolean setPreference(String key, boolean value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putBoolean(key, value); 
			editor.commit(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("setPreference: field=" + key + " value=" + value);
			
			return true;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
			
			return false;
		}
	}
	
	public static int getPreferenceInt(String key, int defValue) { 
		try { 
			int result = ResourceHelper.getSharedPreferences().getInt(key, defValue); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getPreferenceInt: field=" + key + " value=" + result);
			
			return result;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
		}
		return defValue;
	}
	
	public static boolean setPreference(String key, int value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putInt(key, value); 
			editor.commit(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("setPreference: field=" + key + " value=" + value);
			
			return true;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
			
			return false;
		}
	}
	
	public static long getPreferenceLong(String key, long defValue) { 
		try { 
			long result = ResourceHelper.getSharedPreferences().getLong(key, defValue); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getPreferenceLong: field=" + key + " value=" + result);
			
			return result;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
		}
		return defValue;
	}
	
	public static boolean setPreference(String key, long value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putLong(key, value); 
			editor.commit(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("setPreference: field=" + key + " value=" + value);
			
			return true;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
			
			return false;
		}
	}
	
	public static float getPreferenceFloat(String key, float defValue) { 
		try { 
			float result = ResourceHelper.getSharedPreferences().getFloat(key, defValue); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("getPreferenceFloat: field=" + key + " value=" + result);
			
			return result;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
		}
		return defValue;
	}
	
	public static boolean setPreference(String key, float value) { 
		try { 
			SharedPreferences.Editor editor = ResourceHelper.getSharedPreferences().edit(); 
			editor.putFloat(key, value); 
			editor.commit(); 
			
			if (LOG.isDebugEnabled())
				LOG.debug("setPreference: field=" + key + " value=" + value);
			
			return true;
		} catch (Throwable ex) { 
			if (LOG.isDebugEnabled())
				LOG.debug("preference field: " + key + " error", ex);
			
			return false;
		}
	}
	
}
