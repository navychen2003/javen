package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.data.recycle.SqRecycleRoot;
import org.javenstudio.falcon.datum.data.share.SqShareRoot;
import org.javenstudio.falcon.datum.data.upload.SqUploadRoot;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public final class SqRootHelper {

	public static SqRecycleRoot getRecycleRoot(SqLibrary library) { 
		if (library == null) throw new NullPointerException();
		
		SqRecycleRoot resRoot = null;
		
		for (int i=0; i < library.getSectionCount(); i++) { 
			SqRoot root = library.getSectionAt(i);
			if (root != null && root instanceof SqRecycleRoot) { 
				SqRecycleRoot recycleRoot = (SqRecycleRoot)root;
				if (resRoot == null) resRoot = recycleRoot;
				if (recycleRoot.getName().equals(SqRecycleRoot.RECYCLE_NAME))
					resRoot = recycleRoot;
			}
		}
		
		return resRoot;
	}
	
	public static SqUploadRoot getUploadRoot(SqLibrary library) { 
		if (library == null) throw new NullPointerException();
		
		SqUploadRoot resRoot = null;
		
		for (int i=0; i < library.getSectionCount(); i++) { 
			SqRoot root = library.getSectionAt(i);
			if (root != null && root instanceof SqUploadRoot) { 
				SqUploadRoot uploadRoot = (SqUploadRoot)root;
				if (resRoot == null) resRoot = uploadRoot;
				if (uploadRoot.getName().equals(SqUploadRoot.UPLOAD_NAME))
					resRoot = uploadRoot;
			}
		}
		
		return resRoot;
	}
	
	public static SqShareRoot getShareRoot(SqLibrary library) { 
		if (library == null) throw new NullPointerException();
		
		SqShareRoot resRoot = null;
		
		for (int i=0; i < library.getSectionCount(); i++) { 
			SqRoot root = library.getSectionAt(i);
			if (root != null && root instanceof SqShareRoot) { 
				SqShareRoot shareRoot = (SqShareRoot)root;
				if (resRoot == null) resRoot = shareRoot;
				if (shareRoot.getName().equals(SqShareRoot.SHARE_NAME))
					resRoot = shareRoot;
			}
		}
		
		return resRoot;
	}
	
	public static String[] saveFiles(IUser user, SqLibrary library, 
			FileStream[] streams) throws ErrorException { 
		if (library == null) throw new NullPointerException();
		if (streams == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No upload files");
		}
		
		SqUploadRoot saveRoot = getUploadRoot(library);
		if (saveRoot == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Library has no upload folder");
		}
		
		return saveFiles(user, saveRoot, streams);
	}
	
	public static String[] saveFiles(IUser user, SqRoot root, 
			FileStream[] streams) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		if (streams == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No upload files");
		}
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.TOP);
			try { 
				return SqRootSave.saveFiles(user, root, streams);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static String[] saveFiles(IUser user, SqRootDir dir, 
			FileStream[] streams) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		if (streams == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No upload files");
		}
		
		SqRoot root = dir.getRoot();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.TOP);
			try { 
				return SqRootSave.saveFiles(user, dir, streams);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static boolean newFolder(IUser user, SqRoot root, 
			String folderName) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		if (folderName == null || folderName.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Folder name is empty");
		}
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return SqRootSave.newFolder(user, root, folderName);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static boolean newFolder(IUser user, SqRootDir dir, 
			String folderName) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		if (folderName == null || folderName.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Folder name is empty");
		}
		
		SqRoot root = dir.getRoot();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return SqRootSave.newFolder(user, dir, folderName);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static String[] moveFilesTo(IUser user, SqRoot root, 
			SqSection[] sections, boolean copyOnly) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					(copyOnly?"Copy":"Move") + " files is empty");
		}
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return SqRootMove.moveFiles(user, root, null, sections, copyOnly);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static String[] moveFilesTo(IUser user, SqRootDir dir, 
			SqSection[] sections, boolean copyOnly) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					(copyOnly?"Copy":"Move") + " files is empty");
		}
		
		SqRoot root = dir.getRoot();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return SqRootMove.moveFiles(user, root, dir, sections, copyOnly);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static int listFiles(IUser user, SqRoot root, 
			Collection<SqSection> sections, boolean includeSub) 
			throws ErrorException { 
		if (root == null || sections == null) throw new NullPointerException();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.READ, null);
			try { 
				return SqRootOptimize.doListItems(root, root, sections, includeSub);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			} finally { 
				root.getLock().unlock(ILockable.Type.READ);
			}
		//}
	}
	
	public static int listFiles(IUser user, SqRootDir dir, 
			Collection<SqSection> sections, boolean includeSub) 
			throws ErrorException { 
		if (dir == null || sections == null) throw new NullPointerException();
		SqRoot root = dir.getRoot();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.READ, null);
			try { 
				return SqRootOptimize.doListItems(root, dir, sections, includeSub);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			} finally { 
				root.getLock().unlock(ILockable.Type.READ);
			}
		//}
	}
	
	public static int deleteFiles(IUser user, SqSection[] sections) 
			throws ErrorException { 
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Delete files is empty");
		}
		
		ArrayList<SqSection> secs = new ArrayList<SqSection>();
		ArrayList<SqRoot> roots = new ArrayList<SqRoot>();
		
		for (SqSection section : sections) { 
			if (section == null) continue;
			if (section instanceof SqRoot)
				roots.add((SqRoot)section);
			else
				secs.add(section);
		}
		
		int count = 0;
		
		if (secs.size() > 0) {
			count += SqRootDelete.deleteFiles(user, 
					secs.toArray(new SqSection[secs.size()]));
		}
		
		if (roots.size() > 0) {
			count += SqRootDelete.deleteRoots(user, 
					roots.toArray(new SqRoot[roots.size()]));
		}
		
		return count;
	}
	
	static Map<DataManager,Map<SqLibrary,Set<SqRoot>>> toRootMap(
			SqRoot[] roots) throws ErrorException { 
		Map<DataManager,Map<SqLibrary,Set<SqRoot>>> rootMap = 
				new HashMap<DataManager,Map<SqLibrary,Set<SqRoot>>>();
		
		if (roots == null || roots.length == 0) 
			return rootMap;
		
		for (SqRoot root : roots) { 
			if (root == null) continue;
			
			SqLibrary library = root.getLibrary();
			DataManager manager = library.getManager();
			
			Map<SqLibrary,Set<SqRoot>> list = rootMap.get(manager);
			if (list == null) { 
				list = new HashMap<SqLibrary,Set<SqRoot>>();
				rootMap.put(manager, list);
			}
			
			Set<SqRoot> rts = list.get(library);
			if (rts == null) { 
				rts = new HashSet<SqRoot>();
				list.put(library, rts);
			}
			
			rts.add(root);
		}
		
		return rootMap;
	}
	
	static Map<SqRoot,Map<Text,SqSection>> toSectionMap(
			SqSection[] sections) throws ErrorException { 
		Map<SqRoot,Map<Text,SqSection>> sectionMap = 
				new HashMap<SqRoot,Map<Text,SqSection>>();
		
		if (sections == null || sections.length == 0) 
			return sectionMap;
		
		for (SqSection section : sections) { 
			if (section == null) continue;
			
			SqRoot root = section.getRoot();
			Map<Text,SqSection> list = sectionMap.get(root);
			if (list == null) { 
				list = new HashMap<Text,SqSection>();
				sectionMap.put(root, list);
			}
			
			list.put(new Text(section.getPathKey()), section);
		}
		
		return sectionMap;
	}
	
}
