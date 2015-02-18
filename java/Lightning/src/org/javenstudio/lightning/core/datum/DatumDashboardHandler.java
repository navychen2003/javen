package org.javenstudio.lightning.core.datum;

import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.StoreInfo;
import org.javenstudio.falcon.setting.cluster.ILibraryInfo;
import org.javenstudio.falcon.setting.cluster.IStorageInfo;
import org.javenstudio.falcon.setting.cluster.StorageManager;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.user.global.Announcement;
import org.javenstudio.falcon.user.global.AnnouncementManager;
import org.javenstudio.falcon.user.profile.HistoryItem;
import org.javenstudio.falcon.user.profile.HistoryManager;
import org.javenstudio.falcon.user.profile.HistorySection;
import org.javenstudio.falcon.user.profile.HistorySectionSet;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.user.UserClusterHandler;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class DatumDashboardHandler extends DatumHandlerBase {

	public static RequestHandler createHandler(DatumCore core) { 
		return new DatumDashboardHandler(core);
	}
	
	public DatumDashboardHandler(DatumCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IUserClient client = UserHelper.checkUserClient(req, IUserClient.Op.ACCESS);
		String lang = trim(req.getParam("lang"));
		String action = trim(req.getParam("action"));
		int subfilecount = parseInt(req.getParam("subfilecount"));
		
		if (action == null || action.length() == 0)
			action = "library";
		
		if (action.equalsIgnoreCase("history")) {
			rsp.add("histories", getHistoryItems(req, client));
			
		} else if (action.equalsIgnoreCase("library")) {
			rsp.add("stores", getStores());
			rsp.add("libraries", getLibraries(client, subfilecount));
			rsp.add("storages", getStorageNodes(client, subfilecount));
			rsp.add("announcements", getAnnouncements(client, lang));
			
		} else if (action.equalsIgnoreCase("list")) {
			rsp.add("stores", getStores());
			rsp.add("libraries", getLibraries(client, subfilecount));
			rsp.add("storages", getStorageNodes(client, subfilecount));
			rsp.add("announcements", getAnnouncements(client, lang));
			rsp.add("histories", getHistoryItems(req, client));
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}

	private NamedList<Object> getHistoryItems(Request req, IUserClient client) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		int count = parseInt(req.getParam("count"));
		if (count <= 0) count = 50;
		
		int totalCount = 0;
		
		if (client != null) {
			HistoryManager manager = client.getUser().getHistoryManager();
			if (manager != null) {
				manager.loadHistoryItems(false);
				
				HistorySectionSet sections = manager.getHistorySections(0, count);
				if (sections != null) {
					totalCount = (int)sections.getTotalCount();
					
					NamedList<Object> items = new NamedMap<Object>();
					
					for (int i=0; i < sections.getSectionCount(); i++) {
						HistorySection section = sections.getSectionAt(i);
						NamedList<Object> itemInfo = getHistorySectionInfo(section);
						
						if (section != null && itemInfo != null) 
							items.add(section.getContentId(), itemInfo);
					}
					
					info.add("sections", items);
				}
			}
		}
		
		info.add("count", count);
		info.add("totalcount", totalCount);
		
		return info;
	}
	
	private NamedList<Object> getAnnouncements(IUserClient client, String lang) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		if (lang == null || lang.length() == 0) {
			if (client != null) lang = client.getLanguage();
		}
		
		AnnouncementManager manager = UserManager.getInstance().getAnnouncementManager();
		if (manager != null) { 
			manager.loadAnnouncements(false);
			lang = Strings.getInstance().getLanguage(lang);
			
			Announcement[] annos = manager.getAnnouncements(lang);
			if (annos != null) {
				Arrays.sort(annos, new Comparator<Announcement>() {
						@Override
						public int compare(Announcement o1, Announcement o2) {
							long t1 = o1 != null ? o1.getModifiedTime() : 0;
							long t2 = o2 != null ? o2.getModifiedTime() : 0;
							return t1 > t2 ? -1 : (t1 < t2 ? 1 : 0);
						}
					});
				
				for (int i=0; annos != null && i < annos.length; i++) { 
					Announcement anno = annos[i];
					if (anno == null) continue;
					
					String title = anno.getTitle();
					String body = anno.getBody();
					
					if (title == null || title.length() == 0) continue;
					if (body == null || body.length() == 0) continue;
					
					NamedList<Object> info = getAnnouncementInfo(anno);
					if (info != null) { 
						items.add(anno.getKey(), info);
						if (items.size() >= 3) break;
					}
				}
			}
		}
		
		return items;
	}
	
	private NamedList<Object> getLibraries(IUserClient client, 
			int subfilecount) throws ErrorException { 
		return getLibraries(client != null ? client.getUser() : (IMember)null, 
				subfilecount);
	}
	
	private NamedList<Object> getLibraries(IMember user, 
			int subfilecount) throws ErrorException { 
		return getLibraries(user != null ? getManager(user) : (DataManager)null, 
				subfilecount);
	}
	
	public static NamedList<Object> getLibraries(DataManager manager, 
			int subfilecount) throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		if (manager != null) {
			ILibrary[] libraries = manager.getLibraries();
			
			for (int i=0; libraries != null && i < libraries.length; i++) { 
				ILibrary lib = libraries[i];
				NamedList<Object> info = getLibraryInfo(lib, subfilecount, false);
				if (lib != null && info != null) 
					items.add(lib.getContentId(), info);
			}
		}
		
		return items;
	}
	
	private NamedList<Object> getStorageNodes(IUserClient client, 
			int subfilecount) throws ErrorException { 
		return getStorageNodes(client != null ? client.getUser() : (IMember)null, 
				subfilecount);
	}
	
	private NamedList<Object> getStorageNodes(IMember user, 
			int subfilecount) throws ErrorException { 
		return getStorageNodes(user != null ? getStorageManager(user) : (StorageManager)null, 
				subfilecount);
	}
	
	public static NamedList<Object> getStorageNodes(StorageManager manager, 
			int subfilecount) throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		if (manager != null) {
			IStorageInfo[] storages = manager.getStorages();
			
			for (int i=0; storages != null && i < storages.length; i++) { 
				IStorageInfo storage = storages[i];
				if (storage == null) continue;
				NamedList<Object> storageInfo = new NamedMap<Object>();
				
				NamedList<Object> hostInfo = UserClusterHandler.getHostInfo(storage.getHostNode(), IUser.ShowType.DEFAULT);
				storageInfo.add("host", hostInfo);
				
				NamedList<Object> userInfo = NamedHelper.toNamedMap(storage.getStorageUser());
				storageInfo.add("user", userInfo);
				
				NamedList<Object> librariesInfo = getStorageLibraries(storage, subfilecount);
				storageInfo.add("libraries", librariesInfo);
				
				items.add(storage.getHostNode().getHostKey(), storageInfo);
			}
		}
		
		return items;
	}
	
	public static NamedList<Object> getStorageLibraries(IStorageInfo storage, 
			int subfilecount) throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		if (storage != null) {
			ILibraryInfo[] libraries = storage.getStorageLibraries();
			
			for (int i=0; libraries != null && i < libraries.length; i++) { 
				ILibraryInfo lib = libraries[i];
				NamedList<Object> info = getLibraryInfo(storage, lib, subfilecount, false);
				if (lib != null && info != null) 
					items.add(lib.getContentId(), info);
			}
		}
		
		return items;
	}
	
	private NamedList<Object> getStores() throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		StoreInfo[] stores = getCore().getStoreInfos();
		for (int i=0; stores != null && i < stores.length; i++) { 
			StoreInfo store = stores[i];
			if (store != null)
				items.add(store.getStoreUri(), store.getStoreUri());
		}
		
		return items;
	}
	
	static NamedList<Object> getAnnouncementInfo(Announcement item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("key", toString(item.getKey()));
			info.add("lang", toString(item.getLanguage()));
			info.add("title", toString(item.getTitle()));
			info.add("link", toString(item.getLink()));
			info.add("body", toString(item.getBody()));
			info.add("poster", toString(item.getPoster()));
			info.add("mtime", item.getModifiedTime());
		}
		
		return info;
	}
	
	static NamedList<Object> getHistorySectionInfo(HistorySection item) 
			throws ErrorException {
		if (item == null) return null;
		NamedList<Object> info = getSectionInfo(item.getSection());
		
		if (info != null) {
			HistoryItem hi = item.getItem();
			if (hi != null) {
				NamedList<Object> hinfo = new NamedMap<Object>();
				hinfo.add("time", hi.getTime());
				hinfo.add("id", toString(hi.getContentId()));
				hinfo.add("type", toString(hi.getContentType()));
				hinfo.add("link", toString(hi.getLink()));
				hinfo.add("title", toString(hi.getTitle()));
				hinfo.add("owner", toString(hi.getOwner()));
				hinfo.add("op", toString(hi.getOperation()));
				
				info.add("history", hinfo);
			}
			
			IUser owner = item.getOwner();
			if (owner != null) {
				long now = System.currentTimeMillis();
				int idleSecs = (int)(now - owner.getAccessTime()) / 1000;
				
				NamedList<Object> oinfo = new NamedMap<Object>();
				oinfo.add("key", toString(owner.getUserKey()));
				oinfo.add("name", toString(owner.getUserName()));
				oinfo.add("mailaddr", toString(owner.getUserEmail()));
				oinfo.add("nick", toString(owner.getPreference().getNickName()));
				oinfo.add("category", toString(owner.getPreference().getCategory()));
				oinfo.add("type", toString(IUser.Util.typeOfUser(owner)));
				oinfo.add("flag", toString(IUser.Util.stringOfFlag(owner.getUserFlag())));
				oinfo.add("avatar", toString(owner.getPreference().getAvatar()));
				oinfo.add("background", toString(owner.getPreference().getBackground()));
				oinfo.add("idle", toString(Integer.toString(idleSecs) + 's'));
				
				info.add("user", oinfo);
			}
		}
		
		return info;
	}
	
}
