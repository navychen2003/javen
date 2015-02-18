package org.javenstudio.android.data.media;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.android.data.DataPath;

public abstract class MediaSet extends MediaObject {
	//private static final Logger LOG = Logger.getLogger(MediaSet.class);
	
	public MediaSet(DataPath path, long version) { 
		super(path, version);
	}
	
	public abstract String getName();
    public int getSubSetCount() { return 0; }

    public MediaSet getSubSetAt(int index) {
        throw new IndexOutOfBoundsException();
    }
	
    public int getItemCount() { return 0; }
    
    public int getTotalItemCount() {
        int total = getItemCount();
        for (int i = 0, n = getSubSetCount(); i < n; i++) {
        	MediaSet subset = getSubSetAt(i);
        	if (subset == null) continue;
            total += subset.getTotalItemCount();
        }
        return total;
    }
    
    public List<MediaItem> getItemList(int start, int count) {
        return new ArrayList<MediaItem>();
    }
    
}
