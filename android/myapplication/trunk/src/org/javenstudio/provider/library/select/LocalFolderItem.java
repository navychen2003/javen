package org.javenstudio.provider.library.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.drawable.Drawable;
import android.os.Environment;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.storage.fs.FileSystems;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.IFileSystem;
import org.javenstudio.cocoka.storage.fs.LocalFile;
import org.javenstudio.cocoka.storage.fs.LocalFileSystem;
import org.javenstudio.cocoka.storage.fs.Path;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.SectionHelper;

public class LocalFolderItem extends SelectFolderItem 
		implements ISelectData {
	private static final Logger LOG = Logger.getLogger(LocalFolderItem.class);

	private final LocalFile mFolder;
	private final boolean mIsRoot;
	
	public LocalFolderItem(SelectOperation op, 
			LocalFolderItem parent, LocalFile folder) {
		this(op, parent, folder, false);
	}
	
	public LocalFolderItem(SelectOperation op, 
			LocalFolderItem parent, LocalFile folder, boolean isRoot) {
		super(op, parent);
		if (folder == null) throw new NullPointerException();
		mFolder = folder;
		mIsRoot = isRoot;
	}

	public LocalFile getFile() { return mFolder; }
	
	@Override
	public String getName() {
		String name = mFolder.getName();
		if (name != null && name.length() > 0) return name;
		return mFolder.getPath();
	}

	@Override
	public String getFolderInfo() {
		//String path = mFolder.getPath();
		//if (path == null || path.equals("/"))
		if (mIsRoot) return ResourceHelper.getResources().getString(R.string.root_folder_information_label);
		return SectionHelper.getFolderModifiedInfo(mFolder.lastModified());
	}

	@Override
	public Drawable getFolderIcon() {
		return AppResources.getInstance().getSectionNavIcon(getData()); 
	}

	@Override
	public ISelectData getData() {
		return this;
	}
	
	public ISelectData[] listFiles() {
		try {
			IFile[] files = mFolder.getFileSystem().listFiles(mFolder);
			if (files != null) {
				ArrayList<ISelectData> list = new ArrayList<ISelectData>();
				sortFiles(files);
				
				for (IFile file : files) {
					if (file == null || !(file instanceof LocalFile)) 
						continue;
					
					if (LOG.isDebugEnabled())
						LOG.debug("listFiles: file=" + file);
					
					if (file.isFile()) {
						list.add(new LocalFileItem(getOperation(), 
								this, (LocalFile)file));
						
					} else if (file.isDirectory()) {
						list.add(new LocalFolderItem(getOperation(), 
								this, (LocalFile)file));
					}
				}
				
				return list.toArray(new ISelectData[list.size()]);
			}
		} catch (IOException e) {
			if (LOG.isDebugEnabled())
				LOG.debug("listFiles: error: " + e, e);
		}
		
		return null;
	}
	
	public static ISelectData[] listRoots(SelectOperation op, String[] paths, 
			boolean includeExternalDir) {
		if (op == null) return null;
		
		try {
			IFileSystem fs = FileSystems.get(LocalFileSystem.LOCAL_SCHEME, null);
			if (fs != null) {
				IFile[] folders = fs.listRoots();
				if (folders != null) {
					ArrayList<ISelectData> list = new ArrayList<ISelectData>();
					sortFiles(folders);
					
					for (IFile folder : folders) {
						if (folder == null || !(folder instanceof LocalFile) || !folder.isDirectory()) 
							continue;
						
						if (LOG.isDebugEnabled())
							LOG.debug("listRoots: root=" + folder);
						
						list.add(new LocalFolderItem(op, null, (LocalFile)folder, true));
					}
					
					try {
						IFile externDir = fs.getFile(new Path(
								Environment.getExternalStorageDirectory().getPath()));
						
						if (externDir != null && (externDir instanceof LocalFile) && 
							externDir.exists() && externDir.isDirectory()) {
							list.add(new LocalFolderItem(op, null, (LocalFile)externDir));
						}
						
						if (paths != null) {
							for (String path : paths) {
								if (path == null || path.length() == 0) continue;
								IFile dir = fs.getFile(new Path(path));
								if (dir != null && dir instanceof LocalFile && 
									dir.exists() && dir.isDirectory()) {
									list.add(new LocalFolderItem(op, null, (LocalFile)dir));
								}
							}
						}
					} catch (IOException ee) {
						if (LOG.isWarnEnabled())
							LOG.warn("listRoots: getExternalStorageDirectory error: " + ee, ee);
					}
					
					return list.toArray(new ISelectData[list.size()]);
				}
			}
		} catch (IOException e) {
			if (LOG.isDebugEnabled())
				LOG.debug("listRoots: error: " + e, e);
		}
		
		return null;
	}
	
	static void sortFiles(IFile[] files) {
		if (files == null || files.length < 2)
			return;
		
		Arrays.sort(files, new Comparator<IFile>() {
				@Override
				public int compare(IFile lhs, IFile rhs) {
					boolean isfolder1 = lhs.isDirectory();
					boolean isfolder2 = rhs.isDirectory();
					if (isfolder1 != isfolder2) {
						if (isfolder1) return -1;
						else return 1;
					}
					String name1 = lhs.getName();
					String name2 = rhs.getName();
					if (name1 == null || name2 == null) {
						if (name1 == null) return -1;
						return 1;
					}
					return name1.compareToIgnoreCase(name2);
				}
			});
	}
	
}
