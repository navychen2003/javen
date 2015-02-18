package org.anybox.android.library;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.ServiceHelper;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderController;
import org.javenstudio.provider.activity.AccountMenuActivity;

public class MainActivity extends AccountMenuActivity {
	private static final Logger LOG = Logger.getLogger(MainActivity.class);

	public static void actionActivity(Context from) { 
		actionActivity(from, Intent.FLAG_ACTIVITY_CLEAR_TOP);
	}
	
	public static void actionActivity(Context from, int flag) { 
		Intent intent = new Intent(from, MainActivity.class); 
		intent.setFlags(flag); 
		
		from.startActivity(intent); 
	}
	
	private static MainActivity sInstance = null;
	public static MainActivity getInstance() { return sInstance; }
	
	@Override
	public MyApp getDataApp() { 
		return MyApp.getInstance();
	}
	
	@Override
	public AccountApp getAccountApp() {
		return MyApp.getInstance().getAccountApp();
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		super.doOnCreate(savedInstanceState);
		setTitle(R.string.app_name);
		resetContentBackground();
	}
	
	@Override
	public void setContentBackground(Provider p) {
		if (LOG.isDebugEnabled()) LOG.debug("setContentBackground");
		resetContentBackground0();
		super.setContentBackground(p);
	}
	
	@Override
	public void resetContentBackground() {
		if (LOG.isDebugEnabled()) LOG.debug("resetContentBackground");
		super.resetContentBackground();
		
		setActionBarIcon(R.drawable.ic_home_anybox_dark);
		setHomeAsUpIndicator(R.drawable.ic_ab_menu_holo_light);
		setActionBarTitleColor(getResources().getColor(R.color.actionbar_title_light));
		
		//setActionBarBackgroundColor(getResources().getColor(R.color.actionbar_background_light));
		//setContentBackgroundColor(getResources().getColor(R.color.content_background_light));
		//setActivityBackgroundColor(getResources().getColor(R.color.content_background_light));
		
		resetContentBackground0();
	}
	
	private void resetContentBackground0() {
		setActionBarBackgroundColor(getResources().getColor(R.color.blue_background_color));
		setContentBackgroundColor(getResources().getColor(R.color.blue_background_color));
		setActivityBackgroundColor(getResources().getColor(R.color.blue_background_color));
	}
	
	@Override
	protected void doOnCreateDone(Bundle savedInstanceState) { 
		super.doOnCreateDone(savedInstanceState);
		sInstance = this;
		setContentProviderOnStart();
	}
	
	@Override
    public void onStart() {
        super.onStart();
        ServiceHelper.checkServiceStart(this); 
        setContentProviderOnStart();
	}
	
	@Override
	protected void onResume() {
        super.onResume();
        setContentProviderOnStart();
        Provider provider = getCurrentProvider();
        if (provider != null) {
        	setContentBackground(provider);
        	provider.setContentBackground(this);
        }
	}
	
	@Override
	protected void onDestroy() {
		getDataApp().getMetricsNotifier().updateNotification();
		sInstance = null;
		super.onDestroy();
	}
	
	private void setContentProviderOnStart() {
		setContentProvider(getController().getProvider(), false);
	}
	
	@Override
	public void onNetworkAvailableChanged(NetworkHelper helper) {
		super.onNetworkAvailableChanged(helper);
		getActivityHelper().showContentMessage(
			(helper.isNetworkAvailable() ? null : getString(R.string.network_unavailable_message)), 0, 
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyResources.startNetworkSetting(MainActivity.this);
				}
			});
		ServiceHelper.checkServiceStart(this); 
	}

	@Override
	public ProviderController getController() {
		return getAccountApp().getNavigationController(this);
	}

	@Override
	public ActionItem[] getNavigationItems() {
		return getAccountApp().getNavigationItems(this);
	}
	
	@Override
	public Provider getCurrentProvider() {
		return getAccountApp().getCurrentProvider(this);
	}
	
	@Override
    public Provider setCurrentProvider(Provider provider) {
    	return getAccountApp().setCurrentProvider(this, provider);
    }
	
}
