package org.javenstudio.common.util;

import java.util.HashMap;
import java.util.Map;

public final class MimeTypes {

	public static final class FileTypeInfo { 
		private final MimeTypeInfo mMimeTypeInfo; 
		private final String mExtensionName; 
		private final String mContentType;
		
		public FileTypeInfo(MimeTypeInfo info, String name) { 
			mMimeTypeInfo = info; 
			mExtensionName = name; 
			
			String mimeType = info.getMimeType();
			if (mimeType.endsWith("/*")) 
				mimeType = mimeType.substring(0, mimeType.length()-1) + name;
			
			mContentType = mimeType;
		}
		
		public final MimeTypeInfo getMimeTypeInfo() { 
			return mMimeTypeInfo; 
		}
		
		public final String getExtensionName() { 
			return mExtensionName; 
		}
		
		public final String getContentType() { 
			return mContentType;
		}
	}
	
	public static final class MimeTypeInfo { 
		private final String mMimeType; 
		private final MimeType mType; 
		private Map<String, FileTypeInfo> mFileTypes = null; 
		
		public MimeTypeInfo(String mimeType, MimeType type) { 
			mMimeType = mimeType; 
			mType = type; 
		}
		
		public final String getMimeType() { return mMimeType; }
		public final MimeType getType() { return mType; }
		
		public synchronized FileTypeInfo[] getFileTypes() { 
			Map<String, FileTypeInfo> types = mFileTypes;
			if (types != null)
				return types.values().toArray(new FileTypeInfo[types.size()]);
			return null;
		}
		
		public synchronized FileTypeInfo addFileType(String name) { 
			if (name != null && name.length() > 0) { 
				int pos = name.lastIndexOf('.'); 
				if (pos >= 0) { 
					name = name.substring(pos+1); 
					if (name == null || name.length() <= 0) 
						return null; 
				}
				name = name.toLowerCase(); 
				
				if (mFileTypes == null) 
					mFileTypes = new HashMap<String, FileTypeInfo>(); 
				
				FileTypeInfo info = mFileTypes.get(name); 
				if (info == null) { 
					info = new FileTypeInfo(this, name); 
					mFileTypes.put(name, info); 
					onFileTypeAdded(name, info); 
				}
				
				return info; 
			}
			
			return null; 
		}
		
		public synchronized boolean hasFileType(String name) { 
			if (name != null && mFileTypes != null) { 
				int pos = name.lastIndexOf('.'); 
				if (pos >= 0) { 
					name = name.substring(pos+1); 
					if (name == null || name.length() <= 0) 
						return false; 
				}
				
				return mFileTypes.containsKey(name.toLowerCase()); 
				
			} else 
				return false; 
		}
	}

	private static final Map<String, MimeTypeInfo> sMimeTypes = 
			new HashMap<String, MimeTypeInfo>(); 
	private static final Map<String, FileTypeInfo> sFileTypes = 
			new HashMap<String, FileTypeInfo>(); 
	
	public static MimeTypeInfo registerMimeType(String mimeType, MimeType type) { 
		if (mimeType != null && mimeType.length() > 0) { 
			mimeType = mimeType.toLowerCase(); 
			
			synchronized (sMimeTypes) { 
				MimeTypeInfo info = sMimeTypes.get(mimeType); 
				if (info == null && type != null) { 
					info = new MimeTypeInfo(mimeType, type); 
					sMimeTypes.put(mimeType, info); 
				}
				
				return info; 
			}
		}
		return null; 
	}
	
	private static void onFileTypeAdded(String name, FileTypeInfo info) { 
		if (name != null && name.length() > 0 && info != null) {
			synchronized (sMimeTypes) { 
				sFileTypes.put(name, info); 
			}
		}
	}
	
	public static MimeTypeInfo getMimeTypeInfo(String mimeType) { 
		synchronized (sMimeTypes) { 
			if (mimeType != null) 
				return sMimeTypes.get(mimeType);
		}
		return null;
	}
	
	public static MimeTypeInfo[] getMimeTypeInfos() { 
		synchronized (sMimeTypes) { 
			return sMimeTypes.values().toArray(new MimeTypeInfo[sMimeTypes.size()]);
		}
	}
	
	private static FileTypeInfo getFileTypeInfo(String name) { 
		synchronized (sMimeTypes) { 
			if (name != null) 
				return sFileTypes.get(name);
		}
		return null;
	}
	
