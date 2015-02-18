package org.javenstudio.lightning.core.datum;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.Metadata;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumSectionInfoHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumSectionInfoHandler(core);
	}
	
	public DatumSectionInfoHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		
		String action = trim(req.getParam("action"));
		String key = trim(req.getParam("id"));
		
		if (action == null || action.length() == 0) 
			action = "details";
		
		rsp.add("action", toString(action));
		rsp.add("id", toString(key));
		
		if (action.equalsIgnoreCase("property") || action.equalsIgnoreCase("details")) { 
			handleDefault(req, rsp, user, key, action);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleDefault(Request req, Response rsp, 
			IMember me, String key, String action) throws ErrorException {
		final String accesskey = trim(req.getParam("accesskey"));
		final IData.Access access = action.equalsIgnoreCase("property") ? 
				IData.Access.INFO : IData.Access.DETAILS;
		
		IData data = getData(me, key, access, accesskey);
		if (data == null) {
			throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					"Section: " + key + " not found");
		}
		
		if (data instanceof ISection) {
			ISection section = (ISection)data;
			ILibrary library = section.getLibrary();
			ISectionRoot root = section.getRoot();
			IUser usr = section.getManager().getUser();
			
			String userId = usr.getUserKey();
			String userName = usr.getUserName();
			String userType = IUser.USER;
			String userTitle = usr.getPreference().getNickName();
			String userRole = "";
			
			if (usr instanceof IGroup) {
				IGroup group = (IGroup)usr;
				MemberManager manager = group.getMemberManager();
				if (manager != null) { 
					manager.loadMembers(false);
					
					MemberManager.GroupMember gm = manager.getMember(me.getUserKey());
					if (gm != null) userRole = gm.getRole();
				}
				userType = IUser.GROUP;
			}
			
			String ownerName = section.getOwner();
			String ownerType = IUser.USER;
			String ownerTitle = "";
			
			IUser owner = UserHelper.getLocalUserByName(ownerName);
			if (owner != null) {
				ownerName = owner.getUserName();
				ownerType = owner instanceof IGroup ? IUser.GROUP : IUser.USER;
				ownerTitle = owner.getPreference().getNickName();
			} else {
				ownerName = userName;
				ownerType = userType;
				ownerTitle = userTitle;
			}
			
			String name = section.getName();
			String extension = section.getExtension();
			//String owner = section.getOwner();
			String checksum = section.getChecksum();
			//String path = section.getParentPath();
			String contentType = section.getContentType();
			String parent = section.getParentId();
			String libraryId = library.getContentId();
			String libraryName = library.getName();
			String libraryType = library.getContentType();
			String libraryHostname = library.getHostName();
			String permissions = getPermissions(data);
			String operations = getOperations(data);
			String poster = getPoster(section);
			String background = getBackground(section);
			
			String parentId = libraryId;
			String parentType = libraryType;
			String parentName = libraryName;
			
			if (parent != null && parent.length() > 0) {
				IData parentData = getData(me, parent, access, accesskey);
				if (parentData != null && parentData instanceof ISection) {
					ISection parentSec = (ISection)parentData;
					parentId = parentSec.getContentId();
					parentType = parentSec.getContentType();
					parentName = parentSec.getName();
				}
			}
			
			String rootId = "";
			String rootType = "";
			String rootName = "";
			
			if (root != null) {
				rootId = root.getContentId();
				rootType = root.getContentType();
				rootName = root.getName();
			}
			
			String libname = section.getLibrary().getName();
			String path = section.getParentPath();
			
			int subcount = 0;
			long sublen = 0;
			if (section instanceof ISectionRoot) { 
				ISectionRoot rt = (ISectionRoot)section;
				subcount = rt.getTotalFileCount() + rt.getTotalFolderCount();
				sublen = rt.getTotalFileLength();
			} else { 
				subcount = section.getSubCount();
				sublen = section.getSubLength();
			}
			
			path = "/" + libname + path;
			
			rsp.add("name", toString(name));
			rsp.add("hostname", toString(libraryHostname));
			rsp.add("type", toString(contentType));
			rsp.add("perms", toString(permissions));
			rsp.add("ops", toString(operations));
			rsp.add("poster", toString(poster));
			rsp.add("background", toString(background));
			
			rsp.add("parent_id", toString(parentId));
			rsp.add("parent_name", toString(parentName));
			rsp.add("parent_type", toString(parentType));
			
			rsp.add("root_id", toString(rootId));
			rsp.add("root_name", toString(rootName));
			rsp.add("root_type", toString(rootType));
			
			rsp.add("library_id", toString(libraryId));
			rsp.add("library_name", toString(libraryName));
			rsp.add("library_type", toString(libraryType));
			
			rsp.add("userid", toString(userId));
			rsp.add("username", toString(userName));
			rsp.add("usertype", toString(userType));
			rsp.add("usertitle", toString(userTitle));
			rsp.add("owner", toString(ownerName));
			rsp.add("ownertype", toString(ownerType));
			rsp.add("ownertitle", toString(ownerTitle));
			rsp.add("gmrole", toString(userRole));
			rsp.add("checksum", toString(checksum));
			
			rsp.add("isfolder", section.isFolder());
			rsp.add("extname", toString(extension));
			rsp.add("path", toString(FsUtils.normalizePath(path)));
			rsp.add("length", section.getContentLength());
			rsp.add("width", section.getWidth());
			rsp.add("height", section.getHeight());
			rsp.add("timelen", section.getDuration());
			//rsp.add("ctime", section.getCreatedTime());
			rsp.add("mtime", section.getModifiedTime());
			rsp.add("itime", section.getIndexedTime());
			rsp.add("subcount", subcount);
			rsp.add("sublen", sublen);
			
			if (action.equalsIgnoreCase("property")) {
				NamedMap<Object> info = new NamedMap<Object>();
				getSectionFileInfo(usr, section, info);
				
				rsp.add("details", info);
			} else { 
				NamedMap<Object> info = new NamedMap<Object>();
				getSectionMetaInfo(usr, section, info);
				
				rsp.add("details", info);
			}
			
			UserHelper.addHistory(me, data, "view");
		} else {
			throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					"Section: " + key + " is wrong");
		}
	}
	
	protected void getSectionMetaInfo(IUser usr, ISection item, 
			NamedMap<Object> info) throws ErrorException {
		if (usr == null || item == null) return;
		
		NamedMap<Object> fileInfo = new NamedMap<Object>();
		NamedMap<Object> metaInfo = new NamedMap<Object>();
		
		Map<String,Object> metas = new HashMap<String,Object>();
		item.getMetaInfo(metas);
		getMetaInfo(metaInfo, metas, true);
		
		String userId = usr.getUserKey();
		String userName = usr.getUserName();
		String owner = item.getOwner();
		String checksum = item.getChecksum();
		String libname = item.getLibrary().getName();
		String path = item.getParentPath();
		String extension = item.getExtension();
		String permissions = getPermissions(item);
		
		int subcount = 0;
		long sublen = 0;
		if (item instanceof ISectionRoot) { 
			ISectionRoot root = (ISectionRoot)item;
			subcount = root.getTotalFileCount() + root.getTotalFolderCount();
			sublen = root.getTotalFileLength();
		} else { 
			subcount = item.getSubCount();
			sublen = item.getSubLength();
		}
		
		path = "/" + libname + path;
		
		fileInfo.add("name", toString(item.getName()));
		fileInfo.add("isfolder", item.isFolder());
		fileInfo.add("type", toString(item.getContentType()));
		fileInfo.add("length", item.getContentLength());
		fileInfo.add("extname", toString(extension));
		fileInfo.add("path", toString(FsUtils.normalizePath(path)));
		//fileInfo.add("ctime", item.getCreatedTime());
		fileInfo.add("mtime", item.getModifiedTime());
		fileInfo.add("itime", item.getIndexedTime());
		fileInfo.add("subcount", subcount);
		fileInfo.add("sublen", sublen);
		fileInfo.add("perms", toString(permissions));
		fileInfo.add("userid", toString(userId));
		fileInfo.add("username", toString(userName));
		fileInfo.add("owner", toString(owner));
		fileInfo.add("checksum", toString(checksum));
		
		info.add("id", item.getContentId());
		info.add("file", fileInfo);
		info.add("metadata", metaInfo);
		info.add("posters", item.getPosters());
		info.add("backgrounds", item.getBackgrounds());
	}
	
	protected void getSectionFileInfo(IUser usr, ISection item, 
			NamedMap<Object> info) throws ErrorException {
		if (usr == null || item == null) return;
		
		NamedMap<Object> fileInfo = new NamedMap<Object>();
		NamedMap<Object> mediaInfo = new NamedMap<Object>();
		
		Map<String,Object> metas = new LinkedHashMap<String,Object>();
		item.getMetaTag(metas);
		getMetaTags(mediaInfo, metas);
		getMetaInfo(mediaInfo, metas, false);
		
		String userId = usr.getUserKey();
		String userName = usr.getUserName();
		String owner = item.getOwner();
		String checksum = item.getChecksum();
		String libname = item.getLibrary().getName();
		String path = item.getParentPath();
		String extension = item.getExtension();
		String permissions = getPermissions(item);
		
		int subcount = 0;
		long sublen = 0;
		if (item instanceof ISectionRoot) { 
			ISectionRoot root = (ISectionRoot)item;
			subcount = root.getTotalFileCount() + root.getTotalFolderCount();
			sublen = root.getTotalFileLength();
		} else { 
			subcount = item.getSubCount();
			sublen = item.getSubLength();
		}
		
		path = "/" + libname + path;
		
		fileInfo.add("name", toString(item.getName()));
		fileInfo.add("isfolder", item.isFolder());
		fileInfo.add("type", toString(item.getContentType()));
		fileInfo.add("length", item.getContentLength());
		fileInfo.add("extname", toString(extension));
		fileInfo.add("path", toString(FsUtils.normalizePath(path)));
		//fileInfo.add("ctime", item.getCreatedTime());
		fileInfo.add("mtime", item.getModifiedTime());
		fileInfo.add("itime", item.getIndexedTime());
		fileInfo.add("subcount", subcount);
		fileInfo.add("sublen", sublen);
		fileInfo.add("perms", permissions);
		fileInfo.add("userid", toString(userId));
		fileInfo.add("username", toString(userName));
		fileInfo.add("owner", toString(owner));
		fileInfo.add("checksum", toString(checksum));
		
		info.add("id", toString(item.getContentId()));
		info.add("file", fileInfo);
		info.add("media", mediaInfo);
	}
	
	private void getMetaInfo(NamedMap<Object> metaInfo, 
			Map<String,Object> metas, boolean tolowercase) { 
		for (Map.Entry<String, Object> entry : metas.entrySet()) { 
			String name = entry.getKey();
			Object value = entry.getValue();
			
			if (name == null || name.length() == 0)
				continue;
			
			if (tolowercase)
				name = name.toLowerCase();
			
			if (value != null)
				metaInfo.add(name, value);
		}
	}

	private void getMetaTags(NamedMap<Object> mediaInfo, 
			Map<String,Object> metas) { 
		//addImageMeta(mediaInfo, metas, "Exif SubIFD Date/Time Original", "Date Taken");
		//addImageMeta(mediaInfo, metas, "Exif IFD0 Make", "Make");
		//addImageMeta(mediaInfo, metas, "Exif IFD0 Model", "Model");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD Aperture Value", "Aperture");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD ISO Speed Ratings", "ISO");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD White Balance Mode", "White Balance");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD Exposure Time", "Exposure Time");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD Focal Length", "Focal Length");
		//addImageMeta(mediaInfo, metas, "Exif SubIFD Flash", "Flash");
		
		for (String[] tags : Metadata.EXIF_TAGS) { 
			if (tags == null || tags.length <= 1) 
				continue;
			
			String name = tags[0];
			Object value = null;
			
			if (name == null || name.length() == 0)
				continue;
			
			for (int i=1; i < tags.length; i++) { 
				String tag = tags[i];
				if (tag != null && tag.length() > 0) { 
					value = metas.get(tag);
					if (value != null)
						break;
				}
			}
			
			if (value != null)
				mediaInfo.add(name, value);
		}
	}
	
	@SuppressWarnings("unused")
	private void addImageMeta(NamedMap<Object> mediaInfo, 
			Map<String,Object> metas, String key, String name) { 
		if (key == null || key.length() == 0 || 
			name == null || name.length() == 0) 
			return;
		
		Object val = metas.get(key);
		if (val != null) 
			mediaInfo.add(name, val);
	}
	
}
