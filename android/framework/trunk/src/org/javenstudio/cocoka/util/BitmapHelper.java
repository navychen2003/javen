package org.javenstudio.cocoka.util;

public class BitmapHelper {

    // NOTE: These type numbers are stored in the image cache, so it should not
    // not be changed without resetting the cache.
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;
    public static final int TYPE_ROUNDTHUMBNAIL = 3;
	
    private static final int BYTESBUFFE_POOL_SIZE = 4;
    private static final int BYTESBUFFER_SIZE = 200 * 1024;

    private static int sMicrothumbnailTargetSize = 200;
    private static BitmapPool sMicroThumbPool;
    private static final BytesBufferPool sMicroThumbBufferPool =
            new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);

    private static int sThumbnailTargetSize = 640;
    private static final BitmapPool sThumbPool =
            ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_FACTORY
            ? new BitmapPool(4)
            : null;
    
    public static int getTargetSize(int type) {
        switch (type) {
            case TYPE_THUMBNAIL:
                return sThumbnailTargetSize;
            case TYPE_MICROTHUMBNAIL:
                return sMicrothumbnailTargetSize;
            case TYPE_ROUNDTHUMBNAIL:
                return sMicrothumbnailTargetSize;
            default:
                throw new RuntimeException(
                    "should only request thumb/microthumb from cache");
        }
    }

    public static BitmapPool getMicroThumbPool() {
        if (ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_FACTORY && sMicroThumbPool == null) {
            initializeMicroThumbPool();
        }
        return sMicroThumbPool;
    }

    public static BitmapPool getThumbPool() {
        return sThumbPool;
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }

    private static void initializeMicroThumbPool() {
        sMicroThumbPool =
                ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_FACTORY
                ? new BitmapPool(sMicrothumbnailTargetSize, sMicrothumbnailTargetSize, 16)
                : null;
    }

    public static void setThumbnailSizes(int size, int microSize) {
        sThumbnailTargetSize = size;
        if (sMicrothumbnailTargetSize != microSize) {
            sMicrothumbnailTargetSize = microSize;
            initializeMicroThumbPool();
        }
    }
	
}
