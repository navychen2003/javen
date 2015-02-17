package org.javenstudio.cocoka;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import org.apache.http.params.HttpParams;
import org.javenstudio.cocoka.android.ModuleApp;
import org.javenstudio.cocoka.android.PreferenceAdapter;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.ISocketFactory;
import org.javenstudio.cocoka.net.SocketFactoryCreator;
import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.common.entitydb.db.EntityObserver;

public class ImplementHelper implements StorageManager.Directories, 
		MimeTypes.MimeResourcesHelper, PreferenceAdapter, BitmapHolder, 
		SocketFactoryCreator, SimpleHttpClient.HttpParamsInitializer, 
		EntityObserver.Handler {

	private final Implements mImpl;
	private final MimeTypes.MimeResources mMimeResources;
	
	ImplementHelper(Implements impl) { 
		mImpl = impl;
		mMimeResources = new MimeTypes.MimeResources() { 
				@Override
				public String getString(int resId) { 
					return Implements.getResourceContext().getString(resId); 
				}
				@Override
				public Drawable getDrawable(int resId) { 
					return Implements.getResourceContext().getDrawable(resId); 
				}
			};
	}
	
	@Override
	public Context getContext() { 
		return Implements.getContext();
	}
	
	@Override
	public void post(Runnable runable) { 
		Implements.getHandler().post(runable);
	}
	
	@Override
	public String getStorageDirectory(String name) { 
		return mImpl.getLocalStorageDirectory(name);
	}
	
	@Override
	public MimeTypes.MimeResources getMimeResources() { 
		return mMimeResources;
	}
	
	@Override
	public SharedPreferences getPreferences() { 
		return mImpl.getPreferences();
	}
	
	@Override
	public String getStringKey(String id) { 
		return mImpl.getStringKeyWithId(id);
	}
	
	@Override
	public ISocketFactory createSocketFactory(int handshakeTimeoutMillis, 
			SocketFactoryCreator.Type type) { 
		return mImpl.createSocketFactory(handshakeTimeoutMillis, type);
	}
	
	@Override
	public void initHttpParams(HttpParams params) { 
		mImpl.onInitHttpParams(params);
	}
	
	@Override
	public void addBitmap(BitmapRef bitmap) {
		// global bitmap holder
	}
	
	static Context startActivity(Context fromActivity, 
			String className, Intent intent) { 
		if (className == null && intent == null) 
			return null;
		
		Context context = fromActivity; 
		if (context == null) 
			context = ResourceHelper.getContext(); 
		
		if (intent == null) 
			intent = new Intent();
		
		if (className != null)
			intent.setClassName(context, className);
		
		//if (context instanceof BaseActivity) { 
		//	((BaseActivity)context).startActivitySafely(intent); 
		//	return context;
		//}
		
		context.startActivity(intent);
		
		return context;
	}
	
	static void setAppMessage(ModuleApp[] apps, 
			String className, String message) { 
		if (apps == null || className == null || className.length() == 0) 
			return;
		
		for (int i=0; apps != null && i < apps.length; i++) { 
			ModuleApp app = apps[i]; 
			if (app == null) continue; 
			
			if (app.getClass().getName().equals(className)) { 
				app.setAppMessage(message);
				continue;
			}
			
			String activityName = app.getActivityClassName();
			if (activityName != null && activityName.equals(className)) { 
				app.setAppMessage(message);
				continue;
			}
		}
	}
	
}
