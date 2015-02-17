package org.javenstudio.cocoka.android;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;

import org.javenstudio.common.util.Logger;

public class ModuleAppRegistry {
	private static Logger LOG = Logger.getLogger(ModuleAppRegistry.class);
	
	private List<ModuleApp> mApps = new ArrayList<ModuleApp>(); 
	private boolean mRegistered = false; 
	private boolean mInitialized = false; 
	private boolean mInitializeDone = false; 
	
	public ModuleAppRegistry() {} 
	
	public synchronized ModuleApp[] getModuleApps() { 
		return mApps.toArray(new ModuleApp[0]); 
	}
	
	public void register(String className) {
		register(className, false); 
	}
	
	public synchronized void register(String className, boolean isDefault) {
		if (className == null || mRegistered) 
			return; 
		
		ModuleApp app = newInstance(className); 
		for (ModuleApp a : mApps) { 
			if (a.getClass() == app.getClass()) 
				throw new RuntimeException(className + " already registered"); 
			
			if (isDefault) 
				a.setDefault(false); 
		}
		
		app.setDefault(isDefault); 
		mApps.add(app); 
	}
	
	private ModuleApp newInstance(String className) { 
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("new module instance: " + className);
			
            Class<?> clazz = Class.forName(className);
            Object obj = clazz.newInstance(); 
            if (obj != null && obj instanceof ModuleApp) 
            	return (ModuleApp)obj; 
            
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(
            		className + " could not be loaded", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        }
		
		throw new RuntimeException(
        		className + " isnot a ApplicationModule class");
	}
	
	public synchronized void initConfiguration(Context context, Bundle bundle) { 
		mRegistered = true;
		
		for (ModuleApp app : mApps) { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("configure module: "+app.getClass().getName());
			
			app.initConfiguration(context, bundle); 
		}
	}
	
	public synchronized void initApplications(Context context) { 
		mRegistered = true;
		if (mInitialized) return;
		
		for (ModuleApp app : mApps) { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("init module: " + app.getClass().getName());
			
			app.initApplication(context); 
		}
		
		for (ModuleApp app : mApps) { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("module initialized: " + app.getClass().getName());
			
			app.onInitialized(context); 
		}
		
		mInitialized = true;
	}
	
	public synchronized void initializeDone(Context context) { 
		if (!mInitialized || mInitializeDone) return;
		
		for (ModuleApp app : mApps) { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("module initialize done: " + app.getClass().getName());
			
			app.onInitializeDone(context); 
		}
		
		mInitializeDone = true;
	}
	
}
