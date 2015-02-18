package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.publication.INameValue;
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationSet;
import org.javenstudio.falcon.publication.PublicationHelper;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostNameData;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.setting.cluster.IHostUserData;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.CoreHandlerBase;
import org.javenstudio.lightning.core.datum.DatumHandlerBase;

public abstract class UserHandlerBase extends CoreHandlerBase {
	//private static final Logger LOG = Logger.getLogger(UserHandlerBase.class);
	
	private final UserCore mCore;
	
	public UserHandlerBase(UserCore core) { 
		if (core == null) throw new NullPointerException();
		mCore = core;
	}
	
	public UserCore getCore() { return mCore; }
	public UserManager getManager() { return getCore().getUserManager(); }
	
	static NamedList<Object> getMessageInfo(IMessage item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String streamId = item.getStreamId();
			String folderName = item.getFolder();
			IMessageSet stream = item.getService().getMessages(streamId, folderName);
			
			info.add("id", toString(item.getMessageId()));
			info.add("streamid", toString(item.getStreamId()));
			info.add("replyid", toString(item.getReplyId()));
			info.add("type", toString(item.getService().getType()));
			info.add("folder", toString(item.getFolder()));
			info.add("folderfrom", toString(item.getFolderFrom()));
			
			info.add("from", toString(item.getFrom()));
			info.add("to", toString(item.getTo()));
			info.add("cc", toString(item.getCc()));
			//info.add("bcc", toString(item.getBcc()));
			info.add("replyto", toString(item.getReplyTo()));
			info.add("subject", toString(item.getSubject()));
			//info.add("body", toString(item.getBody()));
			info.add("ctype", toString(item.getContentType()));
			info.add("status", toString(item.getStatus()));
			info.add("flag", toString(item.getFlag()));
			
			info.add("ctime", item.getCreatedTime());
			info.add("utime", item.getUpdateTime());
			info.add("mtime", item.getMessageTime());
			
			//info.add("source", toString(item.getSourceFile()));
			
			String[] atts = item.getAttachmentFiles();
			info.add("attcount", atts != null ? atts.length : 0);
			info.add("streamcount", stream != null ? stream.getTotalCount() : 0);
			
			//IUser user = item.getService().getManager().getUser();
			IUser userfrom = UserHelper.getLocalUserByName(item.getFrom());
			
			//info.add("user", UserInfoHandler.getUserInfo(user));
			info.add("userfrom", UserInfoHandler.getUserInfo(userfrom));
		}
		
