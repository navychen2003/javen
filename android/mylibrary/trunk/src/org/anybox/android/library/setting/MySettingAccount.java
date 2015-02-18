package org.anybox.android.library.setting;

import android.content.Intent;

import org.anybox.android.library.MyApp;
import org.anybox.android.library.R;
import org.anybox.android.library.SettingActivity;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.setting.AccountSetting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public class MySettingAccount extends AccountSetting {

	public MySettingAccount(SettingCategory category) {
		super(category); 
	}

	@Override
	public AccountApp getAccountApp() {
		return MyApp.getInstance().getAccountApp();
	}
	
	@Override
	public int getHomeIconRes() {
		return R.drawable.ic_home_account_dark;
	}
	
	@Override
	protected Intent createIntent(String screenKey) {
		return SettingActivity.createIntent(screenKey); 
	}
	
}
