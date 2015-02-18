package org.javenstudio.lightning.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.Theme;
import org.javenstudio.falcon.setting.cluster.HostHelper;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.FileUtils;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.NetUtils;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.context.Config;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.util.InputSource;
import org.javenstudio.util.StringUtils;

public class CoreAdminConfig {
	private static final Logger LOG = Logger.getLogger(CoreAdminConfig.class);
	
	//static final String DEFAULT_LIGHTNING_XML = Constants.DEFAULT_LIGHTNING_XML;
	static final String DEFAULT_LIGHTNING_XML_FILENAME = Constants.LIGHTNING_XML_FILENAME;
	static final String SAMPLE_LIGHTNING_XML_FILENAME = Constants.SAMPLE_XML_FILENAME;
	
	static final String DEFAULT_HOST_CONTEXT = Constants.DEFAULT_HOST_CONTEXT;
	static final String DEFAULT_HOST_PORT = Constants.DEFAULT_HOST_PORT;
	
	private final Config mConfig;
	private final Configuration mConf;
	
	private final String mDefaultCoreName;
	//private final boolean mPersistent;
	private final boolean mRequestCache;
	private final boolean mResponseTrace;
	
	private final String mAppDir;
	private final String mHomeDir;
	private final String mLibDir;
	private final String mLocalDir;
	private final String mCloudDir;
	private final String mAdminUser;
	private final ClassLoader mLibLoader;
	
	private final String mClusterId;
	private final String mClusterDomain;
	private final String mClusterSecret;
	private final String mMailDomain;
	private final String mHostDomain;
	private final String mHostAddress;
	private final String mLanAddress;
	private final String mHostName;
	private final String mHostKey;
	private final int mHttpPort;
	private final int mHttpsPort;
	private final int mHostHash;
	//private final String mHostPort;
	//private final String mHostContext;
	//private final String mHost;
	
	private final HostMode mHostMode;
	private final String mJoinAddress;
	//private final String mAttachAddress;
	private final String[] mAttachUsers;
	
	private final String mAdminPath;
	private final String mAdminHandler;
	private final String mManagementPath;
	
