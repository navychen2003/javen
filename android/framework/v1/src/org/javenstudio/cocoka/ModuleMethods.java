package org.javenstudio.cocoka;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The follow functions for module package.
 */
public final class ModuleMethods {

	private static Context sModuleContext = null;
	private static MainMethods sMainMethods = null;
	private static final Object sLock = new Object();
	
	public static Context getModuleContext() { 
		synchronized (sLock) {
			return sModuleContext;
		}
	}
	
	public static void initMainMethods(Class<?> mainClass, Context moduleContext) { 
		if (moduleContext == null || mainClass == null)
			throw new NullPointerException("module context org methods is null");
		
		try {
			synchronized (sLock) { 
				sModuleContext = moduleContext; 
				sMainMethods = new MainMethods(mainClass); 
			}
		} catch (Exception ex) { 
			// main package is wrong?
			throw new RuntimeException("init module methods error: "+ex, ex);
		}
	}
	
	public static Object invokeMainMethod(String methodName, Object... args) { 
		if (methodName == null) 
			throw new NullPointerException();
		
		try {
			return sMainMethods.invokeMethod(methodName, args);
			
		} catch (Exception ex) { 
			throw new RuntimeException("invoke main method: " + methodName + " error", ex);
		}
	}
	
	public static String getStringKeyWithId(String id) { 
		return (String)invokeMainMethod("getStringKeyWithId", id);
	}
	
	public static String getLocalStorageDirectory() { 
		return (String)invokeMainMethod("getLocalStorageDirectory");
	}
	
	public static SharedPreferences getPreferences() { 
		return (SharedPreferences)invokeMainMethod("getPreferences");
	}
	
	public static Method getMainMethod(String className, String methodName) { 
		return (Method)invokeMainMethod("getMethod", className, methodName);
	}
	
	public static Method getGlobalMethod(String methodName) { 
		return (Method)invokeMainMethod("getGlobalMethod", methodName);
	}
	
	public static void registerGlobalMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		invokeMainMethod("registerGlobalMethod", clazz, methodName, parameterTypes);
	}
	
	public static Object newModuleClass(String packageName, String className) { 
		return invokeMainMethod("newModuleClass", packageName, className);
	}
	
	public static void registerModuleMethod(Object moduleClass, String methodName) { 
		invokeMainMethod("registerModuleMethod", moduleClass, methodName);
	}
	
	public static final class MainMethods { 
		private final Class<?> mMethodsClass;
		private final Map<String, Method> mMethods;
		
		public MainMethods(Class<?> methodsClass) { 
			mMethodsClass = methodsClass;
			mMethods = new HashMap<String, Method>();
			
			if (methodsClass == null) 
				throw new NullPointerException();
			
			Method[] methods = methodsClass.getMethods();
			for (int i=0; methods != null && i < methods.length; i++) { 
				Method method = methods[i];
				if (method != null) 
					mMethods.put(method.getName(), method);
			}
		}
		
		public final Class<?> getMethodsClass() { return mMethodsClass; }
		
		public Method getMethod(String methodName) { 
			if (methodName == null) 
				throw new NullPointerException();
			
			return mMethods.get(methodName);
		}
		
		public Object invokeMethod(String methodName, Object... args) { 
			Method method = getMethod(methodName);
			if (method == null)
				throw new RuntimeException(methodName + " not found");
			
			return ModuleHelper.invokeStaticMethod(method, args);
		}
	}
	
	public static final class ClassMethod { 
		private final String mClassName; 
		private final String mMethodName; 
		private Method mMethod = null; 
		private boolean mInited = false;
		
		public ClassMethod(String className, String methodName) { 
			mClassName = className; 
			mMethodName = methodName; 
			
			if (className == null || methodName == null) 
				throw new NullPointerException();
		}
		
		public final String getClassName() { return mClassName; }
		public final String getMethodName() { return mMethodName; }
		
		public synchronized final Method getMethod() { 
			if (mMethod == null && mInited == false) {
				mMethod = getMainMethod(mClassName, mMethodName);
				if (mMethod == null) 
					throw new RuntimeException(toString() + " not found");
				mInited = true;
			}
			
			return mMethod;
		}
		
		public final Object invoke(Object... args) { 
			return ModuleHelper.invokeStaticMethod(getMethod(), args);
		}
		
		@Override
		public String toString() { 
			return "ManagerMethod(" + mClassName + "/" + mMethodName + ")";
		}
	}
	
	public static final class ClassMethods { 
		private final String mClassName;
		private final Map<String, ClassMethod> mMethods = 
				new HashMap<String, ClassMethod>();
		
		public ClassMethods(String className) { 
			mClassName = className;
			
			if (className == null) 
				throw new NullPointerException();
		}
		
		public ClassMethod getMethod(String methodName) { 
			if (methodName == null) 
				throw new NullPointerException();
			
			synchronized (mMethods) { 
				ClassMethod method = mMethods.get(methodName);
				if (method == null) { 
					method = new ClassMethod(mClassName, methodName); 
					mMethods.put(methodName, method);
				}
				
				return method;
			}
		}
	}
	
}
