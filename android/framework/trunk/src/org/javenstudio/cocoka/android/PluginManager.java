package org.javenstudio.cocoka.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import org.javenstudio.common.util.Logger;

public final class PluginManager extends BroadcastReceiver {
	private static Logger LOG = Logger.getLogger(PluginManager.class);

	public interface OnPackageListener { 
		public void onPackageInit(String category, PackageInfo packageInfo);
		public void onPackageRelease(String category, PackageInfo packageInfo);
	}
	
	public interface OnSelectedPackageListener { 
		public void onSelectedPackageChanged(String category, PackageInfo selectedPackage); 
	}
	
	public static final class PackageInfo { 
		private final android.content.pm.PackageInfo mPackage;
		private final ResolveInfo mResolveInfo;
		private final ComponentName mComponentName; 
		private final CharSequence mTitle; 
		private final String mVersionName;
		private final Drawable mIcon; 
		
		private Drawable mSmallIcon = null;
		private CharSequence mSummary = null;
		
		public PackageInfo(ComponentName name, 
				CharSequence title, String version, Drawable icon) { 
			this(null, null, name, title, version, icon);
		}
		
		public PackageInfo(ResolveInfo info, android.content.pm.PackageInfo pkg, 
				ComponentName name, CharSequence title, String version, Drawable icon) { 
			mPackage = pkg;
			mResolveInfo = info;
			mComponentName = name; 
			mTitle = title; 
			mVersionName = version;
			mIcon = icon; 
			
			if (name == null) 
				throw new NullPointerException();
		}
		
		public android.content.pm.PackageInfo getPackage() { return mPackage; }
		public final ResolveInfo getResolveInfo() { return mResolveInfo; }
		public final ComponentName getComponentName() { return mComponentName; }
		public final CharSequence getTitle() { return mTitle; }
		public final Drawable getIcon() { return mIcon; }
		
		public final String getVersionName() { 
			android.content.pm.PackageInfo pkg = mPackage;
			if (pkg != null) return pkg.versionName;
			return mVersionName;
		}
		
		public Drawable getSmallIcon() { return mSmallIcon != null ? mSmallIcon : mIcon; }
		public void setSmallIcon(Drawable icon) { mSmallIcon = icon; }
		
		public void setSummary(CharSequence text) { mSummary = text; }
		public CharSequence getSummary() { return mSummary; }
		
		@Override 
		public boolean equals(Object obj) { 
			if (obj == this) return true; 
			if (obj != null && obj instanceof PackageInfo) { 
				PackageInfo other = (PackageInfo)obj; 
				
				ResolveInfo thisInfo = getResolveInfo();
				ResolveInfo otherInfo = other.getResolveInfo();
				
				if (thisInfo != null || otherInfo != null) { 
					if (thisInfo == null || otherInfo == null)
						return false;
					
					if (!thisInfo.equals(otherInfo))
						return false;
				}
				
				return getComponentName().equals(other.getComponentName()); 
			}
			
			return false; 
		}
		
		@Override
		public int hashCode() { 
			return mComponentName.hashCode();
		}
		
		@Override 
		public String toString() { 
			return getComponentName().getPackageName(); 
		}
	}
	
	protected final class PackageInfos { 
		public final String mCategory; 
		public final PackageInfo[] mPackages; 
		public final long mUpdateTime; 
		public PackageInfo mSelected = null; 
		
		public PackageInfos(String category, PackageInfo[] infos) { 
			mCategory = category; 
			mPackages = infos; 
			mUpdateTime = System.currentTimeMillis(); 
		}
		
		public final List<PackageInfo> cloneList() { 
			ArrayList<PackageInfo> list = new ArrayList<PackageInfo>();
			for (int i=0; mPackages != null && i < mPackages.length; i++) { 
				PackageInfo info = mPackages[i]; 
				if (info != null) 
					list.add(info);
			}
			return list;
		}
	}
	
	private final Application mApp; 
	private final Context mContext; 
	private final PreferenceAdapter mPreference; 
	private final Map<String, PackageInfos> mPackages; 
	private final Map<String, OnPackageListener> mPackageListeners;
	private final boolean mIsModuleApp;
	private long mPackageChangeTime = 0; 
	
	public PluginManager(Application app, Context context, 
			PreferenceAdapter preference, boolean isModuleApp) { 
		mApp = app; 
		mContext = context; 
		mPreference = preference; 
		mPackages = new HashMap<String, PackageInfos>(); 
		mPackageListeners = new HashMap<String, OnPackageListener>();
		mIsModuleApp = isModuleApp;
		
		if (!isModuleApp) 
			registerReceiver(app); 
	}
	
	public final Application getApplication() { 
		return mApp; 
	}
	
	public final Context getContext() { 
		return mContext; 
	}
	
	public final PreferenceAdapter getPreferences() { 
		return mPreference;
	}
	
	protected void registerReceiver(Application app) { 
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		
		app.registerReceiver(this, intentFilter);
	}
	
