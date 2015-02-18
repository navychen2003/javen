package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.raptor.io.Text;

public final class SqRootSave {
	private static final Logger LOG = Logger.getLogger(SqRootSave.class);
	
	public static boolean newFolder(IUser user, SqRoot root, 
			String folderName) throws ErrorException {
		String rootPathKey = root.getPathKey();
		String rootName = ""; //root.getName(); 
		
		String dirKey = rootPathKey;
		String dirName = rootName;
		String dirParent = null;
		long dirLastModified = root.getModifiedTime();
		
		return doNewFolder(user, root, dirKey, dirName, dirLastModified, dirParent, folderName);
	}
	
	public static boolean newFolder(IUser user, SqRootDir dir, 
			String folderName) throws ErrorException {
		SqRoot root = dir.getRoot();
		//String rootName = root.getName(); 
		
		String dirKey = dir.getPathKey();
		String dirName = dir.getName(); 
		String dirParent = dir.getParentKey();
		long dirLastModified = dir.getModifiedTime();
		
		return doNewFolder(user, root, dirKey, dirName, dirLastModified, dirParent, folderName);
	}
	
	public static String[] saveFiles(IUser user, SqRoot root, 
			FileStream[] streams) throws ErrorException {
		String rootPathKey = root.getPathKey();
		String rootName = root.getName(); 
		
		String dirKey = rootPathKey;
		String dirName = rootName;
		String dirParent = null;
		long dirLastModified = root.getModifiedTime();
		
		return doSaveFiles(user, root, dirKey, dirName, dirLastModified, dirParent, streams);
	}
	
	public static String[] saveFiles(IUser user, SqRootDir dir, 
			FileStream[] streams) throws ErrorException {
		SqRoot root = dir.getRoot();
		//String rootName = root.getName(); 
		
		String dirKey = dir.getPathKey();
		String dirName = dir.getName(); 
		String dirParent = dir.getParentKey();
		long dirLastModified = dir.getModifiedTime();
		
		return doSaveFiles(user, root, dirKey, dirName, dirLastModified, dirParent, streams);
	}
	
	@SuppressWarnings("unused")
	private static String newFileName(SqRoot root, 
			String dirPath, String fileName, boolean isDir) 
			throws ErrorException { 
		String pathName = fileName;
		
		if (!fileName.startsWith("/")) {
			if (dirPath.endsWith("/"))
				pathName = dirPath + fileName;
			else
				pathName = dirPath + "/" + fileName;
		}
		
		return newFileName(root, pathName, isDir);
	}
	
	private static String newFileName(SqRoot root, 
			String filePath, boolean isDir) throws ErrorException {
		String baseName = root.getName(); //getContentPath();
		String pathName = filePath;
		String name = "/";
		
		if (!baseName.equals(pathName)) { 
			if (!pathName.startsWith(baseName)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Path: " + pathName + " not in base: " + baseName);
			}
			
			name = pathName.substring(baseName.length());
			//if (name.startsWith("/"))
			//	name = name.substring(1);
			
			if (isDir && !name.endsWith("/"))
				name += "/";
		}
		
		return name;
	}
	
