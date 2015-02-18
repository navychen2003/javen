package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.device.HostDeviceType;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.falcon.util.job.Job;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.falcon.util.job.JobSubmit;
import org.javenstudio.mime.Base64Util;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.util.StringUtils;

public abstract class HostHelper {
	private static final Logger LOG = Logger.getLogger(HostHelper.class);

	public static final long HEARTBEAT_TIMEOUT = 1 * 60 * 60 * 1000L;
	
	public abstract HostJob getJob();
	public abstract Configuration getConf();
	public abstract IHostCluster[] getClusters();
	public abstract HostSelf getHostSelf();
	public abstract void addHost(IHostNode node) throws ErrorException;
	public abstract void removeHost(IHostNode node, String reason) throws ErrorException;
	public abstract boolean isClosed();
	
	public abstract void fetchUri(String location, IFetchListener listener);
	public abstract void scanClusters(IScanListener listener) throws ErrorException;
	
	static long getLong(Configuration conf, String name, long def) {
		long val = conf != null ? conf.getLong(name, def) : def;
		return val >= 0 ? val : 0;
	}
	
	static int getInt(Configuration conf, String name, int def) {
		int val = conf != null ? conf.getInt(name, def) : def;
		return val >= 0 ? val : 0;
	}
	
	private static final long HEARTBEAT_INTERVAL_TIME = 1 * 60 * 1000L;
	private long mLastHeartbeatTime = 0;
	
	private HostJob.Task mHeartbeatTask = new HostJob.Task() {
			public String getUser() { return IUser.SYSTEM; }
			public String getMessage() { return "Hosts heartbeat"; }
			
			@Override
			public void process(HostJob job, JobContext jc) throws ErrorException {
				if (LOG.isDebugEnabled()) LOG.debug("process: hosts heartbeat");
				processPong(jc);
			}
			
			@Override
			public void close() {
				if (LOG.isDebugEnabled()) LOG.debug("close");
			}
		};
	
	@SuppressWarnings("unused")
	private synchronized void requestHeartbeat() {
		long now = System.currentTimeMillis();
		if (now - mLastHeartbeatTime >= getLong(getConf(), 
				"cluster.heartbeat.intervaltime", HEARTBEAT_INTERVAL_TIME)) {
			mLastHeartbeatTime = now;
			
			boolean existed = getJob().existJob(mHeartbeatTask);
			if (LOG.isDebugEnabled())
				LOG.debug("requestHeartbeat: task exists: " + existed);
			
			if (existed == false)
				getJob().startJob(mHeartbeatTask);
		}
	}
	
	public synchronized void requestPong(final IHostNode host) {
		if (host == null || !(host instanceof HostNode)) return;
		
		getJob().startJob(new HostJob.Task() {
				public String getUser() { return IUser.SYSTEM; }
				public String getMessage() { return "Host pong"; }
				
				@Override
				public void process(HostJob job, JobContext jc) throws ErrorException {
					if (LOG.isDebugEnabled()) LOG.debug("process: host pong");
					processPong(jc, (HostNode)host);
				}
				
				@Override
				public void close() {
					if (LOG.isDebugEnabled()) LOG.debug("close");
				}
			});
	}
	
	static final long JOIN_INTERVAL_TIME = 5 * 60 * 1000L;
	
	private AnyboxHost.HostJoinData mJoinData = null;
	private AnyboxHost.HostJoinData getHostJoinData() { return mJoinData; }
	
	private void addHostJoinData(AnyboxHost.HostJoinData data) { 
		mJoinData = data; 
		addClusterHosts(data.getCluster()); 
	}
	
	public IHostInfo[] getJoinHosts() {
		AnyboxHost.HostJoinData data = getHostJoinData();
		if (data != null) {
			IHostInfo host = data.getHostSelf();
			if (host != null) return new IHostInfo[]{ host };
		}
		return null;
	}
	