	@Override 
	public synchronized void onReceive(Context context, final Intent intent) { 
		final String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_CHANGED.equals(action) || 
            Intent.ACTION_PACKAGE_REMOVED.equals(action) || 
            Intent.ACTION_PACKAGE_ADDED.equals(action)) {
        	mPackageChangeTime = System.currentTimeMillis(); 
        }
	}
	
	public synchronized void setOnPackageListener(String category, OnPackageListener listener) { 
		if (category == null || category.length() == 0 || listener == null) 
			return;
		
		mPackageListeners.put(category, listener);
	}
	
	protected synchronized void initPackages(String category) { 
		if (category == null || category.length() == 0) 
			return; 
		
		final OnPackageListener listener = mPackageListeners.get(category);
		
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(category);
        
		final PackageManager packageManager = mContext.getPackageManager();
		List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
		
		List<PackageInfo> packages = new ArrayList<PackageInfo>(); 
		PackageInfo selectedPackage = null; 
		
		if (apps != null) { 
			PackageInfos saveInfos = mPackages.get(category); 
			List<PackageInfo> saveArray = saveInfos != null ? saveInfos.cloneList() : null; 
			PackageInfo saveSelected = saveInfos != null ? saveInfos.mSelected : null; 
			
			for (ResolveInfo info : apps) { 
				String packageName = info.activityInfo.applicationInfo.packageName;
				ComponentName componentName = new ComponentName(
		                info.activityInfo.applicationInfo.packageName,
		                info.activityInfo.name);
				
				PackageInfo packageInfo = null; 
				for (int i=0; saveArray != null && i < saveArray.size(); i++) { 
					PackageInfo saveInfo = saveArray.get(i); 
					if (saveInfo == null) continue; 
					if (componentName.equals(saveInfo.getComponentName())) { 
						packageInfo = saveInfo; 
						saveArray.remove(i); 
						break; 
					}
				}
				
				if (packageInfo == null) { 
					android.content.pm.PackageInfo pkg = null; 
					try {
						pkg = packageManager.getPackageInfo(packageName, 0);
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("initPackages: getPackageInfo(" + packageName+ ") error: " + e, e);
					}
					
					CharSequence title = info.loadLabel(packageManager); 
					Drawable icon = info.activityInfo.loadIcon(packageManager); 
					
					packageInfo = new PackageInfo(info, pkg, componentName, title, null, icon);
					
					if (listener != null) 
						listener.onPackageInit(category, packageInfo);
				}
				
				if (packageInfo.equals(saveSelected)) 
					selectedPackage = packageInfo; 
				
				packages.add(packageInfo); 
			}
			
			if (saveArray != null && saveArray.size() > 0) { 
				for (PackageInfo uninstalled : saveArray) { 
					if (uninstalled != null && listener != null) 
						listener.onPackageRelease(category, uninstalled);
				}
			}
		}
		
		PackageInfos newInfos = new PackageInfos(category, 
				packages.toArray(new PackageInfo[packages.size()])); 
		newInfos.mSelected = selectedPackage; 
		
		mPackages.put(category, newInfos); 
	}
	
	private synchronized PackageInfos getPackageInfos(String category) { 
		if (!mIsModuleApp && category != null && category.length() > 0) { 
			PackageInfos saveInfos = mPackages.get(category); 
			if (saveInfos == null || saveInfos.mUpdateTime < mPackageChangeTime) { 
				initPackages(category); 
				saveInfos = mPackages.get(category); 
			}
			return saveInfos; 
		}
		return null; 
	}
	
	public PackageInfo[] getPackages(String category) { 
		PackageInfos saveInfos = getPackageInfos(category); 
		if (saveInfos != null) 
			return saveInfos.mPackages; 
		return null; 
	}
	
	public boolean setSelectedPackage(String category, ComponentName name) { 
		return setSelectedPackage(category, name, null); 
	}
	
	public synchronized boolean setSelectedPackage(String category, ComponentName name, 
			OnSelectedPackageListener listener) { 
		if (category != null && name != null) { 
			PackageInfos saveInfos = getPackageInfos(category); 
			if (saveInfos == null) 
				return false; 
			
			PluginManager.PackageInfo selectedPackage = null; 
			PluginManager.PackageInfo[] infos = saveInfos.mPackages; 
			
			for (int i=0; infos != null && i < infos.length; i++) { 
				PluginManager.PackageInfo info = infos[i]; 
				if (info != null && name.equals(info.getComponentName())) { 
					selectedPackage = info; 
					break; 
				}
			}
			
			saveInfos.mSelected = selectedPackage; 
			if (listener != null) 
				listener.onSelectedPackageChanged(category, selectedPackage); 
			
			return true; 
		}
		
		return false; 
	}
	
	public boolean setSelectedPackage(String category, String packageName) { 
		return setSelectedPackage(category, packageName, null); 
	}
	
	public synchronized boolean setSelectedPackage(String category, String packageName, 
			OnSelectedPackageListener listener) { 
		if (category != null && packageName != null) { 
			PackageInfos saveInfos = getPackageInfos(category); 
			if (saveInfos == null) 
				return false; 
			
			PluginManager.PackageInfo selectedPackage = null; 
			PluginManager.PackageInfo[] infos = saveInfos.mPackages; 
			
			for (int i=0; infos != null && i < infos.length; i++) { 
				PluginManager.PackageInfo info = infos[i]; 
				if (info != null && packageName.equals(info.getComponentName().getPackageName())) { 
					selectedPackage = info; 
					break; 
				}
			}
			
			saveInfos.mSelected = selectedPackage; 
			if (listener != null) 
				listener.onSelectedPackageChanged(category, selectedPackage); 
			
			return true; 
		}
		
		return false; 
	}
	
}
