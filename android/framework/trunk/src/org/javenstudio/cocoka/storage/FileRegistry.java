package org.javenstudio.cocoka.storage;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;

public class FileRegistry {

	private static final Map<MimeType, Class<? extends StorageFile> > mTypes = 
			new HashMap<MimeType, Class<? extends StorageFile> >(); 
	
	static { 
		registerType(MimeType.TYPE_APPLICATION,  ApplicationStorageFile.class); 
		registerType(MimeType.TYPE_TEXT,  TextStorageFile.class); 
		registerType(MimeType.TYPE_IMAGE, SimpleImageFile.class); 
		registerType(MimeType.TYPE_AUDIO, SimpleAudioFile.class); 
		registerType(MimeType.TYPE_VIDEO, ApplicationStorageFile.class); 
	}
	
	public static void registerType(MimeType type, Class<? extends StorageFile> fileClass) { 
		synchronized (mTypes) { 
			if (type != null && fileClass != null) 
				mTypes.put(type, fileClass);
		}
	}
	
	public static Class<? extends StorageFile> lookup(MimeType type) { 
		synchronized (mTypes) { 
			if (type != null) 
				return mTypes.get(type); 
			else
				return null; 
		}
	}
	
	public static Class<? extends StorageFile> lookupByExtension(String extension) { 
		return lookup(MimeTypes.getExtensionMimeType(extension)); 
	}
	
}
