package org.javenstudio.provider.app.anybox.user;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.AccountInfoBinder;
import org.javenstudio.provider.account.AccountMenuBinder;
import org.javenstudio.provider.account.AccountSecondaryMenuBinder;
import org.javenstudio.provider.activity.AccountMenuActivity;
import org.javenstudio.provider.app.BaseAccountInfoProvider;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxAccount;

public class AnyboxAccountProvider extends BaseAccountInfoProvider {
	//private static final Logger LOG = Logger.getLogger(AnyboxAccountProvider.class);
	
	private final AnyboxAccountItem mAccount;
	private final AnyboxAccountBinder mBinder;
	private final AnyboxAccountMenuBinder mMenuBinder;
	private final AnyboxAccountSecondaryMenuBinder mSecondaryBinder;
	
	public AnyboxAccountProvider(AnyboxApp app, AnyboxAccount account, 
			int iconRes, int indicatorRes) { 
		super(app, account.getAccountName(), iconRes);
		mAccount = createAccountItem(account);
		mBinder = createAccountBinder();
		mMenuBinder = createAccountMenuBinder();
		mSecondaryBinder = createAccountSecondaryMenuBinder();
		//setOptionsMenu(new AnyboxAccountOptionsMenu(app, account));
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxAccountItem getAccountItem() { return mAccount; }
	
	public AnyboxAccount getAccountUser() { 
		return (AnyboxAccount)getAccountItem().getAccountUser(); 
	}
	
	protected AnyboxAccountItem createAccountItem(AnyboxAccount account) {
		return new AnyboxAccountItem(this, account, new AnyboxAccountItem.AccountFactory() {
				@Override
				public AnyboxAccountDetails.AnyboxDetailsBasic createDetailsBasic(AnyboxAccountItem item) {
					return new AnyboxAccountDetails.AnyboxDetailsBasic(item);
				}
				@Override
				public AnyboxAccountDetails.AnyboxDetailsBrief createDetailsBrief(AnyboxAccountItem item) {
					return new AnyboxAccountDetails.AnyboxDetailsBrief(item);
				}
			});
	}
	
	protected AnyboxAccountBinder createAccountBinder() {
		return new AnyboxAccountBinder(this);
	}
	
	protected AnyboxAccountMenuBinder createAccountMenuBinder() {
		return new AnyboxAccountMenuBinder(this);
	}
	
	protected AnyboxAccountSecondaryMenuBinder createAccountSecondaryMenuBinder() {
		return new AnyboxAccountSecondaryMenuBinder(this);
	}
	
	@Override
	public String getTitle() {
		return getAccountItem().getUserTitle();
	}
	
	@Override
	public AccountInfoBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public AccountMenuBinder getMenuBinder() {
		return mMenuBinder;
	}
	
	@Override
	public AccountSecondaryMenuBinder getSecondaryMenuBinder() {
		return mSecondaryBinder;
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) { 
		super.reloadOnThread(callback, type, reloadId);
		
	}
	
	@Override
	public Drawable getBackground(IActivity activity, int width, int height) { 
		Drawable d = super.getBackground(activity, width, height);
		if (d == null && activity instanceof AccountMenuActivity) {
			AccountMenuActivity menuactivity = (AccountMenuActivity)activity;
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountinfo_background);
			if (backgroundRes != 0) d = menuactivity.getResources().getDrawable(backgroundRes);
		}
		return d;
	}
	
}
