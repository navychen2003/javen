package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IFileData;
import org.javenstudio.falcon.datum.IFileInfo;
import org.javenstudio.falcon.datum.IFolderInfo;
import org.javenstudio.raptor.io.Text;

public abstract class SqRootFile extends SqSection 
		implements IFileData, IFileInfo {
	private static final Logger LOG = Logger.getLogger(SqRootFile.class);
	
	private final SqRoot mRoot;
	private final NameData mNameData;
	private final String mContentType;
	
	public SqRootFile(SqRoot root, 
			NameData nameData, String contentType) {
		super(root.getLibrary());
		if (nameData == null) throw new NullPointerException();
		mRoot = root;
		mNameData = nameData;
		mContentType = contentType;
		onCreated();
	}

	public SqRoot getRoot() { return mRoot; }
	public NameData getNameData() { return mNameData; }
	
	@Override
	public final boolean isFolder() { 
		return false; 
	}

	@Override
	public String getName() {
		String ext = getExtension();
		return getNameData().getAttrs().getName().toString() 
				+ (ext != null && ext.length() > 0 ? ("." + ext) : "");
	}
	
	@Override
	public String getExtension() {
		return getNameData().getAttrs().getExtension().toString();
	}
	
	@Override
	public final String getContentKey() {
		return getPathKey();
	}

	@Override
	public final String getPathKey() {
		return getNameData().getKey().toString();
	}
	
	@Override
	public final String getParentPath() {
		return getRoot().getParentPath() + getRoot().getName() 
				+ getNameData().getAttrs().getPath().toString();
	}
	
	@Override
	public final String getContentPath() {
		String path = getParentPath();
		if (path == null) path = "/";
		if (!path.endsWith("/")) path += "/";
		return path + getName();
	}
	
	@Override
	public String getContentType() { 
		return mContentType;
	}
	
	@Override
	public long getContentLength() {
		return getNameData().getAttrs().getLength();
	}
	
	//@Override
	//public long getCreatedTime() { 
	//	return getNameData().getCreatedTime();
	//}
	
	//public void setCreatedTime(long time) { 
	//	getNameData().setCreatedTime(time);
	//}
	
	@Override
	public long getModifiedTime() { 
		return getNameData().getAttrs().getModifiedTime();
	}
	
	public void setModifiedTime(long time) { 
		getNameData().getAttrs().setModifiedTime(time);
	}
	
	@Override
	public long getIndexedTime() { 
		return getNameData().getAttrs().getMarkTime();
	}
	
	//@Override
	//public void setIndexedTime(long time) { 
	//	getNameData().setIndexedTime(time);
	//}
	
	@Override
	public final String getParentId() {
		String parentKey = getParentKey();
		return parentKey != null && parentKey.length() > 0 ? 
				getRoot().getManager().getUserKey() + getRoot().getLibrary().getContentKey() + 
				getRoot().getContentKey() + parentKey : null;
	}

	public final String getParentKey() { 
		return getNameData().getAttrs().getParentKey().toString();
	}
	
	@Override
	public final String getFolderId() {
		return getParentId();
	}

	@Override
	public final InputStream open() throws IOException {
		try {
			return getRoot().openFile(
					getNameData().getAttrs().getFileIndex(), 
					getContentKey());
		} catch (ErrorException ex) { 
			Throwable e = ex.getCause();
			if (e == null) e = ex;
			if (e instanceof IOException)
				throw (IOException)e;
			else
				throw new IOException(e.toString(), e);
		}
	}
	
	@Override
	public int getWidth() { 
		return getNameData().getAttrs().getWidth();
	}
	
	@Override
	public int getHeight() { 
		return getNameData().getAttrs().getHeight();
	}
	
	@Override
	public long getDuration() { 
		return getNameData().getAttrs().getDuration();
	}
	
	private FileData loadFileData() throws IOException, ErrorException { 
		return getRoot().loadFileData(
				getNameData().getAttrs().getFileIndex(), 
				getContentKey());
	}
	
	public FileData getFileData() throws IOException { 
		try {
			return loadFileData();
		} catch (ErrorException ex) { 
			Throwable e = ex.getCause();
			if (e == null) e = ex;
			if (e instanceof IOException)
				throw (IOException)e;
			else
				throw new IOException(e.toString(), e);
		}
	}
	
	@Override
	public IFolderInfo getRootInfo() throws ErrorException {
		return getRoot().getLibrary();
	}

	@Override
	public IFolderInfo getParentInfo() throws ErrorException {
		return (IFolderInfo)getRoot().getSection(getParentKey());
	}
	
	@Override
	public int getMetaTag(Map<String,Object> tags) throws ErrorException {
		if (tags == null) return 0;
		
		int count = 0;
		try {
			FileData fileData = getFileData();
			if (fileData != null) { 
				int width = fileData.getAttrs().getWidth();
				int height = fileData.getAttrs().getHeight();
				
				if (width > 0) tags.put("Width", width);
				if (height > 0) tags.put("Height", height);
				
				FileMetaTag[] metatags = fileData.getMetaTags();
				if (metatags != null) { 
					for (FileMetaTag tag : metatags) { 
						Text name = tag.getTagName();
						Text value = tag.getTagValue();
						
						if (name != null && value != null) {
							tags.put(name.toString(), value.toString());
							count ++;
						}
					}
				}
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getMetaTag: " + getName() + " error: " + e);
			
			//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		return count;
	}
	
	@Override
	public int getMetaInfo(Map<String,Object> infos) throws ErrorException {
		if (infos == null) return 0;
		
		int count = 0;
		try {
			//count += addMetaInfo(infos, "Poster", getNameData().getPoster());
			//count += addMetaInfo(infos, "Background", getNameData().getBackground());
			//count += addMetaInfo(infos, "Title", getNameData().getTitle());
			//count += addMetaInfo(infos, "Sub Title", getNameData().getSubTitle());
			//count += addMetaInfo(infos, "Summary", getNameData().getSummary());
			
			FileData fileData = getFileData();
			if (fileData != null) { 
				FileMetaInfo[] metainfos = fileData.getMetaInfos();
				if (metainfos != null) { 
					for (FileMetaInfo info : metainfos) { 
						Text name = info.getName();
						Text value = info.getValue();
						
						if (name != null && value != null && name.getLength() > 0 && 
							value.getLength() > 0) {
							infos.put(name.toString(), value.toString());
							count ++;
						}
					}
				}
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getMetaInfo: " + getName() + " error: " + e);
			
			//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		return count;
	}
	
	@SuppressWarnings("unused")
	private static int addMetaInfo(Map<String,Object> infos, 
			String name, Text value) { 
		if (name != null && value != null && name.length() > 0 && 
			value.getLength() > 0) {
			infos.put(name, value.toString());
			return 1;
		}
		return 0;
	}
	
}
