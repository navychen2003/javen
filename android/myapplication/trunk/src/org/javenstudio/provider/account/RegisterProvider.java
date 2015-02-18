package org.javenstudio.provider.account;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;

public class RegisterProvider extends ProviderBase implements ProviderBinder {
	private static final Logger LOG = Logger.getLogger(RegisterProvider.class);
	
	public RegisterProvider(String name, int iconRes) { 
		super(name, iconRes);
	}

	@Override
	public ProviderBinder getBinder() {
		return this;
	}

	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
	}
	
	@Override
	public View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		return null;
	}
	
	public boolean addGoogleAccount(IActivity activity) { 
		if (activity == null) return false;
		
		try {
			PackageManager manager = activity.getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo("com.google.android.gsf", 0);
			if (info == null) { 
				activity.getActivityHelper().showWarningMessage(R.string.google_services_framework_not_installed);
				return false;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("addGoogleAccount: GSF packgeInfo: " + info);
			
		} catch (PackageManager.NameNotFoundException e) { 
			activity.getActivityHelper().showWarningMessage(R.string.google_services_framework_not_installed);
			return false;
		}
		
		Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
		intent.putExtra("account_types", new String[] {"com.google"});
		activity.getActivity().startActivity(intent);
		
		return true;
	}
	
}
