package org.javenstudio.lightning.core.datum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.data.FileContentStream;
import org.javenstudio.falcon.datum.data.FileStream;
import org.javenstudio.falcon.datum.data.SqHelper;
import org.javenstudio.falcon.datum.data.SqLibrary;
import org.javenstudio.falcon.datum.data.SqRoot;
import org.javenstudio.falcon.datum.data.SqRootDir;
import org.javenstudio.falcon.datum.data.SqRootHelper;
import org.javenstudio.falcon.datum.data.SqSection;
import org.javenstudio.falcon.datum.data.recycle.SqRecycleRoot;
import org.javenstudio.falcon.datum.data.share.SqShareRoot;
import org.javenstudio.falcon.datum.data.upload.SqUploadRoot;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.util.StringUtils;

public class DatumUploadHandler extends DatumHandlerBase {
	//private static final Logger LOG = Logger.getLogger(DatumUploadHandler.class);

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumUploadHandler(core);
	}
	
	public DatumUploadHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		String action = trim(req.getParam("action"));
		String target = trim(req.getParam("target"));
		
		int count = 0;
		
		if (action == null || action.length() == 0) 
			action = "upload";
		
		if (action.equalsIgnoreCase("upload")) { 
			count = actionUpload(req, user, target);
			
		} else if (action.equalsIgnoreCase("newfolder")) { 
			String folderName = StringUtils.trim(req.getParam("foldername"));
			boolean res = actionNewfolder(req, user, target, folderName);
			count = res ? 1 : 0;
			
		} else if (action.equalsIgnoreCase("move")) { 
			count = actionMove(req, user, target, false);
			
		} else if (action.equalsIgnoreCase("copy")) { 
			count = actionMove(req, user, target, true);
			
		} else if (action.equalsIgnoreCase("trash")) { 
			count = actionTrash(req, user);
			
		} else if (action.equalsIgnoreCase("delete")) { 
			count = actionDelete(req, user);
			
		} else if (action.equalsIgnoreCase("empty")) { 
			count = actionEmpty(req, user);
			
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
		
		rsp.add("action", action);
		rsp.add("result", count > 0 ? "success" : "error");
		rsp.add("count", count);
	}
	
	static int saveHistory(IMember user, String[] contentIds, String[] removeIds) {
		if (user == null || contentIds == null) return 0;
		
		UserHelper.addHistory(user, contentIds, removeIds, "upload");
		
		return contentIds.length;
	}
	
	static int actionSave(IMember me, IUser user, IData data, FileStream[] streams) 
			throws ErrorException { 
		if (data != null) {
			if (!data.canWrite()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Folder: " + data.getName() + " cannot save file");
			}
			
			if (data instanceof SqLibrary) { 
				String[] contentIds = SqRootHelper.saveFiles(user, (SqLibrary)data, streams);
				return saveHistory(me, contentIds, null);
				
			} else if (data instanceof SqRoot) {
				String[] contentIds = SqRootHelper.saveFiles(user, (SqRoot)data, streams);
				return saveHistory(me, contentIds, null);
				
			} else if (data instanceof SqRootDir) {
				String[] contentIds = SqRootHelper.saveFiles(user, (SqRootDir)data, streams);
				return saveHistory(me, contentIds, null);
				
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Save folder is wrong");
	}
	
	private int actionUpload(Request req, IMember user, String target) 
			throws ErrorException { 
		//if (user.getUsableSpace() <= 0) {
		//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//			"User: " + user.getUserName() + " has no enough space");
		//}
		
		String accesskey = trim(req.getParam("accesskey"));
		IData data = getData(user, target, IData.Access.UPDATE, accesskey);
		
		if (data != null) {
			IUser usr = data.getManager().getUser();
			if (usr.getUsableSpace() <= 0) {
				if (usr instanceof IGroup) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Group: " + usr.getUserName() + " has no enough space");
				} else {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + usr.getUserName() + " has no enough space");
				}
			}
			if (!data.canWrite()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Upload target: " + data.getName() + " cannot save file");
			}
			
			if (data instanceof SqLibrary) { 
				String[] contentIds = SqRootHelper.saveFiles(user, (SqLibrary)data, 
						FileContentStream.createStreams(req.getContentStreams(), 
								getCore().getMetadataLoader()));
				
				return saveHistory(user, contentIds, null);
				
			} else if (data instanceof SqRoot) {
				String[] contentIds = SqRootHelper.saveFiles(user, (SqRoot)data, 
						FileContentStream.createStreams(req.getContentStreams(), 
								getCore().getMetadataLoader()));
				
				return saveHistory(user, contentIds, null);
				
			} else if (data instanceof SqRootDir) {
				String[] contentIds = SqRootHelper.saveFiles(user, (SqRootDir)data, 
						FileContentStream.createStreams(req.getContentStreams(), 
								getCore().getMetadataLoader()));
				
				return saveHistory(user, contentIds, null);
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Upload target is wrong");
	}
	
	private boolean actionNewfolder(Request req, IMember user, 
			String target, String folderName) throws ErrorException { 
		String accesskey = trim(req.getParam("accesskey"));
		IData data = getData(user, target, IData.Access.UPDATE, accesskey);
		
		if (data != null) {
			if (!data.canWrite()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"New folder target: " + data.getName() + " cannot create folder");
			}
			
			if (data instanceof SqLibrary) { 
				return SqHelper.addArchiveRoot((SqLibrary)data, folderName);
				
			} else if (data instanceof SqRoot) { 
				return SqRootHelper.newFolder(user, (SqRoot)data, folderName);
				
			} else if (data instanceof SqRootDir) {
				return SqRootHelper.newFolder(user, (SqRootDir)data, folderName);
				
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"New folder target is wrong");
	}
	
	private int actionMove(Request req, IMember user, 
			String target, boolean copyOnly) throws ErrorException { 
		String accesskey = trim(req.getParam("accesskey"));
		String[] ids = req.getParams("id");
		SqSection[] sections = getSections(user, ids, accesskey);
		
		IData data = getData(user, target, IData.Access.UPDATE, accesskey);
		return actionMove(user, data, sections, copyOnly);
	}
	
	private int actionTrash(Request req, IMember user) throws ErrorException { 
		String accesskey = trim(req.getParam("accesskey"));
		String[] ids = req.getParams("id");
		SqSection[] sections = getSections(user, ids, accesskey);
		
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Trash items is empty");
		}
		
		SqLibrary library = null;
		
		for (SqSection section : sections) { 
			if (section == null || !section.canMove()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Trash item: " + section.getName() + " cannot be moved");
			}
			
			if (library == null) { 
				library = section.getLibrary();
			} else if (library != section.getLibrary()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Trash items must be in the same library");
			}
		}
		
		if (library == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Trash items must be in a library");
		}
		
		SqRecycleRoot recycleRoot = SqRootHelper.getRecycleRoot(library);
		if (recycleRoot == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Trash items's library must has a recycle root");
		}
		
		return actionMove(user, recycleRoot, sections, false);
	}
	
	private int actionMove(IMember user, IData data, SqSection[] sections, 
			boolean copyOnly) throws ErrorException { 
		if (data != null) {
			if (!data.canWrite()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						(copyOnly?"Copy":"Move") + " to target: " + data.getName() + " cannot save file");
			}
			
			//String[] ids = req.getParams("id");
			//SqSection[] sections = getSections(userKey, ids);
			
			if (sections == null || sections.length == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						(copyOnly?"Copy":"Move") + " items is empty");
			}
			
			Map<String, String> keyIds = new HashMap<String, String>();
			
			for (SqSection section : sections) { 
				if (section == null) continue;
				if (copyOnly) { 
					if (!section.canCopy()) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Selected item: " + section.getName() + " cannot be copied");
					}
				} else if (!section.canMove()) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Selected item: " + section.getName() + " cannot be moved");
				}
				
				keyIds.put(section.getContentKey(), section.getContentId());
			}
			
			if (data instanceof SqRoot) { 
				String[] contentIds = SqRootHelper.moveFilesTo(user, (SqRoot)data, sections, copyOnly);
				Set<String> removeIds = new HashSet<String>();
				if (contentIds != null) {
					for (String contentId : contentIds) {
						if (contentId != null && contentId.length() >= 8) {
							String key = contentId.substring(contentId.length() - 8);
							String removeId = keyIds.get(key);
							if (removeId != null && removeId.length() > 0)
								removeIds.add(removeId);
						}
					}
				}
				
				return saveHistory(user, contentIds, removeIds.toArray(new String[removeIds.size()]));
				
			} else if (data instanceof SqRootDir) {
				String[] contentIds = SqRootHelper.moveFilesTo(user, (SqRootDir)data, sections, copyOnly);
				Set<String> removeIds = new HashSet<String>();
				if (contentIds != null) {
					for (String contentId : contentIds) {
						if (contentId != null && contentId.length() >= 8) {
							String key = contentId.substring(contentId.length() - 8);
							String removeId = keyIds.get(key);
							if (removeId != null && removeId.length() > 0)
								removeIds.add(removeId);
						}
					}
				}
				
				return saveHistory(user, contentIds, removeIds.toArray(new String[removeIds.size()]));
			}
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				(copyOnly?"Copy":"Move") + " to target is wrong");
	}
	
	private int actionDelete(Request req, IMember user) throws ErrorException { 
		String accesskey = trim(req.getParam("accesskey"));
		String[] ids = req.getParams("id");
		SqSection[] sections = getSections(user, ids, accesskey);
		return deleteSections(user, sections);
	}
	
	static int deleteSections(IMember user, SqSection[] sections) 
			throws ErrorException {
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Delete items is empty");
		}
		
		for (SqSection section : sections) { 
			if (section == null) continue;
			if (section instanceof SqRecycleRoot) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Recycle bin cannot be deleted");
			}
			if (section instanceof SqRoot) { 
				SqRoot root = (SqRoot)section;
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
							"Selected root: " + root.getName() + " is not empty");
				}
			}
			if (!section.canDelete()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Selected item: " + section.getName() + " cannot be deleted");
			}
		}
		
		return SqRootHelper.deleteFiles(user, sections);
	}
	
	private int actionEmpty(Request req, IMember user) throws ErrorException { 
		String accesskey = trim(req.getParam("accesskey"));
		String[] ids = req.getParams("id");
		SqSection[] sections = getSections(user, ids, accesskey);
		
		if (sections == null || sections.length == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Folders to clean up is empty");
		}
		
		int count = 0;
		
		for (SqSection section : sections) { 
			if (section == null) continue;
			Set<SqSection> list = new HashSet<SqSection>();
			
			if (section instanceof SqRoot) { 
				SqRootHelper.listFiles(user, (SqRoot)section, list, true);
				
			} else if (section instanceof SqRootDir) {
				SqRootHelper.listFiles(user, (SqRootDir)section, list, true);
				
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Section: " + section.getName() + " cannot be clean up");
			}
			
			SqSection[] items = list.toArray(new SqSection[list.size()]);
			count += deleteTrash(user, section, items);
		}
		
		return count;
	}
	
	private int deleteTrash(IMember user, SqSection dir, 
			SqSection[] items) throws ErrorException {
		if (items == null || items.length == 0)
			return 0;
		
		SqRoot root = dir.getRoot();
		
		if (root instanceof SqRecycleRoot) {
			for (SqSection item : items) { 
				if (item == null) continue;
				if (!item.canDelete()) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Item: " + item.getName() + " cannot be deleted");
				}
			}
			
			return SqRootHelper.deleteFiles(user, items);
		} else {
			for (SqSection item : items) { 
				if (item == null) continue;
				if (!item.canMove()) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Item: " + item.getName() + " cannot be moved");
				}
			}
			
			SqLibrary library = root.getLibrary();
			SqRecycleRoot recycleRoot = SqRootHelper.getRecycleRoot(library);
			if (recycleRoot == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Trash items's library must has a recycle root");
			}
			
			return actionMove(user, recycleRoot, items, false);
		}
	}
	
	private SqSection[] getSections(IMember user, String[] ids, 
			String accesskey) throws ErrorException { 
		ArrayList<SqSection> list = new ArrayList<SqSection>();
		
		for (int i=0; ids != null && i < ids.length; i++) { 
			IData data = getData(user, ids[i], IData.Access.UPDATE, accesskey);
			if (data != null && data instanceof SqSection)
				list.add((SqSection)data);
		}
		
		return list.toArray(new SqSection[list.size()]);
	}
	
}
