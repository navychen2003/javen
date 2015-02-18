package org.javenstudio.android.data.media;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.LoadCallback;

public interface MediaInfo {

    public static final int COUNT_COMMENT = IMediaObject.COUNT_COMMENT;
    public static final int COUNT_FAVORITE = IMediaObject.COUNT_FAVORITE;
    public static final int COUNT_LIKE = IMediaObject.COUNT_LIKE;
    public static final int COUNT_PHOTO = IMediaObject.COUNT_PHOTO;
    public static final int COUNT_FOLLOW = IMediaObject.COUNT_FOLLOW;
	
	public String getTitle();
	public String getSubTitle();
	public String getAuthor();
	
	public String getUserId();
	public String getAlbumId();
	public String getPhotoId();
	
	public String getAvatarLocation();
	
	public long getDateInMs();
	public int getStatisticCount(int type);
	
	public void getDetails(IMediaDetails details);
	public void getExifs(IMediaDetails details);
	
	public IMediaComments getComments(LoadCallback callback);
	public Drawable getProviderIcon();
	
	public int getMediaType();
	
	public String getShareType();
	public String getShareText();
	
}
