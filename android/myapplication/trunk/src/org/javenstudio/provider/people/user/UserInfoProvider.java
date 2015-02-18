package org.javenstudio.provider.people.user;

import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.MenuOperations;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class UserInfoProvider extends ProviderBase {

	private final MenuOperations mOperations;
	
	public UserInfoProvider(String name, int iconRes) { 
		super(name, iconRes);
		mOperations = new MenuOperations() { 
				@Override
				public IMenuOperation[] getOperations() { 
					return getMenuOperations();
				}
			};
	}

	protected abstract UserItem getUserItem();
	
	private IMenuOperation[] getMenuOperations() { 
		IMenuOperation actionOp = null;
		UserAction action = getSelectedAction();
		if (action != null && action instanceof UserActionProvider) { 
			UserActionProvider ap = (UserActionProvider)action;
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
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		UserAction action = getSelectedAction();
		if (action != null) 
			action.reloadOnThread(callback, type, reloadId);
	}
	
	private UserAction getSelectedAction() { 
		UserItem item = getUserItem();
		if (item != null) { 
			UserAction action = item.getSelectedAction();
			if (action != null) 
				return action;
		}
		return null;
	}
	
}
