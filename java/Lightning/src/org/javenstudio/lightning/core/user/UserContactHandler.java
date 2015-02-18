package org.javenstudio.lightning.core.user;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.Contact;
import org.javenstudio.falcon.user.profile.ContactGroup;
import org.javenstudio.falcon.user.profile.ContactManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.datum.DatumHandlerBase;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserContactHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserContactHandler(core);
	}
	
	public UserContactHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, user);
		} else if (action.equalsIgnoreCase("update")) { 
			handleUpdate(req, rsp, user);
		} else if (action.equalsIgnoreCase("info")) { 
			handleInfo(req, rsp, user);
		} else if (action.equalsIgnoreCase("delete")) { 
			handleDelete(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleDelete(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String key = trim(req.getParam("key"));
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Contact key is empty");
		}
		
		ContactManager manager = user.getContactManager();
		if (manager != null) { 
			manager.loadContacts(false);
			
			ContactGroup[] groups = manager.getContactGroups();
			
			for (int i=0; groups != null && i < groups.length; i++) { 
				ContactGroup group = groups[i];
				if (group == null) continue;
				
				Contact contact = group.removeContact(key);
				if (contact != null) { 
					manager.saveContacts();
					
					NamedList<Object> info = getContactInfo(contact);
					rsp.add("key", contact.getKey());
					rsp.add("contact", info);
					
					return;
				}
			}
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Contact: " + key + " not found");
		}
	}
	
	private void handleInfo(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String accesskey = trim(req.getParam("accesskey"));
		String key = trim(req.getParam("key"));
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Contact key is empty");
		}
		
		ContactManager manager = user.getContactManager();
		if (manager != null) { 
			manager.loadContacts(false);
			
			ContactGroup[] groups = manager.getContactGroups();
			
			for (int i=0; groups != null && i < groups.length; i++) { 
				ContactGroup group = groups[i];
				if (group == null) continue;
				
				Contact contact = group.getContact(key);
				if (contact != null) { 
					NamedList<Object> info = getContactInfo(contact);
					rsp.add("key", contact.getKey());
					rsp.add("contact", info);
					
					String avatarSrc = contact.getAvatar();
					String backgroundSrc = contact.getBackground();
					
					IData avatarData = SectionHelper.getData(user, avatarSrc, IData.Access.THUMB, accesskey);
					IData backgroundData = SectionHelper.getData(user, backgroundSrc, IData.Access.THUMB, accesskey);
					
					if (avatarData != null && avatarData instanceof ISection) {
						NamedList<Object> dataInfo = DatumHandlerBase.getSectionInfo((ISection)avatarData);
						if (dataInfo != null)
							rsp.add("avatar", dataInfo);
					}
					
					if (backgroundData != null && backgroundData instanceof ISection) {
						NamedList<Object> dataInfo = DatumHandlerBase.getSectionInfo((ISection)backgroundData);
						if (dataInfo != null)
							rsp.add("background", dataInfo);
					}
					
					return;
				}
			}
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Contact: " + key + " not found");
		}
	}
	
	private void handleUpdate(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String key = trim(req.getParam("key"));
		String nickname = trim(req.getParam("nickname"));
		String firstname = trim(req.getParam("firstname"));
		String lastname = trim(req.getParam("lastname"));
		String sex = trim(req.getParam("sex"));
		String birthday = trim(req.getParam("birthday"));
		String title = trim(req.getParam("title"));
		String region = trim(req.getParam("region"));
		String tags = trim(req.getParam("tags"));
		String brief = trim(req.getParam("brief"));
		String intro = trim(req.getParam("intro"));
		String avatar = trim(req.getParam("avatar"));
		String background = trim(req.getParam("background"));
		
		sex = UserProfileHandler.checkSex(sex);
		avatar = UserProfileHandler.checkArtwork(user, avatar);
		background = UserProfileHandler.checkArtwork(user, background);
		
		ContactManager manager = user.getContactManager();
		if (manager != null) { 
			manager.loadContacts(false);
			
			if (key != null && key.length() > 0 && !key.equals("new")) {
				ContactGroup[] groups = manager.getContactGroups();
				boolean found = false;
				
				for (int i=0; groups != null && i < groups.length; i++) { 
					ContactGroup group = groups[i];
					if (group == null) continue;
					
					Contact contact = group.getContact(key);
					if (contact != null) { 
						contact.setNickName(nickname, false);
						contact.setFirstName(firstname, false);
						contact.setLastName(lastname, false);
						contact.setSex(sex, false);
						contact.setBirthday(birthday, false);
						contact.setTitle(title, false);
						contact.setRegion(region, false);
						contact.setTags(tags, false);
						contact.setBrief(brief, false);
						contact.setIntroduction(intro, false);
						contact.setAvatar(avatar, false);
						contact.setBackground(background, false);
						
						key = contact.getKey();
						found = true;
						break;
					}
				}
				
				if (found) {
					manager.saveContacts();
					
					rsp.add("key", key);
					return;
				}
				
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Contact: " + key + " not found");
			}
			
			Contact contact = new Contact(UserHelper.newContactKey("Contact-" 
					+ manager.getContactCount() + "-" + System.currentTimeMillis()));
			
			contact.setNickName(nickname, false);
			contact.setFirstName(firstname, false);
			contact.setLastName(lastname, false);
			contact.setSex(sex, false);
			contact.setBirthday(birthday, false);
			contact.setTitle(title, false);
			contact.setRegion(region, false);
			contact.setTags(tags, false);
			contact.setBrief(brief, false);
			contact.setIntroduction(intro, false);
			contact.setAvatar(avatar, false);
			contact.setBackground(background, false);
			
			manager.addContact(contact, Contact.TYPE_FRIEND);
			manager.saveContacts();
			
			rsp.add("key", contact.getKey());
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		NamedList<Object> list = new NamedMap<Object>();
		
		ContactManager manager = user.getContactManager();
		if (manager != null) { 
			manager.loadContacts(false);
			
			ContactGroup[] groups = manager.getContactGroups();
			for (int i=0; groups != null && i < groups.length; i++) { 
				ContactGroup group = groups[i];
				if (group == null) continue;
				
				Contact[] contacts = group.getContacts();
				ArrayList<NamedList<Object>> contactList = new ArrayList<NamedList<Object>>();
				
				for (int j=0; contacts != null && j < contacts.length; j++) { 
					Contact contact = contacts[j];
					if (contact == null) continue;
					
					NamedList<Object> info = getContactInfo2(contact);
					if (info != null) contactList.add(info);
				}
				
				Object[] contactItems = contactList.toArray(new Object[contactList.size()]);
				list.add(group.getContactType(), contactItems);
			}
		}
		
		rsp.add("contacts", list);
	}
	
	static NamedList<Object> getContactInfo2(Contact item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("key", toString(item.getKey()));
			info.add("nickname", toString(item.getNickName()));
			info.add("firstname", toString(item.getFirstName()));
			info.add("lastname", toString(item.getLastName()));
			info.add("sex", toString(item.getSex()));
			info.add("birthday", toString(item.getBirthday()));
			info.add("region", toString(item.getRegion()));
			info.add("title", toString(item.getTitle()));
			info.add("brief", toString(item.getBrief()));
			info.add("avatar", toString(item.getAvatar()));
			info.add("background", toString(item.getBackground()));
		}
		
		return info;
	}
	
	static NamedList<Object> getContactInfo(Contact item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("key", toString(item.getKey()));
			info.add("nickname", toString(item.getNickName()));
			info.add("firstname", toString(item.getFirstName()));
			info.add("lastname", toString(item.getLastName()));
			info.add("sex", toString(item.getSex()));
			info.add("birthday", toString(item.getBirthday()));
			info.add("region", toString(item.getRegion()));
			info.add("title", toString(item.getTitle()));
			info.add("brief", toString(item.getBrief()));
			info.add("tags", toString(item.getTags()));
			info.add("intro", toString(item.getIntroduction()));
			info.add("avatar", toString(item.getAvatar()));
			info.add("background", toString(item.getBackground()));
		}
		
		return info;
	}
	
}
