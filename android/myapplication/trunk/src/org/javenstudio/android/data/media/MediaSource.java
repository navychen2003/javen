package org.javenstudio.android.data.media;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.android.data.DataManager;
import org.javenstudio.android.data.DataObject;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.DataSource;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public abstract class MediaSource extends DataSource implements AlbumSet {
	private static final Logger LOG = Logger.getLogger(MediaSource.class);

	private final String mPrefix;
	
	public MediaSource(String prefix) { 
		mPrefix = prefix;
		
		if (mPrefix == null || mPrefix.length() == 0) 
			throw new IllegalArgumentException("prefix is empty");
	}
	
	public final String getPrefix() { return mPrefix; }
	
	public String getName() { return getPrefix(); }
	public boolean isDeleteEnabled() { return false; }
	public boolean isDirty() { return false; }
	
	public abstract String[] getTopSetPaths();
	protected abstract MediaObject createMediaObject(DataPath path);
	
	public void notifyDirty() {}
	
	@Override
	public MediaSet[] getMediaSets() { 
		MediaObject[] topSets = getTopSets();
		List<MediaSet> sets = new ArrayList<MediaSet>();
		
		for (int i=0; topSets != null && i < topSets.length; i++) { 
			MediaObject obj = topSets[i];
			if (obj != null && obj instanceof MediaSet)
				sets.add((MediaSet)obj);
		}
		
		return sets.toArray(new MediaSet[sets.size()]);
	}
	
    public MediaObject[] getTopSets() { 
    	return getMediaObjects(getTopSetPaths());
    }
    
    public final MediaObject[] getMediaObjects(String[] paths) { 
    	ArrayList<MediaObject> sets = new ArrayList<MediaObject>();
    	
    	for (int i=0; paths != null && i < paths.length; i++) { 
    		MediaObject object = getMediaObject(paths[i]);
    		if (object != null) 
    			sets.add(object);
    	}
    	
    	return sets.toArray(new MediaObject[sets.size()]);
    }
    
    public MediaObject getMediaObject(String s) { 
    	return getMediaObject(DataPath.fromString(s));
    }
    
    public MediaObject peekMediaObject(DataPath path) {
        return (MediaObject)path.getObject();
    }
    
    public final MediaObject getMediaObject(DataPath path) { 
        synchronized (DataManager.LOCK) {
            DataObject obj = path.getObject();
            if (obj != null && obj instanceof MediaObject) 
            	return (MediaObject)obj;

            if (!getPrefix().equals(path.getPrefix())) {
            	if (LOG.isWarnEnabled())
                	LOG.warn("Source: " + getPrefix() + " is not for path: " + path);
            	
                return null;
            }

            try {
                MediaObject object = createMediaObject(path);
                if (object != null) 
                	return object;
                
            	if (LOG.isWarnEnabled())
                	LOG.warn("Cannot create media object: " + path);
            	
            } catch (Throwable t) {
            	if (LOG.isWarnEnabled())
                	LOG.warn("Exception in creating media object: " + path, t);
            }
            
            return null;
        }
    }
	
    @Override
    public DataObject getDataObject(DataPath path) { 
    	return getMediaObject(path);
    }
    
    public static final Comparator<MediaItem> sDateTakenComparator =
            new DateTakenComparator();

    private static class DateTakenComparator implements Comparator<MediaItem> {
	        @Override
	        public int compare(MediaItem item1, MediaItem item2) {
	            return -Utils.compare(item1.getDateInMs(), item2.getDateInMs());
	        }
	    }
    
}
