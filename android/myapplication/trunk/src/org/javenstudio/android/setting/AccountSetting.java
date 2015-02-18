package org.javenstudio.android.setting;

import android.app.Activity;
import android.content.Intent;

import org.javenstudio.android.account.AccountHelper;
import org.javenstudio.android.account.AccountHelper.OnRemoveListener;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;

public abstract class AccountSetting extends AccountSettingBase 
		implements AccountHelper.OnRemoveListener {

	public AccountSetting(SettingCategory category) {
		super(category); 
		
		String screenKey = category.getKey() + ".account"; 
		
		int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_account);
		if (iconRes == 0) iconRes = R.drawable.ic_setting_account_selector;
		
		setKey(screenKey); 
		setTitle(R.string.setting_account_title);
		setIcon(iconRes); 
		
		setIntent(createIntent(screenKey));
	}
	
	protected abstract Intent createIntent(String screenKey);
	
	@Override
	protected Setting createAddSetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".add", 
				R.string.setting_account_add_label);
		
		setting.setIcon(R.drawable.ic_setting_addaccount_selector);
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					onAddAccount();
					return true;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createAccountSetting(SettingCategory category, AccountData user) {
		if (category == null || user == null) return null;
		
		final AccountItemSetting setting = new AccountItemSetting(
				category.getSettingManager(), getAccountApp(), user) {
				@Override
				protected OnRemoveListener getRemoveListener() {
					return AccountSetting.this;
				}
			};
		
		setting.setKey(category.getKey() + "." + user.getMailAddress()); 
		setting.setTitle(user.getFullName());
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting st) {
					if (setting == st) setting.onAccountClick();
					return true;
				}
			});
		
		return setting;
	}
	
	@Override
	public void onAccountRemoved(AccountData account, boolean success) {
		Activity activity = getActivity();
		if (activity != null && !activity.isDestroyed() && activity instanceof SettingActivityImpl) {
			SettingActivityImpl settingActivity = (SettingActivityImpl)activity;
			resetInitializer();
			settingActivity.setContentFragment();
		}
	}
	
}
