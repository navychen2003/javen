package org.javenstudio.provider.app.anybox.user;

import android.app.Activity;

import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.host.HostDialogHelper;
import org.javenstudio.provider.account.host.HostListClusterItem;
import org.javenstudio.provider.account.host.HostListItem;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.util.StringUtils;

public abstract class AnyboxHostHelper extends HostDialogHelper {
	private static final Logger LOG = Logger.getLogger(AnyboxHostHelper.class);

	public AnyboxHostHelper() {}

	@Override
	public void actionRemove(Activity activity, HostListItem item) {
		if (activity == null || item == null) return;
		
		if (item instanceof HostListClusterItem) {
			HostListClusterItem clusterItem = (HostListClusterItem)item;
			HostData data = clusterItem.getData();
			
			try {
				if (data != null) {
					ContentHelper.getInstance().deleteHost(data.getId());
					refreshDataSets();
				}
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("actionRemove: data=" + data + " error=" + e, e);
			}
		}
	}
	
	@Override
	protected boolean onHostUpdate(Activity activity, 
			String domain, String address, int port) {
		if (activity == null) return false;
		if (domain == null || domain.length() == 0) return false;
		if (address == null || address.length() == 0) return false;
		
		long current = System.currentTimeMillis();
		
		HostData hostUpdate = ContentHelper.getInstance().queryHostByAddress(
				domain, address, true);
		if (hostUpdate == null) {
			hostUpdate = ContentHelper.getInstance().newHost();
			hostUpdate.setCreateTime(current);
		} else {
			hostUpdate = hostUpdate.startUpdate();
		}
		
		hostUpdate.setPrefix(AnyboxApp.PREFIX);
		hostUpdate.setClusterDomain(domain);
		hostUpdate.setHostAddr(address);
		hostUpdate.setHttpPort(port);
		hostUpdate.setFlag(HostData.FLAG_OK);
		hostUpdate.setStatus(HostData.STATUS_OK);
		hostUpdate.setFailedCode(0);
		hostUpdate.setFailedMessage("");
		hostUpdate.setUpdateTime(current);
		hostUpdate.commitUpdates();
		
		return true;
	}
	
	@Override
	protected void onHostFound(String[] hostInfo) {
		if (hostInfo == null || hostInfo.length < 6) return;
		
		try {
			String id = StringUtils.trim(hostInfo[1]);
			String domain = StringUtils.trim(hostInfo[2]);
			String address = StringUtils.trim(hostInfo[3]);
			int httpPort = Integer.parseInt(hostInfo[4]);
			int httpsPort = Integer.parseInt(hostInfo[5]);
			
			if (address == null || address.length() == 0)
				return;
			
			if (domain == null || domain.length() == 0)
				domain = address;
			
			long current = System.currentTimeMillis();
			
			HostData hostUpdate = ContentHelper.getInstance().queryHostByAddress(
					domain, address, true);
			if (hostUpdate == null) {
				hostUpdate = ContentHelper.getInstance().newHost();
				hostUpdate.setCreateTime(current);
			} else {
				hostUpdate = hostUpdate.startUpdate();
			}
			
			hostUpdate.setPrefix(AnyboxApp.PREFIX);
			hostUpdate.setClusterId(id);
			hostUpdate.setClusterDomain(domain);
			hostUpdate.setHostAddr(address);
			hostUpdate.setHttpPort(httpPort);
			hostUpdate.setHttpsPort(httpsPort);
			hostUpdate.setFlag(HostData.FLAG_OK);
			hostUpdate.setStatus(HostData.STATUS_OK);
			hostUpdate.setFailedCode(0);
			hostUpdate.setFailedMessage("");
			hostUpdate.setUpdateTime(current);
			hostUpdate.commitUpdates();
			
			postRefreshDataSets();
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("onHostFound: error: " + e, e);
		}
	}
	
}
