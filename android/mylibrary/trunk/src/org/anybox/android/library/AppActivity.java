package org.anybox.android.library;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.ServiceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.activity.AccountAppActivity;

public class AppActivity extends AccountAppActivity {
	private static final Logger LOG = Logger.getLogger(AppActivity.class);
	private static final String EXTRA_NAME = "provider.name";

	private static final Map<String,Provider> sProviders = 
			new HashMap<String,Provider>();
	
	public static void actionActivity(Context from, Provider p) { 
		Intent intent = new Intent(from, AppActivity.class); 
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
		
		synchronized (sProviders) {
			if (p != null) { 
				if (LOG.isDebugEnabled()) LOG.debug("actionActivity: provider=" + p);
				final String name = p.getName();
				sProviders.put(name, p);
				
				intent.putExtra(EXTRA_NAME, name);
				from.startActivity(intent); 
			}
		}
	}
	
	@Override
	public MyApp getDataApp() {
		return MyApp.getInstance();
	}
	
	private void initProviderByName(String name) {
		if (name != null) { 
			synchronized (sProviders) {
				Provider p = sProviders.get(name);
				if (p != null) {
					//Provider old = getController().getProvider();
					getController().setProvider(p);
					//if (old != null) old.onDetach(this);
				}
			}
		}
	}
	
	private final Object mProviderLock = new Object();
	private Provider mCurrentProvider = null;
	
	@Override
	public Provider getCurrentProvider() {
		synchronized (mProviderLock) {
			return mCurrentProvider;
		}
	}

	@Override
	public Provider setCurrentProvider(Provider provider) {
		if (LOG.isDebugEnabled()) 
			LOG.debug("setCurrentProvider: provider=" + provider);
		
		synchronized (mProviderLock) {
			Provider old = mCurrentProvider;
			mCurrentProvider = provider;
			return old;
		}
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		final String name = getIntent().getStringExtra(EXTRA_NAME); 
		initProviderByName(name);
		
		super.doOnCreate(savedInstanceState);
		resetContentBackground();
	}
	
	@Override
	public void setContentBackground(Provider p) {
		if (LOG.isDebugEnabled()) LOG.debug("setContentBackground: provider=" + p);
		resetContentBackground0();
		super.setContentBackground(p);
	}
	
	@Override
	public void resetContentBackground() {
		if (LOG.isDebugEnabled()) LOG.debug("resetContentBackground");
		super.resetContentBackground();
		
		setActionBarIcon(R.drawable.ic_home_anybox_dark);
		setHomeAsUpIndicator(R.drawable.ic_ab_back_holo_dark);
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
		setContentProviderOnStart();
	}
	
	@Override
	protected void onStart() {
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
	
	private void setContentProviderOnStart() {
		setContentProvider(getController().getProvider(), false);
	}
	
	@Override
	public void onNetworkAvailableChanged(NetworkHelper helper) {
		super.onNetworkAvailableChanged(helper);
		ServiceHelper.checkServiceStart(this); 
	}
	
}
