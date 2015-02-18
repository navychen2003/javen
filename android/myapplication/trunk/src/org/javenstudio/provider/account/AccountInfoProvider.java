package org.javenstudio.provider.account;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.MenuOperations;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class AccountInfoProvider extends ProviderBase {

	private final AccountApp mApp;
	private final MenuOperations mOperations;
	
	public AccountInfoProvider(AccountApp app, String name, int iconRes) { 
		super(name, iconRes);
		mApp = app;
		mOperations = new MenuOperations() { 
				@Override
				public IMenuOperation[] getOperations() { 
					return getMenuOperations();
				}
			};
	}

	public final AccountApp getAccountApp() { return mApp; }
	
	public abstract AccountInfoItem getAccountItem();
	public abstract AccountMenuBinder getMenuBinder();
	public abstract AccountSecondaryMenuBinder getSecondaryMenuBinder();
	
	public AccountUser getAccountUser() { return getAccountItem().getAccountUser(); }
	
	public abstract AccountInfoBinder getBinder();
	
	@Override
	public void setContentBackground(IActivity activity) {
		getBinder().bindBackgroundView(activity);
	}
	
	@Override
	public Drawable getBackground(IActivity activity, int width, int height) { 
		AccountInfoItem item = getAccountItem();
		if (item != null) return item.getBackgroundDrawable(width, height);
		return null; 
	}
	
	private IMenuOperation[] getMenuOperations() { 
		IMenuOperation actionOp = null;
		AccountActionTab action = getSelectedAction();
		if (action != null && action instanceof AccountActionProvider) { 
			AccountActionProvider ap = (AccountActionProvider)action;
			Provider p = ap.getProvider();
			if (p != null) 
				actionOp = p.getMenuOperation();
		}
		
		return new IMenuOperation[] { super.getMenuOperation(), actionOp };
	}
	
	@Override
	public IMenuOperation getMenuOperation() { 
		return mOperations; 
	}
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		AccountActionTab action = getSelectedAction();
		if (action != null) return action.getLastUpdatedLabel(activity);
		return null;
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) { 
		AccountActionTab action = getSelectedAction();
		if (action != null) 
			action.reloadOnThread(callback, type, reloadId);
	}
	
	@Override
	public void setOrientation(IActivity activity) {
		activity.getActivityHelper().lockOrientation(Configuration.ORIENTATION_PORTRAIT);
	}
	
	@Override
	public boolean isUnlockOrientationDisabled() { 
		return true; 
	}
	
	private AccountActionTab getSelectedAction() { 
		AccountInfoItem item = getAccountItem();
		if (item != null) { 
			AccountActionTab action = item.getSelectedAction();
			if (action != null) 
				return action;
		}
		return null;
	}
	
}
