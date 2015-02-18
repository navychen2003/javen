package org.javenstudio.cocoka.data;

public interface IMediaObject {
	
	public static final long INVALID_DATA_VERSION = -1;

    public static final int MEDIAITEM_BATCH_FETCH_COUNT = 500;
    public static final int INDEX_NOT_FOUND = -1;
	
    // These are the bits returned from getSupportedOperations():
    public static final int SUPPORT_DELETE = 1 << 0;
    public static final int SUPPORT_ROTATE = 1 << 1;
    public static final int SUPPORT_SHARE = 1 << 2;
    public static final int SUPPORT_CROP = 1 << 3;
    public static final int SUPPORT_SHOW_ON_MAP = 1 << 4;
    public static final int SUPPORT_SETAS = 1 << 5;
    public static final int SUPPORT_FULL_IMAGE = 1 << 6;
    public static final int SUPPORT_PLAY = 1 << 7;
    public static final int SUPPORT_CACHE = 1 << 8;
    public static final int SUPPORT_EDIT = 1 << 9;
    public static final int SUPPORT_INFO = 1 << 10;
    public static final int SUPPORT_IMPORT = 1 << 11;
    public static final int SUPPORT_TRIM = 1 << 12;
    public static final int SUPPORT_UNLOCK = 1 << 13;
    public static final int SUPPORT_BACK = 1 << 14;
    public static final int SUPPORT_ACTION = 1 << 15;
    public static final int SUPPORT_DOWNLOAD = 1 << 16;
    public static final int SUPPORT_CAMERA_SHORTCUT = 1 << 17;
    public static final int SUPPORT_SHARETO = 1 << 18;
    public static final int SUPPORT_ALL = 0xffffffff;

    // These are the bits returned from getMediaType():
    public static final int MEDIA_TYPE_UNKNOWN = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 4;
    public static final int MEDIA_TYPE_AUDIO = 8;
    public static final int MEDIA_TYPE_ALL = MEDIA_TYPE_IMAGE | MEDIA_TYPE_VIDEO | MEDIA_TYPE_AUDIO;

    public static final String MEDIA_TYPE_IMAGE_STRING = "image";
    public static final String MEDIA_TYPE_VIDEO_STRING = "video";
    public static final String MEDIA_TYPE_ALL_STRING = "all";

    // These are flags for cache() and return values for getCacheFlag():
    public static final int CACHE_FLAG_NO = 0;
    public static final int CACHE_FLAG_SCREENNAIL = 1;
    public static final int CACHE_FLAG_FULL = 2;

    // These are return values for getCacheStatus():
    public static final int CACHE_STATUS_NOT_CACHED = 0;
    public static final int CACHE_STATUS_CACHING = 1;
    public static final int CACHE_STATUS_CACHED_SCREENNAIL = 2;
    public static final int CACHE_STATUS_CACHED_FULL = 3;
    
    public static final int COUNT_COMMENT = 1;
    public static final int COUNT_FAVORITE = 2;
    public static final int COUNT_LIKE = 3;
    public static final int COUNT_PHOTO = 4;
    public static final int COUNT_FOLLOW = 5;
    
    public static final int ERROR_CONTENTURI_IS_NULL = 1;
    
}