	static { 
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_TEXT.getType(), MimeType.TYPE_TEXT); 
			info.addFileType("txt"); 
			info.addFileType("xml"); 
			info.addFileType("htm"); 
			info.addFileType("html"); 
			info.addFileType("css"); 
			info.addFileType("js"); 
			info.addFileType("ini"); 
			info.addFileType("java"); 
			info.addFileType("properties"); 
			info.addFileType("bat"); 
			info.addFileType("sh"); 
			info.addFileType("c"); 
			info.addFileType("php"); 
			info.addFileType("cc"); 
			info.addFileType("cpp"); 
			info.addFileType("h"); 
			info.addFileType("mk"); 
			info.addFileType("py"); 
			info.addFileType("hpp"); 
			info.addFileType("conf"); 
			info.addFileType("dtd"); 
			info.addFileType("xsl"); 
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_IMAGE.getType(), MimeType.TYPE_IMAGE); 
			info.addFileType("jpg"); 
			info.addFileType("jpeg"); 
			info.addFileType("png"); 
			info.addFileType("bmp"); 
			info.addFileType("gif"); 
			info.addFileType("tif"); 
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_VIDEO.getType(), MimeType.TYPE_VIDEO); 
			info.addFileType("avi"); 
			info.addFileType("mp4"); 
			info.addFileType("wmv"); 
			info.addFileType("rmvb"); 
			info.addFileType("rm"); 
			info.addFileType("mkv"); 
			info.addFileType("flv"); 
			info.addFileType("3gp"); 
			info.addFileType("m4v"); 
			info.addFileType("asf"); 
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_AUDIO.getType(), MimeType.TYPE_AUDIO); 
			info.addFileType("amr"); 
			info.addFileType("mp3"); 
			info.addFileType("mid"); 
			info.addFileType("wma"); 
			info.addFileType("wav"); 
			info.addFileType("m4a"); 
			info.addFileType("ogg"); 
			info.addFileType("flac"); 
			info.addFileType("aiff"); 
			info.addFileType("ra"); 
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_APPLICATION.getType(), MimeType.TYPE_APPLICATION); 
			info.addFileType("doc");
			info.addFileType("xls");
			info.addFileType("ppt");
			info.addFileType("pdf");
			info.addFileType("psd");
			info.addFileType("apk");
			info.addFileType("zip");
			info.addFileType("jar");
			info.addFileType("rar");
			info.addFileType("tar");
			info.addFileType("gz");
			info.addFileType("tgz");
			info.addFileType("bz2");
			info.addFileType("7z");
			info.addFileType("dll");
			info.addFileType("com");
			info.addFileType("exe");
			info.addFileType("msi");
			info.addFileType("iso");
			info.addFileType("chm");
			info.addFileType("cab");
			info.addFileType("dmg");
			info.addFileType("ttf");
			info.addFileType("ttc");
			info.addFileType("mht");
			info.addFileType("bin");
			info.addFileType("dat");
		}
	}
	
	public static MimeType getExtensionType(String extension) {
		if (extension != null) {
			String ext = extension.toLowerCase(); 
			FileTypeInfo info = getFileTypeInfo(ext); 
			if (info != null) 
				return info.getMimeTypeInfo().getType(); 
		}
		
		return MimeType.TYPE_APPLICATION; 
	}
	
	public static MimeType lookupType(String contentType) { 
		MimeTypeInfo info = lookupMimeType(contentType); 
		if (info != null) 
			return info.getType(); 
		
		return MimeType.TYPE_APPLICATION; 
	}
	
	public static MimeTypeInfo lookupMimeType(String contentType) { 
		if (contentType != null) { 
			String type = contentType.toLowerCase(); 
			MimeTypeInfo info = getMimeTypeInfo(type); 
			if (info == null) { 
				int pos = type.indexOf('/'); 
				if (pos > 0) { 
					type = type.substring(0, pos) + "/*"; 
					info = getMimeTypeInfo(type); 
				}
			}
			if (info != null) 
				return info; 
		}
		
		return null; 
	}
	
	public static FileTypeInfo getFileTypeByExtension(String extname) { 
		if (extname != null) { 
			String name = extname;
			int pos = extname.lastIndexOf('.'); 
			if (pos > 0) 
				name = extname.substring(pos+1); 
				
			if (name != null && name.length() > 0) { 
				name = name.toLowerCase(); 
				FileTypeInfo info = getFileTypeInfo(name); 
				if (info != null) 
					return info; 
				
				MimeTypeInfo mimeInfo = MimeTypes.registerMimeType(
						MimeType.TYPE_APPLICATION.getType(), 
						MimeType.TYPE_APPLICATION); 
				
				return new FileTypeInfo(mimeInfo, name);
			}
		}
		
		return null; 
	}
	
	public static FileTypeInfo getFileTypeByFilename(String filename) { 
		if (filename != null) { 
			String name = null; 
			int pos = filename.lastIndexOf('.'); 
			if (pos > 0) 
				name = filename.substring(pos+1); 
				
			return getFileTypeByExtension(name);
		}
		
		return null; 
	}
	
	public static MimeTypeInfo getMimeTypeByExtension(String extname, 
			String contentType) { 
		if (extname != null) { 
			FileTypeInfo info = getFileTypeByExtension(extname); 
			if (info != null) 
				return info.getMimeTypeInfo(); 
		}
		
		return lookupMimeType(contentType); 
	}
	
	public static MimeTypeInfo getMimeTypeByFilename(String filename, 
			String contentType) { 
		if (filename != null) { 
			FileTypeInfo info = getFileTypeByFilename(filename); 
			if (info != null) 
				return info.getMimeTypeInfo(); 
		}
		
		return lookupMimeType(contentType); 
	}
	
	public static String getContentTypeByExtension(String extname) { 
		FileTypeInfo info = getFileTypeByExtension(extname);
		if (info != null) 
			return info.getContentType();
		
		return MimeType.TYPE_APPLICATION.getType();
	}
	
	public static String getContentTypeByFilename(String filename) { 
		FileTypeInfo info = getFileTypeByFilename(filename);
		if (info != null) 
			return info.getContentType();
		
		return MimeType.TYPE_APPLICATION.getType();
	}
	
}
