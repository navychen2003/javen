package org.javenstudio.cocoka.data;

import java.util.concurrent.atomic.AtomicBoolean;

public class SnailAlbum extends SingleMediaSet {

	private AtomicBoolean mDirty = new AtomicBoolean(false);
	private long mDataVersion = 0;
	
	public SnailAlbum(SnailItem item) { 
		super(item);
	}
	
	@Override
    public long reloadData() {
        if (mDirty.compareAndSet(true, false)) {
            //((SnailItem) getItem()).updateVersion();
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    public void notifyChange() {
        mDirty.set(true);
        notifyContentChanged();
    }
	
}
