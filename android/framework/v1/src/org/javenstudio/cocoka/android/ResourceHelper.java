package org.javenstudio.cocoka.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.widget.activity.BaseResources;

public class ResourceHelper {

	public static Context getContext() { 
		return Implements.getContext();
	}
	
	public static ResourceContext getResourceContext() { 
		return Implements.getResourceContext();
	}
	
	public static SharedPreferences getSharedPreferences() { 
		return Implements.getSharedPreferences();
	}
	
	public static int getDisplaySize(int hdpiSize) {
		return BaseResources.getDisplaySize(hdpiSize);
	}
	
	public static float getDisplaySizeF(float hdpiSize) {
		return BaseResources.getDisplaySizeF(hdpiSize);
	}
	
	public static ResourceContext createResourceContext(
			PreferenceAdapter preference, PluginManager pluginManager, 
			ModuleManager moduleManager, boolean isModuleApp) { 
		if (isModuleApp) 
			return ResourceContextHelper.createModuleResourceManager(preference, pluginManager);

		ResourceManager manager = new ResourceManager(preference, pluginManager);
		manager.setOnResourceChangedListener(moduleManager);
		
		return manager;
	}
	
	public static void onInitialized(ResourceContext context) { 
		if (context == null) return;
		
		if (context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			manager.onInitialized();
		}
	}
	
	public static PluginManager.PackageInfo[] getResourcePackages() { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.getResourcePackages();
		}
		
		return null;
	}
	
	public static PluginManager.PackageInfo getSelectedPackage() { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.getSelectedPackage();
		}
		
		return null;
	}
	
	public static boolean setSelectedPackage(String packageName) { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			return manager.setSelectedPackage(packageName);
		}
		
		return false;
	}
	
	public static void setViewBinder(ResourceManager.ViewBinder binder) { 
		ResourceContext context = Implements.getResourceContext();
		
		if (context != null && context instanceof ResourceManager) { 
			ResourceManager manager = (ResourceManager)context;
			manager.setViewBinder(binder);
		}
	}
	
}
