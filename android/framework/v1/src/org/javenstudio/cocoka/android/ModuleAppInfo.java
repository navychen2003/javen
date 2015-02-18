package org.javenstudio.cocoka.android;

import android.graphics.drawable.Drawable;

public class ModuleAppInfo {

	private final ModuleApp mApp; 
	
	public ModuleAppInfo(ModuleApp app) { 
		mApp = app; 
	}
	
	public final ModuleApp getApp() { 
		return mApp; 
	}
	
	public Drawable getIconDrawable() { 
		return null; 
	}
	
	public Drawable getSmallIconDrawable() { 
		return null; 
	}
	
	public CharSequence getDisplayName() { 
		return null; 
	}
	
	public CharSequence getDisplayTitle() { 
		return null; 
	}
	
}
