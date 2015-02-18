package org.javenstudio.falcon.datum.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.LocalFileStatus;
import org.javenstudio.raptor.fs.Path;

public abstract class FileSource {
	//private static final Logger LOG = Logger.getLogger(ScanPath.class);

	public static interface Item { 
		public String getKey();
		public File getFile();
		public FileAttrs getAttrs();
		public boolean isFolder();
		
		public boolean loadMetadata(FileMetaCollector collector)
			throws IOException, ErrorException;
	}
	
	public static interface DirItem extends Item { 
		public int getItemCount();
		public Item getItemAt(int index);
	}
	
	public static interface BaseItem extends Item { 
		public String getName();
		public String getExtension();
		public String getPath();
		public String getParentKey();
		
		public long getLength();
		public long getModificationTime();
		public boolean isFolder();
	}
	
	public static void initAttrs(BaseItem item, FileAttrs attrs) { 
		if (item == null || attrs == null) return;
		
		attrs.setName(item.getName());
		attrs.setExtension(item.getExtension());
		attrs.setParentKey(item.getParentKey());
		attrs.setPath(item.getPath());
		attrs.setModifiedTime(item.getModificationTime());
		attrs.setLength(item.getLength());
	}
	
	public static class StreamItem implements BaseItem { 
		private final FileStream mStream;
		private final FileAttrs mAttrs;
		private final String mKey;
		private final String mName;
		private final String mExtension;
		private final String mParentKey;
		private final long mLastModified;
		
		public StreamItem(FileStream stream, String filename, 
				String key, String parentKey, long lastModified) { 
			if (stream == null || filename == null || key == null) 
				throw new NullPointerException();
			mStream = stream;
			mAttrs = new FileAttrs();
			mKey = key; //SectionHelper.newFileKey(name, false);
			mParentKey = parentKey != null ? parentKey : "";
			mLastModified = lastModified;
			
			String name = filename;
			String extname = "";
			if (filename != null) { 
				int pos = filename.lastIndexOf('.'); 
				if (pos > 0) { 
					extname = filename.substring(pos+1); 
					name = filename.substring(0, pos);
				}
			}
			
			if (extname == null) extname = "";
			extname = extname.toLowerCase();
			
			mName = name;
			mExtension = extname;
			
			initAttrs(this, mAttrs);
		}
		
		public FileStream getStream() { return mStream; }
		public FileAttrs getAttrs() { return mAttrs; }
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getExtension() { return mExtension; }
		public String getPath() { return "/"; }
		public String getParentKey() { return mParentKey; }
		
		public long getLength() { return getStream().getSize(); }
		public long getModificationTime() { return mLastModified; }
		public boolean isFolder() { return false; }
		
		@Override
		public boolean loadMetadata(FileMetaCollector collector)
				throws IOException, ErrorException { 
			return getStream().loadMetadata(collector);
		}
		
		@Override
		public File getFile() { 
			return getStream().getFile();
		}
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{key=" + getKey() 
					+ ",attrs=" + getAttrs() + "}";
		}
	}
	
	public static class FsFileItem implements BaseItem {
		private final FsDirItem mParent;
		private final FileStatus mStatus;
		private final FileAttrs mAttrs;
		private final String mName;
		private final String mExtension;
		private final String mKey;
		private final String mPath;
		
		public FsFileItem(FsDirItem parent, FileStatus status, 
				String filepath, String key) throws ErrorException { 
			if (status == null || filepath == null || key == null) 
				throw new NullPointerException();
			mParent = parent;
			mStatus = status;
			mKey = key;
			mAttrs = new FileAttrs();
			
			String name = "";
			String extname = "";
			String path = "";
			
			if (filepath != null && filepath.length() > 0 && 
				!filepath.equals("/") && !filepath.equals("\\")) {
				Path p = new Path(filepath);
				Path dir = p.getParent();
				String filename = p.getName();
				
				path = dir != null ? dir.toString() : "/";
				name = filename;
				extname = "";
				
				if (filename != null && !isFolder()) { 
					int pos = filename.lastIndexOf('.'); 
					if (pos > 0) { 
						extname = filename.substring(pos+1); 
						name = filename.substring(0, pos);
					}
				}
			}
			
			if (extname == null) extname = "";
			extname = extname.toLowerCase();
			
			mName = name;
			mExtension = extname;
			mPath = path;
			
			initAttrs(this, mAttrs);
		}
		
