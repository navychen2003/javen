package org.javenstudio.android.data.media.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.content.Context;
import android.content.res.Resources;
import android.media.ExifInterface;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class MediaDetails implements Iterable<Entry<Integer, Object>> {
	private static final Logger LOG = Logger.getLogger(MediaDetails.class);
	
    private Map<Integer, Object> mDetails = new TreeMap<Integer, Object>();
    private Map<Integer, Integer> mUnits = new HashMap<Integer, Integer>();

    public static final int INDEX_TITLE = 1;
    public static final int INDEX_DESCRIPTION = 2;
    public static final int INDEX_DATETIME = 3;
    public static final int INDEX_LOCATION = 4;
    public static final int INDEX_WIDTH = 5;
    public static final int INDEX_HEIGHT = 6;
    public static final int INDEX_ORIENTATION = 7;
    public static final int INDEX_DURATION = 8;
    public static final int INDEX_MIMETYPE = 9;
    public static final int INDEX_SIZE = 10;

    // for EXIF
    public static final int INDEX_MAKE = 100;
    public static final int INDEX_MODEL = 101;
    public static final int INDEX_FLASH = 102;
    public static final int INDEX_FOCAL_LENGTH = 103;
    public static final int INDEX_WHITE_BALANCE = 104;
    public static final int INDEX_APERTURE = 105;
    public static final int INDEX_SHUTTER_SPEED = 106;
    public static final int INDEX_EXPOSURE_TIME = 107;
    public static final int INDEX_ISO = 108;

    // Put this last because it may be long.
    public static final int INDEX_PATH = 200;

    public static class FlashState {
        static int FLASH_FIRED_MASK = 1;
        static int FLASH_RETURN_MASK = 2 | 4;
        static int FLASH_MODE_MASK = 8 | 16;
        static int FLASH_FUNCTION_MASK = 32;
        static int FLASH_RED_EYE_MASK = 64;
        
        private int mState;

        public FlashState(int state) {
            mState = state;
        }

        public boolean isFlashFired() {
            return (mState & FLASH_FIRED_MASK) != 0;
        }
    }

    public void addDetail(int index, Object value) {
        mDetails.put(index, value);
    }

    public Object getDetail(int index) {
        return mDetails.get(index);
    }

    public int size() {
        return mDetails.size();
    }

    @Override
    public Iterator<Entry<Integer, Object>> iterator() {
        return mDetails.entrySet().iterator();
    }

    public void setUnit(int index, int unit) {
        mUnits.put(index, unit);
    }

    public boolean hasUnit(int index) {
        return mUnits.containsKey(index);
    }

    public int getUnit(int index) {
        return mUnits.get(index);
    }

    private static void setExifData(MediaDetails details, ExifInterface exif, String tag,
            int key) {
        String value = exif.getAttribute(tag);
        if (value != null) {
            if (key == MediaDetails.INDEX_FLASH) {
                MediaDetails.FlashState state = new MediaDetails.FlashState(
                        Integer.valueOf(value.toString()));
                details.addDetail(key, state);
            } else {
                details.addDetail(key, value);
            }
        }
    }

    public static void extractExifInfo(MediaDetails details, String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            setExifData(details, exif, ExifInterface.TAG_FLASH, MediaDetails.INDEX_FLASH);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_WIDTH, MediaDetails.INDEX_WIDTH);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_LENGTH,
                    MediaDetails.INDEX_HEIGHT);
            setExifData(details, exif, ExifInterface.TAG_MAKE, MediaDetails.INDEX_MAKE);
            setExifData(details, exif, ExifInterface.TAG_MODEL, MediaDetails.INDEX_MODEL);
            setExifData(details, exif, ExifTags.TAG_APERTURE, MediaDetails.INDEX_APERTURE);
            setExifData(details, exif, ExifTags.TAG_ISO, MediaDetails.INDEX_ISO);
            setExifData(details, exif, ExifInterface.TAG_WHITE_BALANCE,
                    MediaDetails.INDEX_WHITE_BALANCE);
            setExifData(details, exif, ExifTags.TAG_EXPOSURE_TIME,
                    MediaDetails.INDEX_EXPOSURE_TIME);

            double data = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
            if (data != 0f) {
                details.addDetail(MediaDetails.INDEX_FOCAL_LENGTH, data);
                details.setUnit(MediaDetails.INDEX_FOCAL_LENGTH, org.javenstudio.cocoka.app.R.string.unit_mm);
            }
        } catch (IOException ex) {
            // ignore it.
        	if (LOG.isWarnEnabled())
            	LOG.warn(ex.toString(), ex);
        }
    }
	
    private static void setExifData(IMediaDetails details, ExifInterface exif, String tag) {
        String value = exif.getAttribute(tag);
        setExifData(details, tag, value);
    }
    
    private static void setExifData(IMediaDetails details, String tag, String value) {
        if (value != null) {
            details.add(getExifTagName(tag), value);
        }
    }
    
    private static final Map<String, String> sExifTags = new HashMap<String, String>();
    private static String getExifTagName(String tag) { 
    	synchronized (sExifTags) { 
    		if (sExifTags.size() <= 0) {
	    		final Resources res = ResourceHelper.getResources();
	    		
	    		sExifTags.put(ExifInterface.TAG_DATETIME, res.getString(R.string.exifs_datetime));
	    		sExifTags.put(ExifInterface.TAG_IMAGE_WIDTH, res.getString(R.string.exifs_imagewidth));
	    		sExifTags.put(ExifInterface.TAG_IMAGE_LENGTH, res.getString(R.string.exifs_imagelength));
	    		sExifTags.put(ExifInterface.TAG_MAKE, res.getString(R.string.exifs_make));
	    		sExifTags.put(ExifInterface.TAG_MODEL, res.getString(R.string.exifs_model));
	    		sExifTags.put(ExifInterface.TAG_APERTURE, res.getString(R.string.exifs_aperture));
	    		sExifTags.put(ExifInterface.TAG_ISO, res.getString(R.string.exifs_iso));
	    		sExifTags.put(ExifInterface.TAG_WHITE_BALANCE, res.getString(R.string.exifs_whitebalance));
	    		sExifTags.put(ExifInterface.TAG_EXPOSURE_TIME, res.getString(R.string.exifs_exposuretime));
	    		sExifTags.put(ExifInterface.TAG_FOCAL_LENGTH, res.getString(R.string.exifs_focallength));
	    		sExifTags.put(ExifInterface.TAG_FLASH, res.getString(R.string.exifs_flash));
    		}
    		
    		String name = sExifTags.get(tag);
    		if (name == null || name.length() == 0) 
    			return tag;
    		
    		return name;
    	}
    }
    
    public static void extractExifInfo(IMediaDetails details, String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            
            setExifData(details, exif, ExifInterface.TAG_DATETIME);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_WIDTH);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_LENGTH);
            setExifData(details, exif, ExifInterface.TAG_MAKE);
            setExifData(details, exif, ExifInterface.TAG_MODEL);
            setExifData(details, exif, ExifTags.TAG_APERTURE);
            setExifData(details, exif, ExifTags.TAG_ISO);
            setExifData(details, exif, ExifInterface.TAG_WHITE_BALANCE);
            setExifData(details, exif, ExifTags.TAG_EXPOSURE_TIME);
            
            double data = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
            if (data != 0f) {
            	setExifData(details, ExifInterface.TAG_FOCAL_LENGTH, StringUtils.limitDecimalTo2(data) + " mm");
                //details.setUnit(MediaDetails.INDEX_FOCAL_LENGTH, R.string.unit_mm);
            }
            
            setExifData(details, exif, ExifInterface.TAG_FLASH);
        } catch (IOException ex) {
            // ignore it.
        	if (LOG.isWarnEnabled())
            	LOG.warn(ex.toString(), ex);
        }
    }
    
    // Returns a (localized) string for the given duration (in seconds).
    public static String formatDuration(final Context context, int duration) {
        int h = duration / 3600;
        int m = (duration - h * 3600) / 60;
        int s = duration - (h * 3600 + m * 60);
        
        String durationValue;
        if (h == 0) {
            durationValue = String.format(context.getResources()
            		.getString(org.javenstudio.cocoka.app.R.string.details_ms), m, s);
        } else {
            durationValue = String.format(context.getResources()
            		.getString(org.javenstudio.cocoka.app.R.string.details_hms), h, m, s);
        }
        
        return durationValue;
    }
    
}
