package org.javenstudio.provider.media;

import android.app.Activity;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.OnClickListener;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.media.photo.OnPhotoClickListener;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.media.photo.PhotoDataSets;
import org.javenstudio.provider.media.photo.PhotoFactory;
import org.javenstudio.provider.media.photo.PhotoItem;
import org.javenstudio.provider.media.photo.PhotoSource;

public class MediaPhotoSource extends PhotoSource {
	private static final Logger LOG = Logger.getLogger(MediaPhotoSource.class);
	
	private final Provider mProvider;
	private final PhotoSet mPhotoSet;
	private final PhotoDataSets mDataSets;
	private final PhotoFactory mFactory;
	private final SelectAction mSelectAction;
	
	private PhotoBinder mBinderLarge = null; 
	private PhotoBinder mBinderList = null; 
	private PhotoBinder mBinderSmall = null;
	
	private MediaPhotoList mPhotoList = null;
	private boolean mPhotoLoading = false;
	
	public MediaPhotoSource(Provider provider, PhotoSet set, 
			PhotoFactory factory) { 
		super(set.getName());
		mProvider = provider;
		mPhotoSet = set;
		mFactory = factory;
		mDataSets = factory.createPhotoDataSets(this);
		mSelectAction = new MediaAction.PhotoSelectAction();
	}
	
	public Provider getProvider() { return mProvider; }
	public PhotoSet getPhotoSet() { return mPhotoSet; }
	public PhotoDataSets getPhotoDataSets() { return mDataSets; }
	
	public void clearPhotoList() { mPhotoList = null; }
	
	@Override
	public synchronized ProviderBinder getBinder() { 
		switch (mFactory.getViewType().getSelectType()) { 
		case LIST: 
			if (mBinderList == null) 
				mBinderList = mFactory.createPhotoBinder(this, ViewType.Type.LIST);
			return mBinderList; 
			
		case SMALL:
			if (mBinderSmall == null) 
				mBinderSmall = mFactory.createPhotoBinder(this, ViewType.Type.SMALL);
			return mBinderSmall; 
		
		default:
			break;
		}
		
		if (mBinderLarge == null) 
			mBinderLarge = mFactory.createPhotoBinder(this, ViewType.Type.LARGE);
		
		return mBinderLarge; 
	}
	
	@Override
	public void reloadData(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		try { 
			mPhotoLoading = true;
			reloadDataInternal(callback, type);
		} finally { 
			mPhotoLoading = false;
		}
	}
	
	@Override
	public boolean isActionEnabled(DataAction action) { 
		if (mPhotoLoading) return false;
		return super.isActionEnabled(action); 
	}
	
	@Override
	public SelectAction getSelectAction() { 
		SelectAction selectAction = getPhotoSet().getSelectAction();
		if (selectAction != null) return selectAction;
		return mSelectAction; 
	}
	
	@Override
	public void onCreateAlbum(Activity activity) { 
	}
	
	private void reloadDataInternal(final ProviderCallback callback, ReloadType type) { 
		MediaPhotoList photoList = mPhotoList;
		boolean dirty = mPhotoSet.isDirty();
		boolean firstLoad = false;
		int appendCount = 0;
		
		if (type == ReloadType.FORCE || photoList == null || dirty) { 
			//mPhotoSet.reloadData(callback, type);
			mPhotoList = photoList = new MediaPhotoList(mPhotoSet.getMediaSet());
			firstLoad = true;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("reloadDataInternal: type=" + type + " dirty=" + dirty 
					+ " firstLoad=" + firstLoad);
		}
		
		if (photoList != null && (firstLoad || type == ReloadType.NEXTPAGE)) {
			MediaItem[] mediaItems = photoList.nextPhotos(callback, type);
			
			if (firstLoad) { 
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getPhotoDataSets().clear();
							getSelectManager().clearSelectedItems();
						}
					});
			}
			
			for (int i=0; mediaItems != null && i < mediaItems.length; i++) { 
				MediaItem mediaItem = mediaItems[i];
				if (mediaItem == null || !(mediaItem instanceof Photo)) 
					continue;
				
				Photo photo = (Photo)mediaItem;
				mediaItem.reloadData(callback, type);
				
				final MediaPhoto mediaPhoto = new MediaPhoto(photo);
				final PhotoItem item = new PhotoItem(this, mediaPhoto);
				
				final OnPhotoClickListener listener = getOnPhotoUserClickListener();
				if (listener != null) { 
					mediaPhoto.setTitleClickListener(new OnClickListener() {
							@Override
							public void onClick(Activity activity) {
								listener.onPhotoClick(activity, item);
							}
						});
				}
				
				appendCount ++; 
				
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getPhotoDataSets().addPhotoItem(item, false);
							callback.getController().getModel().callbackOnDataSetUpdate(item); 
						}
					});
			}
			
			if (appendCount > 0) { 
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getPhotoDataSets().notifyContentChanged(true);
							getPhotoDataSets().notifyDataSetChanged();
						}
					});
			}
		}
	}
	
}
