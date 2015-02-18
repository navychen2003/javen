package org.javenstudio.lightning.core.user;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostNameData;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.setting.cluster.IHostUserData;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.core.datum.DatumHandlerBase;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserProfileHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserProfileHandler(core);
	}
	
	public UserProfileHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "info";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("info")) { 
			handleInfo(req, rsp, user);
		} else if (action.equalsIgnoreCase("update")) { 
			handleUpdate(req, rsp, user, user, getCore().getHostSelf());
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	static void handleInfo(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String accesskey = trim(req.getParam("accesskey"));
		Profile profile = user.getProfile();
		if (profile != null) { 
			profile.loadProfile(false);
			
			NamedList<Object> info = getProfileInfo(profile);
			rsp.add("profile", info);
			
			String avatarSrc = profile.getAvatar();
			String backgroundSrc = profile.getBackground();
			
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
		}
	}
	
	static void handleUpdate(Request req, Response rsp, 
			IUser user, IMember me, IHostNode host) throws ErrorException { 
		if (req == null || rsp == null || user == null || me == null)
			return;
		
		IHostCluster cluster = host != null ? host.getCluster() : null;
		if (host == null || cluster == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host or cluster is null");
		}
		
		String email = trim(req.getParam("email"));
		String nickname = trim(req.getParam("nickname"));
		String firstname = trim(req.getParam("firstname"));
		String lastname = trim(req.getParam("lastname"));
		String sex = trim(req.getParam("sex"));
		String birthday = trim(req.getParam("birthday"));
		String timezone = trim(req.getParam("timezone"));
		String region = trim(req.getParam("region"));
		String tags = trim(req.getParam("tags"));
		String brief = trim(req.getParam("brief"));
		String intro = trim(req.getParam("intro"));
		String avatar = trim(req.getParam("avatar"));
		String background = trim(req.getParam("background"));
		
		Profile profile = user.getProfile();
		if (profile != null) { 
			profile.loadProfile(false);
		
			final String currentNick = profile.getNickName();
			final String currentEmail = profile.getEmail();
			final String hostkey = host.getHostKey();
			
			sex = checkSex(sex);
			avatar = checkArtwork(user, avatar);
			background = checkArtwork(user, background);
			nickname = checkNickname(cluster, user.getUserName(), nickname);
			email = checkEmail(cluster, user.getUserName(), email);
			
			profile.setUserName(user.getUserName(), false);
			profile.setEmail(email, false);
			profile.setNickName(nickname, false);
			profile.setFirstName(firstname, false);
			profile.setLastName(lastname, false);
			profile.setSex(sex, false);
			profile.setBirthday(birthday, false);
			profile.setTimezone(timezone, false);
			profile.setRegion(region, false);
			profile.setTags(tags, false);
			profile.setBrief(brief, false);
			profile.setIntroduction(intro, false);
			profile.setAvatar(avatar, false);
			profile.setBackground(background, false);
			
			profile.saveProfile();
			
			boolean nickChanged = false;
			if (nickname != null && nickname.length() > 0) { 
				if (currentNick == null || currentNick.length() == 0 || 
					!currentNick.equals(nickname)) {
					cluster.updateName(nickname, user.getUserName(), hostkey, 
							IUser.NAME_NICKNAME, null, currentNick);
					
					String currentname = currentNick;
					if (currentname == null) currentname = "";
					
					String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" modified nickname from \"%2$s\" to \"%3$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName(), currentname, nickname));
					
					nickChanged = true;
				}
			}
			
			boolean emailChanged = false;
			if (email != null && email.length() > 0) { 
				if (currentEmail == null || currentEmail.length() == 0 || 
					!currentEmail.equals(email)) {
					cluster.updateName(email, user.getUserName(), hostkey, 
							IUser.NAME_EMAIL, null, currentEmail);
					
					String currentname = currentEmail;
					if (currentname == null) currentname = "";
					
					String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" modified email from \"%2$s\" to \"%3$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName(), currentname, email));
					
					emailChanged = true;
				}
			}
			
			if (nickChanged == false && emailChanged == false) {
				String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" modified your profile");
				MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName()));
			}
		}
	}
	
	static String checkSex(String val) throws ErrorException { 
		if (val == null) return val;
		if (val.equalsIgnoreCase("male") || val.equalsIgnoreCase("female") || 
			val.equalsIgnoreCase("unisex"))
			return val.toLowerCase();
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Sex: \"" + val + "\" input wrong and must be \"male\" or \"female\"");
	}
	
	static String checkArtwork(IUser user, String id) throws ErrorException { 
		if (id == null) return id;
		if (id.equalsIgnoreCase("null")) return "";
		if (id.length() > 0) return id;
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Artwork: \"" + id + "\" input wrong");
	}
	
	static String checkNickname(IHostCluster cluster, String username, 
			String nickname) throws ErrorException { 
		if (cluster == null || username == null) throw new NullPointerException();
		if (nickname == null) return nickname;
		if (nickname != null && nickname.length() > 0) { 
			if (nickname.length() < 3) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Nickname is too short");
			}
			
			for (int i=0; i < nickname.length(); i++) { 
				char chr = nickname.charAt(i);
				if (chr >= 'a' && chr <= 'z') continue;
				if (chr >= 'A' && chr <= 'Z') continue;
				if (chr >= '0' && chr <= '9') continue;
				if (chr < 0 || chr > 127) continue;
				if (chr == '.' || chr == '-' || chr == '_' || chr == '@' || chr == '&') // || 
					//chr == '[' || chr == ']' || chr == '~' || chr == '<' || chr == '>' || 
					//chr == '%' || chr == '#' || chr == '+' || chr == '=' || chr == '/' ||
					//chr == '{' || chr == '}' || chr == '|' || chr == '\\' || chr == '^')
					continue;
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Nickname has illegal character");
			}
			
			if (nickname.equalsIgnoreCase(IUser.SYSTEM)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"System user cannot be registered");
			}
			
			IHostUserData userData = cluster.searchUser(nickname);
			if (userData != null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: \"" + nickname + "\" already existed");
			}
			
			IHostNameData nameData = cluster.searchName(nickname);
			String nameValue = nameData != null ? nameData.getNameValue() : null;
			if (nameValue != null && !nameValue.equals(username)) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Name: \"" + nickname + "\" already existed");
			}
		}
		
		return nickname;
	}
	
}
