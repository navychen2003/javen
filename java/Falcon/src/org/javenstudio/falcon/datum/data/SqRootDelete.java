package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.falcon.datum.data.recycle.SqRecycleRoot;
import org.javenstudio.falcon.datum.data.share.SqShareRoot;
import org.javenstudio.falcon.datum.data.upload.SqUploadRoot;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.io.Text;

public final class SqRootDelete {
	private static final Logger LOG = Logger.getLogger(SqRootDelete.class);

	public static int deleteRoots(IUser user, SqRoot[] roots) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("deleteRoots: roots.size=" + (roots != null ? roots.length : 0));
		
		if (roots != null) { 
			for (SqRoot root : roots) { 
				SectionHelper.checkPermission(user, root, IData.Action.DELETE);
			}
		}
		
		Map<DataManager,Map<SqLibrary,Set<SqRoot>>> map = SqRootHelper.toRootMap(roots);
		if (map == null || map.size() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Delete roots is empty");
		}
		
		int count = 0;
		
		for (Map.Entry<DataManager,Map<SqLibrary,Set<SqRoot>>> entry : map.entrySet()) { 
			DataManager manager = entry.getKey();
			Map<SqLibrary,Set<SqRoot>> list = entry.getValue();
			if (manager == null || list == null) continue;
			
			int removeCount = 0;
			try { 
				for (Map.Entry<SqLibrary, Set<SqRoot>> entry2 : list.entrySet()) {
					SqLibrary library = entry2.getKey();
					Set<SqRoot> rts = entry2.getValue();
					if (library == null || rts == null) continue;
					
					for (SqRoot root : rts) { 
						if (root == null) continue;
						if (root instanceof SqRecycleRoot) { 
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"Recycle bin cannot be deleted");
						}
						if (SqUploadRoot.isDefaultUpload(root)) { 
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"Default upload root cannot be deleted");
						}
						if (SqShareRoot.isDefaultShare(root)) { 
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"Default share root cannot be deleted");
						}
						if (root.getTotalFileCount() > 0 || root.getTotalFolderCount() > 0 || 
							root.getSubCount() > 0) { 
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"Root: " + root.getName() + " is not empty");
						}
						
