package org.javenstudio.cocoka.util;

public final class MimeType {

	public static final MimeType TYPE_APPLICATION = new MimeType("application/*"); 
	public static final MimeType TYPE_TEXT 		  = new MimeType("text/*"); 
	public static final MimeType TYPE_IMAGE 	  = new MimeType("image/*"); 
	public static final MimeType TYPE_VIDEO 	  = new MimeType("video/*"); 
	public static final MimeType TYPE_AUDIO 	  = new MimeType("audio/*"); 
	
	private final String mType; 
	
	private MimeType(String type) {
		mType = type; 
	}
	
	public String getType() {
		return mType; 
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof MimeType)) 
			return false; 
		
		MimeType that = (MimeType)other; 
		if (this == that) 
			return true; 
		
		if (that.getType() == null) 
			return false; 
		
		return that.getType().equals(getType()); 
	}
	
	@Override
	public int hashCode() {
		if (mType == null)
			return 0; 
		else
			return mType.hashCode(); 
	}
	
	public String toString() {
		return mType; 
	}
}
