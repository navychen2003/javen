package org.javenstudio.provider.library.details;

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

public abstract class SectionInfoProvider extends ProviderBase {

	private final AccountApp mApp;
	private final AccountUser mAccount;
	private final SectionInfoBinder mBinder;
	private final MenuOperations mOperations;
	
	public SectionInfoProvider(AccountApp app, AccountUser account, 
			String name, int iconRes) {
		super(name, iconRes);
		mApp = app;
		mAccount = account;
		mBinder = createDetailsBinder();
		mOperations = new MenuOperations() { 
				@Override
				public IMenuOperation[] getOperations() { 
					return getMenuOperations();
				}
			};
	}
	
	public final AccountApp getAccountApp() { return mApp; }
	public final AccountUser getAccountUser() { return mAccount; }
	
	public abstract SectionInfoItem getSectionItem();
	protected abstract SectionInfoBinder createDetailsBinder();

	@Override
	public SectionInfoBinder getBinder() {
		return mBinder;
	}
	
	@Override
	public void setContentBackground(IActivity activity) {
		getBinder().bindBackgroundView(activity);
	}
	
	@Override
	public CharSequence getTitle() {
		String name = getSectionItem().getSectionName();
		if (name != null && name.length() > 0) return name;
		return super.getTitle();
	}
	
	@Override
	public Drawable getBackground(IActivity activity, int width, int height) { 
		SectionInfoItem item = getSectionItem();
		if (item != null) return item.getBackgroundDrawable(width, height);
		return null; 
	}
	
	private IMenuOperation[] getMenuOperations() { 
		IMenuOperation actionOp = null;
		SectionActionTab action = getSelectedAction();
		if (action != null && action instanceof SectionActionProvider) { 
			SectionActionProvider ap = (SectionActionProvider)action;
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
		SectionActionTab action = getSelectedAction();
		if (action != null) return action.getLastUpdatedLabel(activity);
		return null;
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) { 
		SectionActionTab action = getSelectedAction();
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
	
	private SectionActionTab getSelectedAction() { 
		SectionInfoItem item = getSectionItem();
		if (item != null) { 
			SectionActionTab action = item.getSelectedAction();
			if (action != null) 
				return action;
		}
		return null;
	}
	
	public boolean onDownloadClick(IActivity activity) { 
		return getSectionItem().onItemDownload(activity); 
	}
	
}
