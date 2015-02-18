package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.account.AccountDetailsTab;
import org.javenstudio.provider.account.AccountDetailsItem;

final class PicasaAccountDetails extends AccountDetailsTab {

	public PicasaAccountDetails(PicasaAccount account) { 
		super(account, ResourceHelper.getResources().getString(R.string.label_action_about));
		addDetailsItem(new PicasaDetailsProfile(account));
	}
	
	private static class PicasaDetailsProfile extends AccountDetailsItem { 
		public PicasaDetailsProfile(PicasaAccount account) { 
			super(R.string.accountinfo_details_basic_title);
			addNameValue(R.string.details_accountname, account.getAccountName());
		}
	}
	
}
