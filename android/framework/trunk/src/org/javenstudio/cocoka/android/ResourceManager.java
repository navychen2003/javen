package org.javenstudio.cocoka.android;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.Constants;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.common.util.Logger;

public class ResourceManager implements ResourceContext, 
		PluginManager.OnSelectedPackageListener, BitmapHolder {
	private static Logger LOG = Logger.getLogger(ResourceManager.class);
	
	public static interface ViewBinder { 
		public boolean bindView(int viewId, View view, Object... values); 
	}
	
	public static interface OnResourceChangedListener { 
		public void onResourceChanged(Context packageContext, String className);
	}
	
	private final PreferenceAdapter mPreference; 
	private final PluginManager mPluginManager; 
	private PluginManager.PackageInfo mSelectedPackage = null; 
	private Context mSelectedContext = null; 
	private ResourceContext mSelectedResourceContext = null; 
	private long mSelectedChangeTime = 0;
	private ViewBinder mViewBinder = null; 
	private OnResourceChangedListener mChangedListener = null;
	
	public ResourceManager(PreferenceAdapter preference, PluginManager manager) { 
		mPreference = preference; 
		mPluginManager = manager; 
	}
	
	public void onInitialized() { 
		setSelectedPackage(getSelectedResourcePreference()); 
	}
	
	public final String getIntentFilterCategory() { 
		return mPreference.getStringKey(Constants.STRINGKEY_RESOURCE_INTENTFILTER_CATEGORY); 
	}
	
	public final String getSelectedResourcePreferenceKey() { 
		return mPreference.getStringKey(Constants.STRINGKEY_RESOURCE_SELECTED_PREFERENCE_KEY); 
	}
	
	public final String getSelectedResourcePreference() { 
		return mPreference.getPreferences().getString(getSelectedResourcePreferenceKey(), null); 
	}
	
	public final void setSelectedResourcePreference(String value) { 
		SharedPreferences.Editor editor = mPreference.getPreferences().edit(); 
		editor.putString(getSelectedResourcePreferenceKey(), value); 
		editor.commit(); 
	}
	
	public final Application getApplication() { 
		return mPluginManager.getApplication(); 
	}
	
	public final Context getContext() { 
		return mPluginManager.getContext(); 
	}
	
	@Override 
	public final Context getPackageContext() { 
		return getContext(); 
	}
	
	public final PluginManager getPluginManager() { 
		return mPluginManager;
	}
	
	public final void setViewBinder(ViewBinder binder) { 
		mViewBinder = binder; 
	}
	
	public PluginManager.PackageInfo[] getResourcePackages() { 
		return mPluginManager.getPackages(getIntentFilterCategory()); 
	}
	
	public synchronized PluginManager.PackageInfo getSelectedPackage() { 
		return mSelectedPackage; 
	}
	
	public long getSelectedChangeTime() { 
		return mSelectedChangeTime;
	}
	
	public boolean setSelectedPackage(ComponentName name) { 
		return mPluginManager.setSelectedPackage(
				getIntentFilterCategory(), name, this); 
	}
	
	public boolean setSelectedPackage(String packageName) { 
		return mPluginManager.setSelectedPackage(
				getIntentFilterCategory(), packageName, this); 
	}
	
	public void setOnResourceChangedListener(OnResourceChangedListener listener) { 
		mChangedListener = listener;
	}
	
	@Override 
	public synchronized void onSelectedPackageChanged(String category, 
			PluginManager.PackageInfo info) { 
		if (mSelectedPackage != info) { 
			String className = null; 
			Context packageContext = null; 
			ResourceContext resourceContext = null; 
			
			try {
				if (info != null) { 
					className = info.getComponentName().getPackageName() + "." + 
							Constants.PLUGIN_CLASSNAME_RESOURCE_PACKAGE;
					
					packageContext = getContext().createPackageContext(
							info.getComponentName().getPackageName(), 
							Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
					
					if (packageContext != null) { 
						resourceContext = createResourceContext(packageContext, className); 
						
					} else if (LOG.isErrorEnabled())
						LOG.error("reource package: " + info + " context create failed"); 
				}
			} catch (PackageManager.NameNotFoundException e) {
				resourceContext = null;
				
				if (LOG.isWarnEnabled())
					LOG.warn("reource package: " + info + " not found error", e); 
			}
			
			mSelectedPackage = info; 
			mSelectedContext = packageContext; 
			mSelectedResourceContext = resourceContext; 
			mSelectedChangeTime = System.currentTimeMillis();
			
			String packageName = (resourceContext != null) ? info.getComponentName().getPackageName() : ""; 
			setSelectedResourcePreference(packageName); 
			
			OnResourceChangedListener listener = mChangedListener;
			if (listener != null && resourceContext != null && packageContext != null)
				listener.onResourceChanged(packageContext, className);
		}
	}
	
	private ResourceContext createResourceContext(Context packageContext, String className) { 
		if (packageContext == null || className == null) 
			return null; 
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("create resource context instance: "+className); 
		
		try { 
			return ResourceContextHelper.createPackageResourceContext(
					this, packageContext, className);
			
		} catch (Throwable ex) { 
			if (LOG.isWarnEnabled())
				LOG.warn("cannot create resource context instance: " + className, ex); 
		}
		
		return null; 
	}
	
	@Override 
	public final Resources getResources() { 
		return getContext().getResources(); 
	}
	
	public final synchronized Context getSelectedContext() { 
		return mSelectedContext; 
	}
	
	public final synchronized ResourceContext getSelectedResourceContext() { 
		return mSelectedResourceContext; 
	}
	
	@Override 
	public String getString(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				String text = res.getString(resId); 
				if (text != null) 
					return text; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getString(resId); 
	}
	
	@Override 
	public String[] getStringArray(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				String[] text = res.getStringArray(resId); 
				if (text != null) 
					return text; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getStringArray(resId); 
	}
	
	@Override 
	public String getQuantityString(int resId, int quantity, Object... formatArgs) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				String text = res.getQuantityString(resId, quantity, formatArgs); 
				if (text != null) 
					return text; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getQuantityString(resId, quantity, formatArgs); 
	}
	
	@Override 
	public Drawable getDrawable(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				Drawable drawable = res.getDrawable(resId); 
				if (drawable != null) 
					return drawable; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getDrawable(resId); 
	}
	
	@Override 
	public int getColor(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				int color = res.getColor(resId); 
				if (color != 0) 
					return color; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getColor(resId); 
	}
	
	@Override 
	public ColorStateList getColorStateList(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				ColorStateList color = res.getColorStateList(resId); 
				if (color != null) 
					return color; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getColorStateList(resId); 
	}
	
	@Override 
	public XmlResourceParser getXml(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				XmlResourceParser xml = res.getXml(resId); 
				if (xml != null) 
					return xml; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return getResources().getXml(resId); 
	}
	
	@Override 
	public BitmapRef decodeBitmap(int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				BitmapRef bitmap = res.decodeBitmap(resId); 
				if (bitmap != null) {
					//BitmapRefs.onBitmapCreated(bitmap);
					return bitmap; 
				}
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		BitmapRef bitmap = BitmapRef.decodeResource(this, getResources(), resId); 
		//BitmapRefs.onBitmapCreated(bitmap);
		
		return bitmap;
	}
	
	@Override 
	public View inflateView(int resource, ViewGroup root, boolean attachToRoot) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				View view = res.inflateView(resource, root, attachToRoot); 
				if (view != null) 
					return view; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return LayoutInflater.from(getContext()).inflate(resource, root, attachToRoot); 
	}
	
	@Override 
	public View inflateView(int resource, ViewGroup root) { 
		return inflateView(resource, root, root != null); 
	}
	
	@Override 
	public View findViewById(View group, int resId) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				View view = res.findViewById(group, resId); 
				if (view != null) 
					return view; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		return group.findViewById(resId); 
	}
	
	@Override 
	public boolean bindView(int viewId, View view, Object... values) { 
		ResourceContext res = getSelectedResourceContext(); 
		if (res != null) { 
			try { 
				boolean handled = res.bindView(viewId, view, values); 
				if (handled) 
					return handled; 
			} catch (Resources.NotFoundException ex) { 
				// ignore
			}
		}
		
		ViewBinder binder = mViewBinder; 
		if (binder != null) 
			return binder.bindView(viewId, view, values); 
		
		return false; 
	}

	@Override
	public void addBitmap(BitmapRef bitmap) {
		// not hold resource bitmap
	}
	
}
