package org.javenstudio.provider.library;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.data.AbstractMediaSet;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.common.util.Logger;

public abstract class AbstractPhotoSet extends AbstractMediaSet {
	private static final Logger LOG = Logger.getLogger(AbstractPhotoSet.class);

	private final long mVersion = nextVersionNumber();
	private final ArrayList<SectionPhotoItem> mList = 
			new ArrayList<SectionPhotoItem>();
	
	//private final IPhotoList mPhotoList;
	private int mIndexHint = 0;
	
	//public AbstractPhotoSet(IPhotoList list) {
	//	if (list == null) throw new NullPointerException();
	//	mPhotoList = list;
	//}
	
	//public IPhotoList getPhotoList() { 
	//	return mPhotoList;
	//}
	
	public int getPhotoCount() { 
		synchronized (mList) { return mList.size(); }
	}
	
	public SectionPhotoItem getPhotoAt(int index) {
		synchronized (mList) {
			if (index >= 0 && index < mList.size())
				return mList.get(index);
			return null;
		}
	}
	
	public void addPhoto(SectionPhotoItem item) {
		if (item == null) return;
		synchronized (mList) {
			mList.add(item);
		}
	}
	
	protected abstract SectionPhotoItem createPhotoItem(IPhotoData data);
	
	@Override
	public List<IMediaItem> getItemList(int start, int count) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("getItemList: start=" + start + " count=" + count);
		
		List<IMediaItem> list = new ArrayList<IMediaItem>();
		for (int i=start; i < start+count && i < getPhotoCount(); i++) { 
			IMediaItem item = getPhotoAt(i);
			if (item != null) list.add(item);
		}
		return list;
	}

	@Override
	public int getItemCount() {
		return getPhotoCount();
	}
	
	@Override
	public int getIndexHint() { return mIndexHint; }
	
	@Override
	public void setIndexHint(int index) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("setIndexHint: index=" + index 
					+ " count=" + getPhotoCount());
		}
		mIndexHint = index; 
	}
	
	@Override
	public long reloadData() { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("reloadData: version=" + mVersion 
					+ " count=" + getPhotoCount());
		}
		return mVersion; 
	}
	
	public Drawable getProviderIcon() { 
		return null;
	}
	
	public boolean onActionDetails(Activity activity, IPhotoData data) {
		return false;
	}
	
	public boolean onActionDownload(Activity activity, IPhotoData data) {
		return false;
	}
	
}
