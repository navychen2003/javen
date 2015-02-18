package org.javenstudio.android.data.media;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.android.data.CacheData;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public abstract class CacheReader {
	private static final Logger LOG = Logger.getLogger(CacheReader.class);
	
	private final CacheData mService;
	
	public CacheReader(CacheData service) { 
		mService = service;
	}
	
	protected String getCacheFileName(MediaSet mediaSet) { 
		return mediaSet.getDataPath().toString();
	}
	
    public final boolean readCache(MediaSet mediaSet) { 
    	InputStream stream = null;
    	try { 
    		String filename = getCacheFileName(mediaSet);
    		StorageFile file = mService.openFile(filename, 0);
    		if (file != null) {
    			stream = file.openFile();
    		
    			if (stream != null) {
		    		DataInputStream in = new DataInputStream(stream);
		    	    return read(in);
    			}
    		} else { 
    			if (LOG.isDebugEnabled())
    				LOG.debug("Cannot open cache file: " + filename);
    		}
    	} catch (Throwable e) { 
    		if (LOG.isErrorEnabled())
    			LOG.error(e.toString(), e);
    		
    	} finally { 
    		Utils.closeSilently(stream);
    	}
    	
    	return false;
    }
	
    protected abstract boolean read(DataInput in) throws IOException;
    protected boolean isInterrupt() { return false; }
	
	protected void readMediaItem(DataInput in, MediaSet set) throws IOException {}
    protected void readMediaSet(DataInput in, MediaSet set) throws IOException {}
    
    protected void readItems(DataInput in, MediaSet set) throws IOException { 
    	{
	    	int count = in.readInt();
	    	for (int i=0; i < count; i++) { 
	    		readMediaItem(in, set);
	    	}
    	}
    	
    	{
	    	int count = in.readInt();
	    	for (int i=0; i < count; i++) { 
	    		readMediaSet(in, set);
	    		if (isInterrupt()) break;
	    	}
    	}
    }
    
}