						//synchronized (root.getLock()) {
							root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
							try {
								root.reset();
								FileIO.closeAll(root);
								
								if (doDeleteRoot(root) && library.removeRoot(root)) {
									count ++;
									removeCount ++;
								}
							} finally { 
								root.getLock().unlock(ILockable.Type.WRITE);
							}
						//}
					}
					
					library.reset();
				}
			} finally {
				if (removeCount > 0)
					manager.saveLibraryList();
			}
		}
		
		return count;
	}
	
	private static boolean doDeleteRoot(SqRoot root) throws ErrorException { 
		if (root == null) return false;
		if (LOG.isDebugEnabled())
			LOG.debug("doDeleteRoot: root=" + root);
		
		try { 
			final SqRootNames names = new SqRootNames();
			names.reloadNames(root);
			
			if (names.size() == 0 || (names.size() == 1 && 
				names.contains(names.getRootKey()))) {
				final FileStorer storer = root.newStorer();
				boolean result = storer.removeFsName();
				storer.close();
				
				if (!result) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Root: " + root.getName() + " cannot clear name map");
				}
				
				return result;
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Root: " + root.getName() + " is not empty");
			}
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static int deleteFiles(IUser user, SqSection[] sections) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("deleteFiles: files.size=" + (sections != null ? sections.length : 0));
		
		if (sections != null) {
			for (SqSection section : sections) { 
				SectionHelper.checkPermission(user, section, IData.Action.DELETE);
			}
		}
		
		Map<SqRoot,Map<Text,SqSection>> map = SqRootHelper.toSectionMap(sections);
		if (map == null || map.size() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Delete items is empty");
		}
		
		int count = 0;
		
		for (Map.Entry<SqRoot, Map<Text,SqSection>> entry : map.entrySet()) { 
			SqRoot root = entry.getKey();
			Map<Text,SqSection> list = entry.getValue();
			if (root == null || list == null) continue;
		
			if (!(root instanceof SqRecycleRoot)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Delete items not in recycle bin");
			}
			
			//synchronized (root.getLock()) { 
				root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
				try { 
					count += doRemoveFiles(root, list);
				} finally { 
					root.getLock().unlock(ILockable.Type.WRITE);
				}
			//}
		}
		
		return count;
	}
	
	static int doRemoveFiles(final SqRoot root, Map<Text,SqSection> list) 
			throws ErrorException { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("doRemoveFiles: root=" + root 
					+ " size=" + list.size());
		}
		
		ErrorException exception = null;
		int count = 0;
		
		try { 
			Set<Text> foundDirs = new HashSet<Text>();
			Set<Text> foundFiles = new HashSet<Text>();
			
			for (SqSection section : list.values()) { 
				if (section == null || section.getRoot() != root) 
					continue;
				
				if (section instanceof SqRoot) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Section: " + section.getName() + " is root and cannot be removed");
				
				} else if (section instanceof SqSectionDir) {
					count += doFindItems(root, (SqSectionDir)section, 
							foundDirs, foundFiles);
					
				} else if (section instanceof SqRootFile) {
					SqRootFile file = (SqRootFile)section;
					foundFiles.add(new Text(file.getPathKey()));
					count += 1;
				}
			}
			
			try {
				if (count > 0)
					removeFiles(root, foundDirs, foundFiles);
			} finally { 
				root.reset();
			}
		} catch (IOException ee) {
			if (exception == null) {
				exception = new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ee);
			} else if (LOG.isErrorEnabled()) {
				LOG.error(ee.toString(), ee);
			}
		} catch (ErrorException ee) {
			if (exception == null) {
				exception = ee;
			} else if (LOG.isErrorEnabled()) {
				LOG.error(ee.toString(), ee);
			}
		}
		
		if (exception != null)
			throw exception;
		
		return count;
	}
	
	private static int doFindItems(final SqRoot root, 
			final SqSectionDir item, final Set<Text> foundDirs, 
			final Set<Text> foundFiles) throws IOException, ErrorException { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("doFindItems: root=" + root + " folder=" + item);
		
		ISectionSet set = item.getSubSections(new SectionQuery(0, 0));
		int count = 0;
		
		if (set != null && set.getSectionCount() > 0) { 
			for (int i=0; i < set.getSectionCount(); i++) { 
				SqSection section = (SqSection)set.getSectionAt(i);
				if (section == null) continue;
				
				if (section instanceof SqRoot) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Item: " + section.getName() + " is root");
					
				} else if (section instanceof SqSectionDir) {
					count += doFindItems(root, (SqSectionDir)section, 
							foundDirs, foundFiles);
					
				} else if (section instanceof SqRootFile) {
					SqRootFile file = (SqRootFile)section;
					foundFiles.add(new Text(file.getPathKey()));
					count += 1;
				}
			}
		}
		
		foundDirs.add(new Text(item.getPathKey()));
		
		return count;
	}
	
	static int removeFiles(final SqRoot root, final Set<Text> dirKeys, 
			final Set<Text> fileKeys) throws IOException, ErrorException { 
		if (fileKeys.size() == 0 && dirKeys.size() == 0)
			return 0;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("removeFiles: root=" + root + " dirCount=" + dirKeys.size() 
					+ " fileCount=" + fileKeys.size());
		}
		
		final FileStorer storer = root.newStorer();
		final FileLoader loader = root.newLoader();
		
		final SqRootNames names = new SqRootNames();
		names.reloadNames(root);
		
		int count = 0;
		
		try {
			Map<Integer,List<NameData>> dataMap = 
					new HashMap<Integer,List<NameData>>();
			
			for (Text key : fileKeys) { 
				if (key == null) continue;
				
				NameData data = names.getNameData(key);
				if (data == null || data.isDirectory())
					continue;
				
				int fileIndex = data.getAttrs().getFileIndex();
				List<NameData> list = dataMap.get(fileIndex);
				if (list == null) { 
					list = new ArrayList<NameData>();
					dataMap.put(fileIndex, list);
				}
				
				list.add(data);
			}
			
			for (Map.Entry<Integer, List<NameData>> entry : dataMap.entrySet()) {
				int fileIndex = entry.getKey();
				List<NameData> fileList = entry.getValue();
				
				Map<Text,FileData> fileMap = fileIndex > 0 ? 
						loader.loadFileDatas(fileIndex) : null;
				
				int mapcount = 0;
				
				for (NameData data : fileList) { 
					if (data == null) continue;
					Text key = data.getKey();
					
					if (fileMap != null && fileMap.containsKey(key)) {
						if (LOG.isInfoEnabled()) {
							LOG.info("removeFiles: remove file in map, key=" + key 
									+ " attrs=" + data.getAttrs());
						}
						
						fileMap.remove(key);
						mapcount ++;
					} else {
						if (LOG.isInfoEnabled()) {
							LOG.info("removeFiles: remove file in fs, key=" + key 
									+ " attrs=" + data.getAttrs());
						}
						
						boolean res = storer.removeFsFile(key.toString());
						if (!res) { 
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
									"Cannot remove stored file with key: " + key);
						}
					}
					
					Text parentKey = new Text(data.getAttrs().getParentKey());
					if (parentKey != null && parentKey.getLength() > 0) { 
						NameData parent = names.getNameData(parentKey);
						if (parent != null && parent.isDirectory())
							names.removeFileKey(parent, key);
					}
					
					names.remove(key);
					count ++;
					
					root.getLibrary().clearCacheData(key.toString());
				}
				
				if (fileMap != null && mapcount > 0)
					storer.storeFile(fileIndex, fileMap);
			}
		} finally { 
			if (count > 0) { 
				names.removeEmpty(dirKeys);
				SqRootOptimize.saveCounts(root, names, storer);
				names.storeNames(storer);
			}
			
			storer.close();
			loader.close();
		}
		
		return count;
	}
	
}
