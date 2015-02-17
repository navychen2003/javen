package org.javenstudio.cocoka.util;

import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;

public final class MimeTypes {

	public static interface MimeResources { 
		public String getString(int resId); 
		public Drawable getDrawable(int resId); 
	}
	
	public static interface MimeResourcesHelper { 
		public MimeResources getMimeResources();
	}
	
	private static MimeResourcesHelper sHelper = null; 
	private static MimeResources sResourcesEmpty = new MimeResources() { 
			public String getString(int resId) { return null; }
			public Drawable getDrawable(int resId) { return null; }
		};
	
	public static synchronized void setResourcesHelper(MimeResourcesHelper helper) { 
		if (helper != null && helper != sHelper) 
			sHelper = helper;
	}
		
	private static synchronized MimeResources getResourceContext() { 
		MimeResources resources = null;
		MimeResourcesHelper helper = sHelper;
		if (helper != null) 
			resources = helper.getMimeResources(); 
		if (resources == null) 
			resources = sResourcesEmpty;
		return resources; 
	}
	
	public static class ResourceInfo { 
		
		private final int mIconResource; 
		private final int mNameResource; 
		
		public ResourceInfo(int iconRes, int nameRes) { 
			mIconResource = iconRes; 
			mNameResource = nameRes; 
		}
		
		public Drawable getIconDrawable() { 
			if (mIconResource != 0) 
				return getResourceContext().getDrawable(mIconResource); 
			else 
				return null; 
		}
		
		public String getDisplayName() { 
			if (mNameResource != 0) 
				return getResourceContext().getString(mNameResource); 
			else 
				return null; 
		}
	}
	
	public static final class FileTypeInfo { 
		private final MimeTypeInfo mMimeTypeInfo; 
		private final String mExtensionName; 
		private final String mContentType;
		private ResourceInfo mResource = null; 
		
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
		
		public final ResourceInfo getResourceInfo() { 
			ResourceInfo info = mResource; 
			if (info != null) 
				return info; 
			else 
				return mMimeTypeInfo.getResourceInfo(); 
		}
		
		public void setResource(int iconRes, int nameRes) { 
			setResourceInfo(new ResourceInfo(iconRes, nameRes)); 
		}
		
		public synchronized void setResourceInfo(ResourceInfo info) { 
			mResource = info; 
		}
		
		public Drawable getIconDrawable() { 
			ResourceInfo info = mResource; 
			if (info != null) 
				return info.getIconDrawable(); 
			else 
				return mMimeTypeInfo.getIconDrawable(); 
		}
		
		public String getDisplayName() { 
			ResourceInfo info = mResource; 
			if (info != null) 
				return info.getDisplayName(); 
			else 
				return mMimeTypeInfo.getDisplayName(); 
		}
	}
	
	public static final class MimeTypeInfo { 
		private final String mMimeType; 
		private final MimeType mType; 
		private Map<String, FileTypeInfo> mFileTypes = null; 
		private ResourceInfo mResource = null; 
		
		public MimeTypeInfo(String mimeType, MimeType type) { 
			mMimeType = mimeType; 
			mType = type; 
		}
		
		public final String getMimeType() { return mMimeType; }
		public final MimeType getType() { return mType; }
		
		public final ResourceInfo getResourceInfo() { 
			return mResource; 
		}
		
		public void setResource(int iconRes, int nameRes) { 
			setResourceInfo(new ResourceInfo(iconRes, nameRes)); 
		}
		
		public synchronized void setResourceInfo(ResourceInfo info) { 
			mResource = info; 
		}
		
		public Drawable getIconDrawable() { 
			ResourceInfo info = mResource; 
			if (info != null) 
				return info.getIconDrawable(); 
			else 
				return null; 
		}
		
		public String getDisplayName() { 
			ResourceInfo info = mResource; 
			if (info != null) 
				return info.getDisplayName(); 
			else 
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
	
	private static MimeTypeInfo getMimeTypeInfo(String mimeType) { 
		synchronized (sMimeTypes) { 
			if (mimeType != null) 
				return sMimeTypes.get(mimeType);
		}
		return null;
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
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_AUDIO.getType(), MimeType.TYPE_AUDIO); 
			info.addFileType("amr"); 
			info.addFileType("mp3"); 
			info.addFileType("mid"); 
		}
		
		{
			MimeTypeInfo info = MimeTypes.registerMimeType(
					MimeType.TYPE_APPLICATION.getType(), MimeType.TYPE_APPLICATION); 
			info.addFileType("doc");
		}
	}
	
	public static MimeType getExtensionMimeType(String extension) {
		if (extension != null) {
			String ext = extension.toLowerCase(); 
			FileTypeInfo info = getFileTypeInfo(ext); 
			if (info != null) 
				return info.getMimeTypeInfo().getType(); 
		}
		
		return MimeType.TYPE_APPLICATION; 
	}
	
	public static FileTypeInfo getExtensionFileType(String extension) {
		if (extension != null) {
			String ext = extension.toLowerCase(); 
			FileTypeInfo info = getFileTypeInfo(ext); 
			if (info != null) 
				return info; 
		}
		return null;
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
	
	public static MimeTypeInfo lookupMimeType(String filename, String contentType) { 
		if (filename != null) { 
			FileTypeInfo info = lookupFileType(filename); 
			if (info != null) 
				return info.getMimeTypeInfo(); 
		}
		
		return lookupMimeType(contentType); 
	}
	
	public static FileTypeInfo lookupFileType(String filename) { 
		if (filename != null) { 
			int pos = filename.lastIndexOf('.'); 
			if (pos >= 0) { 
				String name = filename.substring(pos+1); 
				if (name != null) { 
					name = name.toLowerCase(); 
					FileTypeInfo info = getFileTypeInfo(name); 
					if (info != null) 
						return info; 
				}
			}
		}
		
		return null; 
	}
	
	public static String lookupFileContentType(String filename) { 
		FileTypeInfo info = lookupFileType(filename);
		if (info != null) 
			return info.getContentType();
		
		return MimeType.TYPE_APPLICATION.getType();
	}
	
}
