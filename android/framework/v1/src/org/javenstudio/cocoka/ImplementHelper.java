package org.javenstudio.cocoka;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import org.apache.http.params.HttpParams;
import org.javenstudio.cocoka.android.PreferenceAdapter;
import org.javenstudio.cocoka.net.DelegatedSSLSocketFactory;
import org.javenstudio.cocoka.net.SecureSocketFactory;
import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageListener;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.cocoka.database.sqlite.SQLiteOpenHelper;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.cocoka.util.MimeTypes.MimeResources;
import org.javenstudio.common.entitydb.db.EntityObserver;

public class ImplementHelper implements StorageManager.Directories, 
		MimeTypes.MimeResourcesHelper, PreferenceAdapter, StorageListener, 
		DelegatedSSLSocketFactory.SocketFactoryCreator, SimpleHttpClient.HttpParamsInitializer, 
		SQLiteEntityDB.DatabaseListener, EntityObserver.Handler {

	private final Implements mImpl;
	private final MimeTypes.MimeResources mMimeResources;
	
	ImplementHelper(Implements impl) { 
		mImpl = impl;
		mMimeResources = 
			new MimeTypes.MimeResources() { 
					public String getString(int resId) { 
						return Implements.getResourceContext().getString(resId); 
					}
					public Drawable getDrawable(int resId) { 
						return Implements.getResourceContext().getDrawable(resId); 
					}
				};
	}
	
	@Override
	public void post(Runnable runable) { 
		Implements.getHandler().post(runable);
	}
	
	@Override
	public String getCacheDirectory() { 
		return mImpl.getLocalCacheDirectory();
	}
	
	@Override
	public String getStorageDirectory(String name) { 
		return mImpl.getLocalStorageDirectory(name);
	}
	
	@Override
	public MimeResources getMimeResources() { 
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
	public void onStorageOpened(Storage storage) { 
		mImpl.onStorageOpened(storage);
	}
	
	@Override
	public void onDatabaseCreated(SQLiteOpenHelper db) { 
		// do nothing
	}
	
	@Override
	public SecureSocketFactory createSocketFactory(int handshakeTimeoutMillis, boolean secure) { 
		return mImpl.createSocketFactory(handshakeTimeoutMillis, secure);
	}
	
	@Override
	public void initHttpParams(HttpParams params) { 
		mImpl.initHttpParams(params);
	}
	
}
