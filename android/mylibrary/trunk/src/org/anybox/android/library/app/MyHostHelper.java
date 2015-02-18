package org.anybox.android.library.app;

import android.app.Activity;

import org.anybox.android.library.R;
import org.anybox.android.library.RegisterActivity;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.account.host.HostListClusterItem;
import org.javenstudio.provider.account.host.HostListItem;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.user.AnyboxHostHelper;

public class MyHostHelper extends AnyboxHostHelper {

	private final AnyboxApp mApp;
	
	public MyHostHelper(AnyboxApp app) {
		if (app == null) throw new NullPointerException();
		mApp = app;
	}
	
	public AnyboxApp getApp() { return mApp; }

	@Override
	public CharSequence getHostListTitle() {
		return ResourceHelper.getResources().getString(R.string.select_host_title);
	}
	
	@Override
	public CharSequence getHostSearchingTitle() {
		return ResourceHelper.getResources().getString(R.string.select_host_searching_title);
	}

	@Override
	public void actionSelect(Activity activity, HostListItem item) {
		if (activity == null || item == null) return;
		
		if (item instanceof HostListClusterItem) {
			HostListClusterItem clusterItem = (HostListClusterItem)item;
			HostData data = clusterItem.getData();
			
			if (activity != null && activity instanceof RegisterActivity) {
				final RegisterActivity ra = (RegisterActivity)activity;
				
				if (data != null) {
					getApp().setHostAddressPort(data.getRequestAddressPort());
					getApp().setHostDisplayName(data.getDisplayName());
					
					ra.setActivityHostTitle(data.getDisplayName());
					dismissDialog();
				}
			}
		}
	}
	
	@Override
	protected boolean onHostUpdate(Activity activity, 
			final String domain, String address, int port) {
		if (activity == null) return false;
		if (domain == null || domain.length() == 0) return false;
		if (address == null || address.length() == 0) return false;
		
		boolean result = super.onHostUpdate(activity, domain, address, port);
		
		if (result && activity != null && activity instanceof RegisterActivity) {
			final RegisterActivity ra = (RegisterActivity)activity;
			
			String addr = address;
			if (port > 0 && port != 80)
				addr = addr + ":" + port;
			
			getApp().setHostAddressPort(addr);
			getApp().setHostDisplayName(domain);
			
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						ra.setActivityHostTitle(domain);
					}
				});
		}
		
		return result;
	}
	
}
