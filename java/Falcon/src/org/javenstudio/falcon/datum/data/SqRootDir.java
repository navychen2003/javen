package org.javenstudio.falcon.datum.data;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IFolderInfo;

public abstract class SqRootDir extends SqSectionDir {

	private final SqRoot mRoot;
	private final NameData mNameData;
	
	private long mSubLength = -1;
	
	public SqRootDir(SqRoot root, NameData nameData) {
		super(root.getLibrary());
		if (nameData == null) throw new NullPointerException();
		mRoot = root;
		mNameData = nameData;
		onCreated();
	}

	public SqRoot getRoot() { return mRoot; }
	public NameData getNameData() { return mNameData; }
	
	@Override
	protected synchronized SqSection[] loadSections(
			boolean byfolder) throws ErrorException { 
		return byfolder ? 
				getRoot().loadSectionsInternal(this) : 
				null;
	}
	
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
	public String getName() {
		return getNameData().getAttrs().getName().toString();
	}
	
	//@Override
	//public String getExtension() {
	//	return getNameData().getExtName().toString();
	//}
	
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
	public int getSubCount() { 
		return getNameData().getFileCount();
	}
	
	@Override
	public IFolderInfo getParentInfo() throws ErrorException {
		return (IFolderInfo)getRoot().getSection(getParentKey());
	}
	
	@Override
	public synchronized long getSubLength() { 
		if (mSubLength == -1) { 
			try { 
				SqRootNames names = new SqRootNames();
				names.reloadNames(getRoot());
				mSubLength = names.getFileLength(getNameData().getKey());
			} catch (Throwable e) { 
				mSubLength = 0;
			}
		}
		return mSubLength;
	}
	
}
