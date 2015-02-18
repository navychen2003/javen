package org.javenstudio.cocoka;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.apache.http.params.HttpParams;

import org.javenstudio.cocoka.android.ActivityHelper;
import org.javenstudio.cocoka.android.ModuleAppRegistry;
import org.javenstudio.cocoka.android.ModuleApp;
import org.javenstudio.cocoka.android.CrashHandler;
import org.javenstudio.cocoka.android.ModuleManager;
import org.javenstudio.cocoka.android.NetworkMonitor;
import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.media.ImageMediaFile;
import org.javenstudio.cocoka.net.SecureSocketFactory;
import org.javenstudio.cocoka.net.DelegatedSSLSocketFactory;
import org.javenstudio.cocoka.net.SocketMetrics;
import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.net.http.download.DownloadHelper;
import org.javenstudio.cocoka.worker.LooperThread;
import org.javenstudio.cocoka.worker.work.Scheduler;
import org.javenstudio.cocoka.storage.LocalStorage;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.storage.FileRegistry;
import org.javenstudio.cocoka.storage.fs.FileSystems;
import org.javenstudio.cocoka.util.LogHelper;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.util.Log;

public abstract class Implements {
	private static final Log.LogImpl sLogger = LogHelper.createDefault(); 

	private static Implements sInstance = null; 
	private static Object sLock = new Object(); 
	
	private final Bundle mBundle = new Bundle(); 
	private final ModuleAppRegistry mAppRegistry = new ModuleAppRegistry(); 
	
	private final ImplementHelper mHelper;
	private final Application mApplication; 
	private final Context mContext; 
	
	private CrashHandler mCrashHandler; 
	private PluginManager mPluginManager; 
	private ModuleManager mModuleManager; 
	private ResourceContext mResourceContext; 
	private NetworkMonitor mNetworkMonitor; 
	
	private Scheduler mScheduler = null; 
	private String mTemporaryDirectory = null; 
	private Handler mHandler = null; 
	
	private boolean mInited = false;
	private final boolean mDebug;
	
	protected Implements(Application app, boolean debug) { 
		synchronized (sLock) {
			if (sInstance != null) {
				throw new RuntimeException("Implements already created: " 
						+ sInstance.getClass().getName());
			}
			
			if (app == null) 
				throw new NullPointerException("Application is null");
			
			sInstance = this;
			mApplication = app;
			mContext = app.getApplicationContext();
			mHelper = new ImplementHelper(this);
			mDebug = debug;
			
			onCreated(mBundle);
		}
	} 
	
	protected final boolean isInited() { return mInited; }
	
	public static final Implements getInstance() {
		synchronized (sLock) {
			if (sInstance == null) 
				throw new RuntimeException("Implements not created!"); 
			
			if (!sInstance.isInited()) 
				sInstance.init();
			
			return sInstance; 
		}
	}
	
	public static final Application getApplication() {
		return getInstance().mApplication; 
	}
	
	public static final Context getContext() {
		return getInstance().mContext; 
	}
	
	public static final CrashHandler getCrashHandler() { 
		return getInstance().mCrashHandler; 
	}
	
	public static final ResourceContext getResourceContext() {
		return getInstance().mResourceContext; 
	}
	
	public static final PluginManager getPluginManager() { 
		return getInstance().mPluginManager; 
	}
	
	public static final ModuleManager getModuleManager() { 
		return getInstance().mModuleManager; 
	}
	
	public static final NetworkMonitor getNetworkMonitor() { 
		return getInstance().mNetworkMonitor; 
	}
	
	public static final Bundle getBundle() {
		return getInstance().mBundle; 
	}
	
	public static final Scheduler getScheduler() {
		return getInstance().mScheduler; 
	}
	
	public static final Handler getHandler() { 
		return getInstance().mHandler; 
	}
	
	public static final EntityObserver.Handler getEntityObserverHandler() { 
		return getInstance().mHelper;
	}
	
	public static final LooperThread getLooperThread() {
		return getScheduler().getQueue().getLooperThread(); 
	}
	
	public static final SharedPreferences getSharedPreferences() { 
		return getInstance().getPreferences(); 
	}
	
	public static final String getStringKey(String id) { 
		return getInstance().getStringKeyWithId(id);
	}
	
	public static final String getStorageDirectory() { 
		return getInstance().getLocalStorageDirectory();
	}
	
	public static final String getTemporaryDirectory() { 
		return getInstance().getLocalTemporaryDirectory();
	}
	
	public static final Class<?> loadClass(String className) { 
		try {
			if (className != null && className.length() > 0)
				return getContext().getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			Log.w(Constants.getTag(), "class not found: "+className, e); 
		}
		return null; 
	}
	
	public static final ModuleApp[] getModuleApps() { 
		return getInstance().mAppRegistry.getModuleApps();
	}
	
	public static final void startApp(Activity fromActivity, String className, Intent intent) { 
		getInstance().startAppActivity(fromActivity, className, intent); 
	}
	
