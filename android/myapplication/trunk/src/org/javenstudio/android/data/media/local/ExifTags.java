package org.javenstudio.android.data.media.local;

/**
 * The class holds the EXIF tag names that are not available in
 * {@link android.media.ExifInterface} prior to API level 11.
 */
public interface ExifTags {
    static final String TAG_ISO = "ISOSpeedRatings";
    static final String TAG_EXPOSURE_TIME = "ExposureTime";
    static final String TAG_APERTURE = "FNumber";
}
