package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

final class PicasaAlbumPhoto extends PicasaPhotoBase {
	private static final Logger LOG = Logger.getLogger(PicasaAlbumPhoto.class);
	
	private final PicasaAlbumPhotoSet mPhotoSet;
	private final GAlbumPhotoEntry mEntry;
	
	public PicasaAlbumPhoto(PicasaAlbumPhotoSet source, DataPath path, 
			GAlbumPhotoEntry entry) { 
		super(source, path, entry.id, entry.mediaUrl, source.getAvatarLocation());
		mPhotoSet = source;
		mEntry = entry;
	}

	public PicasaAlbumPhotoSet getPhotoSet() { return mPhotoSet; }
	public GAlbumPhotoEntry getPhotoEntry() { return mEntry; }
	
	@Override
	public String getName() {
		return mEntry.title;
	}

    @Override
    public String getMimeType() { return mEntry.contentType; }

    @Override
    public long getDateInMs() { return mEntry.updated; }
    
    @Override
    public long getSize() { return mEntry.size; }
	
	@Override
	public int getWidth() { return mEntry.width; }

	@Override
	public int getHeight() { return mEntry.height; }
    
    public String getSubTitle() { return getImageSizeInfo(); }
	public String getAuthor() { return mPhotoSet.getAuthor(); }
	
	public String getUserId() { return mEntry.user; }
	public String getAlbumId() { return mEntry.albumId; }
	public String getPhotoId() { return mEntry.photoId; }
	
	@Override
	public int getSupportedOperations() { 
		SystemUser account = getPhotoSet().getAccount();
		if (account != null) 
			return SUPPORT_DELETE;
		return 0;
	}
	
	@Override
	public int getStatisticCount(int type) { 
		if (type == MediaInfo.COUNT_COMMENT && mEntry.commentCount > 0) 
			return mEntry.commentCount;
		
		return super.getStatisticCount(type); 
	}
	
	private String getImageSizeInfo() { 
		return "" + mEntry.mediaWidth + " x " + mEntry.mediaHeight;
	}
	
	@Override
	public void getDetails(IMediaDetails details) { 
		super.getDetails(details);
		
		details.add(R.string.details_imagesize, getImageSizeInfo());
		details.add(R.string.details_posted, Utilities.formatDate(mEntry.updated));
		details.add(R.string.details_title, InformationHelper.formatTitleSpanned(mEntry.title));
		//details.add(R.string.details_summary, InformationHelper.formatSummarySpanned(mEntry.summary));
		details.add(R.string.details_author, getAuthor());
		details.add(R.string.details_nickname, mPhotoSet.getNickName());
		details.add(R.string.details_albumtitle, mPhotoSet.getAlbumTitle());
		details.add(R.string.details_description, InformationHelper.formatSummarySpanned(mEntry.mediaDescription));
	}
	
	@Override
	public boolean delete() throws DataException {
		SystemUser account = getPhotoSet().getAccount();
		if (account != null) { 
			final String accountName = account.getAccountName();
			final String userId = getUserId();
			final String albumId = getAlbumId();
			final String photoId = getPhotoId();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("delete: photo, account=" + accountName  
						+ " userId=" + userId + " albumId=" + albumId 
						+ " photoId=" + photoId);
			}
			
			try {
				boolean result = PicasaDeleter.deletePhoto(getPhotoSet().getDataApp().getContext(), 
						account, /*userId,*/ albumId, photoId);
				
				if (result) { 
					getPhotoSet().setDirty(true);
					ContentHelper.getInstance().updateFetchDirtyWithAccount(accountName);
				}
				
				return result;
			} catch (HttpException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("delete: failed: " + e + ", account=" + accountName 
							+ " photoId=" + photoId);
				}
				
				DataException ex = new DataException(e.getStatusCode(), e.getMessage(), e);
				ex.setAction(DataAction.DELETE);
				throw ex;
			}
		}
		
		return false;
	}
	
}
