package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public final class SqRootUpdate {
	private static final Logger LOG = Logger.getLogger(SqRootUpdate.class);

	public static boolean setName(IUser user, SqRoot root, String name) 
			throws ErrorException { 
		if (root == null) throw new NullPointerException();
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("setName: root=" + root + " name=" + name);
		
		SectionHelper.checkPermission(user, root, IData.Action.MODIFY);
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				if (root.setName(name)) { 
					root.getManager().saveLibraryList();
					return true;
				}
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
		
		return false;
	}
	
	public static boolean saveMetadata(IUser user, SqRootDir dir, 
			String name, String extname) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		
		SectionHelper.checkPermission(user, dir, IData.Action.MODIFY);
		
		final SqRoot root = dir.getRoot();
		final String key = dir.getPathKey();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return doSaveData(root, null, key, name, extname, null);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static boolean saveMetadata(IUser user, SqRootFile file, 
			String name, String extname, Map<String,String> metas) 
			throws ErrorException { 
		if (file == null) throw new NullPointerException();
		
		SectionHelper.checkPermission(user, file, IData.Action.MODIFY);
		
		final SqRoot root = file.getRoot();
		final String key = file.getPathKey();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return doSaveData(root, file, key, name, extname, metas);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	private static boolean doSaveData(final SqRoot root, 
			final SqRootFile file, final String key, final String name, String extname, 
			final Map<String,String> metas) throws ErrorException {
		if (name == null || name.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Name cannot be empty");
		}
		
		if (extname != null) 
			extname = extname.toLowerCase();
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("doSaveData: key=" + key + " name=" + name + " ext=" + extname);
		
		final SqRootNames names = new SqRootNames();
		
		NameData changedName = null;
		FileData changedData = null;
		ErrorException exception = null;
		boolean success = false;
		
		try { 
			final Text keyText = new Text(key);
			names.reloadNames(root);
			
			NameData data = names.getNameData(keyText);
			if (data == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"File key: " + key + " not found");
			}
			
			if (!data.isDirectory() && (extname == null || extname.length() == 0)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"File extension cannot be empty");
			}
			
			final String name0 = data.getAttrs().getName().toString();
			final String extname0 = data.getAttrs().getExtension().toString();
			
			boolean nameChanged = false;
			if (!name.equals(name0)) { 
				data.getAttrs().setName(name);
				nameChanged = true;
			}
			
			boolean extnameChanged = false;
			if (extname != null && !extname.equals(extname0)) { 
				data.getAttrs().setExtension(extname);
				extnameChanged = true;
			}
			
			boolean metaChanged = false;
			if (file != null && metas != null && metas.size() > 0) { 
				FileData fileData = file.getFileData();
				if (fileData != null) { 
					if (!fileData.getFileKey().equals(data.getKey())) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"File key: " + fileData.getFileKey() + 
								" not equals to name key: " + data.getKey());
					}
					
					if (nameChanged) fileData.getAttrs().setName(name);
					if (extnameChanged) fileData.getAttrs().setExtension(extname);
					
					FileMetaInfo[] metaInfos = fileData.getMetaInfos();
					Map<String,FileMetaInfo> metaMap = new HashMap<String,FileMetaInfo>();
					
					int appendCount = 0, changedCount = 0, removedCount = 0;
					
					for (int i=0; metaInfos != null && i < metaInfos.length; i++) { 
						FileMetaInfo metaInfo = metaInfos[i];
						if (metaInfo == null) continue;
						
						Text nameTxt = metaInfo.getName();
						if (nameTxt == null) continue;
						
						String metaKey = nameTxt.toString().toLowerCase();
						metaMap.put(metaKey, metaInfo);
					}
					
					for (Map.Entry<String, String> entry : metas.entrySet()) { 
						String metaName = entry.getKey();
						String metaValue = entry.getValue();
						if (metaName == null || metaName.length() == 0)
							continue;
						
						String metaKey = metaName.toLowerCase();
						FileMetaInfo metaInfo = metaMap.get(metaKey);
						if (metaInfo == null) { 
							if (metaValue != null && metaValue.length() > 0) {
								metaInfo = new FileMetaInfo(new Text(metaName), new Text(metaValue));
								metaMap.put(metaKey, metaInfo);
								appendCount ++;
								
								if (LOG.isDebugEnabled())
									LOG.debug("doSaveData: append meta: " + metaName + "=" + metaValue);
							}
							continue;
						}
						
						Text oldTxt = metaInfo.getValue();
						String oldValue = oldTxt != null ? oldTxt.toString() : null;
						
						if (metaValue == null || metaValue.length() == 0) { 
							metaMap.remove(metaKey);
							removedCount ++;
							
							if (LOG.isDebugEnabled())
								LOG.debug("doSaveData: remove meta: " + metaName + "=" + oldValue);
							
						} else if (!metaValue.equals(oldValue)) { 
							metaInfo = new FileMetaInfo(new Text(metaName), new Text(metaValue));
							metaMap.put(metaKey, metaInfo);
							changedCount ++;
							
							if (LOG.isDebugEnabled())
								LOG.debug("doSaveData: change meta: " + metaName + "=" + metaValue);
						}
					}
					
					if (appendCount > 0 || changedCount > 0 || removedCount > 0) { 
						fileData.setMetaInfos(metaMap.values().toArray(new FileMetaInfo[metaMap.size()]));
						changedName = data;
						changedData = fileData;
						metaChanged = true;
					}
				} else { 
					if (LOG.isDebugEnabled())
						LOG.debug("doSaveData: key=" + key + " file data cannot loaded");
				}
			}
			
			if (nameChanged || extnameChanged || metaChanged) {
				if (LOG.isDebugEnabled())
					LOG.debug("doSaveData: key=" + key + " attrs=" + data.getAttrs());
				
				success = true;
			}
		} catch (IOException ee) { 
			if (exception == null) {
				exception = new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ee);
			}// else if (LOG.isErrorEnabled()) {
			//	LOG.error(ee.toString(), ee);
			//}
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
					if (changedName != null && changedData != null) { 
						final FileLoader loader = root.newLoader();
						final int fileIndex = changedName.getAttrs().getFileIndex();
						if (fileIndex <= 0) { 
							storer.storeFsFile(changedData.getFileKey().toString(), changedData);
						} else { 
							Map<Text,FileData> fileMap = loader.loadFileDatas(fileIndex);
							fileMap.put(changedData.getFileKey(), changedData);
							storer.storeFile(fileIndex, fileMap);
						}
						loader.close();
					}
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
	
}
