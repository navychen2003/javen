package org.javenstudio.lightning.core.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.IFileInfo;
import org.javenstudio.falcon.datum.IFolderInfo;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumFolderHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumFolderHandler(core);
	}
	
	public DatumFolderHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String accesskey = trim(req.getParam("accesskey"));
		String key = trim(req.getParam("id"));
		String type = trim(req.getParam("type"));
		
		NamedList<Object> drivers = new NamedMap<Object>();
		NamedList<Object> folders = new NamedMap<Object>();
		
		IFolderInfo[] roots = null;
		IFolderInfo selectedFolder = null;
		
		final boolean mustWritable;
		if (type != null && type.equalsIgnoreCase("move"))
			mustWritable = true;
		else
			mustWritable = false;
		
		final IFolderInfo.Filter filter = new IFolderInfo.Filter() {
				@Override
				public boolean accept(IFileInfo file) {
					return file != null && (!mustWritable || file.canWrite());
				}
			};
		
		if (type != null && type.equalsIgnoreCase("local")) { 
			if (!user.isManager()) { 
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Access Denied");
			}
			
			roots = SectionHelper.listLocalRoots(getCore().getConfiguration());
			selectedFolder = SectionHelper.getLocalFolder(key);
			
			if (selectedFolder == null)
				selectedFolder = getDefaultFolder(roots);
			
		} else { 
			roots = SectionHelper.listRoots(getManager(user));
			
			IData data = getData(user, key, IData.Access.LIST, accesskey);
			if (data != null) { 
				if (data instanceof ILibrary) {
					ILibrary library = (ILibrary)data;
					selectedFolder = library.getFolderInfo();
					
				} else if (data instanceof ISection && data instanceof IFileInfo) { 
					//ISection section = (ISection)data;
					IFileInfo info = (IFileInfo)data;
					if (info != null && info instanceof IFolderInfo)
						selectedFolder = (IFolderInfo)info;
				}
				
				if (selectedFolder != null) {
					if (mustWritable && !selectedFolder.canWrite())
						selectedFolder = null;
				}
			}
			
			if (selectedFolder == null) {
				selectedFolder = getDefaultFolder(roots);
			
				if (selectedFolder != null && selectedFolder.isRootFolder()) { 
					IFolderInfo[] infos = selectedFolder.listFolderInfos(true, filter);
					for (int i=0; infos != null && i < infos.length; i++) { 
						IFolderInfo folder = infos[i];
						if (folder != null) {
							selectedFolder = folder;
							break;
						}
					}
				}
			}
		}
		
		NamedList<Object> selectedInfo = null;
		IFolderInfo selectedRoot = null;
		
		if (selectedFolder != null) { 
			selectedRoot = selectedFolder.getRootInfo();
			selectedInfo = getFolderInfo(selectedFolder, true);
			
			IFolderInfo[] infos = selectedFolder.listFolderInfos(true, filter);
			
			IFolderInfo backFolder = selectedFolder.getParentInfo();
			if (backFolder != null) { 
				NamedList<Object> backInfo = getFolderInfo(backFolder, "..", false);
				if (backInfo != null) 
					folders.add(backFolder.getContentId(), backInfo);
			}
			
			for (int i=0; infos != null && i < infos.length; i++) { 
				IFolderInfo folder = infos[i];
				NamedList<Object> info = getFolderInfo(folder, false);
				if (folder != null && info != null)
					folders.add(folder.getContentId(), info);
			}
		}
		
		if (selectedInfo == null) 
			selectedInfo = new NamedMap<Object>();
		
		for (int i=0; roots != null && i < roots.length; i++) { 
			IFolderInfo folder = roots[i];
			if (folder == null) continue;
			
			boolean selected = folder.equals(selectedRoot);
			NamedList<Object> info = getFolderInfo(folder, selected);
			
			if (folder != null && info != null)
				drivers.add(folder.getContentId(), info);
		}
		
		rsp.add("selected", selectedInfo);
		rsp.add("drivers", drivers);
		rsp.add("folders", folders);
	}

	private IFolderInfo getDefaultFolder(IFolderInfo[] roots) { 
		IFolderInfo selectedFolder = null;
		if (selectedFolder == null) { 
			IFolderInfo first = null;
			
			for (int i=0; roots != null && i < roots.length; i++) { 
				IFolderInfo folder = roots[i];
				if (folder == null) continue;
				if (first == null) first = folder;
				if (folder.isHomeFolder()) { 
					selectedFolder = folder;
					break;
				}
			}
			
			if (selectedFolder == null) 
				selectedFolder = first;
		}
		return selectedFolder;
	}
	
	protected NamedList<Object> getFolderInfo(IFolderInfo item, 
			boolean selected) throws ErrorException { 
		return getFolderInfo(item, null, selected);
	}
	
	protected NamedList<Object> getFolderInfo(IFolderInfo item, 
			String name, boolean selected) throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		String path = item.getContentPath();
		if (path == null) return null;
		
		String dataDir = FsUtils.normalizePath(path);
		
		if (name == null || name.length() == 0)
			name = item.getName();
		
		if (name == null || name.length() == 0)
			name = dataDir;
		
		info.add("id", item.getContentId());
		info.add("name", name);
		info.add("type", item.getContentType());
		info.add("path", dataDir);
		info.add("ishome", item.isHomeFolder());
		info.add("selected", selected);
		
		return info;
	}
	
}
