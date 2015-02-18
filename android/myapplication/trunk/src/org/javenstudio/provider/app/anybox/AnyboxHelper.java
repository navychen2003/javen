package org.javenstudio.provider.app.anybox;

import java.util.ArrayList;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.FolderOperation;
import org.javenstudio.android.entitydb.content.IHostData;
import org.javenstudio.common.util.Logger;

public final class AnyboxHelper {
	private static final Logger LOG = Logger.getLogger(AnyboxHelper.class);

	public static interface IRequestWrapper {
		public AnyboxApp getApp();
		public IHostData getHostData();
		public String getAuthToken();
	}
	
	static String getFileURL(IRequestWrapper wrapper, 
			String fileId, boolean withToken) {
		if (wrapper == null || fileId == null || fileId.length() == 0) 
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getFileURL: wrapper=" + wrapper 
					+ " fileId=" + fileId);
		}
		
		String url = wrapper.getApp().getRequestUrl(wrapper.getHostData()) + "/datum/file/" 
				+ fileId; 
		
		if (withToken) {
			String token = wrapper.getAuthToken();
			url = url + "?token=" + token;
		}
		
		return url;
	}
	
	static String getImageURL(IRequestWrapper wrapper, String imageId, 
			String params, String ext) {
		if (wrapper == null || imageId == null || imageId.length() == 0) 
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getImageURL: wrapper=" + wrapper 
					+ " imageId=" + imageId);
		}
		
		if (params == null) params = "192";
		if (ext == null || ext.length() == 0) ext = "jpg";
		
		String url = wrapper.getApp().getRequestUrl(wrapper.getHostData()) + "/datum/image/" 
				+ imageId + "_" + params + "." + ext; 
		
		if (true) {
			String token = wrapper.getAuthToken();
			url = url + "?token=" + token;
		}
		
		return url;
	}
	
	static String getImageURL(AnyboxApp app, IHostData host, String authToken,
			String imageId, String params, String ext) {
		if (app == null || host == null || imageId == null || imageId.length() == 0) 
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getImageURL: host=" + host + " authToken=" + authToken 
					+ " imageId=" + imageId);
		}
		
		if (params == null) params = "192";
		if (ext == null || ext.length() == 0) ext = "jpg";
		
		String url = app.getRequestUrl(host) + "/datum/image/" 
				+ imageId + "_" + params + "." + ext; 
		
		if (authToken != null) {
			String token = authToken; //account.getAuthToken();
			url = url + "?token=" + token;
		}
		
		return url;
	}
	
	public static interface IAnyboxFolder {
		public FileOperation getSubFileOps();
		public FileOperation getSubFolderOps();
		
		public String getRootType();
		public String getType();
		public String getPerms();
		public String getOps();
	}
	
	static boolean supportOperation(IAnyboxFolder folder, char op) {
		if (folder != null) return supportOperation(folder.getOps(), op);
		return false;
	}
	
	static boolean supportOperation(String ops, char op) {
		if (ops != null && ops.length() > 0) {
			if (ops.indexOf(op) >= 0) return true;
		}
		return false;
	}
	
	static boolean canPermission(IAnyboxFolder folder, char pm) {
		if (folder != null) return canPermission(folder.getPerms(), pm);
		return false;
	}
	
	static boolean canPermission(String perms, char pm) {
		if (perms != null && perms.length() > 0) {
			if (perms.indexOf(pm) >= 0) return true;
		}
		return false;
	}
	
	static boolean supportShare(IAnyboxFolder folder) {
		if (folder != null) {
			String type = folder.getRootType();
			if (type != null && type.equalsIgnoreCase("application/x-share-root"))
				return true;
		}
		return false;
	}
	
	static boolean supportModify(IAnyboxFolder folder) {
		if (folder != null) {
			String type = folder.getRootType();
			if (type != null && type.equalsIgnoreCase("application/x-recycle-root"))
				return false;
			return true;
		}
		return false;
	}
	
	static boolean supportDownload(IAnyboxFolder folder) {
		if (folder != null) {
			String type = folder.getRootType();
			if (type != null && type.equalsIgnoreCase("application/x-recycle-root"))
				return false;
			return true;
		}
		return false;
	}
	
	static boolean supportOpen(IAnyboxFolder folder) {
		if (folder != null) {
			String type = folder.getRootType();
			if (type != null && type.equalsIgnoreCase("application/x-recycle-root"))
				return false;
			return true;
		}
		return false;
	}
	
	static boolean isSystemFolder(IAnyboxFolder folder) {
		if (folder != null) {
			if (LOG.isDebugEnabled()) LOG.debug("isSystemFolder: folder=" + folder);
			String type = folder.getType();
			if (type != null) {
				if (type.equalsIgnoreCase("application/x-recycle-root") || 
					type.equalsIgnoreCase("application/x-upload-root") || 
					type.equalsIgnoreCase("application/x-share-root"))
					return true;
			}
			return false;
		}
		return false;
	}
	
	static boolean supportSelect(IAnyboxFolder folder) {
		return supportDelete(folder) || supportMove(folder) || supportCopy(folder);
	}
	
	static boolean supportEmpty(IAnyboxFolder folder) {
		return supportOperation(folder, 'e');
	}
	
	static boolean supportNewfolder(IAnyboxFolder folder) {
		return supportOperation(folder, 'n');
	}
	
	static boolean supportUpload(IAnyboxFolder folder) {
		return supportOperation(folder, 'u');
	}
	
	static boolean supportDelete(IAnyboxFolder folder) {
		return supportOperation(folder, 'd');
	}
	
	static boolean supportMove(IAnyboxFolder folder) {
		return supportOperation(folder, 'm');
	}
	
	static boolean supportCopy(IAnyboxFolder folder) {
		return supportOperation(folder, 'c');
	}
	
	static boolean canCopy(IAnyboxFolder folder) {
		return canPermission(folder, 'c');
	}
	
	static boolean canMove(IAnyboxFolder folder) {
		return canPermission(folder, 'm');
	}
	
	static boolean canDelete(IAnyboxFolder folder) {
		return canPermission(folder, 'd');
	}
	
	static boolean canWrite(IAnyboxFolder folder) {
		return canPermission(folder, 'w');
	}
	
	static boolean canRead(IAnyboxFolder folder) {
		return canPermission(folder, 'r');
	}
	
	static FileOperation createSubFileOperation(IAnyboxFolder folder) {
		if (folder == null) return null;
		
		ArrayList<FileOperation.Operation> list = new ArrayList<FileOperation.Operation>();
		if (supportMove(folder)) list.add(FileOperation.Operation.MOVE);
		if (supportDelete(folder)) list.add(FileOperation.Operation.DELETE);
		if (supportModify(folder)) list.add(FileOperation.Operation.RENAME);
		if (supportShare(folder)) list.add(FileOperation.Operation.SHARE);
		if (supportDownload(folder)) list.add(FileOperation.Operation.DOWNLOAD);
		if (supportOpen(folder)) list.add(FileOperation.Operation.OPEN);
		if (supportSelect(folder)) list.add(FileOperation.Operation.SELECT);
		list.add(FileOperation.Operation.DETAILS);
		
		return new FileOperation(list.toArray(new FileOperation.Operation[list.size()]));
	}
	
	static FileOperation createSubFolderOperation(IAnyboxFolder folder) {
		if (folder == null) return null;
		
		ArrayList<FileOperation.Operation> list = new ArrayList<FileOperation.Operation>();
		if (supportMove(folder)) list.add(FileOperation.Operation.MOVE);
		if (supportDelete(folder)) list.add(FileOperation.Operation.DELETE);
		if (supportModify(folder)) list.add(FileOperation.Operation.RENAME);
		if (supportSelect(folder)) list.add(FileOperation.Operation.SELECT);
		list.add(FileOperation.Operation.DETAILS);
		
		return new FolderOperation(list.toArray(new FileOperation.Operation[list.size()]));
	}
	
	static FileOperation createFolderOperation(IAnyboxFolder folder) {
		if (folder == null) return null;
		if (isSystemFolder(folder) == false) return null;
		
		ArrayList<FileOperation.Operation> list = new ArrayList<FileOperation.Operation>();
		//if (supportMove(folder)) list.add(FileOperation.Operation.MOVE);
		//if (supportDelete(folder)) list.add(FileOperation.Operation.DELETE);
		//if (supportModify(folder)) list.add(FileOperation.Operation.RENAME);
		list.add(FileOperation.Operation.SELECT);
		list.add(FileOperation.Operation.DETAILS);
		
		return new FolderOperation(list.toArray(new FileOperation.Operation[list.size()]));
	}
	
	static boolean supportOperation(AnyboxSection section, 
			FileOperation.Operation op) {
		if (section == null || op == null) return false;
		IAnyboxFolder parentFolder = (IAnyboxFolder)section.getParent();
		if (section instanceof AnyboxSectionFolder) {
			AnyboxSectionFolder folder = (AnyboxSectionFolder)section;
			if (isSystemFolder(folder)) {
				switch (op) {
				case MOVE:
				case DELETE:
				case RENAME:
				case MODIFY:
					return false;
				default:
					break;
				}
			}
		}
		if (parentFolder == null) return false;
		switch (op) {
		case SELECT:
			return true;
		case MOVE:
			return section.isAccountOwner() && supportMove(parentFolder);
		case DELETE:
			return section.isAccountOwner() && supportDelete(parentFolder);
		case RENAME:
		case MODIFY:
			return section.isAccountOwner() && supportModify(parentFolder);
		case CHANGEPOSTER:
			return section.isAccountOwner();
		case SHARE:
			return !section.isFolder();
		case OPEN: 
			return !section.isFolder();
		case DOWNLOAD:
			return !section.isFolder();
		case DETAILS:
			return true;
		default:
			return false;
		}
	}
	
	static boolean supportOperation(AnyboxLibrary library, 
			FileOperation.Operation op) {
		if (library == null || op == null) return false;
		switch (op) {
		case SELECT:
			return true;
		case MOVE:
			return supportMove(library);
		case DELETE:
			return supportDelete(library);
		case RENAME:
		case MODIFY:
		case CHANGEPOSTER:
			return supportModify(library);
		case SHARE:
			return false;
		case OPEN: 
			return false;
		case DOWNLOAD:
			return false;
		case DETAILS:
			return true;
		default:
			return false;
		}
	}
	
}
