package org.javenstudio.cocoka.net.http.fetch;

import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.javenstudio.cocoka.storage.MediaStorageProvider;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.TempStorageFile;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.common.util.Logger;

public abstract class FetchCache extends MediaStorageProvider {
	private static Logger LOG = Logger.getLogger(FetchCache.class);

	public static final int CACHETYPE_DEFAULT = 0;
	public static final int CACHETYPE_TEMPFILE = 1;
	public static final int CACHETYPE_SAVEFILE = 2;
	
	public FetchCache(Storage store) throws IOException {
		super(store); 
	}
	
	public final String getPath() { 
		return getStorage().getDirectory().getPath(); 
	}
	
	public final String getLocation() { 
		return getStorage().getDirectoryLocation(); 
	}
	
	public StorageFile getCacheFile(String source) {
		return getCacheFile(source, (MimeType)null); 
	}
	
	public StorageFile getCacheFile(String source, MimeType type) {
		if (source == null) 
			return null; 
		
		try { 
			return openCacheFile(source, type); 
		} catch (Exception e) { 
			return null; 
		}
	}
	
	public void synchronizeDirectory(final String location, final String[] dirnames, 
			SynchronizeCallback callback) { 
		if (location == null || location.length() == 0 || callback == null || 
			dirnames == null || dirnames.length == 0) 
			return; 
		
		final HashSet<String> set = new HashSet<String>(); 
		for (int i=0; dirnames != null && i < dirnames.length; i++) { 
			String dirname = normalizeDirectoryName(dirnames[i]); 
			if (dirname != null && dirname.length() > 0) 
				set.add(dirname); 
		}
		if (set.size() == 0) 
			return; 
		
		SynchronizeCallback.Checker checker = new SynchronizeCallback.Checker() {
				@Override
				public boolean isExisted(String filename) {
					if (filename == null || filename.length() == 0) 
						return false; 
					else
						return set.contains(filename); 
				}
			};
		
		synchronizeDirectory(location, callback, checker); 
	}
	
	public void synchronizeDirectory(final String location, SynchronizeCallback callback, 
			SynchronizeCallback.Checker checker) { 
		if (location == null || location.length() == 0 || callback == null) 
			return; 
		
		try { 
			final String path = toCacheFilePath(location); 
			final Storage store = getStorage(); 
			final IFile file = store.getFsFile(path); 
			final IFileSystem fs = store.getFileSystem(); 
			
			if (file != null && file.isDirectory()) { 
				IFile[] files = fs.listFiles(file); 
				if (files == null || files.length == 0) 
					return; 
				
				for (int i=0; files != null && i < files.length; i++) { 
					IFile f = files[i]; 
					if (f == null) continue; 
					if (!checker.isExisted(f.getName()) && callback.canRemove(f, checker)) { 
						store.removeDir(f); 
					}
				}
			}
			
		} catch (IOException e) { 
			LOG.warn("synchronize directory error", e); 
		}
	}
	
	public void synchronizeFiles(final String location, final String[] filenames, 
			SynchronizeCallback callback) { 
		if (location == null || location.length() == 0 || callback == null || 
			filenames == null || filenames.length == 0) 
			return; 
		
		final HashSet<String> set = new HashSet<String>(); 
		for (int i=0; filenames != null && i < filenames.length; i++) { 
			String filename = normalizeFileName(filenames[i]); 
			if (filename != null && filename.length() > 0) { 
				String tempname = TempStorageFile.getTempFileName(filename); 
				set.add(filename); 
				set.add(tempname); 
			}
		}
		if (set.size() == 0) 
			return; 
		
		SynchronizeCallback.Checker checker = new SynchronizeCallback.Checker() {
				@Override
				public boolean isExisted(String filename) {
					if (filename == null || filename.length() == 0) 
						return false; 
					else
						return set.contains(filename); 
				}
			};
		
		synchronizeFiles(location, callback, checker); 
	}
	
	public void synchronizeFiles(final String location, SynchronizeCallback callback, 
			SynchronizeCallback.Checker checker) { 
		if (location == null || location.length() == 0 || callback == null || checker == null) 
			return; 
		
		try { 
			final String path = toCacheFilePath(location); 
			final Storage store = getStorage(); 
			final IFile file = store.getFsFile(path); 
			final IFileSystem fs = store.getFileSystem(); 
			
			if (file != null && file.isDirectory()) { 
				IFile[] files = fs.listFiles(file); 
				if (files == null || files.length == 0) 
					return; 
				
				for (int i=0; files != null && i < files.length; i++) { 
					IFile f = files[i]; 
					if (f == null) continue; 
					if (!checker.isExisted(f.getName()) && callback.canRemove(f, checker)) { 
						store.removeFile(f); 
					}
				}
			}
			
		} catch (IOException e) { 
			LOG.warn("synchronize files error", e); 
		}
	}
	
	protected String normalizeDirectoryName(String dirname) { 
		if (dirname != null && dirname.indexOf('/') >= 0) { 
			StringTokenizer st = new StringTokenizer(dirname, " \t\r\n/\\"); 
			while (st.hasMoreTokens()) { 
				dirname = st.nextToken(); 
			}
		}
		return dirname; 
	}
	
