package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataConf;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.StoreInfo;
import org.javenstudio.falcon.datum.data.archive.SqArchiveRoot;
import org.javenstudio.falcon.datum.data.recycle.SqRecycleRoot;
import org.javenstudio.falcon.datum.data.share.SqShareRoot;
import org.javenstudio.falcon.datum.data.upload.SqUploadRoot;
import org.javenstudio.falcon.datum.index.IndexSource;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class SqHelper {
	private static final Logger LOG = Logger.getLogger(SqHelper.class);
	
	public static SqLibrary registerDefault(DataManager manager, 
			String name) throws ErrorException { 
		if (manager == null || name == null) throw new NullPointerException();
		if (name.length() == 0) throw new IllegalArgumentException("Library name cannot be empty");
		String storeUri = "file:///";
		
		StoreInfo[] storeInfos = manager.getCore().getStoreInfos();
		if (storeInfos != null && storeInfos.length > 0) { 
			for (int i=0; i < storeInfos.length; i++) {
				StoreInfo info = storeInfos[i];
				if (info != null) { 
					storeUri = info.getStoreUri();
					break;
				}
			}
		}
		
		SqLibrary library = registerFile(null, manager, 
				name, //Strings.get(manager.getUser().getPreference().getLanguage(), "My Files"), 
				storeUri, null, 0);
		
		if (library != null) {
			library.setCanDelete(false);
			library.setDefault(true);
			library.getManager().saveLibraryList();
		}
		
		return library;
	}
	
	public static SqLibrary registerFile(IMember user, DataManager manager, 
			String name, String storeUri, String[] paths, int maxEntries) 
			throws ErrorException { 
		return registerLibrary(user, manager, name, storeUri, paths, 
				"application/x-library-file", maxEntries, new SqAcceptor());
	}
	
	public static SqLibrary registerPhoto(IMember user, DataManager manager, 
			String name, String storeUri, String[] paths, int maxEntries) 
			throws ErrorException { 
		return registerLibrary(user, manager, name, storeUri, paths, 
				"application/x-library-image", maxEntries, new SqAcceptor.PhotoAcceptor());
	}
	
	public static SqLibrary registerMusic(IMember user, DataManager manager, 
			String name, String storeUri, String[] paths, int maxEntries) 
			throws ErrorException { 
		return registerLibrary(user, manager, name, storeUri, paths, 
				"application/x-library-audio", maxEntries, new SqAcceptor.MusicAcceptor());
	}
	
	public static SqLibrary registerVideo(IMember user, DataManager manager, 
			String name, String storeUri, String[] paths, int maxEntries) 
			throws ErrorException { 
		return registerLibrary(user, manager, name, storeUri, paths, 
				"application/x-library-video", maxEntries, new SqAcceptor.VideoAcceptor());
	}
	
	public static boolean addArchiveRoot(SqLibrary library, 
			String name) throws ErrorException { 
		if (library == null) throw new NullPointerException();
		if (name == null || name.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Root name is empty");
		}
		
		String rootName = SectionHelper.newName(name);
		SqArchiveRoot root = SqArchiveRoot.create(library, rootName);
		boolean result = false;
		
		synchronized (library) {
			for (int i=0; i < library.getSectionCount(); i++) { 
				SqRoot section = library.getSectionAt(i);
				if (section != null && section.getName().equals(root.getName())) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Root name: " + root.getName() + " already exists");
				}
			}
			
			library.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try {
				result = library.addRoot(root);
				if (result) library.getManager().saveLibraryList();
			} finally { 
				library.getLock().unlock(ILockable.Type.WRITE);
			}
		}
		
		return result;
	}
	
	static SqLibrary registerLibrary(IMember user, DataManager manager, 
			String name, String storeUri, String[] paths, String contentType, 
			int maxEntries, SqAcceptor filter) throws ErrorException { 
		if (manager == null || name == null)
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("registerLibrary: name=" + name + " storeUri=" + storeUri 
					+ " type=" + contentType);
		}
		
		try { 
			final FileSystem fs; // = manager.getLocalFs();
			//final FileSystem localFs = manager.getLocalFs();
			final String hostname = manager.getCore().getFriendlyName();
			
			//storeUri = manager.getCore().getStoreUri(storeUri);
			fs = manager.getCore().getStoreFs(storeUri);
			
			SqLibrary library = SqLibrary.create(manager, fs, 
					filter, contentType, null, name, hostname, 
					maxEntries);
			
			SqRecycleRoot recycleRoot = SqRecycleRoot.create(library);
			SqShareRoot shareRoot = SqShareRoot.create(library);
			SqUploadRoot uploadRoot = SqUploadRoot.create(library);
			
			int count = 0;
			library.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try {
				library.addRoot(recycleRoot);
				library.addRoot(shareRoot);
				library.addRoot(uploadRoot);
				
				manager.addLibrary(library);
				count = initRoot(user, library, paths);
			} finally {
				library.getLock().unlock(ILockable.Type.WRITE);
			}
			
			if (count > 0) {
				IndexSource source = new IndexSource(library.getManager());
				source.addLibrary(library);
				library.getManager().getJob().startIndex(source);
			}
			
			return library;
		} catch (ErrorException e) {
			throw e;
		} catch (Throwable e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static SqLibrary addLibrary(DataManager manager, 
			DataConf.LibraryInfo info) throws ErrorException { 
		if (info == null) return null;
		
		if (info.className == null || !info.className.equals(SqLibrary.class.getName()))
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("addLibrary: name=" + info.name);
		
		final FileSystem fs; // = manager.getLocalFs();
		final String contentKey = info.key;
		final SqLibrary library;
		
		String storeUri = info.storeUri; //manager.getCore().getStoreUri(info.storeUri);
		fs = manager.getCore().getStoreFs(storeUri);
		
		if (info.contentType != null && info.contentType.startsWith("image/")) {
			library = SqLibrary.create(manager, fs, new SqAcceptor.PhotoAcceptor(), 
					info.contentType, contentKey, info.name, info.hostname, 
					info.maxEntries);
			
		} else { 
			library = SqLibrary.create(manager, fs, new SqAcceptor(), 
					info.contentType, contentKey, info.name, info.hostname, 
					info.maxEntries);
		}
		
		library.setDefault(info.isDefault);
		
		if (info.createdTime > 0)
			library.setCreatedTime(info.createdTime);
		
		if (info.modifiedTime > 0)
			library.setModifiedTime(info.modifiedTime);
		
		if (info.indexedTime > 0)
			library.setIndexedTime(info.indexedTime);
		
		for (int i=0; info.sectionList != null && i < info.sectionList.length; i++) { 
			DataConf.SectionInfo sectionInfo = info.sectionList[i];
			if (sectionInfo == null) continue;
			if (sectionInfo.id == null || sectionInfo.key == null) continue;
			if (sectionInfo.name == null) continue;
			
			String className = sectionInfo.className;
			if (className == null) continue;
			
			String[] keys = SectionHelper.splitKeys(sectionInfo.id);
			
			String rootName = sectionInfo.name;
			String rootKey = sectionInfo.key;
			String rootPathKey = keys != null && keys.length == 4 ? keys[3] : null;
			
			if (rootPathKey == null) 
				continue;
			
			if (className.equals(SqArchiveRoot.class.getName())) {
				SqArchiveRoot root = new SqArchiveRoot(library, rootName, 
						rootKey, rootPathKey);
				
				initRoot(root, sectionInfo);
				
				library.addRoot(root);
				
			} else if (className.equals(SqUploadRoot.class.getName())) {
				SqUploadRoot root = new SqUploadRoot(library, rootName, 
						rootKey, rootPathKey);
				
				initRoot(root, sectionInfo);
				
				library.addRoot(root);
				
			} else if (className.equals(SqRecycleRoot.class.getName())) {
				SqRecycleRoot root = new SqRecycleRoot(library, rootName, 
						rootKey, rootPathKey);
				
				initRoot(root, sectionInfo);
				
				library.addRoot(root);
				
			} else if (className.equals(SqShareRoot.class.getName())) {
				SqShareRoot root = new SqShareRoot(library, rootName, 
						rootKey, rootPathKey);
				
				initRoot(root, sectionInfo);
				
				library.addRoot(root);
			}
		}
		
		manager.addLibrary(library, false);
		
		return library;
	}
	
	static void initLibrary(SqLibrary library, DataConf.LibraryInfo info) { 
		library.setCreatedTime(info.createdTime);
		library.setModifiedTime(info.modifiedTime);
		library.setIndexedTime(info.indexedTime);
		
		library.setCanRead(info.canRead);
		library.setCanWrite(info.canWrite);
		library.setCanMove(info.canMove);
		library.setCanDelete(info.canDelete);
		library.setCanCopy(info.canCopy);
		library.setDefault(info.isDefault);
	}
	
	static void initRoot(SqRoot root, DataConf.SectionInfo sectionInfo) { 
		root.setCreatedTime(sectionInfo.createdTime);
		root.setModifiedTime(sectionInfo.modifiedTime);
		root.setIndexedTime(sectionInfo.indexedTime);
		root.setTotalFolderCount(sectionInfo.totalFolderCount);
		root.setTotalFileCount(sectionInfo.totalFileCount);
		root.setTotalFileLength(sectionInfo.totalFileLength);
	}
	
	static String newRootName(SqLibrary library, String name) { 
		if (name == null || name.length() == 0)
			name = "Untitled";
		
		String title = name;
		int index = 1;
		
		for (int i=0; i < library.getSectionCount(); i++) { 
			SqRoot root = library.getSectionAt(i);
			if (root == null) continue;
			if (name.equals(root.getName())) { 
				index ++;
				name = title + " (" + index + ")";
			}
		}
		
		return name;
	}
	
	public static void modifyLibrary(IMember user, SqLibrary library, 
			String name, String type, String[] paths) throws ErrorException { 
		if (library == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("registerLibrary: library=" + library + " type=" + type 
					+ " newName=" + name);
		}
		
		int count =0;
		library.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			library.setName(name);
			library.setModifiedTime(System.currentTimeMillis());
			
			count = initRoot(user, library, paths);
			library.getManager().saveLibraryList();
		} finally {
			library.getLock().unlock(ILockable.Type.WRITE);
		}
		
		if (count > 0) {
			IndexSource source = new IndexSource(library.getManager());
			source.addLibrary(library);
			library.getManager().getJob().startIndex(source);
		}
	}
	
	private static int initRoot(IMember user, SqLibrary library, 
			String[] paths) throws ErrorException { 
		final FileSystem localFs = library.getManager().getCore().getStoreFs("file:///");
		
		ArrayList<SqSource> list = new ArrayList<SqSource>();
		for (int i=0; paths != null && i < paths.length; i++) { 
			String path = paths[i];
			if (path == null || path.length() == 0) 
				continue;
			
			try {
				FileStatus status = localFs.getFileStatus(new Path(path));
				String rootName = SqHelper.newRootName(library, status.getPath().getName());
				
				String rootKey = SectionHelper.newArchiveRootKey(
						status.getPath().toString() + "@" + System.currentTimeMillis());
				String rootPathKey = SectionHelper.newFileKey(
						rootName + "@" + System.currentTimeMillis(), true);
				
				SqArchiveRoot root = new SqArchiveRoot(library, 
						rootName, rootKey, rootPathKey);
				
				root.setModifiedTime(status.getModificationTime());
				
				SqSource source = new SqSource(user, root, 
						localFs, status, rootKey, rootPathKey);
				
				library.addRoot(root);
				list.add(source);
			} catch (IOException e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
		
		library.getManager().getJob().startJob(list.toArray(new SqSource[list.size()]));
		return list.size();
	}
	
}
