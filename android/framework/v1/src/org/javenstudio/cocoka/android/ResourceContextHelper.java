package org.javenstudio.cocoka.android;

import java.lang.reflect.Method;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.ModuleMethods;
import org.javenstudio.common.util.Logger;

final class ResourceContextHelper {
	private static Logger LOG = Logger.getLogger(ResourceContextHelper.class);

	static ResourceContext createPackageResourceContext(
			final ResourceManager manager, final Context packageContext, 
			final String className) throws ClassNotFoundException { 
		
		Class<?> clazz = packageContext.getClassLoader().loadClass(className); 
		if (clazz == null) 
			return null;
		
		//Constructor<?> ctor = clazz.getConstructor(Context.class); 
		//if (ctor != null) 
		//	return (ResourceContext)ctor.newInstance(context); 
			
		final Method getStringByIdMethod = getClassMethod(clazz, "getStringById", Context.class, int.class); 
		final Method getStringByNameMethod = getClassMethod(clazz, "getStringByName", Context.class, String.class); 
		
		final Method getStringArrayByIdMethod = getClassMethod(clazz, "getStringArrayById", Context.class, int.class); 
		final Method getStringArrayByNameMethod = getClassMethod(clazz, "getStringArrayByName", Context.class, String.class); 
		
		final Method getQuantityStringByIdMethod = getClassMethod(clazz, "getQuantityStringById", 
				Context.class, int.class, int.class, Object[].class); 
		final Method getQuantityStringByNameMethod = getClassMethod(clazz, "getQuantityStringByName", 
				Context.class, String.class, int.class, Object[].class); 
		
		final Method getDrawableByIdMethod = getClassMethod(clazz, "getDrawableById", Context.class, int.class); 
		final Method getDrawableByNameMethod = getClassMethod(clazz, "getDrawableByName", Context.class, String.class); 
		
		final Method getColorByIdMethod = getClassMethod(clazz, "getColorById", Context.class, int.class); 
		final Method getColorByNameMethod = getClassMethod(clazz, "getColorByName", Context.class, String.class); 
		
		final Method getColorStateListByIdMethod = getClassMethod(clazz, "getColorStateListById", Context.class, int.class); 
		final Method getColorStateListByNameMethod = getClassMethod(clazz, "getColorStateListByName", Context.class, String.class); 
		
		final Method getXmlByIdMethod = getClassMethod(clazz, "getXmlById", Context.class, int.class); 
		final Method getXmlByNameMethod = getClassMethod(clazz, "getXmlByName", Context.class, String.class); 
		
		final Method decodeBitmapByIdMethod = getClassMethod(clazz, "decodeBitmapById", Context.class, int.class); 
		final Method decodeBitmapByNameMethod = getClassMethod(clazz, "decodeBitmapByName", Context.class, String.class); 
		
		final Method inflateViewByIdMethod = getClassMethod(clazz, "inflateViewById", Context.class, int.class, ViewGroup.class, boolean.class); 
		final Method inflateViewByNameMethod = getClassMethod(clazz, "inflateViewByName", Context.class, String.class, ViewGroup.class, boolean.class); 
		
		final Method findViewByIdMethod = getClassMethod(clazz, "findViewById", Context.class, View.class, int.class); 
		final Method findViewByNameMethod = getClassMethod(clazz, "findViewByName", Context.class, View.class, String.class); 
		
		final Method bindViewByIdMethod = getClassMethod(clazz, "bindViewById", Context.class, int.class, View.class, Object[].class); 
		final Method bindViewByNameMethod = getClassMethod(clazz, "bindViewByName", Context.class, String.class, View.class, Object[].class); 
		
		return new ResourceContext() { 
				public Context getPackageContext() { return packageContext; }
				public Resources getResources() { return packageContext.getResources(); }
				
				@Override
				public String getString(int resId) { 
					if (getStringByIdMethod != null) { 
						return (String)invokeStaticMethod(getStringByIdMethod, getPackageContext(), resId); 
					} else if (getStringByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.STRING, resId); 
						if (name != null)
							return (String)invokeStaticMethod(getStringByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public String[] getStringArray(int resId) { 
					if (getStringArrayByIdMethod != null) { 
						return (String[])invokeStaticMethod(getStringArrayByIdMethod, getPackageContext(), resId); 
					} else if (getStringArrayByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.STRING, resId); 
						if (name != null)
							return (String[])invokeStaticMethod(getStringArrayByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public String getQuantityString(int resId, int quantity, Object... formatArgs) { 
					if (getQuantityStringByIdMethod != null) { 
						return (String)invokeStaticMethod(getQuantityStringByIdMethod, getPackageContext(), resId, quantity, formatArgs); 
					} else if (getQuantityStringByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.STRING, resId); 
						if (name != null) 
							return (String)invokeStaticMethod(getQuantityStringByNameMethod, getPackageContext(), name, quantity, formatArgs); 
					}
					return null;
				}
				
				@Override
				public Drawable getDrawable(int resId) { 
					if (getDrawableByIdMethod != null) { 
						return (Drawable)invokeStaticMethod(getDrawableByIdMethod, getPackageContext(), resId); 
					} else if (getDrawableByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.DRAWABLE, resId); 
						if (name != null)
							return (Drawable)invokeStaticMethod(getDrawableByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public int getColor(int resId) { 
					Object obj = null;
					if (getColorByIdMethod != null) { 
						obj = invokeStaticMethod(getColorByIdMethod, getPackageContext(), resId); 
					} else if (getColorByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.COLOR, resId); 
						if (name != null)
							obj = invokeStaticMethod(getColorByNameMethod, getPackageContext(), name); 
					}
					if (obj != null && obj instanceof Integer) 
						return ((Integer)obj).intValue(); 
					return 0; 
				}
				
				@Override
				public ColorStateList getColorStateList(int resId) { 
					if (getColorStateListByIdMethod != null) { 
						return (ColorStateList)invokeStaticMethod(getColorStateListByIdMethod, getPackageContext(), resId); 
					} else if (getColorStateListByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.COLOR, resId); 
						if (name != null)
							return (ColorStateList)invokeStaticMethod(getColorStateListByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public XmlResourceParser getXml(int resId) { 
					if (getXmlByIdMethod != null) { 
						return (XmlResourceParser)invokeStaticMethod(getXmlByIdMethod, getPackageContext(), resId); 
					} else if (getXmlByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.XML, resId); 
						if (name != null)
							return (XmlResourceParser)invokeStaticMethod(getXmlByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public Bitmap decodeBitmap(int resId) { 
					if (decodeBitmapByIdMethod != null) { 
						return (Bitmap)invokeStaticMethod(decodeBitmapByIdMethod, getPackageContext(), resId); 
					} else if (decodeBitmapByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.DRAWABLE, resId); 
						if (name != null)
							return (Bitmap)invokeStaticMethod(decodeBitmapByNameMethod, getPackageContext(), name); 
					}
					return null;
				}
				
				@Override
				public View inflateView(int resId, ViewGroup root, boolean attachToRoot) { 
					Object obj = null; 
					if (inflateViewByIdMethod != null) { 
						obj = invokeStaticMethod(inflateViewByIdMethod, getPackageContext(), resId, root, attachToRoot); 
					} else if (inflateViewByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.LAYOUT, resId); 
						if (name != null)
							obj = invokeStaticMethod(inflateViewByNameMethod, getPackageContext(), name, root, attachToRoot); 
					}
					if (obj != null && obj instanceof View) 
						return (View)obj; 
					return null;
				}
				
				@Override
				public View findViewById(View view, int resId) { 
					Object obj = null;
					if (findViewByIdMethod != null) { 
						obj = invokeStaticMethod(findViewByIdMethod, getPackageContext(), view, resId); 
					} else if (findViewByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.ID, resId); 
						if (name != null)
							obj = invokeStaticMethod(findViewByNameMethod, getPackageContext(), view, name); 
					}
					if (obj != null && obj instanceof View) 
						return (View)obj; 
					return null;
				}
				
				@Override
				public boolean bindView(int viewId, View view, Object... values) { 
					Object obj = null; 
					if (bindViewByIdMethod != null) { 
						obj = invokeStaticMethod(bindViewByIdMethod, getPackageContext(), viewId, view, values); 
					} else if (bindViewByNameMethod != null) { 
						String name = ResourceMap.getResourceName(ResourceMap.ID, viewId); 
						if (name != null)
							obj = invokeStaticMethod(bindViewByNameMethod, getPackageContext(), name, view, values); 
					}
					if (obj != null && obj instanceof Boolean) 
						return ((Boolean)obj).booleanValue(); 
					return false; 
				}
				
				@Override 
				public View inflateView(int resource, ViewGroup root) { 
					return inflateView(resource, root, root != null); 
				}
			};
		
	}
	
	static ResourceContext createModuleResourceContext() { 
		return new ResourceContext() {
				@Override
				public Context getPackageContext() {
					return ModuleMethods.getModuleContext();
				}
	
				@Override
				public Resources getResources() {
					return getPackageContext().getResources();
				}
	
				@Override
				public String getString(int resId) {
					return (String)ModuleMethods.invokeMainMethod("getString", resId);
				}
	
				@Override
				public String[] getStringArray(int resId) {
					return (String[])ModuleMethods.invokeMainMethod("getStringArray", resId);
				}
	
				@Override
				public String getQuantityString(int resId, int quantity, Object... formatArgs) {
					return (String)ModuleMethods.invokeMainMethod("getQuantityString", resId, quantity, formatArgs);
				}
	
				@Override
				public Drawable getDrawable(int resId) {
					return (Drawable)ModuleMethods.invokeMainMethod("getDrawable", resId);
				}
	
				@Override
				public int getColor(int resId) {
					return (Integer)ModuleMethods.invokeMainMethod("getColor", resId);
				}
	
				@Override
				public ColorStateList getColorStateList(int resId) {
					return (ColorStateList)ModuleMethods.invokeMainMethod("getColorStateList", resId);
				}
	
				@Override
				public XmlResourceParser getXml(int resId) {
					return (XmlResourceParser)ModuleMethods.invokeMainMethod("getXml", resId);
				}
	
				@Override
				public Bitmap decodeBitmap(int resId) {
					return (Bitmap)ModuleMethods.invokeMainMethod("decodeBitmap", resId);
				}
	
				@Override
				public View inflateView(int resource, ViewGroup root, boolean attachToRoot) {
					return (View)ModuleMethods.invokeMainMethod("inflateView", resource, root, attachToRoot);
				}
	
				@Override
				public View inflateView(int resource, ViewGroup root) {
					return inflateView(resource, root, root != null); 
				}
	
				@Override
				public View findViewById(View view, int resId) {
					return (View)ModuleMethods.invokeMainMethod("findViewById", view, resId);
				}
	
				@Override
				public boolean bindView(int viewId, View view, Object... values) {
					return (Boolean)ModuleMethods.invokeMainMethod("bindView", viewId, view, values);
				}
			};
	}
	
	static ResourceManager createModuleResourceManager(
			final PreferenceAdapter preference, final PluginManager manager) { 
		return new ModuleResourceManager(preference, manager);
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
	
	static Method getClassMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) { 
		try { 
			return clazz.getMethod(methodName, parameterTypes); 
		} catch (NoSuchMethodException ex) { 
			//if (LOG.isDebugEnabled()) 
			//	LOG.debug("getClassMethod: "+methodName+" error", ex); 
			
			return null; 
		}
	}
	
	static class ModuleResourceManager extends ResourceManager { 
		public ModuleResourceManager(PreferenceAdapter preference, PluginManager manager) { 
			super(preference, manager);
		}
		
		@Override
		public void onInitialized() { 
			// do nothing
		}
		
		@Override
		public boolean setSelectedPackage(ComponentName name) { 
			return false;
		}
		
		@Override
		public boolean setSelectedPackage(String packageName) { 
			return false;
		}
		
		@Override 
		public void onSelectedPackageChanged(String category, PluginManager.PackageInfo info) { 
			// do nothing
		}
	}
	
}
