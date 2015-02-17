package org.javenstudio.cocoka.android;

import java.lang.reflect.Method;

import android.content.Context;

import org.javenstudio.common.util.Logger;

final class ModuleManagerHelper {
	private static Logger LOG = Logger.getLogger(ModuleManagerHelper.class);

	public static ModuleContext createModuleContext(
			final Context packageContext, final String className) 
			throws ClassNotFoundException, NoSuchMethodException { 
		final Class<?> clazz = packageContext.getClassLoader().loadClass(className); 
		if (clazz == null) 
			return null;
		
		final Method initModuleMethod = getClassMethod(clazz, 
				"initModule", Class.class, Context.class); 
		final Method onResourceChangedMethod = getClassMethodNoThrow(clazz, 
				"onResourceChanged", Context.class, String.class); 
		final Method getStaticMethodMethod = getClassMethod(clazz, 
				"getStaticMethod", String.class, String.class); 
		
		return new ModuleContext() {
				@Override
				public Context getPackageContext() {
					return packageContext;
				}
	
				@Override
				public void initModule(Class<?> mainClass, Context moduleContext) {
					invokeStaticMethod(initModuleMethod, mainClass, moduleContext);
				}
	
				@Override
				public void onResourceChanged(Context packageContext, String className) { 
					invokeStaticMethod(onResourceChangedMethod, packageContext, className);
				}
				
				@Override
				public Method getStaticMethod(String className, String methodName) {
					return (Method)invokeStaticMethod(getStaticMethodMethod, className, methodName);
				}
			};
	}
	
	static Object invokeStaticMethod(Method method, Object... args) { 
		try { 
			if (method != null)
				return method.invoke(null, args); 
		} catch (Exception ex) { 
			LOG.error("invoke method error: "+method, ex);
		}
		return null; 
	}
	
	static Method getClassMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) 
			throws NoSuchMethodException { 
		try { 
			return clazz.getMethod(methodName, parameterTypes); 
		} catch (NoSuchMethodException ex) { 
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("getClassMethod: "+methodName+" error", ex); 
			
			throw ex; 
		}
	}
	
	static Method getClassMethodNoThrow(Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		try { 
			return clazz.getMethod(methodName, parameterTypes); 
		} catch (NoSuchMethodException ex) { 
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("getClassMethod: "+methodName+" error", ex); 
			
			//throw ex; 
			return null;
		}
	}
	
}
