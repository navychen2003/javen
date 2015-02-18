package org.javenstudio.provider.media;

import android.app.Activity;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.media.album.AlbumDataSets;
import org.javenstudio.provider.media.album.AlbumFactory;
import org.javenstudio.provider.media.album.AlbumItem;
import org.javenstudio.provider.media.album.AlbumSource;

public class MediaAlbumSource extends AlbumSource {
	private static final Logger LOG = Logger.getLogger(MediaAlbumSource.class);
	
	private final Provider mProvider;
	private final AlbumSet mAlbumSet;
	private final AlbumDataSets mDataSets;
	private final AlbumFactory mFactory;
	private final SelectAction mSelectAction;
	
	private AlbumBinder mBinderLarge = null;
	private AlbumBinder mBinderList = null;
	private AlbumBinder mBinderSmall = null;
	
	private MediaAlbumList mAlbumList = null;
	private boolean mAlbumLoading = false;
	
	public MediaAlbumSource(Provider provider, AlbumSet set, 
			AlbumFactory factory) { 
		super(set.getName());
		mProvider = provider;
		mAlbumSet = set;
		mFactory = factory;
		mDataSets = factory.createAlbumDataSets(this);
		mSelectAction = new MediaAction.AlbumSelectAction();
	}
	
	public Provider getProvider() { return mProvider; }
	
	public AlbumSet getAlbumSet() { return mAlbumSet; }
	public AlbumDataSets getAlbumDataSets() { return mDataSets; }
	
	@Override
	public synchronized ProviderBinder getBinder() { 
		switch (mFactory.getViewType().getSelectType()) { 
		case LIST: 
			if (mBinderList == null) 
				mBinderList = mFactory.createAlbumBinder(this, ViewType.Type.LIST);
			return mBinderList; 
			
		case SMALL:
			if (mBinderSmall == null) 
				mBinderSmall = mFactory.createAlbumBinder(this, ViewType.Type.SMALL);
			return mBinderSmall; 
			
		default:
			break;
		}
		
		if (mBinderLarge == null) 
			mBinderLarge = mFactory.createAlbumBinder(this, ViewType.Type.LARGE);
		
		return mBinderLarge; 
	}
	
	@Override
	public void reloadData(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		try { 
			mAlbumLoading = true;
			reloadDataInternal(callback, type);
		} finally { 
			mAlbumLoading = false;
		}
	}
	
	@Override
	public boolean isActionEnabled(DataAction action) { 
		if (mAlbumLoading) return false;
		return super.isActionEnabled(action); 
	}
	
	@Override
	public SelectAction getSelectAction() { 
		SelectAction selectAction = getAlbumSet().getSelectAction();
		if (selectAction != null) return selectAction;
		return mSelectAction; 
	}
	
	@Override
	public void onCreateAlbum(Activity activity) { 
		mFactory.onCreateAlbum(activity, getAlbumSet());
	}
	
	private void reloadDataInternal(final ProviderCallback callback, ReloadType type) { 
		MediaAlbumList albumList = mAlbumList;
		boolean dirty = mAlbumSet.isDirty();
		boolean firstLoad = false;
		int appendCount = 0;
		
		if (type == ReloadType.FORCE || albumList == null || dirty) { 
			//mAlbumSet.reloadData(callback, type);
			mAlbumList = albumList = new MediaAlbumList(mAlbumSet.getMediaSets());
			firstLoad = true;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("reloadDataInternal: type=" + type + " dirty=" + dirty 
					+ " firstLoad=" + firstLoad);
		}
		
		if (albumList != null && (firstLoad || type == ReloadType.NEXTPAGE)) {
			MediaSet[] mediaSets = albumList.nextAlbums(callback, type);
			
			if (firstLoad) { 
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getAlbumDataSets().clear();
							getSelectManager().clearSelectedItems();
						}
					});
			}
			
			for (int i=0; mediaSets != null && i < mediaSets.length; i++) { 
				MediaSet mediaSet = mediaSets[i];
				if (mediaSet == null || !(mediaSet instanceof PhotoSet)) 
					continue;
				
				PhotoSet photoSet = (PhotoSet)mediaSet;
				mediaSet.reloadData(callback, type);
				
				if (!mFactory.showAlbum(photoSet)) 
					continue;
				
				final AlbumItem item = new AlbumItem(this, 
						new MediaAlbum(photoSet));
				
				appendCount ++; 
				
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getAlbumDataSets().addAlbumItem(item, false);
							callback.getController().getModel().callbackOnDataSetUpdate(item); 
						}
					});
			}
			
			if (appendCount > 0) { 
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() { 
							getAlbumDataSets().notifyContentChanged(true);
							getAlbumDataSets().notifyDataSetChanged();
						}
					});
			}
		}
	}
	
}
