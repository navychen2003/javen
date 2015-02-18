package org.javenstudio.android.setting;

import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Logger;

public abstract class AccountSettingBase extends SettingScreenBase {
	private static final Logger LOG = Logger.getLogger(AccountSettingBase.class);

	public AccountSettingBase(SettingCategory category) {
		super(category.getSettingManager(), null); 
		resetInitializer();
	}
	
	public void resetInitializer() {
		setScreenAdapter(null);
		setInitializer(new SettingGroup.GroupInitializer() {
				@Override
				public boolean initialize(SettingGroup group) {
					initSettingScreen(); 
					return true;
				}
			});
	}
	
	@Override
	protected void onInitialized(Activity activity) { 
		super.onInitialized(activity);
		resetInitializer();
	}
	
	protected final void initSettingScreen() { 
		if (LOG.isDebugEnabled()) LOG.debug("initSettingScreen");
		
		removeAll(false);
		
		initAccountListScreen(this); 
		initAccountAddScreen(this); 
		
		addSetting(createSettingCategory(null));
	}
	
	public abstract AccountApp getAccountApp();
	
	protected abstract Setting createAddSetting(SettingCategory category); 
	protected abstract Setting createAccountSetting(SettingCategory category, AccountData user); 
	
	protected void initAccountAddScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".add"); 
		
		category.addSetting(createAddSetting(category));
		
		screen.addSetting(category);
	}
	
	protected void initAccountListScreen(SettingScreen screen) { 
		SettingCategory category = screen.createSettingCategory(
				screen.getKey() + ".list"); 
		
		AccountData[] accounts = getAccountApp().getAccounts();
		if (accounts != null) { 
			Arrays.sort(accounts, new Comparator<AccountData>() {
					@Override
					public int compare(AccountData lhs, AccountData rhs) {
						long ltm = lhs.getUpdateTime();
						long rtm = rhs.getUpdateTime();
						if (ltm > rtm) return -1;
						else if (ltm < rtm) return 1;
						
						String lname = lhs.getUserName();
						String rname = lhs.getUserName();
						if (lname == null || rname == null) {
							if (lname == null) return 1;
							else if (rname == null) return -1;
						}
						return lname.compareTo(rname);
					}
				});
			
			for (AccountData account : accounts) {
				if (account != null) {
					if (LOG.isDebugEnabled()) LOG.debug("initAccountListScreen: add account: " + account);
					category.addSetting(createAccountSetting(category, account));
				}
			}
		}
		
		screen.addSetting(category);
	}
	
	protected void onAddAccount() {
		if (LOG.isDebugEnabled()) LOG.debug("onAddAccount");
		
    	final Activity activity = getActivity();
    	if (activity == null || activity.isDestroyed())
    		return;
    	
    	int titleRes = AppResources.getInstance().getStringRes(AppResources.string.addaccount_confirm_title);
		if (titleRes == 0) titleRes = R.string.addaccount_confirm_title;
		
		int messageRes = AppResources.getInstance().getStringRes(AppResources.string.addaccount_confirm_message);
		if (messageRes == 0) messageRes = R.string.addaccount_confirm_message;
		
		String message = activity.getString(messageRes);
		AccountUser account = getAccountApp().getAccount();
		if (account != null) message = String.format(message, account.getAccountFullname());
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_error))
			.setTitle(titleRes)
			.setMessage(message)
			.setCancelable(true);
	
		builder.setNegativeButton(R.string.dialog_yes_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						getAccountApp().logout(activity, AccountApp.LoginAction.ADD_ACCOUNT, null);
					}
				});
		
		builder.setPositiveButton(R.string.dialog_no_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
	
		builder.show(activity);
	}
	
}
