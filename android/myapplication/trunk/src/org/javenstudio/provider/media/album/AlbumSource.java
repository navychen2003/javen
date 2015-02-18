package org.javenstudio.provider.media.album;

import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.media.MediaSourceBase;

public abstract class AlbumSource extends MediaSourceBase {
	private static final Logger LOG = Logger.getLogger(AlbumSource.class);
	
	public AlbumSource(String name) { 
		this(name, null);
	}
	
	public AlbumSource(String name, SelectManager manager) { 
		super(name, manager);
	}
	
	private OnAlbumClickListener mItemClickListener = null;
	private OnAlbumClickListener mViewClickListener = null;
	
	public void setOnAlbumItemClickListener(OnAlbumClickListener l) { mItemClickListener = l; }
	public OnAlbumClickListener getOnAlbumItemClickListener() { return mItemClickListener; }
	
	public void setOnAlbumViewClickListener(OnAlbumClickListener l) { mViewClickListener = l; }
	public OnAlbumClickListener getOnAlbumViewClickListener() { return mViewClickListener; }
	
	public abstract AlbumSet getAlbumSet();
	public abstract AlbumDataSets getAlbumDataSets();
	
	@Override
	public boolean isActionEnabled(DataAction action) { 
		if (action == DataAction.DELETE) {
			AlbumSet albumSet = getAlbumSet();
			if (albumSet != null) return albumSet.isDeleteEnabled();
		}
		return false; 
	}
	
	@Override
	public void onActionDone(DataAction action, int progress, int success) { 
		AlbumSet albumSet = getAlbumSet();
		if (albumSet != null && success > 0) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("onActionDone: notifyDirty, albumSet=" 
						+ albumSet + " success=" + success);
			}
			
			albumSet.notifyDirty();
		}
	}
	
}
