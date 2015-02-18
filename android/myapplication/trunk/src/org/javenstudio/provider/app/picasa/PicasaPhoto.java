package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.util.Utilities;

final class PicasaPhoto extends PicasaPhotoBase {

	private final PicasaPhotoSet mPhotoSet;
	private final GPhotoEntry mEntry;
	
	public PicasaPhoto(PicasaPhotoSet source, DataPath path, 
			GPhotoEntry entry) { 
		super(source, path, entry.id, entry.mediaUrl, 
			GPhotoHelper.normalizeAvatarLocation(entry.authorThumbnail));
		mPhotoSet = source;
		mEntry = entry;
	}

	public PicasaPhotoSet getPhotoSet() { return mPhotoSet; }
	public GPhotoEntry getPhotoEntry() { return mEntry; }
	
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
	public String getAuthor() { return mEntry.authorName; }
	
	public String getUserId() { return mEntry.authorUser; }
	public String getAlbumId() { return mEntry.albumId; }
	public String getPhotoId() { return mEntry.photoId; }
	
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
		details.add(R.string.details_author, mEntry.authorName);
		details.add(R.string.details_nickname, mEntry.authorNickName);
		details.add(R.string.details_albumtitle, mEntry.albumTitle);
		details.add(R.string.details_description, InformationHelper.formatSummarySpanned(mEntry.mediaDescription));
	}
	
}
