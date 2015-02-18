package org.javenstudio.cocoka.data;

import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.cocoka.util.MediaUtils;

public class MediaHelper {

    private static AtomicLong sVersionSerial = new AtomicLong(0);
    
    public static long nextVersionNumber() {
        return sVersionSerial.incrementAndGet();
    }

    public static int getTypeFromString(String s) {
        if (IMediaObject.MEDIA_TYPE_ALL_STRING.equals(s)) return IMediaObject.MEDIA_TYPE_ALL;
        if (IMediaObject.MEDIA_TYPE_IMAGE_STRING.equals(s)) return IMediaObject.MEDIA_TYPE_IMAGE;
        if (IMediaObject.MEDIA_TYPE_VIDEO_STRING.equals(s)) return IMediaObject.MEDIA_TYPE_VIDEO;
        throw new IllegalArgumentException(s);
    }

    public static String getTypeString(int type) {
        switch (type) {
            case IMediaObject.MEDIA_TYPE_IMAGE: return IMediaObject.MEDIA_TYPE_IMAGE_STRING;
            case IMediaObject.MEDIA_TYPE_VIDEO: return IMediaObject.MEDIA_TYPE_VIDEO_STRING;
            case IMediaObject.MEDIA_TYPE_ALL: return IMediaObject.MEDIA_TYPE_ALL_STRING;
        }
        throw new IllegalArgumentException();
    }
	
    public static String getMimeType(int type) {
        switch (type) {
            case IMediaObject.MEDIA_TYPE_IMAGE :
                return MediaUtils.MIME_TYPE_IMAGE;
            case IMediaObject.MEDIA_TYPE_VIDEO :
                return MediaUtils.MIME_TYPE_VIDEO;
            default: 
            	return MediaUtils.MIME_TYPE_ALL;
        }
    }
    
}
