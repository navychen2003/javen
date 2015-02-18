package org.javenstudio.lightning.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.Setting;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingCheckbox;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.setting.SettingSelect;
import org.javenstudio.falcon.setting.SettingText;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.setting.cluster.HostSelf;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.lightning.logging.DefaultLogger;
import org.javenstudio.raptor.util.StringUtils;

public class GlobalSetting extends CoreSetting 
		implements SettingGroup.UpdateHandler {
	private static final Logger LOG = Logger.getLogger(GlobalSetting.class);
	
	public static enum SystemFlag {
		ENABLED, REGISTER_DISABLED, LOGINREGISTER_DISABLED, ALL_DISABLED
	}
	
	public static String stringOfSystemFlag(SystemFlag flag) {
		if (flag == null) return null;
		switch (flag) {
		case ENABLED:
			return "enabled";
		case REGISTER_DISABLED:
			return "register-disabled";
		case LOGINREGISTER_DISABLED:
			return "loginregister-disabled";
		case ALL_DISABLED:
			return "all-disabled";
		default:
			return "";
		}
	}
	
	public static SystemFlag valueOfSystemFlag(String val) {
		if (val != null) {
			if (val.equalsIgnoreCase("enabled"))
				return SystemFlag.ENABLED;
			else if (val.equalsIgnoreCase("register-disabled"))
				return SystemFlag.REGISTER_DISABLED;
			else if (val.equalsIgnoreCase("loginregister-disabled"))
				return SystemFlag.LOGINREGISTER_DISABLED;
			else if (val.equalsIgnoreCase("all-disabled"))
				return SystemFlag.ALL_DISABLED;
		}
		return null;
	}
	
	private final Set<SettingGroup> mGroups;
	private final List<Pattern> mReservedPatterns;
	private SystemFlag mSystemFlag = SystemFlag.ENABLED;
	private String mFriendlyName = null;
	private String mReservedList = null;
	private String mSystemNotice = null;
	private String mAdminList = null;
	private boolean mSendUsage = false;
	
	public GlobalSetting(CoreAdminSetting setting) throws ErrorException {
		super(setting);
		mReservedPatterns = new ArrayList<Pattern>();
		mFriendlyName = setting.getContainers().getAdminConfig().getHostName();
		mGroups = initSetting(setting);
	}
	
	public boolean isLoggerDebug() { return DefaultLogger.isDebug(); }
	public void setLoggerDebug(boolean debug) { DefaultLogger.setDebug(debug); }
	
	public boolean isSendUsage() { return mSendUsage; }
	public void setSendUsage(boolean send) { mSendUsage = send; }
	
	public String getSystemNotice() { return mSystemNotice; }
	public void setSystemNotice(String val) { mSystemNotice = val; }
	
	public String getFriendlyName() { return mFriendlyName; }
	public String getReservedList() { return mReservedList; }
	public String getAdminList() { return mAdminList; }
	
	public void setFriendlyName(String name) { 
		if (name != null && name.length() > 0)
			mFriendlyName = FsUtils.normalizeFriendlyName(name);
	}
	
	public void setAdminList(String names) { 
		mAdminList = names; 
		getContainers().getUserStore().getUserManager().resetAdmin();
	}
	
	public SystemFlag getSystemFlag() { return mSystemFlag; }
	public String getSystemFlagAsString() { return stringOfSystemFlag(getSystemFlag()); }
	
	public void setSystemFlag(SystemFlag flag) { mSystemFlag = flag; }
	public void setSystemFlag(String flag) { mSystemFlag = valueOfSystemFlag(flag); }
	
	public void setReservedList(String names) {
		mReservedList = names;
		
		synchronized (mReservedPatterns) {
			if (names != null && names.length() > 0) {
				mReservedPatterns.clear();
				
				StringTokenizer st = new StringTokenizer(names, " \t\r\n");
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					Pattern pattern = Pattern.compile(token);
					if (pattern != null) 
						mReservedPatterns.add(pattern);
				}
			}
		}
	}
	
	public boolean isReservedName(String name) {
		if (name == null || name.length() == 0)
			return false;
		
		synchronized (mReservedPatterns) {
			for (Pattern pattern : mReservedPatterns) {
				if (pattern == null) continue;
				
				Matcher matcher = pattern.matcher(name);
				if (matcher != null && matcher.matches())
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isLoginDisabled() {
		SystemFlag flag = getSystemFlag();
		if (flag == null) return false;
		switch (flag) {
		case ENABLED:
			return false;
		case REGISTER_DISABLED:
			return false;
		case LOGINREGISTER_DISABLED:
			return true;
		case ALL_DISABLED:
			return true;
		default:
			return false;
		}
	}
	
	public boolean isRegisterDisabled() {
		SystemFlag flag = getSystemFlag();
		if (flag == null) return false;
		switch (flag) {
		case ENABLED:
			return false;
		case REGISTER_DISABLED:
			return true;
		case LOGINREGISTER_DISABLED:
			return true;
		case ALL_DISABLED:
			return true;
		default:
			return false;
		}
	}
	
	HostSelf newHostSelf() throws ErrorException { 
		return new HostSelfImpl(); 
	}
	
	private class HostSelfImpl extends HostSelf {
		private String mHostAddress;
		
		public HostSelfImpl() throws ErrorException { 
			mHostAddress = getContainers().getAdminConfig().getHostAddress();
		}
		
		@Override
		public final HostMode getHostMode() {
			return getContainers().getAdminConfig().getHostMode();
		}
		
		@Override
		public long getHeartbeatTime() {
			return System.currentTimeMillis();
		}
		
		@Override
		public final String getHostKey() { 
			return getContainers().getAdminConfig().getHostKey();
		}
		
		@Override
		public final int getHostHash() {
			return getContainers().getAdminConfig().getHostHash();
		}
		
		@Override
		public String getClusterId() {
			IHostCluster cluster = getCluster();
			if (cluster != null) return cluster.getClusterId();
			return getContainers().getAdminConfig().getClusterId();
		}

		@Override
		public String getClusterDomain() {
			String domain = null;
			IHostCluster cluster = getCluster();
			if (cluster != null) domain = cluster.getDomain();
			if (domain == null || domain.length() == 0)
				domain = getContainers().getAdminConfig().getClusterDomain();
			return domain;
		}

		@Override
		public String getMailDomain() {
			String domain = null;
			IHostCluster cluster = getCluster();
			if (cluster != null) domain = cluster.getMailDomain();
			if (domain == null || domain.length() == 0)
				domain = getContainers().getAdminConfig().getMailDomain();
			return domain;
		}
		
		@Override
		public String getClusterSecret() {
			return getContainers().getAdminConfig().getClusterSecret();
		}
		
		@Override
		public String getAdminUser() {
			return getContainers().getAdminConfig().getAdminUser();
		}
		
		@Override
		public String getHostDomain() {
			return getContainers().getAdminConfig().getHostDomain();
		}
		
		@Override
		public synchronized String getHostAddress() {
			return mHostAddress; //getContainers().getAdminConfig().getHostAddress();
		}

		@Override
		public synchronized void setHostAddress(String addr) {
			if (addr != null && addr.length() > 0)
				mHostAddress = addr;
		}
		
		@Override
		public String getLanAddress() {
			return getContainers().getAdminConfig().getLanAddress();
		}
		
		@Override
		public String getHostName() {
			return GlobalSetting.this.getFriendlyName();
		}

		@Override
		public int getHttpPort() {
			return getContainers().getAdminConfig().getHttpPort();
		}

		@Override
		public int getHttpsPort() {
			return getContainers().getAdminConfig().getHttpsPort();
		}
	}
	
	private Set<SettingGroup> initSetting(CoreAdminSetting setting) 
			throws ErrorException { 
		Set<SettingGroup> groups = new HashSet<SettingGroup>();
		
		SettingCategory category = setting.createCategory(GLOBAL_NAME);
		category.setTitle("Global");
		
		if (true) {
			SettingGroup group = initGeneral(category);
			groups.add(group);
		}
		
		if (true) { 
			SettingGroup group = initUser(category);
			groups.add(group);
		}
		
		if (true) { 
			SettingGroup group = initAdministrator(category);
			groups.add(group);
		}
		
		if (true) { 
			SettingGroup group = initSystem(category);
			groups.add(group);
		}
		
		return groups;
	}
	
	@Override
	protected void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
		if (categoryName.equals(GLOBAL_NAME)) { 
			if (groupName.equals(GENERAL_NAME)) { 
				if (name.equals(FRIENDLYNAME_NAME)) { 
					String value = toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: friendlyname=" + value);
						
						setFriendlyName(value);
					}
				} else if (name.equals(SENDUSAGE_NAME)) { 
					boolean value = toBool(val, isSendUsage());
					if (true/*value != isSendUsage()*/) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: sendusage=" + value);
						
						setSendUsage(value);
					}
				} else if (name.equals(LOGGERDEBUG_NAME)) { 
					boolean value = toBool(val, isLoggerDebug());
					if (true/*value != isLoggerDebug()*/) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: loggerDebug=" + value);
						
						setLoggerDebug(value);
					}
				}
			} else if (groupName.equals(USER_NAME)) { 
				if (name.equals(RESERVEDLIST_NAME)) { 
					String value = toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: reservedlist=" + value);
						
						setReservedList(value);
					}
				}
			} else if (groupName.equals(ADMINISTRATOR_NAME)) { 
				if (name.equals(ADMINLIST_NAME)) { 
					String value = toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: adminlist=" + value);
						
						setAdminList(value);
					}
				}
			} else if (groupName.equals(SYSTEM_NAME)) { 
				if (name.equals(SYSTEMFLAG_NAME)) {
					String value = toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: systemflag=" + value);
						
						setSystemFlag(value);
					}
				} else if (name.equals(SYSTEMNOTICE_NAME)) {
					String value = toString(val);
					if (value != null && value.length() > 0) { 
						if (LOG.isDebugEnabled())
							LOG.debug("loadSetting: systemnotice=" + value);
						
						setSystemNotice(value);
					}
				}
			}
		}
	}
	
	@Override
	public synchronized boolean updateSetting(SettingGroup group, Object input)
			throws ErrorException {
		if (group == null || input == null)
			throw new NullPointerException();
		
		if (!mGroups.contains(group)) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Update failed with wrong handler");
		}
		
		if (!(input instanceof IParams)) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Update failed with wrong input");
		}
		
		IParams req = (IParams)input;
		if (GENERAL_NAME.equals(group.getName())) {
			return updateGeneral(group, req);
		
		} else if (USER_NAME.equals(group.getName())) {
			return updateUser(group, req);
			
		} else if (ADMINISTRATOR_NAME.equals(group.getName())) {
			return updateAdministrator(group, req);
			
		} else if (SYSTEM_NAME.equals(group.getName())) {
			return updateSystem(group, req);
			
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Setting group update not implements");
		}
	}
	
	private static final String GLOBAL_NAME = "global";
	private static final String GENERAL_NAME = "general";
	private static final String USER_NAME = "user";
	private static final String SYSTEM_NAME = "system";
	private static final String ADMINISTRATOR_NAME = "administrator";
	private static final String FRIENDLYNAME_NAME = "friendlyname";
	private static final String SENDUSAGE_NAME = "sendusage";
	private static final String LOGGERDEBUG_NAME = "loggerdebug";
	private static final String SYSTEMFLAG_NAME = "systemflag";
	private static final String SYSTEMNOTICE_NAME = "systemnotice";
	private static final String RESERVEDLIST_NAME = "reservedlist";
	private static final String ADMINLIST_NAME = "adminlist";
	
	private SettingGroup initGeneral(SettingCategory category) throws ErrorException { 
		SettingGroup group = category.createGroup(GENERAL_NAME, this);
		group.setTitle("General");
		
		SettingText friendlyName = new SettingText(category.getManager(), FRIENDLYNAME_NAME) { 
				public String getValue() { return getFriendlyName(); }
				public void setValue(String value) { setFriendlyName(value); }
			};
		friendlyName.setTitle("Friendly Name");
		friendlyName.setDescription("a friendly name to identify your application.");
		
		SettingCheckbox sendusage = new SettingCheckbox(category.getManager(), SENDUSAGE_NAME) { 
				public boolean isChecked() { return isSendUsage(); }
				public void setChecked(boolean b) { setSendUsage(b); }
			};
		sendusage.setValue("true");
		sendusage.setTitle("Send anonymous usage data to us");
		sendusage.setDescription("This helps us improve your experience.");
		
		SettingCheckbox loggerDebug = new SettingCheckbox(category.getManager(), LOGGERDEBUG_NAME) { 
				public boolean isChecked() { return isLoggerDebug(); }
				public void setChecked(boolean b) { setLoggerDebug(b); }
			};
		loggerDebug.setValue("true");
		loggerDebug.setTitle("Output debug logs");
		loggerDebug.setDescription("If set true, logger will output full debug information to log files.");
		
		group.addSetting(friendlyName);
		group.addSetting(sendusage);
		group.addSetting(loggerDebug);
		
		return group;
	}
	
	private boolean updateGeneral(SettingGroup group, IParams req) 
			throws ErrorException { 
		if (group == null || req == null) throw new NullPointerException();
		
		String input_friendlyName = req.getParam(FRIENDLYNAME_NAME);
		String input_sendusage = req.getParam(SENDUSAGE_NAME, "false");
		String input_loggerDebug = req.getParam(LOGGERDEBUG_NAME, "false");
		
		boolean changed = false;
		
		if (input_friendlyName != null) { 
			input_friendlyName = StringUtils.trim(input_friendlyName);
			if (input_friendlyName.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Friendly name cannot be empty");
			}
			
			String firendlyName = getFriendlyName();
			if (!input_friendlyName.equals(firendlyName)) { 
				setFriendlyName(input_friendlyName);
				changed = true;
				
				Setting setting = group.getSetting(FRIENDLYNAME_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateGeneral: setting 'friendlyname' changed to '" 
							+ input_friendlyName + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Friendly name"), input_friendlyName));
			}
		}
		
		if (input_sendusage != null) { 
			boolean b_sendusage = input_sendusage.equalsIgnoreCase("true");
			if (b_sendusage != isSendUsage()) { 
				setSendUsage(b_sendusage);
				changed = true;
				
				Setting setting = group.getSetting(SENDUSAGE_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateGeneral: setting 'sendusage' changed to '" 
							+ b_sendusage + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Send usage"), Boolean.toString(b_sendusage)));
			}
		}
		
		if (input_loggerDebug != null) { 
			boolean b_loggerDebug = input_loggerDebug.equalsIgnoreCase("true");
			if (b_loggerDebug != isLoggerDebug()) { 
				setLoggerDebug(b_loggerDebug);
				changed = true;
				
				Setting setting = group.getSetting(LOGGERDEBUG_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateGeneral: setting 'loggerdebug' changed to '" 
							+ b_loggerDebug + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Logger debug"), Boolean.toString(b_loggerDebug)));
			}
		}
		
		return changed;
	}
	
	private SettingGroup initUser(SettingCategory category) throws ErrorException { 
		SettingGroup group = category.createGroup(USER_NAME, this);
		group.setTitle("User");
		
		SettingText reservedList = new SettingText(category.getManager(), RESERVEDLIST_NAME) { 
				public String getValue() { return getReservedList(); }
				public void setValue(String value) { setReservedList(value); }
			};
		reservedList.setTitle("Reserved Name List");
		reservedList.setDescription("a list of user names reserved by system.");
		
		group.addSetting(reservedList);
		
		return group;
	}
	
	private boolean updateUser(SettingGroup group, IParams req) 
			throws ErrorException { 
		if (group == null || req == null) throw new NullPointerException();
		
		String input_reservedList = req.getParam(RESERVEDLIST_NAME);
		
		boolean changed = false;
		
		if (input_reservedList != null) { 
			input_reservedList = StringUtils.trim(input_reservedList);
			//if (input_reservedList.length() == 0) { 
			//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			//			"Reserved name list cannot be empty");
			//}
			
			String reservedList = getReservedList();
			if (!input_reservedList.equals(reservedList)) { 
				setReservedList(input_reservedList);
				changed = true;
				
				Setting setting = group.getSetting(RESERVEDLIST_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateUser: setting 'reservedlist' changed to '" 
							+ input_reservedList + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Reserved names"), input_reservedList));
			}
		}
		
		return changed;
	}
	
	private SettingGroup initAdministrator(SettingCategory category) throws ErrorException { 
		SettingGroup group = category.createGroup(ADMINISTRATOR_NAME, this);
		group.setTitle("Administrator");
		
		SettingText adminList = new SettingText(category.getManager(), ADMINLIST_NAME) { 
				public String getValue() { return getAdminList(); }
				public void setValue(String value) { setAdminList(value); }
			};
		adminList.setTitle("Administrator List");
		adminList.setDescription("a list of administrator user names seperated by space.");
		
		group.addSetting(adminList);
		
		return group;
	}
	
	private boolean updateAdministrator(SettingGroup group, IParams req) 
			throws ErrorException { 
		if (group == null || req == null) throw new NullPointerException();
		
		String input_adminList = req.getParam(ADMINLIST_NAME);
		
		boolean changed = false;
		
		if (input_adminList != null) { 
			input_adminList = StringUtils.trim(input_adminList);
			//if (input_adminList.length() == 0) { 
			//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			//			"Administrator list cannot be empty");
			//}
			
			String adminList = getAdminList();
			if (!input_adminList.equals(adminList)) { 
				setAdminList(input_adminList);
				changed = true;
				
				Setting setting = group.getSetting(ADMINLIST_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateAdministrator: setting 'adminlist' changed to '" 
							+ input_adminList + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("Admin names"), input_adminList));
			}
		}
		
		return changed;
	}
	
	private SettingGroup initSystem(SettingCategory category) throws ErrorException { 
		SettingGroup group = category.createGroup(SYSTEM_NAME, this);
		group.setTitle("System");
		
		SettingText systemNotice = new SettingText(category.getManager(), SYSTEMNOTICE_NAME) { 
				public String getValue() { return getSystemNotice(); }
				public void setValue(String value) { setSystemNotice(value); }
			};
		systemNotice.setTitle("System Notice");
		systemNotice.setDescription("system notice sent to all users.");
		
		SettingSelect systemflag = new SettingSelect(category.getManager(), SYSTEMFLAG_NAME) { 
				public String getValue() { return getSystemFlagAsString(); }
				public void setValue(String value) { setSystemFlag(value); }
			};
		systemflag.setTitle("System Flag");
		systemflag.setDescription("enable or disable user's operations.");
		
		if (systemflag != null) {
			systemflag.addOption(stringOfSystemFlag(SystemFlag.ENABLED), "Enabled");
			systemflag.addOption(stringOfSystemFlag(SystemFlag.REGISTER_DISABLED), "Register Disabled");
			systemflag.addOption(stringOfSystemFlag(SystemFlag.LOGINREGISTER_DISABLED), "Login and Register Disabled");
			systemflag.addOption(stringOfSystemFlag(SystemFlag.ALL_DISABLED), "All Operations Disabled");
			
			if (mSystemFlag == null)
				mSystemFlag = SystemFlag.ENABLED;
			
			//systemflag.setSorter(new Comparator<SettingSelect.Option>() {
			//		@Override
			//		public int compare(SettingSelect.Option o1, SettingSelect.Option o2) {
			//			String value1 = o1 != null ? o1.getValue() : null;
			//			String value2 = o2 != null ? o2.getValue() : null;
			//			
			//			if (value1 == null || value2 == null) { 
			//				if (value1 == null && value2 == null) return 0;
			//				if (value1 == null) return -1;
			//				return 1;
			//			} else
			//				return value1.compareTo(value2);
			//		}
			//	});
		}
		
		group.addSetting(systemNotice);
		group.addSetting(systemflag);
		
		return group;
	}
	
	private boolean updateSystem(SettingGroup group, IParams req) 
			throws ErrorException { 
		if (group == null || req == null) throw new NullPointerException();
		
		String input_systemNotice = req.getParam(SYSTEMNOTICE_NAME);
		String input_systemFlag = req.getParam(SYSTEMFLAG_NAME);
		
		boolean changed = false;
		
		if (input_systemNotice != null) { 
			input_systemNotice = StringUtils.trim(input_systemNotice);
			//if (input_systemNotice.length() == 0) { 
			//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			//			"System notice cannot be empty");
			//}
			
			String systemNotice = getSystemNotice();
			if (!input_systemNotice.equals(systemNotice)) { 
				setSystemNotice(input_systemNotice);
				changed = true;
				
				Setting setting = group.getSetting(SYSTEMNOTICE_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateUser: setting 'systemnotice' changed to '" 
							+ input_systemNotice + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("System Notice"), input_systemNotice));
			}
		}
		
		if (input_systemFlag != null) { 
			if (input_systemFlag.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"System flag cannot be empty");
			}
			
			SystemFlag systemFlag = getSystemFlag();
			SystemFlag input_flag = valueOfSystemFlag(input_systemFlag);
			
			if (input_flag != null && input_flag != systemFlag) { 
				setSystemFlag(input_flag);
				changed = true;
				
				Setting setting = group.getSetting(SYSTEMFLAG_NAME);
				if (setting != null)
					setting.setUpdateTime();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateUser: setting 'systemflag' changed to '" 
							+ input_systemFlag + "'");
				}
				
				//Notification.getSystemNotifier().addNotification(
				//		Strings.format("Setting \"%1$s\" changed to \"%2$s\"", 
				//				Strings.get("System Flag"), input_systemFlag));
			}
		}
		
		return changed;
	}
	
}
