package org.javenstudio.android.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import org.javenstudio.android.data.media.download.DownloadMediaSource;
import org.javenstudio.android.data.media.local.LocalMediaSource;
import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.common.util.Logger;

public class DataManager {
	private static final Logger LOG = Logger.getLogger(DataManager.class);
	
    // Any one who would like to access data should require this lock
    // to prevent concurrency issue.
    public static final Object LOCK = new Object();
    
	private final DataApp mApplication;
	private final Handler mDefaultMainHandler;
	
	private final Map<String, DataSource> mSourceMap =
            new LinkedHashMap<String, DataSource>();
	
	private final Map<Uri, NotifyBroker> mNotifierMap =
            new HashMap<Uri, NotifyBroker>();
	
    private static class NotifyBroker extends ContentObserver {
        private WeakHashMap<ChangeNotifier, Object> mNotifiers =
                new WeakHashMap<ChangeNotifier, Object>();

        public NotifyBroker(Handler handler) {
            super(handler);
        }

        public synchronized void registerNotifier(ChangeNotifier notifier) {
            mNotifiers.put(notifier, null);
        }

        @Override
        public synchronized void onChange(boolean selfChange) {
            for (ChangeNotifier notifier : mNotifiers.keySet()) {
                notifier.onChange(selfChange);
            }
        }
    }
	
	public DataManager(DataApp app) { 
		mApplication = app;
		mDefaultMainHandler = new Handler(app.getMainLooper());
	
		MediaUtils.initialize(app.getContext());
		initializeSourceMap();
	}
	
	private void initializeSourceMap() {
        if (!mSourceMap.isEmpty()) return;

        // the order matters, the UriSource must come last
        addSource(new LocalMediaSource(mApplication));
        addSource(new DownloadMediaSource(mApplication));
	}
	
	public LocalMediaSource getLocalSource() { 
		DataSource source = getSource(LocalMediaSource.PREFIX);
		if (source == null)
			throw new RuntimeException("Local source not initialized");
		
		return (LocalMediaSource)source;
	}
	
	public DownloadMediaSource getDownloadSource() { 
		DataSource source = getSource(DownloadMediaSource.PREFIX);
		if (source == null)
			throw new RuntimeException("Download source not initialized");
		
		return (DownloadMediaSource)source;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends DataSource> List<T> getSources(Class<T> clazz) { 
		List<T> sources = new ArrayList<T>();
		synchronized (this) {
			for (DataSource source : mSourceMap.values()) { 
				if (clazz.isAssignableFrom(source.getClass())) 
					sources.add((T)source);
			}
		}
		return sources;
	}
	
    public synchronized void addSource(DataSource source) {
        if (source == null) return;
        
        if (mSourceMap.containsKey(source.getPrefix())) 
        	throw new RuntimeException("Source: " + source.getPrefix() + " already exists");
        
        mSourceMap.put(source.getPrefix(), source);
    }
    
    public synchronized DataSource getSource(String prefix) { 
    	DataSource source = mSourceMap.get(prefix);
    	if (source == null) 
    		throw new RuntimeException("Source: " + prefix + " not registered");
    	
    	return source;
    }
    
    public synchronized DataSource getSourceOrNull(String prefix) { 
    	return mSourceMap.get(prefix);
    }
    
    public void registerChangeNotifier(Uri uri, ChangeNotifier notifier) {
        NotifyBroker broker = null;
        synchronized (mNotifierMap) {
            broker = mNotifierMap.get(uri);
            if (broker == null) {
                broker = new NotifyBroker(mDefaultMainHandler);
                mApplication.getContentResolver().registerContentObserver(uri, true, broker);
                mNotifierMap.put(uri, broker);
            }
        }
        broker.registerNotifier(notifier);
    }
    
    public DataObject getDataObject(String path) { 
    	return getDataObject(DataPath.fromString(path));
    }
    
    public DataObject getDataObject(DataPath path) { 
    	synchronized (DataManager.LOCK) {
            DataObject obj = path.getObject();
            if (obj != null) 
            	return obj;
            
            DataSource source = getSource(path.getPrefix());
            if (source != null) 
            	return source.getDataObject(path);
            
            if (LOG.isWarnEnabled())
            	LOG.warn("Source for path: " + path + " not registered");
    	}
    	
    	return null;
    }
    
}
