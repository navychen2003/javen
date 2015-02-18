package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IFragment;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.notify.NotifyFactory;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.app.anybox.AnyboxAccount;

public class AnyboxNotifyProvider extends NotifyProvider {
	private static final Logger LOG = Logger.getLogger(AnyboxNotifyProvider.class);
	
	private final AnyboxAccount mUser;;
	
	public AnyboxNotifyProvider(AnyboxAccount user, int nameRes, int iconRes, 
			NotifyFactory factory) { 
		super(ResourceHelper.getResources().getString(nameRes), 
				iconRes, factory);
		mUser = user;
	}
	
	public AnyboxAccount getAccountUser() { return mUser; }
	
	@Override
	public void onDataChanged(AccountUser user, AccountUser.DataType type, 
			AccountUser.DataState state) {
		if (user != getAccountUser() || type == null || state == null)
			return;
		
		if (type == AccountUser.DataType.ACCOUNTINFO || type == AccountUser.DataType.ANNOUNCEMENT) {
			if (state == AccountUser.DataState.UPDATED) {
				updateDataSets(getAccountUser().getAnyboxAccountInfo(), null);
			}
		}
	}
	
	@Override
	public void onMenuOpened(IFragment fragment) {
		if (LOG.isDebugEnabled()) LOG.debug("onMenuOpened: fragment=" + fragment);
		
		AnyboxAccount account = getAccountUser();
		if (account != null) account.onNotifyMenuOpened();
		if (fragment != null) {
			IOptionsMenu optionsMenu = fragment.getOptionsMenu();
			if (optionsMenu != null) 
				optionsMenu.onUpdateOptionsMenu(fragment.getActivity());
		}
	}
	
}
