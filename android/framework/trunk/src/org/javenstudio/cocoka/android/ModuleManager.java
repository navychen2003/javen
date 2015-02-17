package org.javenstudio.cocoka.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageManager;

import org.javenstudio.cocoka.Constants;
import org.javenstudio.common.util.Logger;

public final class ModuleManager implements PluginManager.OnPackageListener, 
		ResourceManager.OnResourceChangedListener {
	private static Logger LOG = Logger.getLogger(ModuleManager.class);

	private final PreferenceAdapter mPreference; 
	private final PluginManager mPluginManager; 
	
	private final Map<String, ModuleContext> mModules = 
			new HashMap<String, ModuleContext>();
	
	private final List<PluginManager.PackageInfo> mPackages = 
			new ArrayList<PluginManager.PackageInfo>();
	
	public ModuleManager(PreferenceAdapter preference, PluginManager manager, 
			boolean isModuleApp) { 
		mPreference = preference; 
		mPluginManager = manager; 
		
		manager.setOnPackageListener(getIntentFilterCategory(), this);
	}
	
	public void registerPackage(PluginManager.PackageInfo packageInfo) { 
		if (packageInfo == null) return;
		
		synchronized (mPackages) { 
			for (PluginManager.PackageInfo info : mPackages) { 
				if (packageInfo.equals(info)) {
					if (LOG.isDebugEnabled())
						LOG.debug("registerPackage: package: " + info + " already registered");
					
					return;
				}
			}
			
			mPackages.add(packageInfo);
			
			if (LOG.isDebugEnabled())
				LOG.debug("registerPackage: package: " + packageInfo);
		}
	}
	
	public void onInitialized(Context context) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onInitialized: module plugins");
		
		synchronized (mPackages) { 
			for (PluginManager.PackageInfo info : mPackages) { 
				initModulePackage(context, info);
			}
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("onInitialized: package plugins");
		
		getPluginPackages(); // init plugins
	}
	
	public final Context getContext() { 
		return mPluginManager.getContext(); 
	}
	
	public final PluginManager getPluginManager() { 
		return mPluginManager;
	}
	
	public final String getIntentFilterCategory() { 
		return mPreference.getStringKey(Constants.STRINGKEY_MODULE_INTENTFILTER_CATEGORY); 
	}
	
	public PluginManager.PackageInfo[] getModulePackages() { 
		ArrayList<PluginManager.PackageInfo> packages = new ArrayList<PluginManager.PackageInfo>();
		
		synchronized (mPackages) { 
			for (PluginManager.PackageInfo info : mPackages) { 
				if (info != null) packages.add(info);
			}
		}
		
		PluginManager.PackageInfo[] plugins = getPluginPackages();
		if (plugins != null && plugins.length > 0) { 
			for (PluginManager.PackageInfo info : plugins) { 
				if (info != null) packages.add(info);
			}
		}
		
		return packages.toArray(new PluginManager.PackageInfo[packages.size()]);
	}
	
	private PluginManager.PackageInfo[] getPluginPackages() { 
		return mPluginManager.getPackages(getIntentFilterCategory()); 
	}
	
	@Override
	public void onResourceChanged(Context packageContext, String className) { 
		synchronized (mModules) { 
			for (ModuleContext module : mModules.values()) { 
				module.onResourceChanged(packageContext, className);
			}
		}
	}
	
	@Override
	public void onPackageInit(String category, PluginManager.PackageInfo packageInfo) { 
		if (category == null || packageInfo == null) 
			return;
		
		if (!category.equals(getIntentFilterCategory())) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onPackageInit: package: " + packageInfo);
		
		try {
			Context packageContext = getContext().createPackageContext(
					packageInfo.getComponentName().getPackageName(), 
					Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			
			if (packageContext != null) { 
				initModulePackage(packageContext, packageInfo);
				
			} else {
				if (LOG.isErrorEnabled())
					LOG.error("module package: " + packageInfo + " context create failed"); 
			}
		} catch (PackageManager.NameNotFoundException e) {
			if (LOG.isWarnEnabled())
				LOG.warn("module package: " + packageInfo + " not found error", e); 
		}
	}
	
	@Override
	public void onPackageRelease(String category, PluginManager.PackageInfo packageInfo) { 
		if (category == null || packageInfo == null) 
			return;
		
		if (!category.equals(getIntentFilterCategory())) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onPackageRelease: package: " + packageInfo);
		
		releaseModulePackage(packageInfo);
	}
	
	private void initModulePackage(Context packageContext, PluginManager.PackageInfo packageInfo) { 
		if (packageContext == null || packageInfo == null) 
			return;
		
		final String packageName = packageInfo.getComponentName().getPackageName();
		initModulePackage(packageContext, packageName);
	}
	
	private void initModulePackage(Context packageContext, String packageName) {
		if (packageContext == null || packageName == null) 
			return;
		
		final String className = packageName + "." + Constants.PLUGIN_CLASSNAME_MODULE_PACKAGE;
		
		if (LOG.isDebugEnabled())
			LOG.debug("initModulePackage: className: " + className);
		
		try {
			ModuleContext module = ModuleManagerHelper.createModuleContext(
					packageContext, className);
			if (module != null) { 
				synchronized (mModules) { 
					if (mModules.containsKey(packageName))
						throw new RuntimeException("module package: " + packageName + " already initialized");
					
					mModules.put(packageName, module);
				}
				
				module.initModule(MainMethods.class, packageContext);
				
				if (LOG.isInfoEnabled())
					LOG.info("module package: " + packageName + " initialized");
			}
		} catch (Throwable ex) { 
			if (LOG.isWarnEnabled())
				LOG.warn("cannot init module: " + packageName, ex);
		}
	}
	
	private void releaseModulePackage(PluginManager.PackageInfo packageInfo) { 
		if (packageInfo == null) 
			return;
		
		final String packageName = packageInfo.getComponentName().getPackageName();
		
		synchronized (mModules) { 
			mModules.remove(packageName);
		}
		
		if (LOG.isInfoEnabled())
			LOG.info("module package: " + packageName + " released");
	}
	
	public Context getModuleContext(String packageName) { 
		if (packageName == null) 
			return null;
		
		synchronized (mModules) { 
			ModuleContext module = mModules.get(packageName);
			if (module != null) 
				return module.getPackageContext();
		}
		
		return null;
	}
	
	public Method getModuleMethod(String packageName, String className, String methodName) { 
		if (packageName == null) 
			return null;
		
		synchronized (mModules) { 
			ModuleContext module = mModules.get(packageName);
			if (module != null) 
				return module.getStaticMethod(className, methodName);
		}
		
		return null;
	}
	
	public ModuleClass createModuleClass(String packageName, String className) { 
		if (packageName == null || className == null) 
			throw new NullPointerException();
		
		synchronized (mModules) { 
			if (!mModules.containsKey(packageName)) 
				throw new RuntimeException("module package: " + packageName + " not found");
		}
		
		return new ModuleClass(packageName, className);
	}
	
	public final class ModuleClass { 
		private final String mPackageName; 
		private final String mClassName; 
		private final Map<String, ModuleMethod> mMethods;
		
		private ModuleClass(String packageName, String className) {
			mPackageName = packageName; 
			mClassName = className; 
			mMethods = new HashMap<String, ModuleMethod>();
		}
		
		public final String getPackageName() { return mPackageName; }
		public final String getClassName() { return mClassName; }
		
		public final void registerMethod(String methodName) { 
			if (methodName == null) 
				throw new NullPointerException();
			
			if (methodName.length() == 0) 
				throw new IllegalArgumentException("method name cannot be empty");
			
			synchronized (mMethods) { 
				if (mMethods.containsKey(methodName))
					throw new RuntimeException("method name already registered");
				
				mMethods.put(methodName, new ModuleMethod(this, methodName));
			}
		}
		
		private Method lookupMethod(String methodName) { 
			return getModuleMethod(getPackageName(), getClassName(), methodName);
		}
		
		public ModuleMethod getMethod(String methodName) { 
			if (methodName == null) 
				throw new NullPointerException();
			
			synchronized (mMethods) { 
				ModuleMethod method = mMethods.get(methodName);
				if (method == null) { 
					method = new ModuleMethod(this, methodName);
					mMethods.put(methodName, method);
					
					//if (LOG.isDebugEnabled()) {
					//	LOG.debug("module package: " + mPackageName + " class: " 
					//			+ mClassName + " method: " + methodName + " not registered");
					//}
				}
				return method;
			}
		}
		
		@Override
		public String toString() { 
			return "ModuleClass(" + mPackageName + "/" + mClassName + ")";
		}
	}
	
	public final class ModuleMethod { 
		private final ModuleClass mModuleClass; 
		private final String mMethodName; 
		private Method mMethod = null; 
		private boolean mInited = false;
		
		private ModuleMethod(ModuleClass moduleClass, String methodName) { 
			mModuleClass = moduleClass; 
			mMethodName = methodName; 
		}
		
		public final ModuleClass getModuleClass() { return mModuleClass; }
		public final String getMethodName() { return mMethodName; }
		
		public final boolean existMethod() { 
			return getMethod(false) != null;
		}
		
		public final Method getMethod() { 
			return getMethod(true);
		}
		
		public synchronized final Method getMethod(boolean throwExp) { 
			if (mMethod == null && mInited == false) {
				mMethod = mModuleClass.lookupMethod(mMethodName);
				if (mMethod == null && throwExp) 
					throw new RuntimeException(toString() + " not found");
				mInited = true;
			}
			return mMethod;
		}
		
		public final Object invoke(Object... args) { 
			return invokeStaticMethod(getMethod(), args);
		}
		
		@Override
		public String toString() { 
			return mModuleClass + ".ModuleMethod(" + mMethodName + ")";
		}
	}
	
	private final Map<String, GlobalMethod> mGlobalMethods = 
			new HashMap<String, GlobalMethod>();
	
	public GlobalMethod getGlobalMethod(String methodName) { 
		if (methodName == null) 
			throw new NullPointerException();
		
		synchronized (mGlobalMethods) { 
			GlobalMethod method = mGlobalMethods.get(methodName);
			if (method == null) { 
				method = new GlobalMethod(methodName);
				mGlobalMethods.put(methodName, method);
			}
			
			return method;
		}
	}
	
	public final class GlobalMethod { 
		private final String mMethodName; 
		private Method mMethod = null; 
		private boolean mInited = false;
		
		private GlobalMethod(String methodName) { 
			mMethodName = methodName; 
			
			if (methodName == null) 
				throw new NullPointerException();
		}
		
		public final String getMethodName() { return mMethodName; }
		
		public final boolean existMethod() { 
			return getMethod(false) != null;
		}
		
		public final Method getMethod() { 
			return getMethod(true);
		}
		
		public synchronized final Method getMethod(boolean throwExp) { 
			if (mMethod == null && mInited == false) {
				mMethod = MainMethods.getGlobalMethod(mMethodName);
				if (mMethod == null && throwExp) 
					throw new RuntimeException(toString() + " not found");
				mInited = true;
			}
			return mMethod;
		}
		
		public final Object invoke(Object... args) { 
			return invokeStaticMethod(getMethod(), args);
		}
		
		@Override
		public String toString() { 
			return "GlobalMethod(" + mMethodName + ")";
		}
	}
	
	private static Object invokeStaticMethod(Method method, Object... args) { 
		try { 
			if (method != null)
				return method.invoke(null, args); 
			
		} catch (Throwable ex) { 
			if (ex instanceof InvocationTargetException) { 
				InvocationTargetException ite = (InvocationTargetException)ex;
				Throwable target = ite.getTargetException();
				if (target != null) 
					ex = target;
			}
			
			Throwable cause = ex.getCause();
			if (cause == null) 
				cause = ex;
			
			throw new RuntimeException("invoke static method: " + method 
					+ " error: " + ex, cause);
		}
		
		return null; 
	}
	
}
