package org.javenstudio.provider.people.group;

import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.MenuOperations;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class GroupInfoProvider extends ProviderBase {

	private final MenuOperations mOperations;
	
	public GroupInfoProvider(String name, int iconRes) { 
		super(name, iconRes);
		mOperations = new MenuOperations() { 
				@Override
				public IMenuOperation[] getOperations() { 
					return getMenuOperations();
				}
			};
	}

	protected abstract GroupItem getGroupItem();
	
	private IMenuOperation[] getMenuOperations() { 
		IMenuOperation actionOp = null;
		GroupAction action = getSelectedAction();
		if (action != null && action instanceof GroupActionProvider) { 
			GroupActionProvider ap = (GroupActionProvider)action;
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
		GroupAction action = getSelectedAction();
		if (action != null) 
			action.reloadOnThread(callback, type, reloadId);
	}
	
	private GroupAction getSelectedAction() { 
		GroupItem item = getGroupItem();
		if (item != null) { 
			GroupAction action = item.getSelectedAction();
			if (action != null) 
				return action;
		}
		return null;
	}
	
}
