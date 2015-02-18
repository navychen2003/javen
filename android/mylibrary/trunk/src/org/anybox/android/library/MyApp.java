package org.anybox.android.library;

import java.io.IOException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Looper;

import org.anybox.android.library.app.MyAnyboxApp;
import org.anybox.android.library.setting.MySetting;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.MetricsNotification;
import org.javenstudio.android.data.CacheData;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataManager;
import org.javenstudio.android.data.image.http.HttpCache;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.entitydb.TDefaultDB;
import org.javenstudio.android.entitydb.app.LibraryListener;
import org.javenstudio.android.reader.AnalyzerMethods;
import org.javenstudio.android.reader.CommentItemMethods;
import org.javenstudio.android.reader.CommentListMethods;
import org.javenstudio.android.reader.ReaderMethods;
import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.android.ModuleApp;
import org.javenstudio.cocoka.android.ModuleAppInfo;
import org.javenstudio.cocoka.android.ModuleAppSetting;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.SocketStatistic;
import org.javenstudio.cocoka.storage.CacheManager;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.worker.work.Scheduler;
import org.javenstudio.common.util.Logger;

public class MyApp extends ModuleApp implements DataApp {
	private static final Logger LOG = Logger.getLogger(MyApp.class);
	private static MyApp sInstance = null;
	
	public static MyApp getInstance() { 
		synchronized (MyApp.class) {
			if (sInstance == null) 
				throw new RuntimeException("instance not set");
			
			return sInstance;
		}
	}
	
	public static final int NOTIFICATIONID_UPLOAD = 1;
	public static final int NOTIFICATIONID_STATISTIC = 2;
	
	private final MySetting mSetting;
	private final DataManager mDataManager;
	private final HttpResource mHttpRes;
	private final MyAnyboxApp mAccountApp;
	
	private CacheManager mCacheManager = null;
	private CacheData mImageCache = null;
	private MetricsNotification mMetricsNotifier = null;
	
	public MyApp() { 
		ReaderMethods.registerMethods();
		CommentListMethods.registerMethods();
		CommentItemMethods.registerMethods();
		AnalyzerMethods.registerMethods();
		AppResources.setImplementClassName(MyResources.class.getName());
		MyResources.setDownloadSources();
		
		mHttpRes = new HttpResource(this) {
				@Override
				protected HttpCache createHttpCache(Storage store) throws IOException {
					return new HttpCache(store) {
							@Override
							protected String encodeCacheName(String source) {
								return toMD5CacheName(source);
							}
						};
				}
			};
		
		mSetting = new MySetting(this);
		mDataManager = new DataManager(this);
		mAccountApp = new MyAnyboxApp(this);
	}
	
	public MyAnyboxApp getAccountApp() { return mAccountApp; }
	synchronized void onTerminate() { getAccountApp().onTerminate(); }
	
	public synchronized MetricsNotification getMetricsNotifier() { 
		if (mMetricsNotifier == null) { 
			Intent intent = MyResources.createIntentForMetricsNotification(getContext());
			mMetricsNotifier = new MetricsNotification(MyApp.this, 
					NOTIFICATIONID_STATISTIC, intent, 
					getContext().getString(R.string.app_name),
					R.drawable.ic_nav_anybox_dark);
		}
		return mMetricsNotifier;
	}
	
	@Override
	public DataManager getDataManager() {
		return mDataManager;
	}
	
	public synchronized CacheManager getCacheManager() { 
		if (mCacheManager == null) { 
			try {
				StorageManager storageManager = StorageManager.getInstance();
				mCacheManager = new CacheManager(storageManager.getCacheStorage());
			} catch (Throwable e) { 
				throw new RuntimeException(e);
			}
		}
		return mCacheManager;
	}
	
	@Override
	public synchronized CacheData getCacheData() { 
		if (mImageCache == null) 
			mImageCache = new CacheData(getCacheManager());
		return mImageCache;
	}
	
	@Override
	public HttpResource getHttpResource() {
		return mHttpRes;
	}
	
	@Override
	public Looper getMainLooper() { 
		return Implements.getApplication().getMainLooper();
	}
	
	@Override
	public Activity getMainActivity() { 
		return MainActivity.getInstance();
	}
	
	@Override
	public Context getContext() { 
		return ResourceHelper.getContext();
	}
	
	@Override
	public ContentResolver getContentResolver() { 
		return Implements.getApplication().getContentResolver();
	}
	
	@Override
	public Scheduler getScheduler() { 
		return Implements.getScheduler();
	}
	
	@Override 
	protected void initApplication(Context context) { 
		super.initApplication(context); 
		
		TDefaultDB.initDatabase(context, new LibraryListener());
		TDefaultDB.getDatabase();
	}
	
	@Override 
	protected void onInitialized(Context context) { 
		super.onInitialized(context); 
		
		SocketStatistic.addSocketListener(getMetricsNotifier());
		ResourceHelper.getModuleManager().registerPackage(getAccountApp().getPluginInfo(context));
		getAccountApp().onInitialized(context);
		
		synchronized (MyApp.class) {
			if (sInstance != null) 
				throw new RuntimeException("instance already set");
			
			sInstance = this;
		}
	}
	
	@Override 
	public ModuleAppInfo getAppInfo() { 
		final Resources res = ResourceHelper.getResources(); 
		
		return new ModuleAppInfo(this) { 
				@Override 
				public Drawable getIconDrawable() { 
					return res.getDrawable(R.drawable.ic_launcher); 
				}
				@Override 
				public Drawable getSmallIconDrawable() { 
					return res.getDrawable(R.drawable.ic_launcher); 
				}
				@Override 
				public CharSequence getDisplayName() { 
					return res.getString(R.string.app_name); 
				}
				@Override 
				public CharSequence getDisplayTitle() { 
					return res.getString(R.string.app_name); 
				}
			};
	}
	
	@Override 
	public ModuleAppSetting getAppSetting() { 
		return mSetting;
	}
	
	@Override 
	public String getActivityClassName() { 
		return MainActivity.class.getName(); 
	}
	
	@Override
	protected void onInitializeDone(final Context context) { 
		if (LOG.isDebugEnabled()) LOG.debug("onInitializeDone");
		super.onInitializeDone(context);
		
		//final DataApp app = this;
		//final Resources res = ResourceHelper.getResources(); 
		//final boolean zh = ResourceHelper.isLanguageZh(context);
		
	}
	
}