	public void requestJoin(final HostSelf host, final String joinAddress) {
		if (host == null || joinAddress == null) return;
		
		if (joinAddress != null && joinAddress.length() > 0) {
			JobSubmit.submit(new Job<Void>() {
					private Thread mThread = null;
					
					public String getName() { return "HostJoinJob"; }
					public String getUser() { return IUser.SYSTEM; }
					public String getMessage() { return null; }
					public Map<String, String> getStatusMessages() { return null; }
					
					@Override
					public Void run(JobContext jc) {
						mThread = Thread.currentThread();
						boolean interrupted = false;
						long sleepMillis = 0;
						try {
							sleepMillis = 30 * 1000L;
							if (sleepMillis > 0) { 
								if (LOG.isDebugEnabled()) {
									LOG.debug("runHostJoinJob: sleep " + (sleepMillis/1000) 
											+ " secs, thread=" + mThread);
								}
								Thread.sleep(sleepMillis);
							}
							processJoin(jc, host, joinAddress);
							sleepMillis = host.getJoinSleepMillis(getConf());
							if (sleepMillis > 0) { 
								if (LOG.isDebugEnabled()) {
									LOG.debug("runHostJoinJob: sleep " + (sleepMillis/1000) 
											+ " secs, thread=" + mThread);
								}
								Thread.sleep(sleepMillis);
							}
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("runHostJoinJob: error: " + e, e);
							if (e instanceof InterruptedException)
								interrupted = true;
						}
						if (!jc.isCancelled() && !interrupted)
							requestJoin(host, joinAddress);
						mThread = null;
						return null;
					}
					
					@Override
					public void onCancel() {
						if (LOG.isDebugEnabled())
							LOG.debug("onCancel: job=" + this);
						
						Thread thread = mThread;
						if (thread != null) thread.interrupt();
					}
					
					@Override
					public String toString() {
						return getName() + "{thread=" + mThread + "}";
					}
				});
		}
	}
	
	static final long ATTACH_INTERVAL_TIME = 5 * 60 * 1000L;
	
	private final Map<String,AnyboxHost.HostAttachData> mAttachMap = 
			new HashMap<String,AnyboxHost.HostAttachData>();
	
	private void addHostAttachData(AnyboxHost.HostAttachData data) {
		if (data == null) return;
		synchronized (mAttachMap) {
			mAttachMap.put(data.getHostSelf().getHostKey(), data);
		}
		addClusterHosts(data.getCluster());
		addAttachHosts(data.getAttachHosts());
	}
	
	private AnyboxHost.HostAttachData getHostAttachData(String hostkey) {
		if (hostkey == null) return null;
		synchronized (mAttachMap) {
			return mAttachMap.get(hostkey);
		}
	}
	
	public IHostInfo[] getAttachHosts() {
		ArrayList<IHostInfo> hosts = new ArrayList<IHostInfo>();
		synchronized (mAttachMap) {
			for (AnyboxHost.HostAttachData attach : mAttachMap.values()) {
				if (attach != null) {
					IHostInfo host = attach.getHostSelf();
					if (host != null) hosts.add(host);
				}
			}
		}
		return hosts.toArray(new IHostInfo[hosts.size()]);
	}
	
	private void addClusterHosts(AnyboxHost.HostClusterData cluster) {
		if (cluster == null) return;
		
		AnyboxHost.HostNodeData[] hosts = cluster.getHosts();
		addAttachHosts(hosts);
	}
	
	private void addAttachHosts(AnyboxHost.HostNodeData[] hosts) {
		if (hosts != null) {
			for (AnyboxHost.HostNodeData node : hosts) {
				if (node == null) continue;
				try {
					addHost(createHostNode(node));
				} catch (Throwable e) {
					if (LOG.isErrorEnabled())
						LOG.error("addClusterHosts: add host: " + node + " error: " + e, e);
				}
			}
		}
	}
	
	public IHostNode createHostNode(IHostInfo node) {
		if (node == null) return null;
		return new HostNodeInfo(node);
	}
	
	private long mAttachAddressChangedTime = 0;
	private final Map<String,String[]> mAttachAddressMap = 
			new HashMap<String,String[]>();
			
