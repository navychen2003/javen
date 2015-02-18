package org.javenstudio.provider.media.photo;

import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.media.MediaSourceBase;

public abstract class PhotoSource extends MediaSourceBase 
		implements SelectMode.Callback {
	private static final Logger LOG = Logger.getLogger(PhotoSource.class);
	
	public PhotoSource(String name) { 
		this(name, null);
	}
	
	public PhotoSource(String name, SelectManager manager) { 
		super(name, manager);
	}
	
	private OnPhotoClickListener mItemClickListener = null;
	private OnPhotoClickListener mViewClickListener = null;
	private OnPhotoClickListener mUserClickListener = null;
	
	public void setOnPhotoItemClickListener(OnPhotoClickListener l) { mItemClickListener = l; }
	public OnPhotoClickListener getOnPhotoItemClickListener() { return mItemClickListener; }
	
	public void setOnPhotoViewClickListener(OnPhotoClickListener l) { mViewClickListener = l; }
	public OnPhotoClickListener getOnPhotoViewClickListener() { return mViewClickListener; }
	
	public void setOnPhotoUserClickListener(OnPhotoClickListener l) { mUserClickListener = l; }
	public OnPhotoClickListener getOnPhotoUserClickListener() { return mUserClickListener; }
	
	public abstract PhotoSet getPhotoSet();
	public abstract PhotoDataSets getPhotoDataSets();
	public abstract void clearPhotoList();
	
	@Override
	public boolean isActionEnabled(DataAction action) { 
		if (action == DataAction.DELETE) {
			PhotoSet photoSet = getPhotoSet();
			if (photoSet != null) return photoSet.isDeleteEnabled();
		}
		return false; 
	}
	
	@Override
	public void onActionDone(DataAction action, int progress, int success) { 
		PhotoSet photoSet = getPhotoSet();
		if (photoSet != null && success > 0) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("onActionDone: notifyDirty, photoSet=" 
						+ photoSet + " success=" + success);
			}
			
			photoSet.notifyDirty();
		}
	}
	
}
