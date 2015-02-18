package org.javenstudio.android.account;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.common.util.Logger;

public class AccountHelper {
	private static final Logger LOG = Logger.getLogger(AccountHelper.class);

	public static void onAccountLogin(final Activity activity, 
			final AccountApp app, final AccountData account) {
		if (LOG.isDebugEnabled()) LOG.debug("onAccountLogin: account=" + account);
    	if (activity == null || activity.isDestroyed()) return;
    	if (app == null || account == null) return;
    	
    	AppResources.getInstance().startLoginActivity(activity, 
    			AccountApp.LoginAction.SELECT_ACCOUNT, account.getMailAddress());
	}
	
	public static void onAccountSwitch(final Activity activity, 
			final AccountApp app, final AccountData account) {
		if (LOG.isDebugEnabled()) LOG.debug("onAccountSwitch: account=" + account);
    	if (activity == null || activity.isDestroyed()) return;
    	if (app == null || account == null) return;
    	
    	int titleRes = AppResources.getInstance().getStringRes(AppResources.string.switchaccount_confirm_title);
		if (titleRes == 0) titleRes = R.string.switchaccount_confirm_title;
		
		int messageRes = AppResources.getInstance().getStringRes(AppResources.string.switchaccount_confirm_message);
		if (messageRes == 0) messageRes = R.string.switchaccount_confirm_message;
		
		String message = activity.getResources().getString(messageRes);
		message = String.format(message, account.getFullName());
		
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
						app.logout(activity, AccountApp.LoginAction.SWITCH_ACCOUNT, 
								account.getMailAddress());
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
    
	public static interface OnRemoveListener {
		public void onAccountRemoved(AccountData account, boolean success);
	}
	
    public static void onAccountRemove(final Activity activity, 
			final AccountApp app, final AccountData account, final OnRemoveListener listener) {
    	if (LOG.isDebugEnabled()) LOG.debug("onAccountRemove: account=" + account);
    	if (activity == null || activity.isDestroyed()) return;
    	if (app == null || account == null || listener == null) return;
    	
    	int titleRes = AppResources.getInstance().getStringRes(AppResources.string.deleteaccount_confirm_title);
		if (titleRes == 0) titleRes = R.string.deleteaccount_confirm_title;
		
		int messageRes = AppResources.getInstance().getStringRes(AppResources.string.deleteaccount_confirm_message);
		if (messageRes == 0) messageRes = R.string.deleteaccount_confirm_message;
		
		String message = activity.getString(messageRes);
		message = String.format(message, account.getFullName());
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_error_warning))
			.setTitle(titleRes)
			.setMessage(message)
			.setCancelable(true);
	
		builder.setNegativeButton(R.string.dialog_yes_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						try {
							ContentHelper.getInstance().deleteAccount(account.getId());
							app.onAccountRemoved(account, true);
							if (listener != null) 
								listener.onAccountRemoved(account, true);
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("onAccountRemove: deleteAccount: " + account + " error=" +e, e);
							if (listener != null) 
								listener.onAccountRemoved(account, false);
						}
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
	
	public static boolean checkPassword(String password) {
		if (password == null || password.length() == 0)
			return false;
		
		if (password.length() < 6) 
			return false;
		
		return true;
	}
	
	public static boolean checkEmailAddress(String name) {
		if (name == null || name.length() < 3)
			return false;
		
		if (name.indexOf('@') >= 0) {
			Matcher matcher = EMAIL_PATTERN.matcher(name);
			return matcher.matches();
		}
		
		return false;
	}
	
	public static boolean checkUserName(String name) {
		if (name == null || name.length() < 3)
			return false;
		
		@SuppressWarnings("unused")
		int count1 = 0;
		@SuppressWarnings("unused")
		int count2 = 0;
		int count3 = 0;
		
		for (int i=0; i < name.length(); i++) { 
			char chr = name.charAt(i);
			
			if (chr >= 'a' && chr <= 'z') {
				count1 ++;
				continue;
			}
			
			if (chr >= '0' && chr <= '9') {
				if (i == 0) return false;
				count2 ++;
				continue;
			}
			
			if (chr == '-' || chr == '_' || chr == '.') {
				if (i == 0) return false;
				count3 ++;
				continue;
			}
			
			return false;
		}
		
		if (count3 > 1) return false;
		
		return true;
	}
	
	private static final Pattern EMAIL_PATTERN = 
			Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	
}
