package org.javenstudio.android.data.image.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.javenstudio.cocoka.net.http.fetch.FetchCache;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.ImageFile;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public abstract class HttpCache extends FetchCache {
	private static Logger LOG = Logger.getLogger(HttpCache.class);

	public static final int CACHETYPE_CACHEINFOFILE = 100;
	
	public static final String EXTENSION_TEXT = "cachetext";
	public static final String EXTENSION_IMAGE = "cacheimage";
	public static final String EXTENSION_AUDIO = "cacheaudio";
	public static final String EXTENSION_VIDEO = "cachevideo";
	public static final String EXTENSION_APPLICATION = "cachedat";
	
	public static final String EXTENSION_THUMB = "thumb"; 
	public static final String EXTENSION_CACHEINFO = "cacheinfo"; 
	
	public static final String EXTENSION_TEXT_WITHDOT = "." + EXTENSION_TEXT;
	public static final String EXTENSION_IMAGE_WITHDOT = "." + EXTENSION_IMAGE;
	public static final String EXTENSION_AUDIO_WITHDOT = "." + EXTENSION_AUDIO;
	public static final String EXTENSION_VIDEO_WITHDOT = "." + EXTENSION_VIDEO;
	public static final String EXTENSION_APPLICATION_WITHDOT = "." + EXTENSION_APPLICATION;
	
	public static final String EXTENSION_THUMB_WITHDOT = "." + EXTENSION_THUMB;
	public static final String EXTENSION_CACHEINFO_WITHDOT = "." + EXTENSION_CACHEINFO;
	
	public HttpCache(Storage store) throws IOException {
		super(store); 
	}
	
	public static boolean isDownloadCacheFile(IFile file) { 
		if (file != null) { 
			String filename = file.getName();
			if (filename != null) { 
				if (filename.endsWith(EXTENSION_TEXT_WITHDOT))
					return true;
				if (filename.endsWith(EXTENSION_IMAGE_WITHDOT))
					return true;
				if (filename.endsWith(EXTENSION_AUDIO_WITHDOT))
					return true;
				if (filename.endsWith(EXTENSION_VIDEO_WITHDOT))
					return true;
				if (filename.endsWith(EXTENSION_APPLICATION_WITHDOT))
					return true;
				//if (filename.endsWith(EXTENSION_THUMB_WITHDOT))
				//	return true;
				if (filename.endsWith(EXTENSION_CACHEINFO_WITHDOT))
					return true;
			}
		}
		return false;
	}
	
	@Override
	protected StorageFile doOpenStorageFile(String filename, String extension, 
			MimeType mimeType, int cacheType) throws IOException {
		if (cacheType == CACHETYPE_CACHEINFOFILE) { 
			if (extension == null) 
				extension = "";
			else if (!extension.endsWith("."))
				extension = extension + ".";
			
			extension = extension + EXTENSION_CACHEINFO;
			
			return createFile(mimeType, toSaveFileName(filename), extension); 
		}
		
		return super.doOpenStorageFile(filename, extension, mimeType, cacheType);
	}
	
	@Override
	protected String normalizeCacheName(String source, String name) { 
		String filename = encodeCacheName(source);
		if (filename == null || filename.length() == 0) 
			filename = "0";
		
		if (name != null) { 
			name = name.toLowerCase();
			
			String ext = getExtensionName(name);
			if (ext != null && ext.length() > 0) 
				filename += "." + ext;
		}
		
		name = filename.charAt(0) + "/" + filename;
		
		return toNormalCacheName(source, name);
	}
	
	public static String getExtensionName(String name) { 
		if (name != null) { 
			int pos = -1; //name.lastIndexOf('.');
			for (int i=name.length()-1; i > 0; i--) { 
				char chr = name.charAt(i);
				if ((chr >= '0' && chr <= '9') || (chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z'))
					continue;
				if (chr == '-' || chr == '_') 
					continue;
				if (chr == '.') pos = i;
				break;
			}
			
			if (pos >= 0) {
				String ext = name.substring(pos+1);
				if (ext != null && ext.length() > 0 && ext.length() <= 5) 
					return ext;
			}
		}
		
		return null;
	}
	
	public static boolean isDownloadImage(FetchCache cache, IFile subFile) { 
		if (cache == null || subFile == null) return false;
		
		StorageFile f = cache.getCacheFile(subFile.getPath());
		String ext = HttpCache.getExtensionName(subFile.getName());
		
		if ((f != null && f instanceof ImageFile) || (ext != null && ext.equalsIgnoreCase("dat"))) 
			return true;
		
		return false;
	}
	
	@Override
	protected String toSaveFileName(String filename) { 
		if (filename == null) 
			filename = "";
		
		int pos = filename.length();
		while ((--pos) >= 0) { 
			char chr = filename.charAt(pos);
			if (chr != '/' && chr != '\\') 
				break;
		}
		
		if (pos < filename.length() - 1) 
			filename = filename.substring(0, pos+1);
		
		return filename;
	}
	
	@Override
	protected String toSaveExtension(MimeType type, String extension) { 
		if (extension == null) 
			extension = "";
		else if (!extension.endsWith("."))
			extension = extension + ".";
		
		if (type == MimeType.TYPE_TEXT) { 
			extension = extension + EXTENSION_TEXT;
		} else if (type == MimeType.TYPE_IMAGE) { 
			extension = extension + EXTENSION_IMAGE;
		} else if (type == MimeType.TYPE_AUDIO) { 
			extension = extension + EXTENSION_AUDIO;
		} else if (type == MimeType.TYPE_VIDEO) { 
			extension = extension + EXTENSION_VIDEO;
		} else { 
			extension = extension + EXTENSION_APPLICATION;
		}
		
		return extension;
	}
	
	@Override
	public Object loadSaveFile(StorageFile file) throws IOException { 
		if (file != null) { 
			String filename = file.getFileName();
			if (!filename.endsWith(EXTENSION_TEXT_WITHDOT)) 
				return file.loadFile();
			
			InputStream input = file.openFile();
			if (input == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("saved file not found: " + file.getFilePath());
				
				return null;
			}
			
			StringBuilder sbuf = new StringBuilder();
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(input));
				
				String line;
				
				while ((line = reader.readLine()) != null) { 
					sbuf.append(line);
					sbuf.append("\n");
				}
			} finally { 
				input.close();
			}
			
			return sbuf.toString();
		}
		
		return null;
	}
	
	public static String toMD5CacheName(final String source) {
		String text = source;
		if (text == null) text = "";
		int pos = text.indexOf('?');
		if (pos > 0) text = text.substring(0, pos);
		String result = Utilities.toMD5(text);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("toMD5CacheName: source=" + source 
					+ " text=" + text + " result=" + result);
		}
		
		return result;
	}
	
}
