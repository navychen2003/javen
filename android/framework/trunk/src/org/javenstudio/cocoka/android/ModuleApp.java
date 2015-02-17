package org.javenstudio.cocoka.android;

import android.content.Context;
import android.os.Bundle;

public abstract class ModuleApp {

	public interface OnAppMessageListener { 
		public void onAppMessage(ModuleApp app, String message);
	}
	
	private final Bundle mBundle = new Bundle();
	private OnAppMessageListener mOnAppMessageListener = null; 
	private String mMessage = null; 
	private boolean mIsDefault = false; 
	
	public ModuleApp() {} 
	
	protected void initConfiguration(Context context, Bundle bundle) {}
	protected void initApplication(Context context) {}
	protected void onInitialized(Context context) {}
	protected void onInitializeDone(Context context) {}
	
	public final Bundle getBundle() { 
		return mBundle; 
	}
	
	public final boolean isDefault() { 
		return mIsDefault; 
	}
	
	void setDefault(boolean isDefault) { 
		mIsDefault = isDefault; 
	}
	
	public String getApplicationName() { 
		return getClass().getName(); 
	}
	
	public ModuleAppInfo getAppInfo() { 
		return new ModuleAppInfo(this); 
	}
	
	public ModuleAppSetting getAppSetting() { 
		return null; 
	}
	
	public OptionsMenu getOptionsMenu() { 
		return null; 
	}
	
	public String getActivityClassName() { 
		return null; 
	}
	
	public boolean gotoDefaultActivity() { 
		return false;
	}
	
	public void setOnAppMessageListener(OnAppMessageListener listener) { 
		mOnAppMessageListener = listener;
	}
	
	public final String getMessage() { 
		return mMessage;
	}
	
	public final void setAppMessage(String message) { 
		mMessage = message;
		onAppMessage(message); 
		
		OnAppMessageListener listener = mOnAppMessageListener; 
		if (listener != null) 
			listener.onAppMessage(this, message);
	}
	
	public void onAppMessage(String message) { 
		// do nothing
	}
	
}
