package org.javenstudio.provider.app.anybox;

import java.util.HashSet;
import java.util.Set;

public class AnyboxMetadata {

	public static final String AUTHOR = "author";
	public static final String TITLE = "title";
	public static final String SUBTITLE = "subtitle";
	public static final String SUMMARY = "summary";
	public static final String ALBUM = "album";
	public static final String GENRE = "genre";
	public static final String YEAR = "year";
	public static final String TAGS = "tags";
	
	public static final String TAKEN = "Date Taken";
	public static final String[] TAKEN_TAGS = {TAKEN, "Exif SubIFD Date/Time Original", "Exif SubIFD Date/Time Digitized", "Exif IFD0 Date/Time"};
	
	public static final String LATITUDE = "Latitude";
	public static final String[] LATITUDE_TAGS = {LATITUDE, "GPS GPS Latitude"};
	
	public static final String LONGITUDE = "Longitude";
	public static final String[] LONGITUDE_TAGS = {LONGITUDE, "GPS GPS Longitude"};
	
	public static final String MAKE = "Make";
	public static final String[] MAKE_TAGS = {MAKE, "Exif IFD0 Make", "Xmp Make"};
	
	public static final String MODEL = "Model";
	public static final String[] MODEL_TAGS = {MODEL, "Exif IFD0 Model", "Exif IFD0 Model", "Xmp Model", "Canon Makernote Image Type"};
	
	public static final String LENSMODEL = "Lens Model";
	public static final String[] LENSMODEL_TAGS = {LENSMODEL, "Exif SubIFD Lens Model", "Canon Makernote Lens Model"};
	
	public static final String APERTURE = "Aperture";
	public static final String[] APERTURE_TAGS = {APERTURE, "Exif SubIFD F-Number", "Exif SubIFD Aperture Value", "Xmp F-Number", "Xmp Aperture Value"};
	
	public static final String ISO = "ISO";
	public static final String[] ISO_TAGS = {ISO, "Exif SubIFD ISO Speed Ratings"};
	
	public static final String WHITEBALANCE = "White Balance";
	public static final String[] WHITEBALANCE_TAGS = {WHITEBALANCE, "Exif SubIFD White Balance", "Canon Makernote White Balance"};
	
	public static final String EXPOSURETIME = "Exposure Time";
	public static final String[] EXPOSURETIME_TAGS = {EXPOSURETIME, "Exif SubIFD Exposure Time", "Xmp Exposure Time"};
	
	public static final String EXPOSUREBIAS = "Exposure Bias";
	public static final String[] EXPOSUREBIAS_TAGS = {EXPOSUREBIAS, "Exif SubIFD Exposure Bias Value"};
	
	public static final String FOCALLENGTH = "Focal Length";
	public static final String[] FOCALLENGTH_TAGS = {FOCALLENGTH, "Exif SubIFD Focal Length", "Xmp Focal Length"};
	
	public static final String FLASH = "Flash";
	public static final String[] FLASH_TAGS = {FLASH, "Exif SubIFD Flash", "Canon Makernote Flash Activity"};
	
	public static final String WIDTH = "Width";
	public static final String[] WIDTH_TAGS = {WIDTH};
	
	public static final String HEIGHT = "Height";
	public static final String[] HEIGHT_TAGS = {HEIGHT};
	
	public static final String[][] EXIF_TAGS = {
		TAKEN_TAGS, //{TAKEN, "Exif SubIFD Date/Time Original", "Exif SubIFD Date/Time Digitized", "Exif IFD0 Date/Time"},
		MAKE_TAGS, //{MAKE, "Exif IFD0 Make", "Xmp Make"},
		MODEL_TAGS, //{MODEL, "Exif IFD0 Model", "Exif IFD0 Model", "Xmp Model", "Canon Makernote Image Type"},
		LENSMODEL_TAGS, //{LENSMODEL, "Exif SubIFD Lens Model", "Canon Makernote Lens Model"},
		APERTURE_TAGS, //{APERTURE, "Exif SubIFD F-Number", "Exif SubIFD Aperture Value", "Xmp F-Number", "Xmp Aperture Value"},
		EXPOSURETIME_TAGS, //{EXPOSURETIME, "Exif SubIFD Exposure Time", "Xmp Exposure Time"},
		EXPOSUREBIAS_TAGS, //{EXPOSUREBIAS, "Exif SubIFD Exposure Bias Value"},
		FOCALLENGTH_TAGS, //{FOCALLENGTH, "Exif SubIFD Focal Length", "Xmp Focal Length"},
		ISO_TAGS, //{ISO, "Exif SubIFD ISO Speed Ratings"},
		WHITEBALANCE_TAGS, //{WHITEBALANCE, "Exif SubIFD White Balance", "Canon Makernote White Balance"},
		FLASH_TAGS, //{FLASH, "Exif SubIFD Flash", "Canon Makernote Flash Activity"},
		LONGITUDE_TAGS, //{LONGITUDE, "GPS GPS Longitude"},
		LATITUDE_TAGS, //{LATITUDE, "GPS GPS Latitude"},
		WIDTH_TAGS,
		HEIGHT_TAGS
	};
	
	private static final Set<String> mTagNames = new HashSet<String>();
	static {
		synchronized (mTagNames) {
			for (String[] tags : EXIF_TAGS) {
				if (tags != null && tags.length > 0) {
					String name = tags[0];
					if (name != null && name.length() > 0)
						mTagNames.add(name.toLowerCase());
				}
			}
			
			mTagNames.add(AUTHOR);
			mTagNames.add(TITLE);
			mTagNames.add(SUBTITLE);
			mTagNames.add(SUMMARY);
			mTagNames.add(ALBUM);
			mTagNames.add(GENRE);
			mTagNames.add(YEAR);
			mTagNames.add(TAGS);
		}
	}
	
	public static boolean hasTagName(String name) {
		if (name == null || name.length() == 0) return false;
		synchronized (mTagNames) {
			return mTagNames.contains(name.toLowerCase());
		}
	}
	
}
