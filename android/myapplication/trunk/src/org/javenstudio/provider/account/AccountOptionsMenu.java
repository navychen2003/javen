package org.javenstudio.provider.account;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.activity.AccountMenuActivity;

public abstract class AccountOptionsMenu 
		implements IOptionsMenu, AccountUser.OnDataChangeListener {
	private static final Logger LOG = Logger.getLogger(AccountOptionsMenu.class);

	private Activity mActivity = null;
	private IMenu mMenu = null;
	private IMenuItem mNotifyItem = null;
	
	public abstract AccountApp getAccountApp();
	public AccountUser getAccountUser() { return getAccountApp().getAccount(); }
	
	@Override
	public void onDataChanged(AccountUser user, AccountUser.DataType type, 
			AccountUser.DataState state) {
		if (user == null || user != getAccountUser()) return;
		
		if (type == AccountUser.DataType.ACCOUNTINFO || type == AccountUser.DataType.ANNOUNCEMENT) {
			if (state == AccountUser.DataState.UPDATED) {
				final Activity activity = mActivity;
				if (activity != null) postUpdateOptionsMenu(activity);
			}
		}
	}
	
	private void postUpdateOptionsMenu(final Activity activity) {
		if (activity == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onUpdateOptionsMenu(activity);
				}
			});
	}
	
	@Override
	public boolean hasOptionsMenu(Activity activity) {
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Activity activity, IMenu menu, IMenuInflater inflater) {
		if (LOG.isDebugEnabled()) LOG.debug("onCreateOptionsMenu: activity=" + activity);
		
		inflater.inflate(R.menu.account_menu, menu);
		mNotifyItem = menu.findItem(R.id.account_action_notify);
		mMenu = menu;
		mActivity = activity;
		
		AccountUser user = getAccountUser();
		if (user != null) user.addListener(this);
		
		return true; 
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Activity activity, IMenu menu) { 
		if (LOG.isDebugEnabled()) LOG.debug("onPrepareOptionsMenu: activity=" + activity);
		onUpdateOptionsMenu(activity);
		return true; 
	}
	
	@Override
    public boolean onOptionsItemSelected(Activity activity, IMenuItem item) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onPrepareOptionsMenu: activity=" + activity + " item=" + item);
		
		if (item.getItemId() == R.id.account_action_notify) {
			return onNotifyItemSelected(activity);
		} else if (item.getItemId() == R.id.account_action_settings) {
			return onSettingItemSelected(activity);
		} else if (item.getItemId() == R.id.account_action_logout) {
			return onLogoutItemSelected(activity);
		}
		
		return false;
	}

	@Override
	public boolean removeOptionsMenu(Activity activity) {
		if (activity == null || activity != mActivity) return false;
		if (LOG.isDebugEnabled()) LOG.debug("removeOptionsMenu: activity=" + activity);
		
		AccountUser user = getAccountUser();
		if (user != null) user.removeListener(this);
		
		IMenu menu = mMenu;
		mMenu = null;
		mActivity = null;
		
		if (menu != null) { 
			menu.removeItem(R.id.account_action_notify);
			menu.removeItem(R.id.account_action_settings);
			menu.removeItem(R.id.account_action_logout);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onUpdateOptionsMenu(Activity activity) {
		if (LOG.isDebugEnabled()) LOG.debug("onUpdateOptionsMenu: activity=" + activity);
		
		IMenuItem notifyItem = mNotifyItem;
		if (notifyItem != null) {
			AccountUser account = getAccountUser();
			
			if (account != null && account.hasNotification()) {
				int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_notification_enabled);
				if (iconRes == 0) iconRes = R.drawable.ic_menu_notification_enabled_holo_light;
				notifyItem.setIcon(iconRes);
				
			} else {
				int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_notification);
				if (iconRes == 0) iconRes = R.drawable.ic_menu_notification_holo_light;
				notifyItem.setIcon(iconRes);
			}
			
			return true;
		}
		
		return false;
	}
	
	protected boolean onNotifyItemSelected(Activity activity) {
		if (activity != null && activity instanceof AccountMenuActivity) {
			AccountMenuActivity menuactivity = (AccountMenuActivity)activity;
			menuactivity.showSecondaryMenu();
		}
		return true;
	}
	
	protected boolean onSettingItemSelected(Activity activity) {
		return false;
	}
	
	protected boolean onLogoutItemSelected(Activity activity) {
		return showLogoutConfirm(activity, getAccountApp(), getAccountUser());
	}
	
	public static boolean showLogoutConfirm(final Activity activity, 
			final AccountApp app, final AccountUser user) {
		if (app == null || user == null || activity == null || activity.isDestroyed()) 
			return false;
		
		int titleRes = AppResources.getInstance().getStringRes(AppResources.string.accountmenu_logout_confirm_title);
		if (titleRes == 0) titleRes = R.string.logout_confirm_title;
		
		int messageRes = AppResources.getInstance().getStringRes(AppResources.string.accountmenu_logout_confirm_message);
		if (messageRes == 0) messageRes = R.string.logout_confirm_message;
		
		String message = activity.getString(messageRes);
		message = String.format(message, user.getAccountFullname());
		
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
						app.logout(activity, AccountApp.LoginAction.SELECT_ACCOUNT, null);
					}
				});
		
		builder.setPositiveButton(R.string.dialog_no_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
	
		return builder.show(activity) != null;
	}
	
}