		return info;
	}
	
	static NamedList<Object> getMessageInfo2(IMessage item, IMember member, 
			String accesskey) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String streamId = item.getStreamId();
			String folderName = item.getFolder();
			IMessageSet stream = item.getService().getMessages(streamId, folderName);
			
			info.add("id", toString(item.getMessageId()));
			info.add("streamid", toString(item.getStreamId()));
			info.add("replyid", toString(item.getReplyId()));
			info.add("type", toString(item.getService().getType()));
			info.add("folder", toString(item.getFolder()));
			info.add("folderfrom", toString(item.getFolderFrom()));
			
			info.add("from", toString(item.getFrom()));
			info.add("to", toString(item.getTo()));
			info.add("cc", toString(item.getCc()));
			info.add("bcc", toString(item.getBcc()));
			info.add("replyto", toString(item.getReplyTo()));
			info.add("subject", toString(item.getSubject()));
			info.add("body", toString(item.getBody()));
			info.add("ctype", toString(item.getContentType()));
			info.add("status", toString(item.getStatus()));
			info.add("flag", toString(item.getFlag()));
			
			info.add("ctime", item.getCreatedTime());
			info.add("utime", item.getUpdateTime());
			info.add("mtime", item.getMessageTime());
			
			info.add("source", toString(item.getSourceFile()));
			
			String[] atts = item.getAttachmentFiles();
			info.add("attcount", atts != null ? atts.length : 0);
			info.add("streamcount", stream != null ? stream.getTotalCount() : 0);
			
			if (atts != null && atts.length > 0) {
				NamedList<Object> items = new NamedMap<Object>();
				
				for (String attkey : atts) {
					if (attkey == null || attkey.length() == 0)
						continue;
					
					final IData data = SectionHelper.getData(member, attkey, IData.Access.THUMB, accesskey);
					final NamedList<Object> dataInfo;
					
					if (data != null) {
						if (data instanceof ISection) {
							dataInfo = DatumHandlerBase.getSectionInfo((ISection)data);
						} else {
							dataInfo = new NamedMap<Object>();
							dataInfo.add("id", toString(data.getContentId()));
							dataInfo.add("name", toString(data.getName()));
							dataInfo.add("type", toString(data.getContentType()));
							dataInfo.add("extname", toString(data.getExtension()));
							dataInfo.add("owner", toString(data.getOwner()));
							//dataInfo.add("ctime", data.getCreatedTime());
							dataInfo.add("mtime", data.getModifiedTime());
							//dataInfo.add("itime", data.getIndexedTime());
						}
						items.add(data.getContentId(), dataInfo);
					}
				}
				
				info.add("attachments", items);
			}
			
			String[] tonames = MessageHelper.splitAddresses(item.getTo());
			if (tonames != null && tonames.length > 0) {
				NamedList<Object> items = new NamedMap<Object>();
				
				for (String username : tonames) {
					IUser user = UserHelper.getLocalUserByName(username);
					NamedList<Object> userInfo = UserInfoHandler.getUserInfo(user);
					
					if (user != null && userInfo != null)
						items.add(user.getUserName(), userInfo);
				}
				
				info.add("tousers", items);
			}
			
			IUser user = item.getService().getManager().getUser();
			IUser userfrom = UserHelper.getLocalUserByName(item.getFrom());
			
			info.add("user", UserInfoHandler.getUserInfo(user));
			info.add("userfrom", UserInfoHandler.getUserInfo(userfrom));
		}
		
		return info;
	}
	
	static NamedList<Object> getPublicationInfo(IPublication item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("id", toString(item.getId()));
			
			INameValue<?>[] attrs = item.getAttrs();
			INameValue<?>[] headers = item.getHeaders();
			//INameValue<?>[] contents = item.getContents();
			
			if (attrs != null) {
				for (INameValue<?> nameVal : attrs) {
					if (nameVal != null && nameVal.getValue() != null)
						info.add(nameVal.getName(), nameVal.getValue());
				}
			}
			
			if (headers != null) {
				for (INameValue<?> nameVal : headers) {
					if (nameVal != null && nameVal.getValue() != null)
						info.add(nameVal.getName(), nameVal.getValue());
				}
			}
			
			String[] atts = PublicationHelper.splitValues(
					item.getContentString(IPublication.CONTENT_ATTACHMENTS));
			String[] shots = PublicationHelper.splitValues(
					item.getContentString(IPublication.CONTENT_SCREENSHOTS));
			
			IPublicationSet stream = item.getService().getPublications(
					item.getAttrString(IPublication.ATTR_STREAMID), 
					item.getAttrString(IPublication.ATTR_CHANNEL));
			
			info.add("attcount", atts != null ? atts.length : 0);
			info.add("shotcount", shots != null ? shots.length : 0);
			info.add("streamcount", stream != null ? stream.getTotalCount() : 0);
			
			IUser userowner = UserHelper.getLocalUserByName(item.getAttrString(IPublication.ATTR_OWNER));
			if (userowner != null) 
			  info.add("userowner", UserInfoHandler.getUserInfo(userowner));
		}
		
		return info;
	}
	
	static NamedList<Object> getPublicationInfo2(IPublication item, IMember member, 
			String accesskey) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("id", toString(item.getId()));
			
			INameValue<?>[] attrs = item.getAttrs();
			INameValue<?>[] headers = item.getHeaders();
			INameValue<?>[] contents = item.getContents();
			
			if (attrs != null) {
				for (INameValue<?> nameVal : attrs) {
					if (nameVal != null && nameVal.getValue() != null)
						info.add(nameVal.getName(), nameVal.getValue());
				}
			}
			
			if (headers != null) {
				for (INameValue<?> nameVal : headers) {
					if (nameVal != null && nameVal.getValue() != null)
						info.add(nameVal.getName(), nameVal.getValue());
				}
			}
			
			if (contents != null) {
				for (INameValue<?> nameVal : contents) {
					if (nameVal != null && nameVal.getValue() != null)
						info.add(nameVal.getName(), nameVal.getValue());
				}
			}
			
			String[] atts = PublicationHelper.splitValues(
					item.getContentString(IPublication.CONTENT_ATTACHMENTS));
			String[] shots = PublicationHelper.splitValues(
					item.getContentString(IPublication.CONTENT_SCREENSHOTS));
			
			IPublicationSet stream = item.getService().getPublications(
					item.getAttrString(IPublication.ATTR_STREAMID), 
					item.getAttrString(IPublication.ATTR_CHANNEL));
			
			info.add("attcount", atts != null ? atts.length : 0);
			info.add("shotcount", shots != null ? shots.length : 0);
			info.add("streamcount", stream != null ? stream.getTotalCount() : 0);
			
			if (atts != null && atts.length > 0) {
				NamedList<Object> items = new NamedMap<Object>();
				
				for (String attkey : atts) {
					if (attkey == null || attkey.length() == 0)
						continue;
					
					final IData data = SectionHelper.getData(member, attkey, IData.Access.THUMB, accesskey);
					final NamedList<Object> dataInfo;
					
					if (data != null) {
						if (data instanceof ISection) {
							dataInfo = DatumHandlerBase.getSectionInfo((ISection)data);
						} else {
							dataInfo = new NamedMap<Object>();
							dataInfo.add("id", toString(data.getContentId()));
							dataInfo.add("name", toString(data.getName()));
							dataInfo.add("type", toString(data.getContentType()));
							dataInfo.add("extname", toString(data.getExtension()));
							dataInfo.add("owner", toString(data.getOwner()));
							//dataInfo.add("ctime", data.getCreatedTime());
							dataInfo.add("mtime", data.getModifiedTime());
							//dataInfo.add("itime", data.getIndexedTime());
						}
						items.add(data.getContentId(), dataInfo);
					}
				}
				
				info.add("attachments", items);
			}
			
			IUser userowner = UserHelper.getLocalUserByName(item.getAttrString(IPublication.ATTR_OWNER));
			if (userowner != null) 
			  info.add("userowner", UserInfoHandler.getUserInfo(userowner));
		}
		
		return info;
	}
	
	static NamedList<Object> getProfileInfo(Profile item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("mtime", item.getModifiedTime());
			info.add("email", toString(item.getEmail()));
			info.add("nickname", toString(item.getNickName()));
			info.add("firstname", toString(item.getFirstName()));
			info.add("lastname", toString(item.getLastName()));
			info.add("sex", toString(item.getSex()));
			info.add("birthday", toString(item.getBirthday()));
			info.add("timezone", toString(item.getTimezone()));
			info.add("region", toString(item.getRegion()));
			info.add("tags", toString(item.getTags()));
			info.add("brief", toString(item.getBrief()));
			info.add("intro", toString(item.getIntroduction()));
			info.add("avatar", toString(item.getAvatar()));
			info.add("background", toString(item.getBackground()));
		}
		
		return info;
	}
	
	@SuppressWarnings("unused")
	private String normalizeUsername2(String username) {
		if (username != null && username.length() > 0) {
			username = trim(username);
			
			int pos = username.indexOf('@');
			if (pos > 0) {
				IHostNode host = getCore().getHostSelf();
				
				String name = username.substring(0, pos);
				String domain = username.substring(pos+1);
				
				String clusterId = host.getClusterId();
				String clusterDomain = host.getClusterDomain();
				String mailDomain = host.getMailDomain();
				
				String hostDomain = host.getHostDomain();
				String hostAddr = host.getHostAddress();
				
				String adminname = name;
				if (name != null) {
					int pos2 = name.indexOf('#');
					if (pos2 > 0) adminname = name.substring(0, pos2);
				}
				
				if (name != null && name.length() > 0) {
					if (adminname.equalsIgnoreCase(getAdminConfig().getAdminUser())) {
						if (domain.equalsIgnoreCase(clusterId) || 
							domain.equalsIgnoreCase(clusterDomain) || 
							domain.equalsIgnoreCase(mailDomain) || 
							domain.equalsIgnoreCase(hostDomain) || 
							domain.equalsIgnoreCase(hostAddr)) {
							username = adminname;
						}
					} else {
						if (domain.equalsIgnoreCase(clusterId) || 
							domain.equalsIgnoreCase(clusterDomain) || 
							domain.equalsIgnoreCase(mailDomain) || 
							domain.equalsIgnoreCase(hostDomain) || 
							domain.equalsIgnoreCase(hostAddr)) {
							username = name;
						}
					}
				}
			}
		}
		
		return username;
	}
	
	protected void checkGroupname(String groupname) throws ErrorException { 
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (groupname.equals(getAdminConfig().getAdminUser())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Administrator name cannot be registered");
		}
		
		if (groupname.equalsIgnoreCase(IUser.SYSTEM)) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"System user cannot be registered");
		}
		
		if (!UserHelper.checkUserName(groupname)) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname must be at least 4 lowercase characters(a-z/0-9)");
		}
		
		if (getAdminSetting().getGlobal().isReservedName(groupname)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname is reserved by system");
		}
	}
	
	protected void checkUsername(String username, String appaddr) 
			throws ErrorException { 
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (username.equals(getAdminConfig().getAdminUser())) { 
			if (!"127.0.0.1".equals(appaddr)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Administrator user must register from localhost");
			}
			
			return;
		}
		
		if (username.equalsIgnoreCase(IUser.SYSTEM)) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"System user cannot be registered");
		}
		
		if (!UserHelper.checkUserName(username)) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username must be at least 4 lowercase characters(a-z/0-9)");
		}
		
		if (getAdminSetting().getGlobal().isReservedName(username)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username is reserved by system");
		}
	}
	
	protected void checkPassword(String password) throws ErrorException { 
		if (password == null || password.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Password cannot be empty");
		}
		
		if (password.length() < 6) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Password must be at least 6 characters");
		}
	}
	
	static String checkEmail(IHostCluster cluster, String username, String email) 
			throws ErrorException { 
		if (cluster == null) throw new NullPointerException();
		if (email == null) return email;
		if (email != null && email.length() > 0) { 
			if (UserHelper.checkEmailAddress(email) == false) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Email: \"" + email + "\" is not illegal format");
			}
			
			IHostUserData userData = cluster.searchUser(email);
			if (userData != null && !userData.getUserName().equals(username)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Email: \"" + email + "\" already existed");
			}
			
			IHostNameData nameData = cluster.searchName(email);
			String nameValue = nameData != null ? nameData.getNameValue() : null;
			if (nameValue != null && !nameValue.equals(username)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Email: \"" + email + "\" already existed");
			}
		}
		
		return email;
	}
	
}
