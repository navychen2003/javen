package org.javenstudio.lightning.core.user;

import java.util.ArrayList;
import java.util.TimeZone;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.Theme;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.setting.cluster.IStorageInfo;
import org.javenstudio.falcon.setting.cluster.StorageManager;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.job.JobSubmit;
import org.javenstudio.falcon.util.job.JobSubmit.JobWork;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserHeartbeatHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserHeartbeatHandler(core);
	}
	
	public UserHeartbeatHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		String action = trim(req.getParam("action"));
		boolean showAll = false, showInfo = false, refresh = false, access = false;
		if (action != null) {
			if (action.equalsIgnoreCase("all")) showAll = true;
			else if (action.equalsIgnoreCase("info")) showInfo = true;
			else if (action.equalsIgnoreCase("refresh")) refresh = true;
			else if (action.equalsIgnoreCase("access")) access = true;
		}
		if (refresh) showAll = true;
		
		String token = UserHelper.getParamToken(req);
		IUserClient.Op op = refresh ? IUserClient.Op.REFRESH : 
			((access || showAll || showInfo) ? IUserClient.Op.ACCESS : 
				IUserClient.Op.HEARTBEAT);
		
		IUserClient client = UserHelper.checkUserClient(token, op);
		if (client == null && token != null && token.length() > 0) { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Unauthorized Access: wrong token");
		}
		
		int maxInvites = parseInt(req.getParam("maxinvites"));
		int maxMessages = parseInt(req.getParam("maxmessages"));
		int maxWorks = parseInt(req.getParam("maxworks"));
		
		IUser.ShowType showType = showAll ? IUser.ShowType.ALL : 
			(showInfo ? IUser.ShowType.INFO : IUser.ShowType.DEFAULT);
		
		handleAction(req, rsp, getCore(), client, 
				maxInvites, maxMessages, maxWorks, showType);
	}
	
	static void handleAction(Request req, Response rsp, 
			Core core, IUserClient client, int maxInvites, int maxMessages, 
			int maxWorks, IUser.ShowType showType) throws ErrorException {
		if (client != null) {
			rsp.add("system", getSystemInfo(core, req, showType));
			if (showType == IUser.ShowType.ALL) {
				rsp.add("hosts", UserClusterHandler.getHostInfos(core.getCluster().getJoinHosts(), showType));
				rsp.add("setting", getSettingInfo(client, showType));
				rsp.add("langs", getLangList());
				rsp.add("themes", getThemeList());
			}
			
			NamedMap<Object> user = NamedHelper.toNamedMap(client, 
					maxInvites, maxMessages, showType);
			if (user != null) rsp.add("user", user);
			
			NamedMap<Object> storages = getStorageInfos(client.getUser(), showType);
			if (storages != null) rsp.add("storages", storages);
			
			NamedMap<?>[] works = getWorkInfos(client.getUser(), maxWorks, showType);
			if (works != null) rsp.add("works", works);
			
		} else { 
			rsp.add("system", getSystemInfo(core, req, showType));
			rsp.add("setting", getSettingInfo(req, showType));
			if (showType == IUser.ShowType.ALL) {
				rsp.add("hosts", UserClusterHandler.getHostInfos(core.getCluster().getJoinHosts(), showType));
				rsp.add("langs", getLangList());
				rsp.add("themes", getThemeList());
			}
		}
	}
	
	static NamedMap<Object> getSystemInfo(Core core, Request req, 
			IUser.ShowType showType) throws ErrorException {
		IHostNode host = core.getCluster().getHostSelf();
		NamedMap<Object> info = new NamedMap<Object>();
		
		String notice = core.getAdminSetting().getGlobal().getSystemNotice();
		if (showType == IUser.ShowType.ALL) {
			info.add("now", System.currentTimeMillis());
			info.add("tz", TimeZone.getDefault().getRawOffset());
			info.add("notice", toString(notice));
			
			info.add("mode", toString(host.getHostMode()));
			info.add("hostkey", toString(host.getHostKey()));
			info.add("scheme", req.getRequestInput().getScheme());
			info.add("clusterid", toString(host.getClusterId()));
			info.add("clusterdomain", toString(host.getClusterDomain()));
			info.add("maildomain", toString(host.getMailDomain()));
			info.add("domain", toString(host.getHostDomain()));
			info.add("hostname", toString(host.getHostName()));
			info.add("hostaddr", toString(host.getHostAddress()));
			info.add("httpport", host.getHttpPort());
			info.add("httpsport", host.getHttpsPort());
			
		} else { 
			info.add("now", System.currentTimeMillis());
			info.add("tz", TimeZone.getDefault().getRawOffset());
			info.add("notice", toString(notice));
		}
		
		return info;
	}

	static NamedMap<Object> getLangList() throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		String[] values = Strings.getInstance().getLanguages();
		
		for (int i=0; values != null && i < values.length; i++) { 
			String value = values[i];
			String title = Strings.getInstance().getResourceName(value);
			if (value != null && title != null) 
				info.add(value, title);
		}
		
		return info;
	}
	
	static NamedMap<Object> getThemeList() throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		Theme[] values = Theme.getThemes();
		
		for (int i=0; values != null && i < values.length; i++) { 
			Theme value = values[i];
			if (value == null) continue;
			String name = value.getName();
			String title = value.getTitle();
			if (name != null && title != null) 
				info.add(name, title);
		}
		
		return info;
	}
	
	static NamedMap<Object> getSettingInfo(Request req, 
			IUser.ShowType showType) throws ErrorException {
		String lang = trim(req.getParam("lang"));
		String theme = trim(req.getParam("theme"));
		
		lang = Strings.getInstance().getLanguage(lang);
		theme = Theme.getThemeName(theme);
		
		NamedMap<Object> info = new NamedMap<Object>();
		info.add("lang", toString(lang));
		info.add("theme", toString(theme));
		
		return info;
	}
	
	static NamedMap<Object> getSettingInfo(IUserClient client, 
			IUser.ShowType showType) throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		info.add("lang", toString(client.getLanguage()));
		info.add("theme", toString(client.getTheme()));
		info.add("tz", toString(client.getUser().getPreference().getTimezone()));
		
		return info;
	}
	
	static NamedMap<Object> getSettingInfo(IMember user, 
			IUser.ShowType showType) throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		info.add("lang", toString(user.getPreference().getLanguage()));
		info.add("tz", toString(user.getPreference().getTimezone()));
		
		return info;
	}
	
	static NamedMap<Object> getStorageInfos(final IUser user, 
			IUser.ShowType showType) throws ErrorException {
		if (user == null) return null;
		NamedMap<Object> infos = new NamedMap<Object>();
		
		StorageManager manager = user.getUserManager().getStorageManager(user.getUserKey());
		if (manager != null) {
			IStorageInfo[] storages = manager.getStorages();
			
			for (int i=0; storages != null && i < storages.length; i++) { 
				IStorageInfo storage = storages[i];
				if (storage == null) continue;
				infos.add(storage.getHostNode().getHostKey(), 
						storage.getRequestTime());
			}
		}
		
		return infos;
	}
	
	static NamedMap<?>[] getWorkInfos(final IUser user, int maxWorks, 
			IUser.ShowType showType) throws ErrorException {
		if (user == null) return null;
		ArrayList<NamedMap<?>> list = new ArrayList<NamedMap<?>>();
		
		JobSubmit.Filter filter = new JobSubmit.Filter() {
				@Override
				public boolean accept(JobWork<?> work) {
					if (work == null) return false;
					String userKey = work.getJob().getUser();
					if (userKey != null && userKey.equals(user.getUserKey()))
						return true;
					return false;
				}
			};
		
		if (maxWorks > 0) {
			for (JobSubmit.JobWork<?> work : JobSubmit.getWorks(filter)) { 
				if (work == null) continue;
				NamedMap<Object> info = getWorkInfo(work);
				if (info != null) {
					list.add(info); 
					if (list.size() >= maxWorks) break;
				}
			}
		}
		
		return list.toArray(new NamedMap<?>[list.size()]);
	}
	
	static NamedMap<Object> getWorkInfo(JobSubmit.JobWork<?> work) 
			throws ErrorException {
		if (work == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		info.add("id", toString(work.toString()));
		info.add("type", toString(work.getJob().getClass().getSimpleName()));
		info.add("message", toString(work.getJob().getMessage()));
		info.add("start", work.getStartTime());
		info.add("finish", work.getFinishTime());
		//info.add("status", work.getJob().getStatusMessages());
		
		return info;
	}
	
}