	protected String normalizeFileName(String filename) { 
		if (filename != null && filename.indexOf('/') >= 0) { 
			StringTokenizer st = new StringTokenizer(filename, " \t\r\n/\\"); 
			while (st.hasMoreTokens()) { 
				filename = st.nextToken(); 
			}
		}
		return filename; 
	}
	
	public StorageFile openCacheFile(String source) throws IOException {
		return openCacheFile(source, null); 
	}
	
	public StorageFile openCacheFile(String source, MimeType type) throws IOException {
		return openStorageFile(source, type, CACHETYPE_DEFAULT); 
	}
	
	public StorageFile openSaveFile(String source) throws IOException {
		return openSaveFile(source, null); 
	}
	
	public StorageFile openSaveFile(String source, MimeType type) throws IOException {
		return openStorageFile(source, type, CACHETYPE_SAVEFILE); 
	}
	
	public StorageFile openTempFile(String source) throws IOException {
		return openTempFile(source, null); 
	}
	
	public StorageFile openTempFile(String source, MimeType type) throws IOException {
		return openStorageFile(source, type, CACHETYPE_TEMPFILE); 
	}
	
	public StorageFile openStorageFile(String source, int cacheType) throws IOException {
		return openStorageFile(source, null, cacheType);
	}
	
	public StorageFile openStorageFile(String source, MimeType mimeType, 
			int cacheType) throws IOException {
		if (source == null) return null; 
		
		String filepath = toCacheFilePath(source); 
		String filename = filepath, extension = null; 
		
		int pathpos1 = filepath.lastIndexOf('/');
		int pathpos2 = filepath.lastIndexOf('\\');
		
		int pos = filepath.lastIndexOf('.'); 
		if (pos > 0 && pos > pathpos1 && pos > pathpos2) {
			filename = filepath.substring(0, pos); 
			extension = filepath.substring(pos+1); 
		}
		
		if (mimeType == null || mimeType == MimeType.TYPE_APPLICATION) {
			if (extension != null) 
				mimeType = MimeTypes.getExtensionMimeType(extension); 
		}
		
		if (mimeType == null) 
			mimeType = MimeType.TYPE_APPLICATION; 
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("openStorageFile: source=" + source 
					+ " filename=" + filename + " extension=" + extension 
					+ " mimeType=" + mimeType + " cacheType=" + cacheType);
		}
		
		return doOpenStorageFile(filename, extension, mimeType, cacheType);
	}
	
	protected StorageFile doOpenStorageFile(String filename, String extension, 
			MimeType mimeType, int cacheType) throws IOException {
		if (cacheType == CACHETYPE_TEMPFILE) 
			return createTempFile(mimeType, filename, extension);
		
		if (cacheType == CACHETYPE_SAVEFILE) {
			return createFile(mimeType, toSaveFileName(filename), 
					toSaveExtension(mimeType, extension)); 
		}
		
		if (cacheType == CACHETYPE_DEFAULT)
			return createFile(mimeType, filename, extension);
		
		return null;
	}
	
	protected String toCacheFilePath(String source) {
		String origsource = source; 
		if (source != null) {
			int pos = source.indexOf("://"); 
			if (pos >= 0) source = source.substring(pos+3); 
		}
		
		String path = null, name = null; 
		
		int pos = source.indexOf('/'); 
		if (pos > 0) {
			path = source.substring(0, pos); 
			name = source.substring(pos+1); 
		} else {
			path = "unknown"; 
			name = source; 
		}
		
		if (path == null || path.length() == 0)
			path = "unknown";
		
		if (name == null) 
			name = origsource; 
		
		path = path.replaceAll(":", "_");
		
		return path + "/" + normalizeCacheName(source, name); 
	}
	
	protected abstract String encodeCacheName(String source);
	
	//protected String encodeCacheName(String source) {
	//	return Utilities.toMD5(source);
	//}
	
	protected String normalizeCacheName(String source, String name) { 
		name = encodeCacheName(source);
		name = toNormalCacheName(source, name);
		
		return name;
	}
	
	public static String toNormalCacheName(String source, String name) { 
		StringBuilder sb = new StringBuilder();
		boolean foundW = false;
		char preChr = 0;
		
		for (int i=0; name != null && i < name.length(); i++) { 
			char chr = name.charAt(i);
			
			if ((chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z') || (chr >= '0' && chr <= '9') || 
				(chr == '-' || chr == '#' || chr == '-' || chr == '+' || chr == '.') || 
				(chr == '$' || chr == '%' || chr == '&' || chr == '=' || chr == '@')) {
				//sb.append(chr);
			} else if (chr == '?') { 
				foundW = true;
				chr = '/';
			} else if (chr == '/' || chr == '\\') {
				chr = foundW ? '-' : '/';
			} else 
				chr = '-';
			
			if (chr != preChr || (preChr != '/' && preChr != '-'))
				sb.append(chr);
			
			preChr = chr;
		}
		
		return sb.toString();
	}
	
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
	
	protected String toSaveExtension(MimeType type, String extension) { 
		return extension;
	}
	
	public Object loadSaveFile(StorageFile file) throws IOException { 
		if (file != null) 
			return file.loadFile();
		
		return null;
	}
	
}
