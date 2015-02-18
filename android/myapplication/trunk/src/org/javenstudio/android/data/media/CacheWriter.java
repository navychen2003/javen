package org.javenstudio.android.data.media;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.javenstudio.android.data.CacheData;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public abstract class CacheWriter {
	private static final Logger LOG = Logger.getLogger(CacheWriter.class);

	private final CacheData mService;
	
	public CacheWriter(CacheData service) { 
		mService = service;
	}
	
	protected String getCacheFileName(MediaSet mediaSet) { 
		return mediaSet.getDataPath().toString();
	}
	
    public final boolean saveCache(MediaSet mediaSet) {
    	OutputStream stream = null;
    	try {
    		String filename = getCacheFileName(mediaSet);
    		StorageFile file = mService.openFile(filename, 0);
    		if (file != null) {
	    		stream = file.createFile();
	    		
	    		if (stream != null) {
		    		DataOutputStream out = new DataOutputStream(stream);
		    		boolean result = write(out);
			    	out.flush();
					
			    	return result;
	    		}
    		} else { 
    			if (LOG.isWarnEnabled())
    				LOG.warn("Cannot open cache file: " + filename);
    		}
    	} catch (Throwable e) { 
    		if (LOG.isErrorEnabled())
    			LOG.error(e.toString(), e);
    		
    	} finally { 
    		Utils.closeSilently(stream);
    	}
    	
    	return false;
    }
	
    protected abstract boolean write(DataOutput out) throws IOException;
	
	protected void writeMediaItem(DataOutput out, MediaItem item) 
			throws IOException {}
	protected void writeMediaSet(DataOutput out, MediaSet item) 
			throws IOException {}
	
    protected void writeItems(DataOutput out, MediaSet set) throws IOException { 
    	List<MediaItem> items = set.getItemList(0, set.getItemCount());
    	out.writeInt(items != null ? items.size() : 0);
    	
    	if (items != null) { 
    		for (MediaItem item : items) { 
	    		writeMediaItem(out, item);
	    	}
    	}
    	
    	out.writeInt(set.getSubSetCount());
    	
    	for (int i=0; i < set.getSubSetCount(); i++) { 
    		MediaSet subSet = set.getSubSetAt(i);
    		writeMediaSet(out, subSet);
    	}
    }
    
}