	private final ContextLoader mLoader;
	private final List<String> mStoreUris;
	private File mConfigFile = null;
	
	
	public static CoreAdminConfig load(ContextLoader loader, 
			String homeDir, String appDir) throws ErrorException { 
		final String configFileName = DEFAULT_LIGHTNING_XML_FILENAME;
		final String sampleFileName = SAMPLE_LIGHTNING_XML_FILENAME;
		//final String configDefault = DEFAULT_LIGHTNING_XML;
		
		final File configFile = new File(homeDir, configFileName);
		File fconf = configFile;
		
		if (!fconf.exists())
			fconf = new File(homeDir, sampleFileName);
		
		if (LOG.isInfoEnabled())
			LOG.info("Looking for config file: " + fconf.getAbsolutePath());
		
		final CoreAdminConfig config;
		
		if (fconf.exists()) { 
			config = CoreAdminConfig.loadConfig(fconf, homeDir, appDir, loader);
			
		} else { 
			if (LOG.isErrorEnabled())
				LOG.error("load: config file: " + fconf + " not found");
			
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Lightning config file: " + fconf + " not found");
			
			//if (LOG.isInfoEnabled())
			//	LOG.info("no " + configFileName + " file found - use default");
			
			//try {
			//	fconf = null;
				
			//	config = CoreAdminConfig.loadConfig("<default>", 
			//			new ByteArrayInputStream(configDefault.getBytes("UTF-8")), 
			//			homeDir, loader);
				
			//} catch (UnsupportedEncodingException e) { 
			//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			//}
		}
		
		config.mConfigFile = configFile;
		
		if (!configFile.exists()) {
			if (LOG.isInfoEnabled())
				LOG.info("Save config file: " + configFile);
			try {
				FileWriter writer = new FileWriter(configFile);
				config.getConfig().writeConfig(writer);
				writer.flush();
				writer.close();
			} catch (Throwable e) {
				if (e instanceof ErrorException) 
					throw (ErrorException)e;
				else
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
		
		return config;
	}
	
	private static CoreAdminConfig loadConfig(File fconf, 
			String homeDir, String appDir, ContextLoader loader) 
			throws ErrorException { 
		try {
			return loadConfig(fconf.getName(), new FileInputStream(fconf), 
					homeDir, appDir, loader);
		} catch (FileNotFoundException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"config file not found: " + fconf, e);
		}
	}
	
	private static CoreAdminConfig loadConfig(String sourceName, 
			InputStream source, String homeDir, String appDir, ContextLoader loader) 
			throws ErrorException { 
		if (homeDir == null)
			homeDir = loader.getInstanceDir();
		if (appDir == null)
			appDir = homeDir;
		
		if (LOG.isInfoEnabled()) {
			LOG.info("Loading config using Home: " + homeDir 
					+ " appHome: " + appDir);
		}
		
		return new CoreAdminConfig(
				loader, homeDir, appDir, sourceName, source); 
	}
	
	private static String createHostKey(String hostname, String domain, 
			String hostaddr, String lanaddr, int httpport, int httpsport) throws ErrorException {
		String hostAddress = domain; //getHostDomain();
		if (hostAddress == null || hostAddress.length() == 0)
			hostAddress = hostaddr; //getHostAddress();
		if (hostAddress == null || hostAddress.length() == 0 || 
			hostAddress.equalsIgnoreCase("127.0.0.1") || 
			hostAddress.equalsIgnoreCase("localhost")) {
			hostAddress = hostname; //getHostName();
		}
		
		return UserHelper.newHostKey("" + hostname + "/" + domain + "/" 
				+ lanaddr + "/" + hostaddr + ":" + httpport + ":" + httpsport);
	}
	
	private CoreAdminConfig(final ContextLoader loader, String homeDir, 
			String appDir, String sourceName, InputStream source) 
			throws ErrorException { 
		mConfig = (Config)loader.openResource(sourceName, source);
		mStoreUris = new ArrayList<String>();
		mLoader = loader;
		mHomeDir = homeDir;
		mAppDir = appDir;
		
		final Config config = mConfig;
		
		String dcoreName = config.get("*/cores/@defaultCoreName", null);
	    if (dcoreName != null && !dcoreName.isEmpty()) 
	    	mDefaultCoreName = dcoreName;
	    else
	    	mDefaultCoreName = null;
	    
	    //mPersistent = config.getBool("*/settings/@persistent", false);
	    mRequestCache = config.getBool("*/settings/@requestcache", false);
	    mResponseTrace = config.getBool("*/settings/@responsetrace", false);
	    
	    mLibDir = config.get("*/settings/@sharedLib", null);
	    mLocalDir = config.get("*/settings/store/@localDir", null);
	    mCloudDir = config.get("*/settings/store/@cloudDir", "/user");
	    
	    mClusterId = config.get("*/settings/cluster/@clusterId", Long.toString(System.currentTimeMillis()));
	    mClusterDomain = config.get("*/settings/cluster/@clusterDomain", null);
	    mClusterSecret = config.get("*/settings/cluster/@clusterSecret", null);
	    mMailDomain = config.get("*/settings/cluster/@mailDomain", null);
	    
	    mLanAddress = NetUtils.getNetworkAddress();
	    mHostDomain = config.get("*/settings/host/@hostDomain", null);
	    mHostName = config.get("*/settings/host/@hostName", FsUtils.getFriendlyName());
	    mHostAddress = config.get("*/settings/host/@hostAddress", mLanAddress);
	    mHttpPort = config.getInt("*/settings/host/@httpPort", 80);
	    mHttpsPort = config.getInt("*/settings/host/@httpsPort", 443);
	    mAdminUser = config.get("*/settings/administrator/@user", null);
	    
	    mHostKey = config.get("*/settings/host/@hostKey", createHostKey(mHostName, mHostDomain, mLanAddress, mHostAddress, mHttpPort, mHttpsPort));
	    mHostHash = config.getInt("*/settings/host/@hostHash", UserHelper.createHostHash(mHostKey));
	    
	    mJoinAddress = config.get("*/settings/cloud/@joinAddress", null);
	    //mAttachAddress = config.get("*/settings/cloud/@attachAddress", null);
	    //mAttachUsers = StringUtils.splitToken(config.get("*/settings/cloud/@attachUsers", null), " \t\r\n,;");
	    
	    //mHostPort = config.get("*/cores/@hostPort", DEFAULT_HOST_PORT);
	    //mHostContext = config.get("*/cores/@hostContext", DEFAULT_HOST_CONTEXT);
	    //mHost = config.get("*/cores/@host", null);
	    
	    mHostMode = HostHelper.parseMode(config.get("*/settings/cloud/@nodeMode", null));
	    
	    if (mLocalDir == null || mLocalDir.length() == 0) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			"Local store dir (/settings/store) not configured");
	    }
	    
