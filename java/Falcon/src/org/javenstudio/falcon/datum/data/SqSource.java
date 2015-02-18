package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataJob;
import org.javenstudio.falcon.datum.DataSource;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.util.StringUtils;

public final class SqSource extends FileSource implements DataSource {
	private static final Logger LOG = Logger.getLogger(SqSource.class);

	public static final long MAPFILE_MAXLEN = 2 * 1024 * 1024;
	public static final long METAFILE_MAXLEN = 8 * 1024 * 1024;
	
	public static enum STEP { SCAN, STORE, ABORT, DONE }
	
	private final IMember mUser;
	private final SqRoot mRoot;
	private final FileStorer mStorer;
	private final FileSystem mFs;
	private final FileStatus mStatus;
	private final String mRootKey;
	private final String mRootPathKey;
	
	private STEP mProcessStep = STEP.DONE;
	private int mTotalFileCount = 0;
	private long mTotalFileLength = 0;
	private int mStoreDirCount = 0;
	private int mStoreFileCount = 0;
	private long mStoreFileLength = 0;
	
	public SqSource(IMember user, SqRoot root, FileSystem fs, 
			FileStatus status, String rootKey, String pathKey) 
			throws ErrorException { 
		if (user == null || root == null || fs == null || status == null || 
			rootKey == null || pathKey == null) 
			throw new NullPointerException();
		
		mUser = user;
		mRoot = root;
		mFs = fs;
		mStatus = status;
		mRootKey = rootKey;
		mRootPathKey = pathKey;
		
		mStorer = new FileStorer() {
				@Override
				public FileSource getSource() {
					return SqSource.this;
				}
			};
	}
	
	public IMember getUser() { return mUser; }
	public SqRoot getRoot() { return mRoot; }
	public SqLibrary getLibrary() { return getRoot().getLibrary(); }
	
	public FileSystem getFs() { return mFs; }
	public FileStatus getStatus() { return mStatus; }
	public FileStorer getStorer() { return mStorer; }
	
	public String getRootKey() { return mRootKey; }
	public String getRootPathKey() { return mRootPathKey; }
	
	public FileFilter getFilter() { 
		return getLibrary().getAcceptor().getFilter();
	}
	
	@Override
	public boolean isStoreAllToMapFile(FileSource.Item file) { 
		return file.getAttrs().getLength() < MAPFILE_MAXLEN;
	}
	
	@Override
	public boolean isStoreAllToMetaFile(FileSource.Item file) {
		return file.getAttrs().getLength() < METAFILE_MAXLEN;
	}
	
	@Override
	public InputStream getInputStream(Item item) throws IOException { 
		if (item == null) return null;
		if (item instanceof DirItem) return null;
		if (item instanceof FsFileItem) { 
			FsFileItem fileItem = (FsFileItem)item;
			return fileItem.open(getFs());
		}
		return null;
	}
	
	@Override
	public void close() { 
		try { 
			getStorer().close();
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("close: error: " + e, e);
		}
	}
	
	private FsFileItem createItem(FsDirItem parent, FileStatus status) 
			throws IOException, ErrorException { 
		if (status == null || (!status.isDir() && status.getLen() == 0)) 
			return null;
		
		Path base = getStatus().getPath();
		Path path = status.getPath();
		
		String filename = path.getName();
		if (filename == null || filename.length() == 0 || 
			filename.startsWith("."))
			return null;
		
		String baseName = base.toString();
		String pathName = path.toString();
		
		String filepath = "/";
		String key = SectionHelper.newFileKey(pathName 
				+ "@" + System.currentTimeMillis(), status.isDir());
		
		if (!baseName.equals(pathName)) { 
			if (!pathName.startsWith(baseName)) {
				throw new IOException("Path: " + pathName 
						+ " not in base: " + baseName);
			}
			
			filepath = pathName.substring(baseName.length());
			//if (filepath.startsWith("/"))
			//	filepath = filepath.substring(1);
			
			if (status.isDir() && !filepath.endsWith("/"))
				filepath += "/";
		}
		
		return status.isDir() ? 
			new FsDirItem(parent, status, filepath, key) : 
			new FsFileItem(parent, status, filepath, key);
	}
	