	private static boolean doNewFolder(final IUser user, final SqRoot root, 
			final String dirKey, final String dirName, final long dirLastModified, 
			final String dirParent, String folderName) throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doNewFolder: dirKey=" + dirKey + " dirName=" + dirName 
					+ " folderName=" + folderName + " user=" + user);
		}
		
		final SqRootNames names = new SqRootNames();
		
		ErrorException exception = null;
		boolean success = false;
		
		try { 
			final Text dirKeyText = new Text(dirKey);
			names.reloadNames(root);
			
			NameData dirData = names.getNameData(dirKeyText, 
				new SqRootNames.Creator() {
					@Override
					public NameData create() throws ErrorException {
						NameData data = new NameData(dirKeyText, null, null);
						data.getAttrs().setName(dirName);
						data.getAttrs().setParentKey(dirParent != null ? dirParent : "");
						data.getAttrs().setOwner(user != null ? user.getUserName() : "");
						return data;
					}
				});
			
			folderName = SectionHelper.newName(folderName);
			if (folderName == null || folderName.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Folder name is empty");
			}
			if (names.existsFileName(dirData, folderName)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Folder name: " + folderName + " already exists");
			}
			
			final String name = folderName; 
			final String key = SectionHelper.newFileKey(name 
					+ "@" + System.currentTimeMillis(), true);
			
			final Text keyText = new Text(key);
			if (names.contains(keyText)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Folder name: " + name + " already exists");
			}
			
			NameData nameData = names.getNameData(keyText, 
				new SqRootNames.Creator() {
					@Override
					public NameData create() throws ErrorException {
						NameData data = new NameData(keyText, null, null);
						data.getAttrs().setName(name);
						data.getAttrs().setParentKey(dirKeyText);
						data.getAttrs().setOwner(user != null ? user.getUserName() : "");
						data.getAttrs().setModifiedTime(System.currentTimeMillis());
						return data;
					}
				});
			
			if (nameData != null) {
				names.addFileKey(dirData, nameData.getKey());
				success = true;
			}
		} catch (ErrorException ee) {
			if (exception == null) {
				exception = ee;
			}// else if (LOG.isErrorEnabled()) {
			//	LOG.error(ee.toString(), ee);
			//}
		} finally { 
			boolean updated = false;
			try {
				if (success) {
					FileStorer storer = root.newStorer();
					SqRootOptimize.saveCounts(root, names, storer);
					names.storeNames(storer);
					storer.close(); // flush
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
		
		return success;
	}
	
	private static String[] doSaveFiles(final IUser user, final SqRoot root, 
			final String dirKey, final String dirName, final long dirLastModified, 
			final String dirParent, FileStream[] streams) 
			throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doSaveFiles: dirKey=" + dirKey + " dirName=" + dirName 
					+ " user=" + user);
		}
		
		final FileStorer storer = root.newStorer();
		final SqRootNames names = new SqRootNames();
		
		Set<String> contentIds = new HashSet<String>();
		ErrorException exception = null;
		//int count = 0;
		
		try { 
			final Text dirKeyText = new Text(dirKey);
			names.reloadNames(root);
			
			NameData dirData = names.getNameData(dirKeyText, 
				new SqRootNames.Creator() {
					@Override
					public NameData create() throws ErrorException {
						NameData data = new NameData(dirKeyText, null, null);
						data.getAttrs().setName(dirName);
						data.getAttrs().setParentKey(dirParent != null ? dirParent : "");
						data.getAttrs().setOwner(user != null ? user.getUserName() : "");
						data.getAttrs().setModifiedTime(dirLastModified);
						return data;
					}
				});
			
			for (int i=0; streams != null && i < streams.length; i++) {
				final FileStream stream = streams[i]; 
				if (stream == null) continue;
				
				final String streamName = names.newFileName(dirData, 
						SectionHelper.newName(stream.getName()));
				
				if (LOG.isDebugEnabled()) { 
					LOG.debug("doSaveFiles: streamName=" + stream.getName() 
							+ " streamLength=" + stream.getSize() 
							+ " streamType=" + stream.getContentType() 
							+ " saveName=" + streamName);
				}
				
				if (names.existsFileName(dirData, streamName)) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"File name: " + streamName + " already exists");
				}
				
				final String name = streamName; 
				final String key = SectionHelper.newFileKey(name 
						+ "@" + System.currentTimeMillis(), false);
				
				final String parentKey = dirKey;
				final long lastModified = System.currentTimeMillis();
				
				final FileSource.StreamItem item = new FileSource.StreamItem(
						stream, name, key, parentKey, lastModified);
				
				final FileData fileData = storer.storeFile(item);
				
				if (fileData != null) {
					NameData nameData = names.getNameData(fileData.getFileKey(), 
						new SqRootNames.Creator() {
							@Override
							public NameData create() throws ErrorException {
								NameData data = new NameData(fileData.getFileKey(), fileData.getAttrs(), null);
								data.getAttrs().setParentKey(dirKeyText);
								data.getAttrs().setOwner(user != null ? user.getUserName() : "");
								data.getAttrs().setFileIndex(fileData.getAttrs().getFileIndex());
								return data;
							}
						});
					
					if (nameData != null) { 
						names.addFileKey(dirData, nameData.getKey());
						String contentId = root.getManager().getUserKey() 
								+ root.getLibrary().getContentKey() + root.getContentKey() 
								+ nameData.getKey();
						contentIds.add(contentId);
						//count ++;
					}
				}
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
				if (contentIds.size() > 0) {
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
	
}
