package org.javenstudio.cocoka.data;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.ContentListener;

public abstract class AbstractMediaSet extends MediaHelper 
		implements IMediaSet {
	
	private final long mIdentity = ResourceHelper.getIdentity();
	
	public final long getIdentity() {
		return mIdentity;
	}
	
    @Override
    public int getIndexHint() { return 0; }
    
    @Override
    public void setIndexHint(int index) {}
    
    @Override
	public int getItemCount() { return 0; }
	
    @Override
    public final int getTotalItemCount() {
        int total = getItemCount();
        for (int i = 0, n = getSubSetCount(); i < n; i++) {
        	IMediaSet subset = getSubSetAt(i);
        	if (subset == null) continue;
            total += subset.getTotalItemCount();
        }
        return total;
    }
    
    @Override
    public IMediaItem findItem(int index) {
        for (int i = 0, n = getSubSetCount(); i < n; ++i) {
            IMediaSet subset = getSubSetAt(i);
            int count = subset.getTotalItemCount();
            if (index < count) 
                return findItem(index);
            
            index -= count;
        }
        
        List<IMediaItem> list = getItemList(index, 1);
        return list.isEmpty() ? null : list.get(0);
    }
    
    // Returns the media items in the range [start, start + count).
    //
    // The number of media items returned may be less than the specified count
    // if there are not enough media items available. The number of
    // media items available may not be consistent with the return value of
    // getMediaItemCount() because the contents of database may have already
    // changed.
	@Override
	public List<IMediaItem> getItemList(int start, int count) { 
		List<IMediaItem> list = new ArrayList<IMediaItem>();
		return list;
	}
	
	@Override
	public int getSubSetCount() { 
		return 0;
	}
	
	@Override
	public IMediaSet getSubSetAt(int index) { 
		return null;
	}
	
    // TODO: we should have better implementation of sub classes
	@Override
    public final int getIndexOf(IMediaItem path, int hint) {
        // hint < 0 is handled below
        // first, try to find it around the hint
        int start = Math.max(0,
                hint - MEDIAITEM_BATCH_FETCH_COUNT / 2);
        List<IMediaItem> list = getItemList(
                start, MEDIAITEM_BATCH_FETCH_COUNT);
        int index = getIndexOf(path, list);
        if (index != INDEX_NOT_FOUND) return start + index;

        // try to find it globally
        start = start == 0 ? MEDIAITEM_BATCH_FETCH_COUNT : 0;
        list = getItemList(start, MEDIAITEM_BATCH_FETCH_COUNT);
        while (true) {
            index = getIndexOf(path, list);
            if (index != INDEX_NOT_FOUND) return start + index;
            if (list.size() < MEDIAITEM_BATCH_FETCH_COUNT) return INDEX_NOT_FOUND;
            start += MEDIAITEM_BATCH_FETCH_COUNT;
            list = getItemList(start, MEDIAITEM_BATCH_FETCH_COUNT);
        }
    }

    private final int getIndexOf(IMediaItem path, List<IMediaItem> list) {
        for (int i = 0, n = list.size(); i < n; ++i) {
            // item could be null only in ClusterAlbum
        	IMediaItem item = list.get(i);
            if (item != null && item == path) 
            	return i;
        }
        return INDEX_NOT_FOUND;
    }
	
    private WeakHashMap<ContentListener, Object> mListeners =
            new WeakHashMap<ContentListener, Object>();

    // NOTE: The MediaSet only keeps a weak reference to the listener. The
    // listener is automatically removed when there is no other reference to
    // the listener.
    @Override
    public final void addContentListener(ContentListener listener) {
        if (mListeners.containsKey(listener)) 
            throw new IllegalArgumentException();
        
        mListeners.put(listener, null);
    }

    @Override
    public final void removeContentListener(ContentListener listener) {
        if (!mListeners.containsKey(listener)) 
            throw new IllegalArgumentException();
        
        mListeners.remove(listener);
    }

    // This should be called by subclasses when the content is changed.
    @Override
    public final void notifyContentChanged() {
        for (ContentListener listener : mListeners.keySet()) {
            listener.onContentDirty();
        }
    }
	
	@Override
	public long reloadData() { return 0; }
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + getIdentity() + "{}";
	}
	
}