	private FsFileItem[] listItems(DataJob job, JobContext jc, 
			FsDirItem dirItem) throws IOException, ErrorException { 
		FileStatus[] files = getFs().listStatus(
				dirItem.getStatus().getPath());
		
		if (files == null || files.length == 0)
			return null;
		
		ArrayList<FsFileItem> list = new ArrayList<FsFileItem>();
		for (int i=0; i < files.length; i++) { 
			if (jc.isCancelled() || job.isCanceled()) return null;
			FileStatus file = files[i];
			if (file == null) continue;
			
			if (file.isDir() || getFilter().accept(file)) {
				FsFileItem item = createItem(dirItem, file);
				if (item != null) list.add(item);
			}
		}
		
		FsFileItem[] items = list.toArray(new FsFileItem[list.size()]);
		Arrays.sort(items, new Comparator<FsFileItem>() {
				@Override
				public int compare(FsFileItem o1, FsFileItem o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		
		return items;
	}
	
	private void scanPath(DataJob job, JobContext jc, 
			FsDirItem dirItem, Set<FsFileItem> files) 
			throws IOException, ErrorException { 
		if (jc == null || job == null || dirItem == null) return;
		if (jc.isCancelled() || job.isCanceled()) return;
		
		FsFileItem[] items = listItems(job, jc, dirItem);
		if (items == null || items.length == 0) {
			FsDirItem parent = dirItem.getParent();
			if (parent != null) parent.removeItem(dirItem);
			return;
		}
		
		dirItem.setItems(items);
		
		for (int i=0; items != null && i < items.length; i++) { 
			if (jc.isCancelled() || job.isCanceled()) break;
			FsFileItem item = items[i];
			if (item != null) {
				files.add(item);
				
				if (item instanceof FsDirItem) {
					scanPath(job, jc, (FsDirItem)item, files);
				} else {
					mTotalFileCount += 1;
					mTotalFileLength += item.getStatus().getLen();
				}
			}
		}
	}
	
	@Override
	public synchronized void process(DataJob job, JobContext jc, 
			Collector collector) throws IOException, ErrorException { 
		SqRoot root = getRoot();
		root.getLock().lock(ILockable.Type.WRITE, null);
		try { 
			root.setModifiedTime(System.currentTimeMillis());
			scan0(job, jc, collector);
			
		} catch (IOException ie) { 
			throw ie;
		} catch (ErrorException ee) { 
			throw ee;
		} finally { 
			if (jc.isCancelled() || job.isCanceled())
				mProcessStep = STEP.ABORT;
			else
				mProcessStep = STEP.DONE;
			
			try {
				root.setTotalFolderCount(mStoreDirCount);
				root.setTotalFileCount(mStoreFileCount);
				root.setTotalFileLength(mStoreFileLength);
				root.reset();
				
			} finally {
				root.getLock().unlock(ILockable.Type.WRITE);
				root.getManager().saveLibraryList();
			}
		}
	}
	
	private synchronized void scan0(DataJob job, JobContext jc, 
			Collector collector) throws IOException, ErrorException { 
		if (jc == null || job == null) return;
		if (jc.isCancelled() || job.isCanceled()) return;
		
		Set<FsFileItem> files = new TreeSet<FsFileItem>(
			new Comparator<FsFileItem>() {
				@Override
				public int compare(FsFileItem o1, FsFileItem o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});
		
		FileStatus status = getStatus();
		
		mProcessStep = STEP.SCAN;
		mTotalFileCount = 0;
		mTotalFileLength = 0;
		mStoreDirCount = 0;
		mStoreFileCount = 0;
		mStoreFileLength = 0;
		
		if (status != null && status.isDir()) {
			FsDirItem item = new FsRootItem(status, getRootPathKey());
			if (item != null) {
				if (jc.isCancelled() || job.isCanceled()) return;
				
				files.add(item);
				scanPath(job, jc, item, files);
			}
		}
		
		mProcessStep = STEP.STORE;
		
		// store file first for load metadata
		for (FsFileItem item : files) { 
			if (jc.isCancelled() || job.isCanceled()) return;
			if (!(item instanceof FsDirItem)) { 
				FileData data = getStorer().storeFile(item);
				if (data != null) {
					item.getAttrs().setOwner(mUser != null ? mUser.getUserName() : "");
					item.getAttrs().setChecksum(data.getAttrs().getChecksum());
					item.getAttrs().setFileIndex(data.getAttrs().getFileIndex());
					
					if (collector != null) {
						String contentId = getRoot().getManager().getUserKey() 
								+ getRoot().getLibrary().getContentKey() + getRoot().getContentKey() 
								+ item.getKey();
						collector.addContentId(contentId);
					}
					
					mStoreFileCount += 1;
					mStoreFileLength += item.getStatus().getLen();
				}
			}
		}
		
		for (FsFileItem item : files) { 
			if (jc.isCancelled() || job.isCanceled()) return;
			if (item instanceof FsDirItem) { 
				FsDirItem dirItem = (FsDirItem)item;
				if (!dirItem.isEmpty()) {
					getStorer().storeName(item);
					mStoreDirCount += 1;
				}
			} else { 
				getStorer().storeName(item);
			}
		}
	}
	
	public String getMessage(String lang) { 
		STEP step = mProcessStep;
		int totalFileCount = mTotalFileCount;
		long totalFileLength = mTotalFileLength;
		//int storeDirCount = mStoreDirCount;
		int storeFileCount = mStoreFileCount;
		long storeFileLength = mStoreFileLength;
		
		switch (step) { 
		case SCAN: {
			String msg = "Scanning library \"%1$s\", found %2$s(%3$s) files.";
			//String path = SectionHelper.normalizePath(getStatus().getPath().toString());
			String name = getLibrary().getName() + "/" + getStatus().getPath().getName();
			String count = ""+totalFileCount;
			String length = StringUtils.byteDesc(totalFileLength);
			return String.format(Strings.get(lang, msg), name, count, length);
		}
		case STORE: {
			String msg = "Storing library \"%1$s\", stored %2$s(%3$s/%4$s) files.";
			String name = getLibrary().getName() + "/" + getStatus().getPath().getName();
			String count = ""+storeFileCount;
			String length = StringUtils.byteDesc(storeFileLength);
			float rate = totalFileLength > 0 ? ((float)storeFileLength/(float)totalFileLength) : 0;
			String percent = "" + (int)(rate * 100) + "%";
			return String.format(Strings.get(lang, msg), name, count, length, percent);
		}
		case ABORT: { 
			String msg = "Aborted library \"%1$s\", stored %2$s(%3$s) files.";
			String name = getLibrary().getName() + "/" + getStatus().getPath().getName();
			String count = ""+storeFileCount;
			String length = StringUtils.byteDesc(storeFileLength);
			return String.format(Strings.get(lang, msg), name, count, length);
		}
		case DONE: {
			String msg = "Finished library \"%1$s\", stored %2$s(%3$s) files.";
			String name = getLibrary().getName() + "/" + getStatus().getPath().getName();
			String count = ""+storeFileCount;
			String length = StringUtils.byteDesc(storeFileLength);
			return String.format(Strings.get(lang, msg), name, count, length);
		}}
		
		return null;
	}
	
	public String getMessage() { 
		String lang = null;
		try { 
			lang = getRoot().getManager().getUser().getPreference().getLanguage();
		} catch (Throwable e) { 
			lang = null;
		}
		return getMessage(lang);
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{library=" + getLibrary().getContentKey() 
				+ ",path=" + mStatus.getPath() + "}";
	}
	
}
