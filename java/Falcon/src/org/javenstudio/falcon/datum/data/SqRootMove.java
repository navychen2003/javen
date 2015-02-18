package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public final class SqRootMove {
	private static final Logger LOG = Logger.getLogger(SqRootMove.class);
	
	public static String[] moveFiles(IUser user, SqRoot root, SqRootDir dir, 
			SqSection[] sections, boolean copyOnly) throws ErrorException { 
		Set<String> contentIds = new HashSet<String>();
		
		if (sections != null) {
			for (SqSection section : sections) { 
				SectionHelper.checkPermission(user, section, IData.Action.MOVE);
			}
		}
		
		Map<SqRoot,Map<Text,SqSection>> map = SqRootHelper.toSectionMap(sections);
		if (map == null || map.size() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					(copyOnly?"Copy":"Move") + " items is empty");
		}
		
		for (Map.Entry<SqRoot, Map<Text,SqSection>> entry : map.entrySet()) { 
			SqRoot rt = entry.getKey();
			Map<Text,SqSection> list = entry.getValue();
			if (rt == null || list == null) continue;
			
			if (rt.getLibrary() == root.getLibrary()) { 
				if (copyOnly) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Copy operation must process between different library");
				}
				//synchronized (root.getLock()) {
					root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
					try {
						rt.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
						try {
							String[] ids = doMoveNamesTo(root, dir, list, rt);
							if (ids != null) {
								for (String id : ids) {
									if (id != null && id.length() > 0) 
										contentIds.add(id);
								}
							}
						} finally { 
							rt.getLock().unlock(ILockable.Type.WRITE);
						}
					} finally { 
						root.getLock().unlock(ILockable.Type.WRITE);
					}
				//}
			} else { 
				//synchronized (root.getLock()) {
					root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
					try {
						rt.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
						try {
							String[] ids = doMoveFilesTo(root, dir, list, rt, copyOnly);
							if (ids != null) {
								for (String id : ids) {
									if (id != null && id.length() > 0) 
										contentIds.add(id);
								}
							}
						} finally { 
							rt.getLock().unlock(ILockable.Type.WRITE);
						}
					} finally { 
						root.getLock().unlock(ILockable.Type.WRITE);
					}
				//}
			}
		}
		
		return contentIds.toArray(new String[contentIds.size()]);
	}
	
	private static String[] doMoveNamesTo(final SqRoot root, final SqRootDir dir, 
			final Map<Text,SqSection> list, final SqRoot listrt) throws ErrorException { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("doMoveNamesTo: root=" + root + " dir=" + dir 
					+ " size=" + list.size() + " from=" + listrt);
		}
		
		final SqRootNames toNames = new SqRootNames();
		SqRootNames fromNames = null;
		
		Set<String> contentIds = new HashSet<String>();
		ErrorException exception = null;
		int count = 0;
		
		try { 
			toNames.reloadNames(root);
			if (listrt.equals(root)) { 
				fromNames = toNames;
			} else { 
				fromNames = new SqRootNames();
				fromNames.reloadNames(listrt);
			}
			
			String targetStr = dir != null ? dir.getPathKey() : root.getPathKey();
			final Text targetKey = new Text(targetStr);
			final NameData targetData = toNames.getNameData(targetKey);
			
			if (targetData == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Move to target key: " + targetKey + " not found in name map");
			}
			
			if (listrt.equals(root)) { 
				for (Map.Entry<Text, SqSection> entry : list.entrySet()) { 
					Text key = entry.getKey();
					SqSection value = entry.getValue();
					if (key == null || value == null) continue;
					
					NameData data = fromNames.getNameData(key);
					if (data == null) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Item: " + value.getName() + " not found in name map");
					}
					
					if (key.equals(targetKey)) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Cannot move item: " + value.getName() + " to itself");
					}
					
					Text parentKey = new Text(data.getAttrs().getParentKey());
					if (parentKey != null && parentKey.equals(targetKey)) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Cannot move item: " + value.getName() + " to it's current folder");
					}
					
					if (fromNames.existsIn(data, targetKey)) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Cannot move item: " + value.getName() + " to it's sub folder");
					}
				}
			}
			
			for (Map.Entry<Text, SqSection> entry : list.entrySet()) { 
				Text key = entry.getKey();
				SqSection section = entry.getValue();
				if (key == null || section == null) continue;
				
				String parentStr = section.getParentKey();
				if (parentStr == null || parentStr.length() == 0)
					parentStr = listrt.getPathKey();
				
				Text parentKey = new Text(parentStr);
				NameData parentData = fromNames.getNameData(parentKey);
				if (parentData == null) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Item's folder: " + parentKey + " not found in name map");
				}
				if (parentData == targetData) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Cannot move item: " + section.getName() + " to it's current folder");
				}
				
				String fileStr = section.getPathKey();
				final Text fileKey = new Text(fileStr);
				
				final NameData fileData = fromNames.getNameData(fileKey);
				if (fileData == null) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Item: " + section.getName() + " not found in name map");
				}
				
				if (fromNames.removeFileKey(parentData, fileKey)) { 
					if (!listrt.equals(root)) { 
						final Map<Text,NameData> removes = new HashMap<Text,NameData>();
						
						NameData tofileData = toNames.getNameData(fileKey, 
							new SqRootNames.Creator() {
								@Override
								public NameData create() throws ErrorException {
									NameData ndata = new NameData(fileKey, fileData.getAttrs(), fileData.getFileKeys());
									ndata.getAttrs().setParentKey(targetKey);
									
									if (LOG.isDebugEnabled())
										LOG.debug("doMoveNamesTo: add " + ndata + " to root: " + root);
									return ndata;
								}
							});
						
						if (tofileData != null) {
							removes.put(fileKey, fileData);
							
							if (fileData.isDirectory()) { 
								fromNames.getNameDatasIn(fileData, new SqRootNames.Collector() {
										@Override
										public void add(final Text key, final NameData data) throws ErrorException {
											if (key == null || data == null) return;
											NameData toData = toNames.getNameData(key, 
												new SqRootNames.Creator() {
													@Override
													public NameData create() throws ErrorException {
														NameData ndata = new NameData(key, data.getAttrs(), data.getFileKeys());
														
														if (LOG.isDebugEnabled())
															LOG.debug("doMoveNamesTo: add " + ndata + " to root: " + root);
														return ndata;
													}
												});
											if (toData != null)
												removes.put(key, data);
										}
									});
							}
							
							for (Map.Entry<Text, NameData> removeEntry : removes.entrySet()) { 
								Text removeKey = removeEntry.getKey();
								NameData removeVal = removeEntry.getValue();
								
								if (removeKey != null && removeVal != null) { 
									fromNames.remove(removeKey);
									
									if (LOG.isDebugEnabled())
										LOG.debug("doMoveNamesTo: remove " + removeVal + " from root: " + listrt);
								}
							}
						}
					}
					
					toNames.addFileKey(targetData, fileKey);
					count ++;
					
					if (fileKey != null) {
						String contentId = root.getManager().getUserKey() 
								+ root.getLibrary().getContentKey() + root.getContentKey() 
								+ fileKey.toString();
						contentIds.add(contentId);
					}
					
					if (LOG.isDebugEnabled()) { 
						LOG.debug("doMoveNamesTo: move file: " + fileStr 
								+ " to directory: " + targetStr);
					}
				} else { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"File: " + fileKey + " of item: " + section.getName() +  " not found");
				}
			}
		} catch (ErrorException ee) {
			if (exception == null) {
				exception = ee;
			} else if (LOG.isErrorEnabled()) {
				LOG.error(ee.toString(), ee);
			}
		} finally { 
			boolean updated = false;
			try {
				if (count > 0) {
					if (LOG.isDebugEnabled()) { 
						LOG.debug("doMoveNamesTo: store nameMap.size=" + toNames.size() 
								+ " to root: " + root);
					}
					
					FileStorer toStorer = root.newStorer();
					SqRootOptimize.saveCounts(root, toNames, toStorer);
					toNames.storeNames(toStorer);
					toStorer.close(); // flush
					
					if (!listrt.equals(root)) {
						if (LOG.isDebugEnabled()) { 
							LOG.debug("doMoveNamesTo: store nameMap.size=" + fromNames.size() 
									+ " to root: " + listrt);
						}
						
						FileStorer fromStorer = listrt.newStorer();
						SqRootOptimize.saveCounts(listrt, fromNames, fromStorer);
						fromNames.storeNames(fromStorer);
						fromStorer.close(); // flush
					}
					
					updated = true;
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
			} finally { 
				if (updated) {
					try { 
						root.reset();
						listrt.reset();
					} catch (ErrorException ee) { 
						if (exception == null) {
							exception = ee;
						} else if (LOG.isErrorEnabled()) {
							LOG.error(ee.toString(), ee);
						}
					}
				}
			}
		}
		
		if (exception != null)
			throw exception;
		
		return contentIds.toArray(new String[contentIds.size()]);
	}
	
	private static String[] doMoveFilesTo(SqRoot root, SqRootDir dir, 
			Map<Text,SqSection> list, SqRoot listrt, boolean copyOnly) 
			throws ErrorException { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("doMoveFilesTo: root=" + root + " dir=" + dir 
					+ " size=" + list.size() + " from=" + listrt 
					+ " copy=" + copyOnly);
		}
		
		final FileStorer storer = root.newStorer();
		final SqRootNames names = new SqRootNames();
		
		Set<String> contentIds = new HashSet<String>();
		ErrorException exception = null;
		int count = 0;
		
		try { 
			names.reloadNames(root);
			
			String targetStr = dir != null ? dir.getPathKey() : root.getPathKey();
			Text targetKey = new Text(targetStr);
			NameData target = names.getNameData(targetKey);
			
			if (target == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						(copyOnly?"Copy":"Move") + " to target key: " + targetKey + " not found");
			}
			
			Set<Text> doneDirs = new HashSet<Text>();
			Set<Text> doneFiles = new HashSet<Text>();
			
			for (SqSection section : list.values()) { 
				if (section == null || section.getRoot() != listrt) 
					continue;
				
				if (section instanceof SqSectionDir) {
					count += doMoveFolderTo(root, names, target, 
							(SqSectionDir)section, storer, 
							doneDirs, doneFiles);
					
				} else if (section instanceof SqRootFile) {
					count += doMoveFileTo(root, names, target, 
							(SqRootFile)section, storer, 
							doneFiles);
				}
			}
			
			try {
				if (copyOnly == false)
					SqRootDelete.removeFiles(listrt, doneDirs, doneFiles);
			} finally { 
				listrt.reset();
			}
			
			for (Text fileKey : doneFiles) {
				if (fileKey == null) continue;
				String contentId = root.getManager().getUserKey() 
						+ root.getLibrary().getContentKey() + root.getContentKey() 
						+ fileKey.toString();
				contentIds.add(contentId);
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
		} finally { 
			boolean updated = false;
			try {
				if (count > 0) {
					SqRootOptimize.optimizeFiles(root, names, storer);
					SqRootOptimize.saveCounts(root, names, storer);
					names.storeNames(storer);
					updated = true;
				}
				storer.close(); // flush
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
			} finally { 
				if (updated) {
					try { 
						root.reset();
					} catch (ErrorException ee) { 
						if (exception == null) {
							exception = ee;
						} else if (LOG.isErrorEnabled()) {
							LOG.error(ee.toString(), ee);
						}
					}
				}
			}
		}
		
		if (exception != null)
			throw exception;
		
		return contentIds.toArray(new String[contentIds.size()]);
	}
	
	private static int doMoveFolderTo(final SqRoot root, 
			final SqRootNames names, final NameData target, 
			final SqSectionDir item, final FileStorer storer, 
			final Set<Text> doneDirs, final Set<Text> doneFiles) 
			throws IOException, ErrorException { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("doMoveFolderTo: root=" + root + " target=" + target 
					+ " folder=" + item);
		}
		
		final Text dirKey = new Text(item.getPathKey());
		
		NameData dirData = names.getNameData(dirKey, 
			new SqRootNames.Creator() {
				@Override
				public NameData create() throws ErrorException {
					NameData data = new NameData(dirKey, null, null);
					data.getAttrs().setName(item.getName());
					data.getAttrs().setParentKey(target.getKey());
					data.getAttrs().setModifiedTime(System.currentTimeMillis());
					return data;
				}
			});
		
		ISectionSet set = item.getSubSections(new SectionQuery(0, 0));
		int count = 0;
		
		if (set != null && set.getSectionCount() > 0) { 
			for (int i=0; i < set.getSectionCount(); i++) { 
				SqSection section = (SqSection)set.getSectionAt(i);
				if (section == null) continue;
				
				if (section instanceof SqSectionDir) {
					count += doMoveFolderTo(root, names, dirData, 
							(SqSectionDir)section, storer, 
							doneDirs, doneFiles);
					
				} else if (section instanceof SqRootFile) {
					count += doMoveFileTo(root, names, dirData, 
							(SqRootFile)section, storer, 
							doneFiles);
				}
			}
		}
		
		if (count > 0 || dirData.getFileCount() > 0) {
			names.addFileKey(target, dirKey);
			doneDirs.add(dirKey);
		}
		
		return count;
	}
	
	private static int doMoveFileTo(final SqRoot root, 
			final SqRootNames names, final NameData target, 
			final SqRootFile item, final FileStorer storer, 
			final Set<Text> doneFiles) throws IOException, ErrorException { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("doMoveFileTo: root=" + root + " target=" + target 
					+ " file=" + item);
		}
		
		final Text fileKey = new Text(item.getPathKey());
		
		NameData nameData = names.getNameData(fileKey, 
			new SqRootNames.Creator() {
				@Override
				public NameData create() throws ErrorException {
					NameData data = new NameData(fileKey, item.getNameData().getAttrs(), null);
					data.getAttrs().setParentKey(target.getKey());
					return data;
				}
			});
		
		if (nameData != null) { 
			FileSource.SqFileItem sourceItem = new FileSource.SqFileItem(item, nameData);
			FileData fileData = storer.storeFile(sourceItem, item.getFileData());
			
			if (fileData != null) { 
				nameData.getAttrs().setFileIndex(fileData.getAttrs().getFileIndex());
				names.addFileKey(target, fileKey);
				doneFiles.add(fileKey);
				return 1;
			}
		}
		
		return 0;
	}
	
}