	    if (mCloudDir == null || mCloudDir.length() == 0) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			"Cloud store dir (/settings/store) not configured");
	    }
	    
	    if (mAdminUser == null || mAdminUser.length() == 0) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			"Administrator user (/settings/administrator) not configured");
	    }
	    
	    //if (mClusterDomain == null || mClusterDomain.length() == 0) { 
	    //	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    //			"Cluster domain (/settings/cluster) not configured");
	    //}
	    
	    if (mHostAddress == null || mHostAddress.length() == 0) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			"Host address (/settings/cluster) not configured");
	    }
	    
	    File lzDir = new File(homeDir, "localizations");
	    if (lzDir.exists() && lzDir.isDirectory()) { 
	    	if (LOG.isInfoEnabled())
	        	LOG.info("loading localizations: " + lzDir.getAbsolutePath());
	        
	        Strings.addJsonDir(lzDir.getAbsolutePath());
	    }
	    
	    File applzDir = new File(appDir, "localizations");
	    if (applzDir.exists() && applzDir.isDirectory()) { 
	    	if (LOG.isInfoEnabled())
	        	LOG.info("loading localizations: " + applzDir.getAbsolutePath());
	        
	        Strings.addJsonDir(applzDir.getAbsolutePath());
	    }
	    
	    ArrayList<String> locDirs = new ArrayList<String>();
	    String envDir = System.getProperty("localizations.dir");
	    if (envDir != null && envDir.length() > 0)
	    	locDirs.add(envDir);
	    
	    Iterator<ContextNode> locIter = config.getNodes("*/settings/localizations");
	    if (locIter != null) { 
	    	while (locIter.hasNext()) { 
	    		ContextNode node = locIter.next();
	    		String name = node.getNodeName();
	    		String value = StringUtils.trim(node.getAttribute("settingDir"));
	    		
	    		if (LOG.isDebugEnabled())
	    			LOG.debug("settings: " + name + " settingDir: " + value);
	    		
	    		if (value != null && value.length() > 0)
	    			locDirs.add(value);
	    	}
	    }
	    
	    for (String locDir : locDirs) { 
	    	File file = FileUtils.resolvePath(new File(homeDir), locDir);
	        if (file.exists() && file.isDirectory()) {
		        if (LOG.isInfoEnabled())
		        	LOG.info("loading localizations: " + file.getAbsolutePath());
		        
		        Strings.addJsonDir(file.getAbsolutePath());
	        }
	    }
	    
	    if (mLibDir != null) {
	        File file = FileUtils.resolvePath(new File(homeDir), mLibDir);
	        
	        if (LOG.isInfoEnabled())
	        	LOG.info("loading shared library: " + file.getAbsolutePath());
	        
	        mLibLoader = ContextLoader.createClassLoader(file, null);
	    } else
	    	mLibLoader = null;
		
		mAdminPath = config.get("*/cores/@adminPath", "/admin/cores");
		mAdminHandler  = config.get("*/cores/@adminHandler", null);
	    mManagementPath  = config.get("*/cores/@managementPath", null);
	    
		try { 
			final String resourceName = "configuration.xml";
			InputStream stream = loader.openResourceAsStream(resourceName);
			if (stream == null) 
				throw new FileNotFoundException(resourceName + " not found");
			stream.close();
			
			mConf = ConfigurationFactory.create(
					new InputSource() {
						@Override
						public InputStream openStream() throws IOException {
							return loader.openResourceAsStream(resourceName);
						}
					}, 
					!LOG.isDebugEnabled());
		} catch (Throwable e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	    
		String storeUri = config.get("*/settings/store/@storeUri", null);
		if (storeUri != null) { 
			StringTokenizer st = new StringTokenizer(storeUri, " \t\r\n,;");
			while (st.hasMoreTokens()) { 
				String token = st.nextToken();
				if (token != null && token.startsWith("dfs://") && token.length() > 6)
					addStoreUri(token);
			}
		}
		
		String[] attachFiles = StringUtils.splitToken(mConf.get("cluster.attachuser.files"), " \t\r\n,;");
		String[] attachUsers = StringUtils.splitToken(config.get("*/settings/cloud/@attachUsers", null), " \t\r\n,;");
		Set<String> attachSet = new TreeSet<String>();
		if (attachUsers != null) {
			for (String username : attachUsers) {
				if (username == null || username.length() == 0)
					continue;
				attachSet.add(username.toLowerCase());
			}
		}
		if (attachFiles != null) {
			for (String attachFile : attachFiles) {
				if (attachFile == null || attachFile.length() == 0)
					continue;
				
				try { 
					InputStream is = loader.openResourceAsStream(attachFile);
					if (is != null) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
						String line = null;
						
						while ((line = reader.readLine()) != null) {
							attachUsers = StringUtils.splitToken(line, " \t\r\n,;");
							
							if (attachUsers != null) {
								for (String username : attachUsers) {
									if (username == null || username.length() == 0)
										continue;
									attachSet.add(username.toLowerCase());
								}
							}
						}
						
						is.close();
					}
				} catch (Throwable e) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				}
			}
		}
		mAttachUsers = attachSet.toArray(new String[attachSet.size()]);
		
		loadThemes();
		
	    if (LOG.isInfoEnabled()) {
	    	LOG.info("Config: hostMode=" + mHostMode 
	    			+ " hostDomain=" + mHostDomain + " hostAddress=" + mHostAddress 
	    			+ " httpPort=" + mHttpPort + " httpsPort=" + mHttpsPort 
	    			+ " clusterId=" + mClusterId + " clusterDomain=" + mClusterDomain 
	    			+ " hostName=" + mHostName + " adminPath=" + mAdminPath);
	    }
	}
	
	public final Config getConfig() { return mConfig; }
	public final Configuration getConf() { return mConf; }
	public final ContextLoader getLoader() { return mLoader; }
	public final File getConfigFile() { return mConfigFile; }
	
	public final String getAdminPath() { return mAdminPath; }
	public final String getAdminHandler() { return mAdminHandler; }
	public final String getManagementPath() { return mManagementPath; }
	public final String getAdminUser() { return mAdminUser; }
	
	public final String getDefaultCoreName() { return mDefaultCoreName; }
	public final String getAppDir() { return mAppDir; }
	public final String getHomeDir() { return mHomeDir; }
	public final String getLibDir() { return mLibDir; }
	
	//public final String getStoreDir() { return mStoreDir; }
	//public final String getHostPort() { return mHostPort; }
	//public final String getHostContext() { return mHostContext; }
	//public final String getHost() { return mHost; }
	
	public final String getClusterId() { return mClusterId; }
	public final String getClusterDomain() { return mClusterDomain; }
	public final String getClusterSecret() { return mClusterSecret; }
	public final String getMailDomain() { return mMailDomain; }
	
	public final String getHostDomain() { return mHostDomain; }
	public final String getHostAddress() { return mHostAddress; }
	public final String getHostName() { return mHostName; }
	public final String getLanAddress() { return mLanAddress; }
	public final int getHttpPort() { return mHttpPort; }
	public final int getHttpsPort() { return mHttpsPort; }
	
	public final String getHostKey() { return mHostKey; }
	public final int getHostHash() { return mHostHash; }
	
	public final HostMode getHostMode() { return mHostMode; }
	public final String getJoinAddress() { return mJoinAddress; }
	//public final String getAttachAddress() { return mAttachAddress; }
	public final String[] getAttachUsers() { return mAttachUsers; }
	
	public final ClassLoader getLibLoader() { return mLibLoader; }
	//public final boolean isPersistent() { return mPersistent; }
	public final boolean isRequestCache() { return mRequestCache; }
	public final boolean isResponseTrace() { return mResponseTrace; }
	
	public boolean hasAttachUser(String username) {
		if (username == null || username.length() == 0)
			return false;
		
		String[] users = getAttachUsers();
		if (users != null) {
			for (String user : users) {
				if (user == null) continue;
				if (user.equals(username) || username.startsWith(user+"@")) 
					return true;
			}
		}
		
		return false;
	}
	
	public Iterator<ContextNode> getServiceNodes(String nodeName) 
			throws ErrorException { 
		return getConfig().getNodes("*/services/" + nodeName);
	}
	
	public Iterator<ContextNode> getCoreNodes(String nodeName) 
			throws ErrorException { 
		return getConfig().getNodes("*/cores/" + nodeName);
	}
	
	public String toCanonicalDir(String dir) { 
		if (dir == null || dir.length() == 0) 
			return dir;
		
		String base = (new Path(getHomeDir())).toString();
		String path = (new Path(dir)).toString();
		
		if (path.startsWith(base))
			path = path.substring(base.length());
		
		return path;
	}
	
	public String getCloudStoreDir() { 
		return mCloudDir;
	}
	
	public String getLocalStoreDir() { 
		String dataDir = mLocalDir;
		if (new File(dataDir).isAbsolute()) 
			return dataDir;
		
		return ContextLoader.normalizeDir(getHomeDir() +
				ContextLoader.normalizeDir(dataDir));
	}
	
	public String[] getStoreUris() { 
		synchronized (mStoreUris) { 
			return mStoreUris.toArray(new String[mStoreUris.size()]);
		}
	}
	
	public void addStoreUri(String uri) { 
		if (uri == null || uri.length() == 0)
			return;
		
		synchronized (mStoreUris) { 
			uri = FsUtils.normalizeUri(uri);
			
			for (String u : mStoreUris) { 
				if (uri.equals(u)) return;
			}
			mStoreUris.add(uri);
			
			if (LOG.isDebugEnabled())
				LOG.debug("addStoreUri: uri=" + uri);
		}
	}
	
	@SuppressWarnings("unused")
	private String getDefaultStoreUri() { 
		synchronized (mStoreUris) { 
			if (mStoreUris.size() > 0)
				return mStoreUris.get(0);
			return null;
		}
	}
	
	public Configuration createConf(String name) throws ErrorException { 
		final ContextLoader loader = getLoader();
		Configuration conf0 = getConf();
		
		Configuration conf = new Configuration(conf0);
		if (name != null && name.length() > 0) { 
			final String resourceName = "configuration-" + name + ".xml";
			InputStream stream = null;
			try {
				stream = loader.openResourceAsStream(resourceName);
				if (stream != null) { 
					stream.close();
					
					conf.addResource(new InputSource() {
						@Override
						public InputStream openStream() throws IOException {
							return loader.openResourceAsStream(resourceName);
						}
					});
				} else { 
					if (LOG.isDebugEnabled())
						LOG.debug("createConf: " + resourceName + " not found");
				}
			} catch (FileNotFoundException e) { 
				if (LOG.isDebugEnabled())
					LOG.debug("createConf: " + resourceName + " not found: " + e, e);
				
			} catch (IOException e) {
				//if (LOG.isDebugEnabled())
				//	LOG.debug("createConf: " + resourceName + " not found or error: " + e, e);
				
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (Throwable ignore) { 
				}
			}
		}
		
		return conf;
	}
	
	@SuppressWarnings("resource")
	private void loadThemes() throws ErrorException { 
		final ContextLoader loader = getLoader();
		final String resourceName = "themes.json";
		InputStream stream = null;
		try {
			stream = loader.openResourceAsStream(resourceName);
			if (stream != null) { 
				StringBuilder sbuf = new StringBuilder();
				String line = null;
				
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream, "UTF-8"));
				
				while ((line = reader.readLine()) != null) { 
					sbuf.append(line);
					sbuf.append('\n');
				}
				
				JSONObject json = new JSONObject(sbuf.toString());
				JSONArray names = json.names();
				
				for (int i=0; names != null && i < names.length(); i++) { 
					String name = StringUtils.trim(names.getString(i));
					JSONObject value = json.getJSONObject(name);
					
					if (name != null && value != null && name.length() > 0) {
						String title = StringUtils.trim(value.getString("title"));
						boolean def = value.has("default") ? value.getBoolean("default") : false;
						
						if (title != null && title.length() > 0) {
							Theme theme = Theme.addTheme(name, title);
							if (theme != null && def)
								theme.setDefault(true);
							
						} else { 
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
									"Theme: " + name + " has empty title in " + resourceName);
						}
					} else { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"Theme: " + name + " has wrong value: " + value + 
								" in " + resourceName);
					}
				}
			} else { 
				if (LOG.isDebugEnabled())
					LOG.debug("loadThemes: " + resourceName + " not found");
			}
		//} catch (FileNotFoundException e) { 
		//	if (LOG.isDebugEnabled())
		//		LOG.debug("loadThemes: " + resourceName + " not found: " + e, e);
			
		} catch (IOException e) {
			//if (LOG.isDebugEnabled())
			//	LOG.debug("loadThemes: " + resourceName + " error: " + e, e);
			
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (Throwable ignore) { 
			}
		}
	}
	
}