		public FsDirItem getParent() { return mParent; }
		public FileStatus getStatus() { return mStatus; }
		public FileAttrs getAttrs() { return mAttrs; }
		
		public String getName() { return mName; }
		public String getKey() { return mKey; }
		public String getExtension() { return mExtension; }
		public String getPath() { return mPath; }
		
		public boolean isFolder() { return false; }
		
		public String getParentKey() { 
			FsDirItem parent = getParent();
			if (parent != null)
				return parent.getKey().toString();
			return "";
		}
		
		public long getLength() { 
			return getStatus().getLen();
		}
		
		public long getModificationTime() { 
			return getStatus().getModificationTime();
		}
		
		public InputStream open(FileSystem fs) throws IOException { 
			return fs.open(getStatus().getPath());
		}
		
		@Override
		public boolean loadMetadata(FileMetaCollector collector)
				throws IOException, ErrorException { 
			return false;
		}
		
		@Override
		public File getFile() { 
			if (mStatus instanceof LocalFileStatus) { 
				LocalFileStatus lfs = (LocalFileStatus)mStatus;
				return lfs.getFile();
			}
			return null;
		}
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{key=" + getKey() 
					+ ",attrs=" + getAttrs() + "}";
		}
	}
	
	public static class FsRootItem extends FsDirItem { 
		public FsRootItem(FileStatus status, String key) throws ErrorException {
			super(null, status, "/", key);
		}
	}
	
	public static class FsDirItem extends FsFileItem implements DirItem {
		private FsFileItem[] mItems = null;
		
		public FsDirItem(FsDirItem parent, FileStatus status, 
				String filepath, String key) throws ErrorException {
			super(parent, status, filepath, key);
		}

		@Override
		public boolean isFolder() { return true; }
		
		@Override
		public InputStream open(FileSystem fs) throws IOException { 
			throw new IOException("Directory cannot open");
		}
		
		@Override
		public synchronized int getItemCount() { 
			return mItems != null ? mItems.length : 0;
		}
		
		@Override
		public synchronized FsFileItem getItemAt(int index) { 
			return mItems != null && index >= 0 && index < mItems.length ? 
					mItems[index] : null;
		}
		
		public synchronized FsFileItem[] getItems() { 
			return mItems;
		}
		
		public synchronized void setItems(FsFileItem[] items) { 
			mItems = items;
		}
		
		public synchronized boolean isEmpty() { 
			FsFileItem[] items = mItems;
			return items == null || items.length == 0;
		}
		
		public synchronized void removeItem(FsFileItem removeItem) { 
			if (removeItem == null || removeItem.getParent() != this)
				return;
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("removeItem: " + removeItem);
			
			FsFileItem[] items = mItems;
			if (items != null && items.length > 0) {
				ArrayList<FsFileItem> list = new ArrayList<FsFileItem>(items.length);
				
				for (int i=0; items != null && i < items.length; i++) { 
					FsFileItem item = items[i];
					if (item != null && item != removeItem)
						list.add(item);
				}
				
				items = list.toArray(new FsFileItem[list.size()]);
				mItems = items;
			}
			
			if (items == null || items.length == 0) { 
				FsDirItem parent = getParent();
				if (parent != null) parent.removeItem(this);
			}
		}
	}
	
	public static class SqFileItem implements Item { 
		private final SqRootFile mFile;
		private final NameData mName;
		private final FileAttrs mAttrs;
		
		public SqFileItem(SqRootFile file, NameData data) { 
			if (file == null || data == null) throw new NullPointerException();
			mFile = file;
			mName = data;
			mAttrs = new FileAttrs();
			mAttrs.copyFrom(mName.getAttrs());
		}
		
		public SqRootFile getRootFile() { return mFile; }
		public NameData getNameData() { return mName; }
		public FileAttrs getAttrs() { return mAttrs; }
		public File getFile() { return null; }
		public String getKey() { return getRootFile().getPathKey(); }
		public boolean isFolder() { return getRootFile().isFolder(); }
		
		@Override
		public boolean loadMetadata(FileMetaCollector collector)
				throws IOException, ErrorException { 
			return false;
		}
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{key=" + getKey() 
					+ ",attrs=" + getAttrs() + "}";
		}
	}
	
	public abstract SqRoot getRoot();
	public abstract InputStream getInputStream(Item item) throws IOException;
	
	public abstract boolean isStoreAllToMapFile(FileSource.Item file);
	public abstract boolean isStoreAllToMetaFile(FileSource.Item file);
	
}