	public final void addAttachAddress(String address, String[] users) {
		if (address == null || address.length() == 0)
			return;
		
		synchronized (mAttachAddressMap) {
			if (users == null || users.length == 0) {
				mAttachAddressMap.remove(address);
				return;
			}
			
			mAttachAddressMap.put(address, users);
			mAttachAddressChangedTime = System.currentTimeMillis();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addAttachAddress: address=" + address 
						+ " users=" + Arrays.toString(users));
			}
		}
	}
	
	public final void requestAttachHosts(final HostSelf host) {
		if (host == null) return;
		
		ArrayList<AttachAddressUsers> list = new ArrayList<AttachAddressUsers>();
		
		synchronized (mAttachAddressMap) {
			for (Map.Entry<String, String[]> entry : mAttachAddressMap.entrySet()) {
				String address = entry.getKey();
				String[] users = entry.getValue();
				
				if (address == null || address.length() == 0 || users == null || users.length == 0)
					continue;
				
				list.add(new AttachAddressUsers(address, users));
			}
		}
		
		requestAttach(host, list.toArray(new AttachAddressUsers[list.size()]));
	}
	
	static class AttachAddressUsers {
		public final String address;
		public final String[] users;
		
		public AttachAddressUsers(String address, String[] users) {
			if (address == null || address.length() == 0 || users == null || users.length == 0)
				throw new NullPointerException();
			this.address = address;
			this.users = users;
		}
	}
	
	private void requestAttach(final HostSelf host, final AttachAddressUsers[] attachUsers) {
		if (host == null || attachUsers == null) 
			return;
		
		if (attachUsers != null && attachUsers.length > 0) {
			JobSubmit.submit(new Job<Void>() {
					private Thread mThread = null;
					
					public String getName() { return "HostAttachJob"; }
					public String getUser() { return IUser.SYSTEM; }
					public String getMessage() { return null; }
					public Map<String, String> getStatusMessages() { return null; }
					
					@Override
					public Void run(JobContext jc) {
						mThread = Thread.currentThread();
						boolean interrupted = false;
						long sleepMillis = 0;
						try {
							sleepMillis = 30 * 1000L;
							if (sleepMillis > 0) { 
								if (LOG.isDebugEnabled()) {
									LOG.debug("runHostAttachJob: sleep " + (sleepMillis/1000) 
											+ " secs, thread=" + mThread);
								}
								Thread.sleep(sleepMillis);
							}
							boolean changed = processAttachs(jc, host, attachUsers);
							sleepMillis = host.getAttachSleepMillis(getConf());
							if (sleepMillis > 0 && !changed) { 
								if (LOG.isDebugEnabled()) {
									LOG.debug("runHostAttachJob: sleep " + (sleepMillis/1000) 
											+ " secs, thread=" + mThread);
								}
								Thread.sleep(sleepMillis);
							}
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("runHostAttachJob: error: " + e, e);
							if (e instanceof InterruptedException)
								interrupted = true;
						}
						if (!jc.isCancelled() && !interrupted)
							requestAttachHosts(host);
						mThread = null;
						return null;
					}
					
					@Override
					public void onCancel() {
						if (LOG.isDebugEnabled())
							LOG.debug("onCancel: job=" + this);
						
						Thread thread = mThread;
						if (thread != null) thread.interrupt();
					}
					
					@Override
					public String toString() {
						return getName() + "{thread=" + mThread + "}";
					}
				});
		}
	}
	
	private void processPong(JobContext jc) {
		if (jc == null) return;
		
		IHostCluster[] clusters = getClusters();
		if (clusters != null) { 
			for (IHostCluster cluster : clusters) { 
				if (cluster == null) continue;
				if (jc.isCancelled() || isClosed()) break;
				
				IHostNode[] hosts = cluster.getHosts();
				if (hosts != null) {
					for (IHostNode host : hosts) {
						if (host != null && host instanceof HostNode)
							processPong(jc, (HostNode)host);
					}
				}
			}
		}
	}
	
	private void processPong(JobContext jc, final HostNode host) {
		if (jc == null || host == null) return;
		if (jc.isCancelled() || isClosed()) return;
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String action = "get";
		
		if (host.getHostMode() == HostMode.JOIN || host.getHostMode() == HostMode.NAMED)
			action = "joinget";
		else if (host.getHostMode() == HostMode.ATTACH)
			action = "attachget";
		
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=" + action + "&wt=secretjson"
				+ "&secret.apptype=" + encodeSecret(HostDeviceType.DEV_HOST) 
				+ "&secret.appkey=" + encodeSecret(getHostSelf().getHostKey())
				+ "&secret.idkey=" + encodeSecret(host.getClusterId()+"/"+host.getHostKey());
		
		if (host.getHostMode() == HostMode.ATTACH) {
			IAttachUser[] users = ((StorageNode)host).getAttachUsers(getHostSelf().getHostKey());
			if (users != null) {
				for (IAttachUser user : users) {
					if (user == null) continue;
					url += "&secret.attachuser=" + encodeSecret(user.getUserKey() + "/" 
							+ user.getUserName() + "/" + user.getUserEmail());
				}
			}
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("processPong: requestURL=" + url);
		
		AnyboxListener.PongListener listener = new AnyboxListener.PongListener();
		try {
			fetchUri(url, listener);
			
			ActionError error = listener.mError;
			AnyboxHost.HostNodeData node = null;
			
			if (error == null || error.getCode() == 0) {
				error = null;
				AnyboxHost.HostGetData hosts = AnyboxHost.loadGetData(listener.mData);
				host.setHeartbeatData(hosts);
				if (hosts != null) 
					node = hosts.getHost(host.getClusterId(), host.getHostKey());
			}
			
			if (node != null) {
				host.setStatusCode(HostNode.STATUS_OK);
				scanClusters(new IScanListener() {
						@Override
						public void onHostFound(HostCluster cluster, IHostNode h) {
							if (h == null || cluster == null || h == host) return;
							if (host.getClusterId().equals(h.getClusterId()) || 
								host.getHostKey().equals(h.getHostKey())) { 
								return;
							}
							if (host.getHostAddress().equals(h.getHostAddress()) && 
							   (host.getHttpPort() == h.getHttpPort() || host.getHttpsPort() == h.getHttpsPort())) {
								if (LOG.isInfoEnabled()) 
									LOG.info("processPong: remove dupicated host: " + h);
								try {
									cluster.removeHost(h, "remove dupicated host");
								} catch (Throwable e) {
									if (LOG.isErrorEnabled())
										LOG.error("processPong: remove host error: " + e, e);
								}
							}
						}
					});
			} else {
				host.setStatusCode(HostNode.STATUS_WRONG);
				removeHost(host, "status wrong");
			}
			
			if (LOG.isInfoEnabled()) {
				LOG.info("processPong: hostAddress=" + hostAddress 
						+ " hostNode=" + node);
			}
		} catch (Throwable e) {
			host.setStatusCode(HostNode.STATUS_ERROR);
			host.setHeartbeatTime(System.currentTimeMillis());
			
			if (LOG.isWarnEnabled()) {
				LOG.warn("processPong: hostAddress=" + hostAddress 
						+ " error: " + e, e);
			}
		}
	}
	
	private void processJoin(JobContext jc, HostSelf hostSelf, 
			String joinAddress) {
		if (hostSelf == null || joinAddress == null || joinAddress.length() == 0)
			return;
		
		int pingTimes = hostSelf.increatePingTimes();
		int pingFailed = hostSelf.getPingFailed();
		
		String url = "http://" + joinAddress + "/lightning/user/cluster?action=join&wt=secretjson"
				+ "&secret.apptype=" + encodeSecret(HostDeviceType.DEV_HOST) 
				+ "&secret.appkey=" + encodeSecret(getHostSelf().getHostKey())
				+ "&secret.clusterid=" + encodeSecret(hostSelf.getClusterId())
				+ "&secret.clusterdomain=" + encodeSecret(hostSelf.getClusterDomain())
				+ "&secret.clustersecret=" + encodeSecret(hostSelf.getClusterSecret())
				+ "&secret.maildomain=" + encodeSecret(hostSelf.getMailDomain())
				+ "&secret.mode=" + encodeSecret(hostSelf.getHostMode().toString())
				+ "&secret.key=" + encodeSecret(hostSelf.getHostKey())
				+ "&secret.domain=" + encodeSecret(hostSelf.getHostDomain())
				+ "&secret.hostaddr=" + encodeSecret(hostSelf.getHostAddress())
				+ "&secret.hostname=" + encodeSecret(hostSelf.getHostName())
				+ "&secret.lanaddr=" + encodeSecret(hostSelf.getLanAddress())
				+ "&secret.admin=" + encodeSecret(hostSelf.getAdminUser())
				+ "&httpport=" + hostSelf.getHttpPort()
				+ "&httpsport=" + hostSelf.getHttpsPort()
				+ "&hostcount=" + hostSelf.getHostCount()
				+ "&pingtimes=" + pingTimes
				+ "&pingfailed=" + pingFailed;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("processJoin: pingTimes=" + pingTimes 
					+ " pingFailed=" + pingFailed + " requestURL=" + url);
		}
		
		AnyboxListener.JoinListener listener = new AnyboxListener.JoinListener();
		ActionError error = null;
		AnyboxHost.HostNodeData node = null;
		boolean success = false;
		
		try {
			fetchUri(url, listener);
			
			error = listener.mError;
			if (error == null || error.getCode() == 0) {
				error = null;
				AnyboxHost.HostJoinData joinData = AnyboxHost.loadJoinData(listener.mData);
				addHostJoinData(joinData);
				
				node = joinData != null ? joinData.getHost() : null;
				if (node != null) {
					String clusterId = node.getClusterId();
					String hostKey = node.getHostKey();
					long heartbeat = node.getHeartbeatTime();
					int status = node.getStatusCode();
					
					if (clusterId != null && clusterId.equals(hostSelf.getClusterId()) && 
						hostKey != null && hostKey.equals(hostSelf.getHostKey())) {
						if (status == IHostNode.STATUS_OK) hostSelf.resetPingFailed();
						else hostSelf.increatePingFailed();
						
						hostSelf.setHeartbeatTime(heartbeat);
						hostSelf.setStatusCode(status);
						success = true;
						
						if (LOG.isDebugEnabled()) {
							LOG.debug("processJoin: joinAddress=" + joinAddress 
									+ " join done: " + node);
						}
					}
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isWarnEnabled()) {
				LOG.warn("processJoin: joinAddress=" + joinAddress 
						+ " error: " + e, e);
			}
		}
		
		if (!success) {
			hostSelf.setStatusCode(IHostNode.STATUS_JOINERROR);
			hostSelf.increatePingFailed();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("processJoin: joinAddress=" + joinAddress 
						+ " join error: " + node);
			}
		}
	}
	
	private boolean processAttachs(JobContext jc, HostSelf hostSelf, 
			final AttachAddressUsers[] attachUsers) {
		if (hostSelf == null || attachUsers == null || attachUsers.length == 0)
			return false;
		
		long changedTime1 = mAttachAddressChangedTime;
		
		for (AttachAddressUsers addrUsers : attachUsers) {
			if (addrUsers == null) continue;
			if (jc.isCancelled()) return false;
			
			processAttach(jc, hostSelf, addrUsers.address, addrUsers.users);
		}
		
		long changedTime2 = mAttachAddressChangedTime;
		return changedTime2 > changedTime1;
	}
	
	private boolean processAttach(JobContext jc, HostSelf hostSelf, 
			String attachAddress, String[] attachUsers) {
		if (hostSelf == null || attachAddress == null || attachAddress.length() == 0 || 
			attachUsers == null || attachUsers.length == 0)
			return false;
		
		int pingTimes = hostSelf.increatePingTimes();
		int pingFailed = hostSelf.getPingFailed();
		
		String url = "http://" + attachAddress + "/lightning/user/cluster?action=attach&wt=secretjson"
				+ "&secret.apptype=" + encodeSecret(HostDeviceType.DEV_HOST) 
				+ "&secret.appkey=" + encodeSecret(getHostSelf().getHostKey())
				+ "&secret.clusterid=" + encodeSecret(hostSelf.getClusterId())
				+ "&secret.clusterdomain=" + encodeSecret(hostSelf.getClusterDomain())
				+ "&secret.clustersecret=" + encodeSecret(hostSelf.getClusterSecret())
				+ "&secret.maildomain=" + encodeSecret(hostSelf.getMailDomain())
				+ "&secret.mode=" + encodeSecret(hostSelf.getHostMode().toString())
				+ "&secret.key=" + encodeSecret(hostSelf.getHostKey())
				+ "&secret.domain=" + encodeSecret(hostSelf.getHostDomain())
				+ "&secret.hostaddr=" + encodeSecret(hostSelf.getHostAddress())
				+ "&secret.hostname=" + encodeSecret(hostSelf.getHostName())
				+ "&secret.lanaddr=" + encodeSecret(hostSelf.getLanAddress())
				+ "&secret.admin=" + encodeSecret(hostSelf.getAdminUser())
				+ "&httpport=" + hostSelf.getHttpPort()
				+ "&httpsport=" + hostSelf.getHttpsPort()
				+ "&hostcount=" + hostSelf.getHostCount()
				+ "&pingtimes=" + pingTimes
				+ "&pingfailed=" + pingFailed;
		
		Set<String> userSet = new HashSet<String>();
		for (String attachUser : attachUsers) {
			if (attachUser == null || attachUser.length() == 0)
				continue;
			
			url += "&secret.attachuser=" + encodeSecret(attachUser);
			userSet.add(attachUser);
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("processAttach: pingTimes=" + pingTimes 
					+ " pingFailed=" + pingFailed + " requestURL=" + url);
		}
		
		AnyboxListener.AttachListener listener = new AnyboxListener.AttachListener();
		ActionError error = null;
		AnyboxHost.HostNodeData node = null;
		boolean success = false;
		boolean attachNext = false;
		
		try {
			fetchUri(url, listener);
			
			error = listener.mError;
			if (error == null || error.getCode() == 0) {
				error = null;
				AnyboxHost.HostAttachData attachData = AnyboxHost.loadAttachData(listener.mData);
				addHostAttachData(attachData);
				
				node = attachData != null ? attachData.getHost() : null;
				if (node != null) {
					String clusterId = node.getClusterId();
					String hostKey = node.getHostKey();
					long heartbeat = node.getHeartbeatTime();
					int status = node.getStatusCode();
					
					if (clusterId != null && clusterId.equals(hostSelf.getClusterId()) && 
						hostKey != null && hostKey.equals(hostSelf.getHostKey())) {
						if (status == IHostNode.STATUS_OK) hostSelf.resetPingFailed();
						else hostSelf.increatePingFailed();
						
						hostSelf.setHeartbeatTime(heartbeat);
						hostSelf.setStatusCode(status);
						success = true;
						
						if (LOG.isDebugEnabled()) {
							LOG.debug("processAttach: attachAddress=" + attachAddress 
									+ " join done: " + node);
						}
					}
				}
				
				AnyboxHost.HostNodeData attachHost = attachData != null ? attachData.getHostSelf() : null;
				AnyboxHost.HostNodeData[] attachHosts = attachData != null ? attachData.getAttachHosts() : null;
				
				if (attachHosts != null) {
					for (AnyboxHost.HostNodeData hostData : attachHosts) {
						if (hostData == null) continue;
						
						String address = hostData.getHostDomain();
						if (address == null || address.length() == 0) 
							address = hostData.getHostAddress();
						if (address == null || address.length() == 0) 
							continue;
						
						int port = hostData.getHttpPort();
						if (port > 0 && port != 80) address = address + ":" + port;
						
						String[] users = StringUtils.splitToken(hostData.getAttachUserNames(null, 0), " \t\r\n,;");
						if (users != null && users.length > 0) {
							ArrayList<String> userlist = new ArrayList<String>();
							for (String user : users) {
								if (user != null && user.length() > 0 && userSet.contains(user))
									userlist.add(user);
							}
							users = userlist.toArray(new String[userlist.size()]);
						}
						
						if (users != null && users.length > 0) {
							addAttachAddress(address, users);
							
							if (attachHost != null) {
								String hostkey = attachHost.getHostKey();
								if (hostkey != null && hostkey.length() > 0 && 
									hostkey.equals(hostData.getHostKey())) {
									attachNext = true;
									
									if (LOG.isDebugEnabled()) {
										LOG.debug("processAttach: attachAddress=" + attachAddress 
												+ " attachHost=" + hostData);
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isWarnEnabled()) {
				LOG.warn("processAttach: attachAddress=" + attachAddress 
						+ " error: " + e, e);
			}
		}
		
		if (!success) {
			hostSelf.setStatusCode(IHostNode.STATUS_ATTACHERROR);
			hostSelf.increatePingFailed();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("processAttach: attachAddress=" + attachAddress 
						+ " join error: " + node);
			}
		}
		
		return attachNext;
	}
	
	public IHostInfo getAttachHost(String attachHostKey) 
			throws ErrorException {
		if (getHostSelf().getHostMode() != HostMode.ATTACH) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host not in " + HostMode.ATTACH + " mode");
		}
		
		if (attachHostKey == null || attachHostKey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host key is empty");
		}
		
		AnyboxHost.HostAttachData attachData = getHostAttachData(attachHostKey);
		if (attachData == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host: " + attachHostKey + " not requested yet");
		}
		
		AnyboxHost.HostNodeData hostServer = attachData.getHostSelf();
		if (hostServer == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host server is null");
		}
		
		if (attachHostKey != null && !attachHostKey.equals(hostServer.getHostKey())) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host server key: " + attachHostKey + " is wrong");
		}
		
		return hostServer;
	}
	
	public IAuthInfo attachAuth(IParams req) throws ErrorException {
		if (req == null) throw new NullPointerException();
		
		final String[] tokens = UserHelper.getUserKeyTokens(req);
		if (tokens == null || tokens.length != 3) {
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Attach user auth token is wrong");
		}
		
		final String hostKey = tokens[0];
		final String userKey = tokens[1];
		final String token = tokens[2];
		
		if (hostKey == null || !hostKey.equals(getHostSelf().getHostKey())) {
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Unauthorized Access: wrong host");
		}
		
		final IUser user = UserHelper.getLocalUserByKey(userKey);
		if (user == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User not found");
		}
		
		if (!(user instanceof IMember)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + user.getUserName() + " is a group");
		}
		
		final String attachHostKey = user.getPreference().getAttachHostKey();
		final String attachUserKey = user.getPreference().getAttachUserKey();
		
		if (attachHostKey == null || attachHostKey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host key is empty");
		}
		
		if (attachUserKey == null || attachUserKey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach user key is empty");
		}
		
		final IHostInfo attachHost = getAttachHost(attachHostKey);
		if (attachHost == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach host key: " + attachHostKey + " not found");
		}
		
		final String authtoken = attachHostKey + attachUserKey + token;
		
		String hostAddress = attachHost.getHostAddress() + ":" + attachHost.getHttpPort();
		String action = "check";
		
		String url = "http://" + hostAddress + "/lightning/user/login?action=" + action + "&wt=secretjson"
				+ "&secret.token=" + encodeSecret(authtoken);
		
		if (LOG.isDebugEnabled())
			LOG.debug("attachAuth: requestURL=" + url);
		
		AnyboxListener.AuthListener listener = new AnyboxListener.AuthListener();
		AnyboxAuth.AuthData data = null;
		try {
			fetchUri(url, listener);
			
			ActionError error = listener.mError;
			if (error == null || error.getCode() == 0) {
				error = null;
				data = AnyboxAuth.loadAuth((IMember)user, attachHost, listener.mData);
				if (data != null) {
					if (!attachUserKey.equals(data.getAttachUser().getUserKey()) || 
						!token.equals(data.getAttachUser().getToken())) {
						if (LOG.isWarnEnabled()) {
							LOG.warn("attachAuth: user: " + user.getUserName() 
									+ " auth failed with wrong token: " + token);
						}
						data = null;
					}
				}
			}
			
			if (LOG.isInfoEnabled()) {
				LOG.info("attachAuth: hostAddress=" + hostAddress 
						+ " attachHost=" + attachHost + " data=" + data);
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("attachAuth: hostAddress=" + hostAddress 
						+ " error: " + e, e);
			}
		}
		
		return data;
	}
	
	public static String encode(String str) {
		if (str == null) return "";
		return StringUtils.URLEncode(str, "UTF-8");
	}
	
	public static String encodeSecret(String str) {
		if (str == null) return "";
		try {
			return StringUtils.URLEncode(Base64Util.encodeSecret(str), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	public static HostMode parseMode(String mode) {
		HostMode hostmode = HostMode.HOST;
		if (mode != null && mode.length() > 0) {
			if (mode.equalsIgnoreCase(HostMode.ATTACH.toString()))
				hostmode = HostMode.ATTACH;
			else if (mode.equalsIgnoreCase(HostMode.BACKUP.toString()))
				hostmode = HostMode.BACKUP;
			else if (mode.equalsIgnoreCase(HostMode.JOIN.toString()))
				hostmode = HostMode.JOIN;
			else if (mode.equalsIgnoreCase(HostMode.NAMED.toString()))
				hostmode = HostMode.NAMED;
			else if (mode.equalsIgnoreCase(HostMode.HOST.toString()))
				hostmode = HostMode.HOST;
			else
				throw new IllegalArgumentException("Unknown host mode: " + mode);
		}
		return hostmode;
	}
	
}
