package org.javenstudio.cocoka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ModuleHelper {
	private static final String GLOBAL_CLASSNAME = ".";

	static class Methods { 
		private final String mClassName;
		private final Map<String, Method> mMethods;
		
		public Methods(String className) { 
			mClassName = className; 
			mMethods = new HashMap<String, Method>();
		}
		
		public final String getClassName() { return mClassName; }
		
		public final boolean existMethod(String methodName) { 
			return mMethods.containsKey(methodName);
		}
		
		public final Method getMethod(String methodName) { 
			return mMethods.get(methodName); 
		}
		
		public final String[] getMethodNames() { 
			return mMethods.keySet().toArray(new String[0]);
		}
		
		private void putMethod(String methodName, Method method) { 
			if (methodName != null && method != null) 
				mMethods.put(methodName, method);
		}
	}
	
	private static final Map<String, Methods> sMethods = 
			new HashMap<String, Methods>();
	
	public static void registerStaticMethods(Class<?> clazz) { 
		if (clazz == null) 
			throw new NullPointerException();
		
		registerMethods(clazz.getName(), clazz);
	}
	
	public static void registerGlobalMethods(Class<?> clazz) { 
		registerMethods(GLOBAL_CLASSNAME, clazz);
	}
	
	private static void registerMethods(String className, Class<?> clazz) { 
		if (className == null || clazz == null) 
			throw new NullPointerException();
		
		Method[] methods = clazz.getMethods();
		for (int i=0; methods != null && i < methods.length; i++) { 
			Method method = methods[i];
			if (method != null) 
				registerMethod(className, clazz, method.getName(), method);
		}
	}
	
	public static void registerStaticMethod(
			Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		if (clazz == null || methodName == null) 
			throw new NullPointerException();
		
		final String className = clazz.getName(); 
		registerMethod(className, clazz, methodName, parameterTypes);
	}
	
	public static void registerGlobalMethod(
			Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		if (clazz == null || methodName == null) 
			throw new NullPointerException();
		
		final String className = GLOBAL_CLASSNAME; 
		registerMethod(className, clazz, methodName, parameterTypes);
	}
	
	private static void registerMethod(
			String className, Class<?> clazz, String methodName, 
			Class<?>... parameterTypes) { 
		if (className == null || clazz == null || methodName == null) 
			throw new NullPointerException();
		
		try { 
			Method method = clazz.getMethod(methodName, parameterTypes); 
			if (method != null) 
				registerMethod(className, clazz, methodName, method);
			
		} catch (NoSuchMethodException ex) { 
			throw new RuntimeException("Class: " + className 
					+ " method: " + methodName + " not found", ex);
		}
	}
	
	private static synchronized void registerMethod(
			String className, Class<?> clazz, String methodName, Method method) { 
		if (className == null || clazz == null || methodName == null || method == null) 
			throw new NullPointerException();
		
		Methods methods = sMethods.get(className);
		if (methods == null) { 
			methods = new Methods(className); 
			sMethods.put(className, methods);
		}
		
		if (methods.existMethod(methodName)) { 
			throw new IllegalArgumentException("Class: " + className 
					+ " method: " + methodName + " already registered");
		}
		
		methods.putMethod(methodName, method);
	}
	
	public static synchronized Method getStaticMethod(String className, String methodName) { 
		if (className == null || methodName == null) 
			throw new NullPointerException();
		
		Methods methods = sMethods.get(className);
		if (methods != null) 
			return methods.getMethod(methodName);
		
		return null;
	}
	
	public static synchronized String[] getStaticMethodNames(String className) { 
		if (className == null) 
			throw new NullPointerException();
		
		Methods methods = sMethods.get(className);
		if (methods != null) 
			return methods.getMethodNames();
		
		return null;
	}
	
	public static Method getGlobalMethod(String methodName) { 
		return getStaticMethod(GLOBAL_CLASSNAME, methodName);
	}
	
	public static String[] getGlobalMethodNames() { 
		return getStaticMethodNames(GLOBAL_CLASSNAME);
	}
	
	public static Object invokeStaticMethod(String className, String methodName, Object... args) { 
		return invokeStaticMethod(getStaticMethod(className, methodName), args);
	}
	
	public static Object invokeStaticMethod(Method method, Object... args) { 
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
