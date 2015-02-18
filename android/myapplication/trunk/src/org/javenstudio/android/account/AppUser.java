package org.javenstudio.android.account;

import org.javenstudio.cocoka.data.IMediaObject;

public interface AppUser {

    public static final int COUNT_COMMENT = IMediaObject.COUNT_COMMENT;
    public static final int COUNT_FAVORITE = IMediaObject.COUNT_FAVORITE;
    public static final int COUNT_LIKE = IMediaObject.COUNT_LIKE;
    public static final int COUNT_PHOTO = IMediaObject.COUNT_PHOTO;
    public static final int COUNT_FOLLOW = IMediaObject.COUNT_FOLLOW;
	
	public String getUserId();
	public String getUserTitle();
	
	//public Image getAvatarImage();
	public int getStatisticCount(int type);
	
}