	public static final void setLogDebug(boolean debug) { 
		Log.setLogDebug(debug);
	}
	
	public static void terminate() { 
		synchronized (sLock) {
			if (sInstance == null) return; 
			sInstance.onTerminate();
		}
	}
	
	public final void init() {
		synchronized (sLock) {
			try {
				if (mInited) return; 
				mInited = true;
				
				doInit();
				
			} catch (Exception e) {
				throw new RuntimeException(getClass().getName()+" initialize failed", e); 
			}
		}
	}
	
	private void doInit() { 
		final boolean isModuleApp = (this instanceof ImplementsModule);
		
		Log.setLogImpl(getLogImpl()); 
		Log.setLogTag(getLogTag()); 
		Log.setLogDebug(mDebug); 
		Log.i(Constants.getTag(), getClass().getName()+" initialize"); 
		
		mCrashHandler = new CrashHandler(mApplication, mContext, isModuleApp); 
		mPluginManager = new PluginManager(mApplication, mContext, mHelper, isModuleApp); 
		mModuleManager = new ModuleManager(mHelper, mPluginManager, isModuleApp); 
		mResourceContext = ResourceHelper.createResourceContext(mHelper, 
				mPluginManager, mModuleManager, isModuleApp); 
		mNetworkMonitor = new NetworkMonitor(mApplication, mContext, isModuleApp); 
		mTemporaryDirectory = mApplication.getCacheDir().getAbsolutePath(); 
		mScheduler = new Scheduler(); 
		mHandler = new Handler(Looper.getMainLooper()); 
		
		DelegatedSSLSocketFactory.setSocketFactoryCreator(mHelper);
		mNetworkMonitor.registerListener(SocketMetrics.getNetworkStatus());
		
		initConfiguration(mContext); 
		initFileSystems(mContext); 
		
		onRegisterModules(mAppRegistry); 
		mAppRegistry.initConfiguration(mContext, mBundle); 
		
		MimeTypes.setResourcesHelper(mHelper); 
		SimpleHttpClient.setHttpParamsInitializer(mHelper);
		
		onPreInitModules(mContext); 
		mAppRegistry.initApplications(mContext); 
		onInitialized(mContext); 
		
		mModuleManager.onInitialized(); 
		ResourceHelper.onInitialized(mResourceContext); 
		
		mAppRegistry.initializeDone(mContext);
		onInitializeDone(mContext);
	}
	
	public static final class FsRegistry {
		public void register(String scheme, String className) {
			FileSystems.register(scheme, className); 
		}
	}
	
	private void initConfiguration(final Context context) {
		onInitConfiguration(context, mBundle); 
	}
	
	private void initFileSystems(final Context context) {
		onInitFileSystems(new FsRegistry()); 
		FileRegistry.registerType(MimeType.TYPE_IMAGE, ImageMediaFile.class);
		LocalStorage.setStorageListener(mHelper);
		StorageManager.initInstance(context, mHelper); 
	}
	
	protected String getLocalCacheDirectory() {
		return getLocalStorageDirectory("cache"); 
	}
	
	protected String getLocalStorageDirectory(String name) {
		if (name == null || name.length() == 0) 
			return getLocalStorageDirectory(); 
		
		return getLocalStorageDirectory() + "/" + name; 
	}
	
	protected Log.LogImpl getLogImpl() { 
		return sLogger; 
	}
	
	protected abstract String getLogTag(); 
	
	public abstract SecureSocketFactory createSocketFactory(int handshakeTimeoutMillis, boolean secure); 
	public abstract SharedPreferences getPreferences(); 
	public abstract String getLocalStorageDirectory(); 
	
	public String getStringKeyWithId(String id) { 
		return mBundle.getString(id);
	}
	
	public String getLocalTemporaryDirectory() { 
		return mTemporaryDirectory; 
	}
	
	protected void setAppMessage(String className, String message) { 
		ActivityHelper.setAppMessage(getModuleApps(), className, message);
	}
	
	protected void startAppActivity(Activity fromActivity, String className, Intent intent) { 
		ActivityHelper.startActivity(fromActivity, className, intent); 
	}
	
	protected void onInitConfiguration(Context context, Bundle bundle) { 
		// do nothing
	}
	
	protected void onInitFileSystems(FsRegistry reg) { 
		// do nothing
	}
	
	protected void onRegisterModules(ModuleAppRegistry reg) { 
		// do nothing
	}
	
	protected void onPreInitModules(Context context) { 
		// do nothing
	}
	
	protected void onCreated(Bundle bundle) { 
		// do nothing
	}
	
	protected void onInitialized(Context context) { 
		DownloadHelper.initDownloadManager(StorageManager.getInstance(), mScheduler);
	}
	
	protected void onInitializeDone(Context context) { 
		// do nothing
	}
	
	protected void onTerminate() { 
		// do nothing
	}
	
	protected void onStorageOpened(Storage storage) { 
		// do nothing
	}
	
	protected void initHttpParams(HttpParams params) {
		// do nothing
	}
	
}
