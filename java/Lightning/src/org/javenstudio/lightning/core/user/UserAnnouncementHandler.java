package org.javenstudio.lightning.core.user;

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.global.Announcement;
import org.javenstudio.falcon.user.global.AnnouncementManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserAnnouncementHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserAnnouncementHandler(core);
	}
	
	public UserAnnouncementHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IUserClient client = UserHelper.checkUserClient(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, client);
		} else if (action.equalsIgnoreCase("update")) { 
			handleUpdate(req, rsp, client);
		} else if (action.equalsIgnoreCase("delete")) { 
			handleDelete(req, rsp, client);
		} else if (action.equalsIgnoreCase("info")) { 
			handleInfo(req, rsp, client);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IUserClient client) throws ErrorException { 
		if (req == null || rsp == null || client == null)
			return;
		
		String lang = trim(req.getParam("lang"));
		if (lang == null || lang.length() == 0)
			lang = client.getLanguage();
		else if (lang.equalsIgnoreCase("all"))
			lang = null;
		
		NamedList<Object> list = new NamedMap<Object>();
		
		AnnouncementManager manager = client.getUser()
				.getUserManager().getAnnouncementManager();
		
		if (manager != null) { 
			manager.loadAnnouncements(false);
			
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
					
					NamedList<Object> info = getAnnouncementInfo(anno);
					if (info != null) list.add(anno.getKey(), info);
				}
			}
		}
		
		rsp.add("lang", toString(lang));
		rsp.add("langs", UserHeartbeatHandler.getLangList());
		rsp.add("announcements", list);
	}
	
	static void handleUpdate(Request req, Response rsp, 
			IUserClient client) throws ErrorException { 
		if (req == null || rsp == null || client == null)
			return;
		
		if (!client.getUser().isManager()) { 
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"No permission to update announcement");
		}
		
		String key = trim(req.getParam("key"));
		String title = trim(req.getParam("title"));
		String link = trim(req.getParam("link"));
		String body = trim(req.getParam("body"));
		String poster = trim(req.getParam("poster"));
		String lang = trim(req.getParam("lang"));
		
		link = checkLink(client.getUser(), link);
		poster = UserProfileHandler.checkArtwork(client.getUser(), poster);
		lang = Strings.getInstance().getLanguage(lang);
		
		//if (title == null || title.length() == 0) { 
		//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//			"Announcement title cannot be empty");
		//}
		
		//if (body == null || body.length() == 0) { 
		//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		//			"Announcement body cannot be empty");
		//}
		
		AnnouncementManager manager = client.getUser()
				.getUserManager().getAnnouncementManager();
		
		if (manager != null) { 
			manager.loadAnnouncements(false);
			
			final Announcement anno;
			if (key != null && key.length() > 0 && !key.equals("new")) { 
				anno = manager.getAnnouncement(key);
				if (anno == null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Announcement: " + key + " not found");
				}
			} else { 
				key = UserHelper.newAnnouncementKey("Announcement-" 
						+ manager.getAnnouncementCount() + "-" + System.currentTimeMillis());
				
				anno = new Announcement(key);
				manager.addAnnouncement(anno);
			}
			
			anno.setTitle(title, false);
			anno.setLink(link, false);
			anno.setBody(body, false);
			anno.setPoster(poster, false);
			anno.setLanguage(lang, false);
			anno.setModifiedTime(System.currentTimeMillis());
			
			manager.saveAnnouncements();
			
			rsp.add("key", anno.getKey());
			rsp.add("announcement", getAnnouncementInfo(anno));
		}
	}
	
	static void handleDelete(Request req, Response rsp, 
			IUserClient client) throws ErrorException { 
		if (req == null || rsp == null || client == null)
			return;
		
		if (!client.getUser().isManager()) { 
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"No permission to delete announcement");
		}
		
		String key = trim(req.getParam("key"));
		if (key == null || key.length() == 0)
			return;
		
		AnnouncementManager manager = client.getUser()
				.getUserManager().getAnnouncementManager();
		
		if (manager != null) { 
			manager.loadAnnouncements(false);
			
			final Announcement anno = manager.removeAnnouncement(key);
			if (anno == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Announcement: " + key + " not found");
			}
			
			manager.saveAnnouncements();
			
			rsp.add("key", anno.getKey());
			rsp.add("announcement", getAnnouncementInfo(anno));
		}
	}
	
	static void handleInfo(Request req, Response rsp, 
			IUserClient client) throws ErrorException { 
		if (req == null || rsp == null || client == null)
			return;
		
		String key = trim(req.getParam("key"));
		if (key == null || key.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Announcement key is empty");
		}
		
		AnnouncementManager manager = client.getUser()
				.getUserManager().getAnnouncementManager();
		
		if (manager != null) { 
			manager.loadAnnouncements(false);
			
			final Announcement anno = manager.getAnnouncement(key);
			if (anno == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Announcement: " + key + " not found");
			}
			
			rsp.add("key", anno.getKey());
			rsp.add("announcement", getAnnouncementInfo(anno));
		}
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
	
	static String checkLink(IUser user, String link) throws ErrorException { 
		if (link == null || link.length() == 0) return link;
		if (link.startsWith("http://") || link.startsWith("https://")) {
			try {
				@SuppressWarnings("unused")
				URL url = new URL(link);
			} catch (Throwable e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
			}
			return link;
		}
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Link: \"" + link + "\" input wrong");
	}
	
}
