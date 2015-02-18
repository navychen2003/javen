package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.AccountDetailsTab;
import org.javenstudio.provider.account.AccountDetailsItem;
import org.javenstudio.provider.app.anybox.AnyboxAccountProfile;

public class AnyboxAccountDetails extends AccountDetailsTab {

	public static interface DetailsFactory {
		public AnyboxDetailsBrief createDetailsBrief(AnyboxAccountItem item);
		public AnyboxDetailsBasic createDetailsBasic(AnyboxAccountItem item);
	}
	
	private final AnyboxAccountItem mAccount;
	
	public AnyboxAccountDetails(AnyboxAccountItem account, 
			DetailsFactory factory) { 
		super(account, ResourceHelper.getResources().getString(R.string.label_action_about));
		mAccount = account;
		addDetailsItem(factory.createDetailsBrief(account));
		addDetailsItem(factory.createDetailsBasic(account));
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) {
		if (type == ReloadType.FORCE || mAccount.getUser().getProfile() == null) { 
			AnyboxAccountProfile.getProfile(mAccount.getUser(), callback);
			postUpdateViews();
		}
	}
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) {
		AnyboxAccountProfile profile = mAccount.getUser().getProfile();
		if (profile != null) {
			long requestTime = profile.getRequestTime();
			return AppResources.getInstance().formatRefreshTime(requestTime);
		}
		return null;
	}
	
	public static class AnyboxDetailsBrief extends AccountDetailsItem { 
		public AnyboxDetailsBrief(final AnyboxAccountItem account) {
			super(R.string.accountinfo_details_brief_title);
			
			addNameValue(
				new NameValue(
					ResourceHelper.getResources().getString(R.string.details_brief), null, true) { 
					@Override
					public CharSequence getValue() {
						String value = null;
						AnyboxAccountProfile profile = account.getUser().getProfile();
						if (profile != null) value = profile.getBrief();
						if (value == null) value = "";
						return value;
					}
				});
			
			addNameValue(
				new NameValue(
					ResourceHelper.getResources().getString(R.string.details_introduction), null, true) { 
					@Override
					public CharSequence getValue() {
						String value = null;
						AnyboxAccountProfile profile = account.getUser().getProfile();
						if (profile != null) value = profile.getIntro();
						if (value == null) value = "";
						return value;
					}
				});
			
			addNameValue(
				new NameValue(
					ResourceHelper.getResources().getString(R.string.details_tags), null, true) { 
					@Override
					public CharSequence getValue() {
						String value = null;
						AnyboxAccountProfile profile = account.getUser().getProfile();
						if (profile != null) value = profile.getTags();
						if (value == null) value = "";
						return value;
					}
				});
		}
	}
	
	public static class AnyboxDetailsBasic extends AccountDetailsItem { 
		public AnyboxDetailsBasic(AnyboxAccountItem account) { 
			super(R.string.accountinfo_details_basic_title);
			
			AccountDetailsItem.NameValue itemNick = addNameValue(R.string.details_nickname, account.getNickName());
			AccountDetailsItem.NameValue itemName = addNameValue(R.string.details_accountname, account.getAccountName());
			AccountDetailsItem.NameValue itemHost = addNameValue(R.string.details_hostname, account.getHostDisplayName());
			AccountDetailsItem.NameValue itemEmail = addNameValue(R.string.details_email, account.getUserEmail());
			
			if (itemNick != null) itemNick.setEditable(true);
			if (itemName != null) itemName.setEditable(false);
			if (itemHost != null) itemHost.setEditable(false);
			if (itemEmail != null) itemEmail.setEditable(false);
		}
	}
	
}
