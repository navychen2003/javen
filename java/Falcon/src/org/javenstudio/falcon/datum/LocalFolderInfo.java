package org.javenstudio.falcon.datum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.MimeType;
import org.javenstudio.common.util.MimeTypes;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class LocalFolderInfo implements IFolderInfo {

	private static final Map<String, LocalFolderInfo> sFolders = 
			new HashMap<String, LocalFolderInfo>();
	
	public static void addFolder(LocalFolderInfo info) { 
		if (info == null) return;
		
		synchronized (sFolders) { 
			sFolders.put(info.getContentId(), info);
		}
	}
	
	public static LocalFolderInfo getFolder(String key) { 
		if (key == null) return null;
		
		synchronized (sFolders) { 
			return sFolders.get(key);
		}
	}
			
	private final FileSystem mFs;
	private final Path mPath;
	private final LocalFolderInfo mParent;
	private final String mContentId;
	private final String mContentType;
	private final boolean mUserHome;
	private final boolean mIsRoot;
	
	private LocalFolderInfo[] mFolders = null;
	
	private LocalFolderInfo(FileSystem fs, Path path, LocalFolderInfo parent, 
			boolean isRoot) throws ErrorException { 
		this(fs, path, parent, false, isRoot);
	}
	
	private LocalFolderInfo(FileSystem fs, Path path, LocalFolderInfo parent, 
			boolean userHome, boolean isRoot) throws ErrorException { 
		if (fs == null || path == null) throw new NullPointerException();
		mFs = fs;
		mPath = path;
		mParent = parent;
		mContentId = newContentId(parent, path);
		mUserHome = userHome;
		mIsRoot = isRoot;
		
		String contentType = MimeTypes.getContentTypeByFilename(
				path.toString());
		
		if (contentType == null) 
			contentType = MimeType.TYPE_APPLICATION.getType();
		
		mContentType = contentType;
		
		addFolder(this);
	}
	
	private static String newContentId(LocalFolderInfo parent, 
			Path path) throws ErrorException { 
		if (path == null) return null;
		
		LocalFolderInfo root = parent != null ? parent.getRootInfo() : null;
		String rootId = root != null ? root.getContentId() : "ROOT";
		
		return SectionHelper.newFolderKey(rootId + " " + path.toString());
	}
	
	public FileSystem getFs() { return mFs; }
	public LocalFolderInfo getParentInfo() { return mParent; }
	public Path getPath() { return mPath; }
	public String getName() { return getPath().getName(); }
	
	public String getContentId() { return mContentId; }
	public String getContentPath() { return mPath.toString(); }
	public String getContentType() { return mContentType; }
	
	public boolean isHomeFolder() { return mUserHome; }
	public boolean isRootFolder() { return mIsRoot; }
	
	@Override
	public boolean equals(Object o) { 
		if (this == o) return true;
		if (o == null || !(o instanceof LocalFolderInfo)) 
			return false;
		
		LocalFolderInfo other = (LocalFolderInfo)o;
		return this.getContentId().equals(other.getContentId());
	}
	
	@Override
	public LocalFolderInfo getRootInfo() { 
		LocalFolderInfo parent = getParentInfo();
		if (parent == null) 
			return this;
		
		return parent.getRootInfo();
	}
	
	@Override
	public synchronized LocalFolderInfo[] listFolderInfos(
			boolean refresh, Filter filter) throws ErrorException { 
		if (mFolders == null || refresh || filter != null) { 
			try { 
				ArrayList<LocalFolderInfo> folders = new ArrayList<LocalFolderInfo>();
				FileStatus[] statuss = getFs().listStatus(getPath());
				
				for (int i=0; statuss != null && i < statuss.length; i++) { 
					FileStatus status = statuss[i];
					Path path = status != null ? status.getPath() : null;
					if (path != null && status.isDir() && !status.isHidden()) { 
						LocalFolderInfo info = new LocalFolderInfo(getFs(), 
								path, this, false, false);
						if (filter == null || filter.accept(info))
							folders.add(info);
					}
				}
				
				if (folders.size() > 0)
					mFolders = folders.toArray(new LocalFolderInfo[folders.size()]);
				else
					mFolders = new LocalFolderInfo[0];
			} catch (IOException e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
		
		return mFolders;
	}
	
	@Override
	public IFileInfo[] listFileInfos(boolean refresh, Filter filter) 
			throws ErrorException { 
		return null;
	}
	
	@Override
	public boolean canMove() { 
		return false;
	}
	
	@Override
	public boolean canDelete() { 
		return false;
	}
	
	@Override
	public boolean canWrite() { 
		return false;
	}
	
	private static LocalFolderInfo[] sRoots = null;
	
	public static LocalFolderInfo[] listLocalRoots(Configuration conf) 
			throws ErrorException { 
		try { 
			FileSystem localFs = FileSystem.getLocal(conf);
			return listRoots(localFs);
		}  catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static LocalFolderInfo[] listRoots(FileSystem fs) 
			throws ErrorException { 
		synchronized (sFolders) { 
			if (sRoots != null) return sRoots;
			
			try { 
				ArrayList<LocalFolderInfo> roots = new ArrayList<LocalFolderInfo>();
				
				String userHome = System.getProperty("user.home");
				if (userHome != null && userHome.length() > 0) {
					Path homePath = new Path(userHome);
					roots.add(new LocalFolderInfo(fs, homePath, null, true, false));
				}
				
				File[] files = File.listRoots();
				for (int i=0; files != null && i < files.length; i++) { 
					File file = files[i];
					if (file != null && file.exists() && 
						file.canRead() && file.isDirectory()) {
						roots.add(new LocalFolderInfo(fs, 
								new Path(file.getAbsolutePath()), 
								null, true));
					}
				}
				
				sRoots = roots.toArray(new LocalFolderInfo[roots.size()]);
			} catch (Throwable e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
			
			return sRoots;
		}
	}
	
}
